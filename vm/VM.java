package jbLPC.vm;

import java.lang.Math;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import jbLPC.compiler.Chunk;
import jbLPC.compiler.Compiler;
import jbLPC.compiler.Function;
import jbLPC.compiler.HasArity;
import jbLPC.compiler.ObjectCompiler;
import jbLPC.compiler.OpCode;
import jbLPC.compiler.ScriptCompiler;
import jbLPC.debug.Debugger;
import jbLPC.main.Props;
import jbLPC.main.PropsObserver;
import jbLPC.nativefn.*;

import static jbLPC.compiler.OpCode.*;

public class VM implements PropsObserver {
  //InterpretResult
  public enum InterpretResult {
    INTERPRET_OK,
    INTERPRET_COMPILE_ERROR,
    INTERPRET_RUNTIME_ERROR,
  }

  //Operation
  private enum Operation {
    OPERATION_PLUS,
    OPERATION_SUBTRACT,
    OPERATION_MULT,
    OPERATION_DIVIDE,
    OPERATION_GT,
    OPERATION_LT,
  }

  private Map<String, Object> globals;
  private Stack<Object> vStack; //Value stack
  private Stack<CallFrame> fStack; //CallFrame stack
  private Upvalue openUpvalues; //linked list

  private static final String initString = "init";

  //Cached properties
  private boolean debugMaster;
  private boolean debugPrintProgress;
  private boolean debugTraceExecution;

  //VM()
  public VM() {
    Props.instance().registerObserver(this);

    globals = new HashMap<>();

    defineNativeFn("clock", new NativeClock(this, "Clock", 0));
    defineNativeFn("foo", new NativeFoo(this, "Foo", 3));
    defineNativeFn("print", new NativePrint(this, "Print", 1));
    defineNativeFn("println", new NativePrintLn(this, "PrintLn", -1)); //variadic, 0 or 1 args
    defineNativeFn("load_object", new NativeLoadObject(this, "LoadObject", 1));

    reset(); //vStack, fStack, openUpvalues

    if (debugPrintProgress) Debugger.instance().printProgress("VM initialized.");
  }

  //interpret(String)
  public InterpretResult interpret(String source) {
    Compiler compiler = new ScriptCompiler();
    Function function = compiler.compile("script", source);

    if (function == null)
      return InterpretResult.INTERPRET_COMPILE_ERROR;

    if (debugPrintProgress)
      Debugger.instance().printProgress("Executing....");

    Closure closure = new Closure(function);

    vStack.push(closure);

    call(closure, 0); //pushes new CallFrame on fStack

    return run();
  }

  //run()
  private InterpretResult run() {
    CallFrame frame = fStack.peek();

    //Bytecode dispatch loop.
    for (;;) {
      if (debugTraceExecution)
        Debugger.instance().traceExecution(frame, globals, vStack.toArray());

      byte opCode = readChunkCodeByte(frame);

      switch (opCode) {
        case OP_GET_CONSTANT:
          Object constVal = readChunkConstant(frame);

          vStack.push(constVal);

          break;
        case OP_NIL:   vStack.push(null);  break;
        case OP_TRUE:  vStack.push(true);  break;
        case OP_FALSE: vStack.push(false); break;
        case OP_POP:   vStack.pop();       break;
        case OP_GET_LOCAL:
          short glOffset = readChunkCodeWord(frame);
          Object glValue = vStack.get(frame.base() + glOffset);

          vStack.push(glValue);

          break;
        case OP_SET_LOCAL:
          short slOffset = readChunkCodeWord(frame);
          Object slValue = vStack.peek();

          vStack.set(frame.base() + slOffset, slValue);

          break;
        case OP_GET_GLOBAL:
          String ggKey = readChunkConstantAsString(frame);

          if (!globals.containsKey(ggKey))
            return error("Undefined variable '" + ggKey + "'.");

          Object global = globals.get(ggKey);

          vStack.push(global);

          break;
        case OP_DEFINE_GLOBAL:
          String dgKey = readChunkConstantAsString(frame);
          Object dgValue = vStack.peek();

          globals.put(dgKey, dgValue);

          vStack.pop();

          break;
        case OP_SET_GLOBAL:
          String sgKey = readChunkConstantAsString(frame);

          if (!globals.containsKey(sgKey))
            return error("Undefined variable '" + sgKey + "'.");

          //Peek here, not pop; assignment is an expression,
          //so we leave value vStacked in case the assignment
          //is nested inside a larger expression.
          globals.put(sgKey, vStack.peek());

          break;
        case OP_GET_UPVALUE:
          short guSlot = readChunkCodeWord(frame);
          Upvalue guUpvalue = frame.closure().upvalues()[guSlot];

          if (guUpvalue.location() != -1) //i.e., open
            vStack.push(vStack.get(guUpvalue.location()));
          else //i.e., closed
           vStack.push(guUpvalue.closedValue());

          break;
        case OP_SET_UPVALUE:
          short suSlot = readChunkCodeWord(frame);
          Upvalue suUpvalue = frame.closure().upvalues()[suSlot];

          if (suUpvalue.location() != -1) //i.e., open
            vStack.set(suUpvalue.location(), vStack.peek());
          else //i.e., closed
            suUpvalue.setClosedValue(vStack.peek());

          break;
        case OP_GET_PROPERTY:
          Object gpValue = vStack.peek();

          if (!(gpValue instanceof LoxInstance)) {
            runtimeError("Only instances have properties.");

            return InterpretResult.INTERPRET_RUNTIME_ERROR;
          }

          LoxInstance gpInstance = (LoxInstance)gpValue;
          String name = readChunkConstantAsString(frame);

          if (gpInstance.fields().containsKey(name)) {
            vStack.pop(); // Instance.

            vStack.push(gpInstance.fields().get(name));

            break;
          }

          if (!bindMethod(gpInstance.klass(), name))
            return InterpretResult.INTERPRET_RUNTIME_ERROR;

          break;
        case OP_SET_PROPERTY:
          Object spValue = vStack.get(vStack.size() - 2);

          if (!(spValue instanceof LPCObject)) {
            runtimeError("Only objects have properties.");

            return InterpretResult.INTERPRET_RUNTIME_ERROR;
          }

          LPCObject spObject = (LPCObject)spValue;

          String spPropertyName = readChunkConstantAsString(frame);
          spObject.fields().put(spPropertyName, vStack.peek());

          //pop value that was set plus instance object
          Object propertyValue = vStack.pop();
          vStack.pop(); //instance

          //push value back on vStack (because assignment is an expression)
          vStack.push(propertyValue);

          break;
        case OP_DEFINE_FIELD:
        case OP_GET_SUPER:
          String gsName = readChunkConstantAsString(frame);
          LoxClass gsSuperclass = (LoxClass)vStack.pop();

          if (!bindMethod(gsSuperclass, gsName))
            return InterpretResult.INTERPRET_RUNTIME_ERROR;

          break;
        case OP_EQUAL:
          equate();

          break;
        case OP_GREATER:
          if (!twoNumericOperands())
            return errorTwoNumbers();

          binaryOp(Operation.OPERATION_GT);

          break;
        case OP_LESS:
          if (!twoNumericOperands())
            return errorTwoNumbers();

          binaryOp(Operation.OPERATION_LT);

          break;
        case OP_ADD:
          if (twoStringOperands())
            concatenate();
          else if (twoNumericOperands())
            binaryOp(Operation.OPERATION_PLUS);
          else
            return errorTwoNumbersOrStrings();

          break;
        case OP_SUBTRACT:
          if (!twoNumericOperands())
            return errorTwoNumbers();

          binaryOp(Operation.OPERATION_SUBTRACT);

          break;
        case OP_MULTIPLY:
          if (!twoNumericOperands())
            return errorTwoNumbers();

          binaryOp(Operation.OPERATION_MULT);

          break;
        case OP_DIVIDE:
          if (!twoNumericOperands())
            return errorTwoNumbers();

          binaryOp(Operation.OPERATION_DIVIDE);

          break;
        case OP_NOT:
          vStack.push(isFalsey(vStack.pop()));

          break;
        case OP_NEGATE:
          if (!oneNumericOperand())
            return errorOneNumber();

          vStack.push(-(double)vStack.pop());

          break;
        case OP_JUMP:
          short jumpOffset = readChunkCodeWord(frame);

          frame.setIP(frame.ip() + jumpOffset);

          break;
        case OP_JUMP_IF_FALSE:
          short jumpIfFalseOffset = readChunkCodeWord(frame);

          if (isFalsey(vStack.peek()))
            frame.setIP(frame.ip() + jumpIfFalseOffset);

          break;
        case OP_LOOP:
          short loopOffset = readChunkCodeWord(frame);

          frame.setIP(frame.ip() - loopOffset);

          break;
        case OP_CALL:
          int callArgCount = readChunkCodeByte(frame);
          Object callee = vStack.get(vStack.size() - 1 - callArgCount);

          if (!callValue(callee, callArgCount))
            return InterpretResult.INTERPRET_RUNTIME_ERROR;

          frame = fStack.peek();

          break;
        case OP_INVOKE:
          String invMethod = readChunkConstantAsString(frame);
          int invArgCount = readChunkCodeByte(frame);

          if (!invoke(invMethod, invArgCount))
            return InterpretResult.INTERPRET_RUNTIME_ERROR;

          frame = fStack.peek();

          break;
        case OP_SUPER_INVOKE:
          String siMethod = readChunkConstantAsString(frame);
          int siArgCount = readChunkCodeByte(frame);
          LoxClass siSuperclass = (LoxClass)vStack.pop();

          if (!invokeFromClass(siSuperclass, siMethod, siArgCount))
            return InterpretResult.INTERPRET_RUNTIME_ERROR;

          frame = fStack.peek();

          break;
        case OP_CLOSURE:
          Function function = (Function)readChunkConstant(frame);
          Closure closure = new Closure(function);

          vStack.push(closure);

          for (int i = 0; i < closure.upvalueCount(); i++) {
            byte isLocal = readChunkCodeByte(frame);
            byte index = readChunkCodeByte(frame);

            if (isLocal != 0)
              closure.upvalues()[i] = captureUpvalue(frame.base() + index);
            else
              closure.upvalues()[i] = frame.closure().upvalues()[index];
          }

          break;
        case OP_CLOSE_UPVALUE:
          //close the upvalue at the top of the vStack
          closeUpvalues(vStack.size() - 1);

          vStack.pop();

          break;
        case OP_RETURN:
          //We're about to discard the called function's entire
          //stack window, so pop the function's return value but
          //hold onto a reference to it.
          Object result = vStack.pop();

          closeUpvalues(frame.base());

          //discard the CallFrame for the returning function
          fStack.pop();

          if (fStack.size() == 0) { //entire program finished
            vStack.pop();

            //exit the bytecode dispatch loop
            return InterpretResult.INTERPRET_OK;
          }

          //pop the vStack back to CallFrame base
          while (vStack.size() > frame.base())
            vStack.pop();

          vStack.push(result);

          frame = fStack.peek();

          break;
        case OP_OBJECT:
          String objName = readChunkConstantAsString(frame);
          LPCObject lpcObject = new LPCObject(objName);

          vStack.push(lpcObject);

          break;
        case OP_INHERIT:
          Object superclass = vStack.get(vStack.size() - 2);

          if (!(superclass instanceof LoxClass)) {
            runtimeError("Superclass must be a class.");

            return InterpretResult.INTERPRET_RUNTIME_ERROR;
          }

          LoxClass subclass = (LoxClass)vStack.peek();

          subclass.inheritMethods(((LoxClass)superclass).methods());

          vStack.pop(); // Subclass.

          break;
        case OP_METHOD:
          String methodName = readChunkConstantAsString(frame);
          Closure method = (Closure)vStack.peek();
          LoxClass klass = (LoxClass)vStack.get(vStack.size() - 2);

          klass.methods().put(methodName, method);

          vStack.pop();

          break;
        case OP_DEFINE_METHOD:
          String objMethodName = readChunkConstantAsString(frame);
          Closure objMethod = (Closure)vStack.peek();
          LPCObject obj = (LPCObject)vStack.get(vStack.size() - 2);

          obj.methods().put(objMethodName, objMethod);

          vStack.pop();

          break;
      } //switch
    } //for(;;)
  }

  //reset()
  private void reset() {
    vStack = new Stack<Object>();
    fStack = new Stack<CallFrame>();
    openUpvalues = null;
  }

  //runtimeError(String, String...)
  void runtimeError(String message, String... args) {
    System.err.println("Runtime Error: " + message);

    for (String s : args)
      System.err.println(s);

    //loop through CallFrames on fStack in reverse order
    for (int i = fStack.size() - 1; i >=0; i--) {
      CallFrame frame = fStack.get(i);
      Function function = frame.closure().function();
      int line = function.chunk().lines().get(frame.ip() - 1);

      System.err.print("[line " + line + "] in ");

      if (function.name() == null)
        System.err.print("script.\n");
      else
        System.err.print(function.name() + "().\n");
    }

    reset(); // vStack, fStack, openUpvalues
  }

  //defineNativeFn(String, NativeFn)
  private void defineNativeFn(String name, NativeFn nativeFn) {
    globals.put(name, nativeFn);
  }

  //loadObject(String, String)
  public Object loadObject(String name, String source) {
    Compiler compiler = new ObjectCompiler();
    Function function = compiler.compile(name, source);

    return new Closure(function);
  }

  //syntheticInstruction(byte)
  public void syntheticInstruction(byte b) {
    CallFrame frame = fStack.peek();
    Chunk chunk = frame.closure().function().chunk();

    chunk.insertByte(frame.ip(), b);
  }

  //call(Closure, int)
  private boolean call(Closure closure, int argCount) {
    if (!checkArity(closure.function(), argCount))
      return false;

    if (fStack.size() == Props.instance().getInt("MAX_FRAMES")) {
      runtimeError("Stack overflow.");

      return false;
    }

    //CallFrame window on VM vStack begins at slot
    //occupied by function.
    int base = vStack.size() - 1 - argCount;

    fStack.push(new CallFrame(closure, base));

    return true;
  }

  //callValue(Object, int)
  private boolean callValue(Object callee, int argCount) {
    //Bound Method
    if (callee instanceof BoundMethod) {
      BoundMethod bound = (BoundMethod)callee;

      vStack.set(vStack.size() - 1 - argCount, bound.receiver());

      return call(bound.method(), argCount);
    //Class
    } else if (callee instanceof LoxClass) {
      LoxClass klass = (LoxClass)callee;
      LoxInstance instance = new LoxInstance(klass);

      vStack.set(vStack.size() - 1 - argCount, instance);

      Closure initializer = klass.methods().get(initString);

      if (initializer != null)
        return call(initializer, argCount);
      else if (argCount != 0) {
        runtimeError("Expected 0 arguments but got " + argCount + ".");

        return false;
      }

      return true;
    //Closure
    } else if (callee instanceof Closure)
      return call((Closure)callee, argCount);
    //Native Function
    else if (callee instanceof NativeFn) {
      NativeFn nativeFn = (NativeFn)callee;

      if (!checkArity(nativeFn, argCount))
        return false;

      List<Object> args = vStack.subList(vStack.size() - argCount, vStack.size());
      Object result = nativeFn.execute(args.toArray());

      //pop args plus native function
      for (int i = 0; i < argCount + 1; i++)
        vStack.pop();

      vStack.push(result);

      //if (result instanceof Closure)
        //return call((Closure)result, 0);

      return true;
    }

    runtimeError("Can only call functions and classes.");

    return false;
  }

  //invokeFromClass(LoxClass, String, int)
  private boolean invokeFromClass(LoxClass klass, String name, int argCount) {
    if (!(klass.methods().containsKey(name))) {
      runtimeError("Undefined property '" + name + "'.");

      return false;
    }

    Closure method = klass.methods().get(name);

    return call(method, argCount);
  }

  //invoke(String, int)
  private boolean invoke(String name, int argCount) {
    Object receiver = vStack.get(vStack.size() - 1 - argCount);

    if (!(receiver instanceof LPCObject)) {
      runtimeError("Only objects have methods.");

      return false;
    }

    LPCObject lpcObject = (LPCObject)receiver;

    if (!lpcObject.methods().containsKey(name)) {
      runtimeError("Undefined method '" + name + "'.");

      return false;
    }

    Object value = lpcObject.methods().get(name);

    vStack.set(vStack.size() - 1 - argCount, value);

    return callValue(value, argCount);

    //return invokeFromClass(instance.klass(), name, argCount);
  }

  //bindMethod(LoxClass, String)
  private boolean bindMethod(LoxClass klass, String name) {
    if (!klass.methods().containsKey(name)) {
      runtimeError("Undefined property '" + name + "'.");

      return false;
    }

    Closure method = klass.methods().get(name);
    BoundMethod bound = new BoundMethod(vStack.peek(), method);
    Object value = bound;

    vStack.pop();

    vStack.push(value);

    return true;
  }

  //captureUpvalue(int)
  Upvalue captureUpvalue(int location) {
    Upvalue prevUpvalue = null;
    Upvalue currUpvalue = openUpvalues; //start at head of list

    while (currUpvalue != null && currUpvalue.location() > location) {
      prevUpvalue = currUpvalue;

      currUpvalue = currUpvalue.next();
    }

    if (currUpvalue != null && currUpvalue.location() == location)
      return currUpvalue;

    //create new Upvalue and insert into linked list of
    //open upvalues between previous and current
    Upvalue createdUpvalue = new Upvalue(location);

    createdUpvalue.setNext(currUpvalue);

    if (prevUpvalue == null)
      openUpvalues = createdUpvalue; //new head of list
    else
      prevUpvalue.setNext(createdUpvalue); //insert into list

    return createdUpvalue;
  }

  //closeUpvalues(int)
  void closeUpvalues(int last) {
    while (openUpvalues != null && openUpvalues.location() >= last) {
      Upvalue upvalue = openUpvalues;

      upvalue.setClosedValue(vStack.get(upvalue.location()));
      upvalue.setLocation(-1);

      //after upvalue is closed, reset head of linked list
      //to next upvalue
      openUpvalues = upvalue.next();
      upvalue.setNext(null);
    }
  }

  //isFalsey(Object)
  boolean isFalsey(Object value) {
    //nil and false are falsey and every other value behaves like true.
    return value == null || (value instanceof Boolean && !(boolean)value);
  }

  //concatenate()
  private void concatenate() {
    String b = (String)vStack.pop();
    String a = (String)vStack.pop();

    vStack.push(a + b);
  }

  //equate()
  private void equate() {
    Object b = vStack.pop();
    Object a = vStack.pop();

    if (a == null)
      vStack.push(b == null);
    else
      //pushValue(a.equals(b));
      vStack.push(a.equals(b));
  }

  //readChunkCodeByte(CallFrame)
  private byte readChunkCodeByte(CallFrame frame) {
    return frame.closure().function().chunk().codes().get(frame.getAndIncrementIP());
  }

  //readChunkCodeWord(CallFrame)
  private short readChunkCodeWord(CallFrame frame) {
    byte hi = readChunkCodeByte(frame);
    byte lo = readChunkCodeByte(frame);

    return (short)(((hi & 0xFF) << 8) | (lo & 0xFF));
  }

  //readChunkConstant(CallFrame)
  private Object readChunkConstant(CallFrame frame) {
    short index = readChunkCodeWord(frame);

    return frame.closure().function().chunk().constants().get(index);
  }

  //readChunkConstantAsString(CallFrame)
  private String readChunkConstantAsString(CallFrame frame) {
    return (String)readChunkConstant(frame);
  }

  //oneNumericOperand()
  private boolean oneNumericOperand() {
    return vStack.peek() instanceof Double;
  }

  //errorOneNumber()
  private InterpretResult errorOneNumber() {
    return error("Operand must be a number");
  }

  //twoNumericOperands()
  private boolean twoNumericOperands() {
    Object first = vStack.pop(); //temporarily pop
    Object second = vStack.peek();

    vStack.push(first);

    return first instanceof Double && second instanceof Double;
  }

  //errorTwoNumbers()
  private InterpretResult errorTwoNumbers() {
    return error("Operands must be two numbers.");
  }

  //twoStringOperands()
  private boolean twoStringOperands() {
    Object first = vStack.pop();
    Object second = vStack.peek();

    vStack.push(first); //push back again

    return first instanceof String && second instanceof String;
  }

  //errorTwoStrings()
  private InterpretResult errorTwoStrings() {
    return error("Operands must be two strings.");
  }

  //errorTwoNumbersOrStrings()
  private InterpretResult errorTwoNumbersOrStrings() {
    return error("Operands must be two numbers or two strings.");
  }

  //error(String)
  private InterpretResult error(String message) {
    runtimeError(message);

    return InterpretResult.INTERPRET_RUNTIME_ERROR;
  }

  //binaryOp(Operation)
  private void binaryOp(Operation op) {
    double b = (double)vStack.pop();
    double a = (double)vStack.pop();

    switch (op) {
      case OPERATION_PLUS:
        vStack.push(a + b);

        break;
      case OPERATION_SUBTRACT:
        vStack.push(a - b);

        break;
      case OPERATION_MULT:
        vStack.push(a * b);

        break;
      case OPERATION_DIVIDE:
        vStack.push(a / b);

        break;
      case OPERATION_GT:
        vStack.push(a > b);

        break;
      case OPERATION_LT:
        vStack.push(a < b);

        break;
    } //switch
  }

  //checkArity(HasArity, int)
  private boolean checkArity(HasArity callee, int argCount) {
    int arity = callee.arity();

    if (arity < 0) {
      if (argCount > Math.abs(arity)) {
        runtimeError(
          "Expected up to " + Math.abs(arity) + " argument(s) but got " + argCount + "."
        );

        return false;
      }
    } else if (argCount != arity) {
      runtimeError(
        "Expected " + arity + " arguments but got " + argCount + "."
      );

      return false;
    }

    return true;
  }

  //getLibPath()
  public String getLibPath() {
    return Props.instance().getString("PATH_LIB");
  }

  //updateCachedProperties()
  private void updateCachedProperties() {
    debugMaster = Props.instance().getBool("DEBUG_MASTER");
    debugPrintProgress = debugMaster && Props.instance().getBool("DEBUG_PROG");
    debugTraceExecution = debugMaster && Props.instance().getBool("DEBUG_EXEC");
  }

  //notifyPropertiesChanged()
  public void notifyPropertiesChanged() {
    updateCachedProperties();
  }
}
