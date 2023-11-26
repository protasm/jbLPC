package jbLPC.vm;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import jbLPC.compiler.C_Function;
import jbLPC.compiler.C_Script;
import jbLPC.compiler.Compilation;
//import jbLPC.compiler.CompilerUpvalue;
import jbLPC.compiler.HasArity;
import jbLPC.compiler.Instruction;
import jbLPC.compiler.LPCCompiler;
import jbLPC.compiler.LPCObjectCompiler;
import jbLPC.debug.Debugger;
import jbLPC.nativefn.NativeClock;
import jbLPC.nativefn.NativeCompileLPCObject;
import jbLPC.nativefn.NativeFn;
import jbLPC.nativefn.NativeFoo;
import jbLPC.nativefn.NativePrint;
import jbLPC.nativefn.NativePrintLn;
import jbLPC.util.Prefs;
import jbLPC.util.SourceFile;

public class VM {
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
  private Stack<RunFrame> fStack; //RunFrame stack
  private Upvalue openUpvalues; //linked list

  //VM()
  public VM() {
    globals = new HashMap<>();

    defineNativeFn("clock", new NativeClock(this, "Clock", 0));
    defineNativeFn("foo", new NativeFoo(this, "Foo", 3));
    defineNativeFn("print", new NativePrint(this, "Print", 1));
    defineNativeFn("println", new NativePrintLn(this, "PrintLn", -1)); //variadic, 0 or 1 args
    defineNativeFn("compile", new NativeCompileLPCObject(this, "Compile", 1));

    reset(); //vStack, fStack, openUpvalues

    Debugger.instance().printProgress("VM initialized");
  }

  //interpret(String)
  public InterpretResult interpret(String name, String source) {
    LPCCompiler compiler = new LPCCompiler();
    C_Script cScript = (C_Script)compiler.compile(name, source);

    if (cScript == null)
      return InterpretResult.INTERPRET_COMPILE_ERROR;

    Debugger.instance().printProgress("Executing script '" + name + "'");

    vStack.push(cScript);

    frame(cScript);
    
    vStack.pop();

    return run();
  }

  //run()
  private InterpretResult run() {
    RunFrame frame = fStack.peek(); //cached copy of current RunFrame
      
    //reusable scratchpad vars
    Instruction instr;
    int offset, argCount;
    String key, identifier;
    Object value;
    Closure closure;
    LPCObject lpcObject;
//    Upvalue upvalue;

    //Bytecode dispatch loop.
    for (;;) {
      //Prior pass may have left a new compilation on
      //the vStack; if so, frame it up to be run immediately.
      if (
        !vStack.isEmpty() &&
        vStack.peek() instanceof Compilation
      ) {
        Compilation compilation = (Compilation)vStack.peek();
        
        Debugger.instance().printProgress("Executing '" + compilation.name() + "'");
        
    	frame(compilation);

    	frame = fStack.peek();
    	
    	vStack.pop();
    	  
    	continue;
      }

      instr = frame.next();
      
      if (instr == null) {
    	runtimeError("Unexpected end of instructions in current frame.");
    	  
        return InterpretResult.INTERPRET_RUNTIME_ERROR;
      }

      Debugger.instance().traceExecution(instr, globals, vStack);
      
      switch (instr.opCode()) {
        case OP_CONST:
          value = instr.operands()[0];

          vStack.push(value);

          break;
        
        case OP_NIL: vStack.push(null); break;
        
        case OP_TRUE: vStack.push(true); break;
        
        case OP_FALSE: vStack.push(false); break;
        
        case OP_POP: vStack.pop(); break;
        
        case OP_GET_LOCAL:
          offset = (int)instr.operands()[0];
          value = vStack.get(frame.base() + offset);

          vStack.push(value);

          break;
        
        case OP_SET_LOCAL:
          offset = (int)instr.operands()[0];
          value = vStack.peek();

          vStack.set(frame.base() + offset, value);

          break;
        
        case OP_GLOBAL:
          key = (String)instr.operands()[0];
          value = vStack.peek();

          globals.put(key, value);

          vStack.pop();

          break;
        
        case OP_GET_GLOBAL:
          key = (String)instr.operands()[0]; //global name

          if (!globals.containsKey(key))
            return error("Undefined object '" + key + "'.");

          value = globals.get(key);

          vStack.push(value);

          break;
        
        case OP_SET_GLOBAL:
          key = (String)instr.operands()[0]; //global name

          if (!globals.containsKey(key))
            return error("Undefined object '" + key + "'.");

          //Peek here, not pop; assignment is an expression,
          //so we leave value vStacked in case the assignment
          //is nested inside a larger expression.
          globals.put(key, vStack.peek());

          break;
        
        case OP_GET_UPVAL:
//          offset = (int)instr.operands()[0]; //upvalue slot
//          upvalue = frame.closure().upvalues()[offset];
//
//          if (upvalue.location() != -1) //i.e., open
//            vStack.push(vStack.get(upvalue.location()));
//          else //i.e., closed
//           vStack.push(upvalue.closedValue());

          break;
        
        case OP_SET_UPVAL:
//          offset = (int)instr.operands()[0];
//          upvalue = frame.closure().upvalues()[offset];
//          value = vStack.peek();
//
//          if (upvalue.location() != -1) //i.e., open
//            vStack.set(upvalue.location(), value);
//          else //i.e., closed
//            upvalue.setClosedValue(value);

          break;

        case OP_CALL:
          argCount = (int)instr.operands()[0];
          value = vStack.get(vStack.size() - 1 - argCount); //callee

          if (!callValue(value, argCount))
            return InterpretResult.INTERPRET_RUNTIME_ERROR;

          frame = fStack.peek();

          break;
        
        case OP_INVOKE:
          identifier = (String)instr.operands()[0]; //method name
          argCount = (int)instr.operands()[1];

          if (!invoke(identifier, argCount))
            return InterpretResult.INTERPRET_RUNTIME_ERROR;

          frame = fStack.peek();

          break;
        
        case OP_SUPER_INVOKE:
          identifier = (String)instr.operands()[0]; //method name
          argCount = (int)instr.operands()[0];
          lpcObject = (LPCObject)vStack.pop(); //super object

          if (!invokeFromObject(lpcObject, identifier, argCount))
            return InterpretResult.INTERPRET_RUNTIME_ERROR;

          frame = fStack.peek();

          break;
        
        case OP_CLOSURE:
          C_Function cFunction = (C_Function)instr.operands()[0];
//          CompilerUpvalue[] compilerUpvalues = (CompilerUpvalue[])instr.operands()[1];
          closure = new Closure(cFunction);

          vStack.push(closure);

//          for (int i = 0; i < closure.upvalueCount(); i++) {
//            boolean isLocal = compilerUpvalues[i].isLocal();
//            int index = compilerUpvalues[i].index();
//            
//            if (isLocal)
//              closure.upvalues()[i] = captureUpvalue(frame.base() + index);
//            else
//              closure.upvalues()[i] = frame.closure().upvalues()[index];
//          }

          break;
        
        case OP_CLOSE_UPVAL:
          //close the upvalue at the top of the vStack
//          closeUpvalues(vStack.size() - 1);

          vStack.pop();

          break;
        
        case OP_RETURN:
          //We're about to discard the called function's entire
          //stack window, so pop the function's return value but
          //hold onto a reference to it.
          value = vStack.pop();

//          closeUpvalues(frame.base());

          //pop the RunFrame for the returning function
          fStack.pop();

          if (fStack.isEmpty()) { //entire program finished
//            vStack.pop();

            //exit the bytecode dispatch loop
            return InterpretResult.INTERPRET_OK;
          }

          //pop the vStack back to expiring RunFrame's base
          while (vStack.size() > frame.base())
            vStack.pop();

          //replace the function's return value on vStack
          vStack.push(value);

          frame = fStack.peek();

          break;

        case OP_COMPILE:
          identifier = (String)instr.operands()[0];
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
          identifier = (String)instr.operands()[0];
          lpcObject = new LPCObject(identifier);

          vStack.push(lpcObject);

          break;
        
        case OP_GET_PROP:
          value = vStack.peek();

          if (!(value instanceof LPCObject)) {
            runtimeError("Only LPC Objects have properties.");

            return InterpretResult.INTERPRET_RUNTIME_ERROR;
          }
          
          key = (String)instr.operands()[0];
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

        case OP_SET_PROP:
          value = vStack.get(vStack.size() - 2);

          if (!(value instanceof LPCObject)) {
            runtimeError("Only LPC Objects have fields.");

            return InterpretResult.INTERPRET_RUNTIME_ERROR;
          }
            
          key = (String)instr.operands()[0];
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
          identifier = (String)instr.operands()[0]; //field name
          value = vStack.peek();
          lpcObject = (LPCObject)vStack.get(vStack.size() - 2);

          lpcObject.fields().put(identifier, value);

          vStack.pop(); // dfFieldValue

          break;
          
        case OP_METHOD: //Define a new method in an LPCObject
          identifier = (String)instr.operands()[0]; //method name
          closure = (Closure)vStack.peek();
          lpcObject = (LPCObject)vStack.get(vStack.size() - 2);

          lpcObject.methods().put(identifier, closure);

          vStack.pop(); //mMethod

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
            offset = (int)instr.operands()[0];

//            frame.setIP(frame.ip() + offset);

            break;
          
          case OP_JUMP_IF_FALSE:
            offset = (int)instr.operands()[0];
            value = vStack.peek();

//            if (isFalsey(value))
//              frame.setIP(frame.ip() + offset);

            break;
          
          case OP_LOOP:
            offset = (int)instr.operands()[0];

//            frame.setIP(frame.ip() - offset);

            break;
            
        case OP_GET_SUPER:
        
          break;
      default:
        
        break;
      } //switch
    } //for(;;)
  } //run()

  //reset()
  private void reset() {
    vStack = new Stack<Object>();
    fStack = new Stack<RunFrame>();
    openUpvalues = null;
  }

  //runtimeError(String, String...)
  void runtimeError(String message, String... args) {
    System.err.println("Runtime Error: " + message);

    for (String s : args)
      System.err.println(s);

    //loop through RunFrames on fStack in reverse order
    for (int i = fStack.size() - 1; i >=0; i--) {
      RunFrame frame = fStack.get(i);
      Compilation compilation = frame.compilation();
      int line = frame.previous().line();

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
    String fullPath = getLibPath() + path;
    SourceFile file  = new SourceFile(fullPath);
    LPCObjectCompiler compiler = new LPCObjectCompiler();

    return compiler.compile(
      Paths.get(file.path()),
      file.prefix(),
      file.source()
    );
  }

  //callValue(Object, int)
  private boolean callValue(Object callee, int argCount) {
	//C_Function
	if (callee instanceof C_Function)
      return frame((C_Function)callee, argCount);
    //Native Function
    else if (callee instanceof NativeFn)
      return call((NativeFn)callee, argCount);

    runtimeError("Can only call functions and methods.");

    return false;
  }

  //frame(C_Function, int)
  private boolean frame(C_Function cFunction, int argCount) {
    if (!checkArity(cFunction, argCount))
      return false;

    int base = vStack.size() - 1 - argCount;

    fStack.push(new RunFrame(cFunction, base));

    return true;
  }
  
  //frame(Compilation)
  private void frame(Compilation compilation) {
	  int base = vStack.size() - 1;
	  
	  fStack.push(new RunFrame(compilation, base));
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

    Closure closure = lpcObject.methods().get(methodName);

    return frame(closure.cFunction(), argCount);
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
      Upvalue compilerUpvalue = openUpvalues;

      compilerUpvalue.setClosedValue(vStack.get(compilerUpvalue.location()));
      compilerUpvalue.setLocation(-1);

      //after upvalue is closed, reset head of linked list
      //to next upvalue
      openUpvalues = compilerUpvalue.next();
      compilerUpvalue.setNext(null);
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

//  private Instruction readInstruction(RunFrame frame) {
    //Do some caching to optimize all these lookups!
//    Compilation compilation = frame.closure().cFunction();
//    List<Instruction> instrs = compilation.instructions();
//    Instruction instr = instrs.get(frame.getAndIncrementIP());
//    
//    return instr;
//  }

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
    return Prefs.instance().getString("PATH_LIB");
  }
}
