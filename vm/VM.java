package jbLPC.vm;

import static jbLPC.compiler.OpCode.OP_ADD;
import static jbLPC.compiler.OpCode.OP_CALL;
import static jbLPC.compiler.OpCode.OP_CLOSE_UPVALUE;
import static jbLPC.compiler.OpCode.OP_CLOSURE;
import static jbLPC.compiler.OpCode.OP_COMPILE_OBJ;
import static jbLPC.compiler.OpCode.OP_CONSTANT;
import static jbLPC.compiler.OpCode.OP_DEFINE_GLOBAL;
import static jbLPC.compiler.OpCode.OP_DIVIDE;
import static jbLPC.compiler.OpCode.OP_EQUAL;
import static jbLPC.compiler.OpCode.OP_FALSE;
import static jbLPC.compiler.OpCode.OP_FIELD;
import static jbLPC.compiler.OpCode.OP_GET_GLOBAL;
import static jbLPC.compiler.OpCode.OP_GET_LOCAL;
import static jbLPC.compiler.OpCode.OP_GET_PROPERTY;
import static jbLPC.compiler.OpCode.OP_GET_UPVALUE;
import static jbLPC.compiler.OpCode.OP_GREATER;
import static jbLPC.compiler.OpCode.OP_INHERIT;
import static jbLPC.compiler.OpCode.OP_INVOKE;
import static jbLPC.compiler.OpCode.OP_JUMP;
import static jbLPC.compiler.OpCode.OP_JUMP_IF_FALSE;
import static jbLPC.compiler.OpCode.OP_LESS;
import static jbLPC.compiler.OpCode.OP_LOOP;
import static jbLPC.compiler.OpCode.OP_METHOD;
import static jbLPC.compiler.OpCode.OP_MULTIPLY;
import static jbLPC.compiler.OpCode.OP_NEGATE;
import static jbLPC.compiler.OpCode.OP_NIL;
import static jbLPC.compiler.OpCode.OP_NOT;
import static jbLPC.compiler.OpCode.OP_OBJECT;
import static jbLPC.compiler.OpCode.OP_POP;
import static jbLPC.compiler.OpCode.OP_RETURN;
import static jbLPC.compiler.OpCode.OP_SET_GLOBAL;
import static jbLPC.compiler.OpCode.OP_SET_LOCAL;
import static jbLPC.compiler.OpCode.OP_SET_PROPERTY;
import static jbLPC.compiler.OpCode.OP_SET_UPVALUE;
import static jbLPC.compiler.OpCode.OP_SUBTRACT;
import static jbLPC.compiler.OpCode.OP_SUPER_INVOKE;
import static jbLPC.compiler.OpCode.OP_TRUE;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import jbLPC.compiler.C_Function;
import jbLPC.compiler.C_Script;
import jbLPC.compiler.Compilation;
import jbLPC.compiler.HasArity;
import jbLPC.compiler.LPCCompiler;
import jbLPC.compiler.LPCObjectCompiler;
import jbLPC.debug.Debugger;
import jbLPC.nativefn.NativeClock;
import jbLPC.nativefn.NativeCompileLPCObject;
import jbLPC.nativefn.NativeFn;
import jbLPC.nativefn.NativeFoo;
import jbLPC.nativefn.NativePrint;
import jbLPC.nativefn.NativePrintLn;
import jbLPC.util.Props;
import jbLPC.util.PropsObserver;
import jbLPC.util.SourceFile;

public class VM implements PropsObserver {
  //InterpretResult
  public static enum InterpretResult {
    INTERPRET_OK,
    INTERPRET_COMPILE_ERROR,
    INTERPRET_RUNTIME_ERROR,
  }

  //Operation
  private static enum Operation {
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
    defineNativeFn("compile", new NativeCompileLPCObject(this, "Compile", 1));

    reset(); //vStack, fStack, openUpvalues

    if (debugPrintProgress) Debugger.instance().printProgress("VM initialized");
  }

  //interpret(String)
  public InterpretResult interpret(String name, String source) {
    LPCCompiler compiler = new LPCCompiler();
    C_Script cScript = (C_Script)compiler.compile(name, source);

    if (cScript == null)
      return InterpretResult.INTERPRET_COMPILE_ERROR;

    if (debugPrintProgress)
      Debugger.instance().printProgress("Executing '" + name + "'");

    Closure closure = new Closure(cScript);

    vStack.push(closure);

    call(closure, 0); //pushes new CallFrame on fStack

    return run();
  }

  //run()
  private InterpretResult run() {
    CallFrame frame = fStack.peek(); //cached copy of current CallFrame
    
    //reusable scratchpad vars
    String key;
    Object value;
    String identifier;
    short offset;
    int argCount;
    Closure closure;
    LPCObject lpcObject;
    Upvalue upvalue;

    //Bytecode dispatch loop.
    for (;;) {
      //Prior pass may have stacked a compileable object on
      //the vStack; if so, run that compilation before continuing
      //with execution.
      if (vStack.peek() instanceof Compilation) {
        if (debugPrintProgress) {
          String compilationName = ((Compilation)vStack.peek()).name();
          Debugger.instance().printProgress("Executing '" + compilationName + "'");
        }
        
    	  callValue(vStack.peek(), 0);
    	  
    	  frame = fStack.peek();
    	
    	  vStack.pop();
    	  
    	  continue;
      }

      if (debugTraceExecution)
        Debugger.instance().traceExecution(frame, globals, vStack.toArray());
      
      byte opCode = readChunkOpCodeByte(frame);

      switch (opCode) {
        case OP_CONSTANT:
          value = readChunkConstant(frame);

          vStack.push(value);

          break;
        
        case OP_NIL:   vStack.push(null); break;
        
        case OP_TRUE:  vStack.push(true); break;
        
        case OP_FALSE: vStack.push(false); break;
        
        case OP_POP:   vStack.pop(); break;
        
        case OP_GET_LOCAL:
          offset = readChunkOpCodeWord(frame);
          value = vStack.get(frame.base() + offset);

          vStack.push(value);

          break;
        
        case OP_SET_LOCAL:
          offset = readChunkOpCodeWord(frame);
          value = vStack.peek();

          vStack.set(frame.base() + offset, value);

          break;
        
        case OP_DEFINE_GLOBAL: //Add Object to globals
          key = readChunkConstantAsString(frame);
          value = vStack.peek();

          globals.put(key, value);

          vStack.pop();

          break;
        
        case OP_GET_GLOBAL: //Get Object from globals
          key = readChunkConstantAsString(frame);

          if (!globals.containsKey(key))
            return error("Undefined object '" + key + "'.");

          value = globals.get(key);

          vStack.push(value);

          break;
        
        case OP_SET_GLOBAL: //Set Object to new value in globals
          key = readChunkConstantAsString(frame);

          if (!globals.containsKey(key))
            return error("Undefined object '" + key + "'.");

          //Peek here, not pop; assignment is an expression,
          //so we leave value vStacked in case the assignment
          //is nested inside a larger expression.
          globals.put(key, vStack.peek());

          break;
        
        case OP_GET_UPVALUE:
          offset = readChunkOpCodeWord(frame); //upvalue slot
          upvalue = frame.closure().upvalues()[offset];

          if (upvalue.location() != -1) //i.e., open
            vStack.push(vStack.get(upvalue.location()));
          else //i.e., closed
           vStack.push(upvalue.closedValue());

          break;
        
        case OP_SET_UPVALUE:
          offset = readChunkOpCodeWord(frame); //upvalue slot
          upvalue = frame.closure().upvalues()[offset];
          value = vStack.peek();

          if (upvalue.location() != -1) //i.e., open
            vStack.set(upvalue.location(), value);
          else //i.e., closed
            upvalue.setClosedValue(value);

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
          value = vStack.pop();
          
          vStack.push(isFalsey(value));

          break;
        
        case OP_NEGATE:
          if (!oneNumericOperand())
            return errorOneNumber();
          
          value = vStack.pop();

          vStack.push(-(double)value);

          break;
        
        case OP_JUMP:
          offset = readChunkOpCodeWord(frame);

          frame.setIP(frame.ip() + offset);

          break;
        
        case OP_JUMP_IF_FALSE:
          offset = readChunkOpCodeWord(frame);
          value = vStack.peek();

          if (isFalsey(value))
            frame.setIP(frame.ip() + offset);

          break;
        
        case OP_LOOP:
          offset = readChunkOpCodeWord(frame);

          frame.setIP(frame.ip() - offset);

          break;
        
        case OP_CALL:
          argCount = readChunkOpCodeByte(frame);
          value = vStack.get(vStack.size() - 1 - argCount); //callee

          if (!callValue(value, argCount))
            return InterpretResult.INTERPRET_RUNTIME_ERROR;

          //callValue should have pushed a new CallFrame (via call()),
          //so update locally cached frame
          frame = fStack.peek();

          break;
        
        case OP_INVOKE:
          identifier = readChunkConstantAsString(frame); //method name
          argCount = readChunkOpCodeByte(frame);

          if (!invoke(identifier, argCount))
            return InterpretResult.INTERPRET_RUNTIME_ERROR;

          //invoke should have pushed a new CallFrame (via call()),
          //so update locally cached frame
          frame = fStack.peek();

          break;
        
        case OP_SUPER_INVOKE:
          identifier = readChunkConstantAsString(frame); //method name
          argCount = readChunkOpCodeByte(frame);
          lpcObject = (LPCObject)vStack.pop(); //super object

          if (!invokeFromObject(lpcObject, identifier, argCount))
            return InterpretResult.INTERPRET_RUNTIME_ERROR;

          //invokeFromObject should have pushed a new CallFrame (via call()),
          //so update locally cached frame
          frame = fStack.peek();

          break;
        
        case OP_CLOSURE:
          C_Function cFunction = (C_Function)readChunkConstant(frame);
          closure = new Closure(cFunction);

          vStack.push(closure);

          for (int i = 0; i < closure.upvalueCount(); i++) {
            byte isLocal = readChunkOpCodeByte(frame);
            byte index = readChunkOpCodeByte(frame);

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
          value = vStack.pop();

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

          //replace the function's return value on vStack
          vStack.push(value);

          frame = fStack.peek();

          break;
        
        case OP_COMPILE_OBJ:
          identifier = readChunkConstantAsString(frame);
          Compilation compilation = compilation(identifier);

          vStack.push(compilation);
          
          break;
        
        case OP_INHERIT:
          value = vStack.peek();

          if (!(value instanceof LPCObject)) {
            runtimeError("Inherited object must be an LPCObject.");

            return InterpretResult.INTERPRET_RUNTIME_ERROR;
          }

          lpcObject = (LPCObject)value; //super object

          value = vStack.get(vStack.size() - 2);

          if (!(value instanceof LPCObject)) {
            runtimeError("Inheriting object must be an LPCObject.");

            return InterpretResult.INTERPRET_RUNTIME_ERROR;
          }

          LPCObject iSubObject = (LPCObject)value;

          iSubObject.inherit(lpcObject); //copies down fields and methods

          vStack.pop(); // Inheriting object.

          break;
        
        case OP_OBJECT: //Create a new, empty LPCObject
          identifier = readChunkConstantAsString(frame);
          lpcObject = new LPCObject(identifier);

          vStack.push(lpcObject);

          break;
        
        case OP_GET_PROPERTY:
          value = vStack.peek();

          if (!(value instanceof LPCObject)) {
            runtimeError("Only LPC Objects have properties.");

            return InterpretResult.INTERPRET_RUNTIME_ERROR;
          }
          
          key = readChunkConstantAsString(frame);
          lpcObject = (LPCObject)value;

          //Look first for a matching field.
          if (lpcObject.fields().containsKey(key)) {
            Object field = lpcObject.fields().get(key);
            
            vStack.pop(); // LPCObject

            vStack.push(field);

            break;
          }
          
          //If no field, look for a matching method.
          if (lpcObject.methods().containsKey(key)) {
            Closure method = lpcObject.methods().get(key);
            
            vStack.pop(); // LPCObject
                    
            vStack.push(method);
            
            break;
          }
          
          runtimeError("Undefined property '" + key + "'.");
          
          return InterpretResult.INTERPRET_RUNTIME_ERROR;

        case OP_SET_PROPERTY:
          value = vStack.get(vStack.size() - 2);

          if (!(value instanceof LPCObject)) {
            runtimeError("Only LPC Objects have fields.");

            return InterpretResult.INTERPRET_RUNTIME_ERROR;
          }
            
          key = readChunkConstantAsString(frame);
          lpcObject = (LPCObject)value;

          //Look for a matching field.
          if (!lpcObject.fields().containsKey(key)) {
            runtimeError("Undefined field '" + key + "'.");
                    
            return InterpretResult.INTERPRET_RUNTIME_ERROR;
          }     

          //Set the existing field to its new value.
          lpcObject.fields().put(key, vStack.peek());

          //Pop new field value plus LPCObject
          value = vStack.pop(); //new field value
          vStack.pop(); //LPCObject

          //Push new field value back on vStack.  Assignment is
          //an expression, so new field value remains stacked in
          //case the assignment is nested inside a larger expression.
          vStack.push(value);

          break;
          
        case OP_FIELD: //Define a new field in an LPCObject
          identifier = readChunkConstantAsString(frame); //field name
          value = vStack.peek();
          lpcObject = (LPCObject)vStack.get(vStack.size() - 2);

          lpcObject.fields().put(identifier, value);

          vStack.pop(); // dfFieldValue

          break;
          
        case OP_METHOD: //Define a new method in an LPCObject
          identifier = readChunkConstantAsString(frame); //method name
          closure = (Closure)vStack.peek();
          lpcObject = (LPCObject)vStack.get(vStack.size() - 2);

          lpcObject.methods().put(identifier, closure);

          vStack.pop(); //mMethod

          break;
      } //switch
    } //for(;;)
  } //run()

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
      Compilation compilation = frame.closure().compilation();
      int line = compilation.chunk().lines().get(frame.ip() - 1);

      System.err.print("[line " + line + "] in ");

      if (compilation instanceof C_Script)
        System.err.print("script.\n");
      else
        System.err.print(compilation.name() + "().\n");
    }

    reset(); // vStack, fStack, openUpvalues
  }

  //defineNativeFn(String, NativeFn)
  private void defineNativeFn(String name, NativeFn nativeFn) {
    globals.put(name, nativeFn);
  }

  public Compilation compilation(String path) {
    String libPath = getLibPath();
    String fullPath = libPath + path;
    SourceFile file  = new SourceFile(fullPath);
    LPCObjectCompiler compiler = new LPCObjectCompiler();

    return compiler.compile(
      Paths.get(file.path()),
      file.prefix(),
      file.source()
    );
  }

  //callValue(Object, int)
  //This function is a dispatcher; when we want to call a value
  //from the vStack, first we need to know (by testing) what kind
  //of callable object it is (e.g. Closure, NativeFn, etc.)
  private boolean callValue(Object callee, int argCount) {
	//Compilation
	if (callee instanceof Compilation) {
		Closure closure = new Closure((Compilation) callee);
		
		return call(closure, 0);
    //Closure
	} else if (callee instanceof Closure)
      return call((Closure)callee, argCount);
    //Native Function
    else if (callee instanceof NativeFn)
      return call((NativeFn)callee, argCount);

    runtimeError("Can only call methods and native functions.");

    return false;
  }

  //call(Closure, int)
  //"Calling" a Closure means checking that the argCount is right,
  //making sure the fStack can accept a new CallFrame, creating a
  //new CallFrame with the appropriate base pointer, and then
  //pushing the new CallFrame on the fStack.  Next time run() cycles
  //through, it will update its locally-cached frame variable to the
  //new CallFrame at the top of fStack, and execute its opCodes.
  private boolean call(Closure closure, int argCount) {
    if (
      closure.compilation() instanceof C_Function &&
      !checkArity((C_Function)closure.compilation(), argCount)
    )
      return false;

    if (fStack.size() == Props.instance().getInt("MAX_FRAMES")) {
      runtimeError("Stack overflow.");

      return false;
    }

    //CallFrame window on value stack begins at slot
    //occupied by function.
    int base = vStack.size() - 1 - argCount;

    fStack.push(new CallFrame(closure, base));

    return true;
  }

  //call(NativeFn, int)
  private boolean call(NativeFn nativeFn, int argCount) {
    if (!checkArity(nativeFn, argCount))
      return false;

    List<Object> args = vStack.subList(vStack.size() - argCount, vStack.size());
    Object result = nativeFn.execute(args.toArray());

    //pop args plus native function
    for (int i = 0; i < argCount + 1; i++)
      vStack.pop();

    vStack.push(result); //return value

    return true;
  }

  //invokeFromObject(LPCObject, String, int)
  //Invoking From Object means checking that the LPCObject contains
  //a method with the given name, wrapping that method in a new Closure,
  //and passing the Closure to call with its arg count.
  private boolean invokeFromObject(LPCObject lpcObject, String methodName, int argCount) {
    if (!(lpcObject.methods().containsKey(methodName))) {
      runtimeError("Undefined method '" + methodName + "'.");

      return false;
    }

    Closure method = lpcObject.methods().get(methodName);

    return call(method, argCount);
  }

  //invoke(String, int)
  //Invoking means checking that the second-from-top vStack value
  //is an LPCObject, then sending the object, method name, and arg count
  //to invokeFromObject.
  private boolean invoke(String methodName, int argCount) {
    Object value = vStack.get(vStack.size() - 1 - argCount);

    if (!(value instanceof LPCObject)) {
      runtimeError("Only LPC Objects have methods.");

      return false;
    }

    LPCObject lpcObject = (LPCObject)value;

    return invokeFromObject(lpcObject, methodName, argCount);
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

  //readChunkOpCodeByte(CallFrame)
  private byte readChunkOpCodeByte(CallFrame frame) {
    return frame.closure().compilation().chunk().opCodes().get(frame.getAndIncrementIP());
  }

  //readChunkOpCodeWord(CallFrame)
  private short readChunkOpCodeWord(CallFrame frame) {
    byte hi = readChunkOpCodeByte(frame);
    byte lo = readChunkOpCodeByte(frame);

    return (short)(((hi & 0xFF) << 8) | (lo & 0xFF));
  }

  //readChunkConstant(CallFrame)
  private Object readChunkConstant(CallFrame frame) {
    short index = readChunkOpCodeWord(frame);

    return frame.closure().compilation().chunk().constants().get(index);
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
