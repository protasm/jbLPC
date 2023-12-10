package jbLPC.vm;

import static jbLPC.compiler.C_Compilation.C_CompilationType.TYPE_SCRIPT;
import static jbLPC.compiler.C_OpCode.OP_ADD;
import static jbLPC.compiler.C_OpCode.OP_ARRAY;
import static jbLPC.compiler.C_OpCode.OP_CALL;
import static jbLPC.compiler.C_OpCode.OP_CLOSE_UPVAL;
import static jbLPC.compiler.C_OpCode.OP_CLOSURE;
import static jbLPC.compiler.C_OpCode.OP_COMPILE;
import static jbLPC.compiler.C_OpCode.OP_CONSTANT;
import static jbLPC.compiler.C_OpCode.OP_DEF_GLOBAL;
import static jbLPC.compiler.C_OpCode.OP_DIVIDE;
import static jbLPC.compiler.C_OpCode.OP_EQUAL;
import static jbLPC.compiler.C_OpCode.OP_FALSE;
import static jbLPC.compiler.C_OpCode.OP_FIELD;
import static jbLPC.compiler.C_OpCode.OP_GET_GLOBAL;
import static jbLPC.compiler.C_OpCode.OP_GET_ARR_ELEM;
import static jbLPC.compiler.C_OpCode.OP_GET_LOCAL;
import static jbLPC.compiler.C_OpCode.OP_GET_PROP;
import static jbLPC.compiler.C_OpCode.OP_GET_SUPER;
import static jbLPC.compiler.C_OpCode.OP_GET_UPVAL;
import static jbLPC.compiler.C_OpCode.OP_GREATER;
import static jbLPC.compiler.C_OpCode.OP_INHERIT;
import static jbLPC.compiler.C_OpCode.OP_INVOKE;
import static jbLPC.compiler.C_OpCode.OP_JUMP;
import static jbLPC.compiler.C_OpCode.OP_JUMP_IF_FALSE;
import static jbLPC.compiler.C_OpCode.OP_LESS;
import static jbLPC.compiler.C_OpCode.OP_LOOP;
import static jbLPC.compiler.C_OpCode.OP_MAPPING;
import static jbLPC.compiler.C_OpCode.OP_METHOD;
import static jbLPC.compiler.C_OpCode.OP_MULTIPLY;
import static jbLPC.compiler.C_OpCode.OP_NEGATE;
import static jbLPC.compiler.C_OpCode.OP_NIL;
import static jbLPC.compiler.C_OpCode.OP_NOT;
import static jbLPC.compiler.C_OpCode.OP_OBJECT;
import static jbLPC.compiler.C_OpCode.OP_POP;
import static jbLPC.compiler.C_OpCode.OP_RETURN;
import static jbLPC.compiler.C_OpCode.OP_SET_GLOBAL;
import static jbLPC.compiler.C_OpCode.OP_SET_LOCAL;
import static jbLPC.compiler.C_OpCode.OP_SET_PROP;
import static jbLPC.compiler.C_OpCode.OP_SET_UPVAL;
import static jbLPC.compiler.C_OpCode.OP_SUBTRACT;
import static jbLPC.compiler.C_OpCode.OP_SUPER_INVOKE;
import static jbLPC.compiler.C_OpCode.OP_TRUE;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import jbLPC.compiler.C_Compilation;
import jbLPC.compiler.C_Compiler;
import jbLPC.compiler.C_Function;
import jbLPC.compiler.C_HasArity;
import jbLPC.compiler.C_InstrList;
import jbLPC.compiler.C_ObjectCompiler;
import jbLPC.debug.Debugger;
import jbLPC.nativefn.NativeClock;
import jbLPC.nativefn.NativeCompileLPCObject;
import jbLPC.nativefn.NativeFn;
import jbLPC.nativefn.NativeFoo;
import jbLPC.nativefn.NativePrint;
import jbLPC.nativefn.NativePrintLn;
import jbLPC.util.Prefs;
import jbLPC.util.SourceFile;
import jbLPC.util.ObjStack;

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
  private Map<String, NativeFn> nativeFns;
  private ObjStack vStack; //Value stack
  private Stack<RunFrame> fStack; //RunFrame stack
  private Upvalue openUpvalues; //linked list

  public boolean execCompilation;

  //VM()
  public VM() {
    globals = new HashMap<>();
    nativeFns = new HashMap<>();

    defineNativeFn("clock", new NativeClock(this, "Clock", 0));
    defineNativeFn("foo", new NativeFoo(this, "Foo", 3));
    defineNativeFn("print", new NativePrint(this, "Print", 1));
    defineNativeFn("println", new NativePrintLn(this, "PrintLn", -1)); //variadic, 0 or 1 args
    defineNativeFn("compile", new NativeCompileLPCObject(this, "Compile", 1));

    reset(); //vStack, fStack, openUpvalues, execCompilation

    Debugger.instance().printProgress("VM initialized");
  }

  //interpret(String)
  public InterpretResult interpret(String name, String source) {
    C_Compiler compiler = new C_Compiler();
    C_Compilation cScript = compiler.compile(name, source);

    if (cScript == null)
      return InterpretResult.INTERPRET_COMPILE_ERROR;

    Debugger.instance().printProgress("Executing script '" + name + "'");
    
    vStack.push(cScript);

    frame(cScript);
    
    return run();
  }

  //run()
  private InterpretResult run() {
    RunFrame frame = fStack.peek(); //cached copy of current RunFrame

    //Bytecode dispatch loop.
    for (;;) {
      //Prior pass may have left a "raw" compilation on
      //the vStack; if so, frame it up to be run immediately.
      if (execCompilation) {
        C_Compilation compilation = (C_Compilation)vStack.peek();

        Debugger.instance().printProgress("Executing '" + compilation.name() + "'");

    	frame(compilation);
    	  
    	execCompilation = false;

    	frame = fStack.peek();

    	continue;
      }

      byte opCode = frame.nextInstr();

      Debugger.instance().traceExecution(frame, globals, vStack);

      switch (opCode) {
        case OP_ADD: {
          if (twoStringOperands())
            concatenate();
          else if (twoNumericOperands())
            binaryOp(Operation.OPERATION_PLUS);
          else
            return errorTwoNumbersOrStrings();

          break;
        } //OP_ADD
        
        case OP_ARRAY: {
          byte operand = frame.nextInstr(); //element count
          List<Object> elements = new ArrayList<Object>();
          
          for (int i = 0; i < operand; i++)
            elements.add(vStack.pop());

          vStack.push(new LPCArray(elements.reversed()));

          break;	
        } //OP_ARRAY
        
        case OP_CALL: {
          byte operand = frame.nextInstr(); //arg count
          Object constant = vStack.get(vStack.size() - 1 - operand); //callee

          if (!callValue(constant, operand))
            return InterpretResult.INTERPRET_RUNTIME_ERROR;

          frame = fStack.peek();
          
          break;
        } //OP_CALL
        
        case OP_CLOSE_UPVAL: {
          //close the upvalue at the top of the vStack
          closeUpvalues(vStack.size() - 1);

          vStack.pop();

          break;
        } //OP_CLOSE_UPVAL
        
        case OP_CLOSURE: {
          byte operand = frame.nextInstr(); //constants index
          Object constant = frame.getConstant(operand); //function
          Closure closure = new Closure((C_Function)constant);

          vStack.push(closure);

          for (int i = 0; i < closure.upvalues().length; i++) {
            byte isLocal = frame.nextInstr();
            byte index = frame.nextInstr();

            if (isLocal != 0)
              closure.upvalues()[i] = captureUpvalue(frame.base() + index);
            else
              closure.upvalues()[i] = frame.closure().upvalues()[index];
          }

          break;
        } //OP_CLOSURE
        
        case OP_COMPILE: {
          byte operand = frame.nextInstr(); //constants index
          Object constant = frame.getConstant(operand); //path to source code
          C_Compilation c_Compilation = getCompilation((String)constant);

          vStack.push(c_Compilation);
          
          execCompilation = true;

          break;
      } //OP_COMPILE

        case OP_CONSTANT: {
          byte operand = frame.nextInstr(); //constants index
          Object constant = frame.getConstant(operand); //constant

          vStack.push(constant);

          break;
        } //OP_CONSTANT
        
        case OP_DEF_GLOBAL: {
          byte operand = frame.nextInstr(); //constants index
          Object constant = frame.getConstant(operand); //global name
          Object value = vStack.peek(); //global value

          globals.put((String)constant, value);

          vStack.pop();

          break;
        } //OP_DEF_GLOBAL
        
        case OP_DIVIDE: {
          if (!twoNumericOperands())
            return errorTwoNumbers();

          binaryOp(Operation.OPERATION_DIVIDE);

          break;
        } //OP_DIVIDE
        
        case OP_EQUAL: {
          equate();

          break;
        } //OP_EQUAL
        
        case OP_FALSE: {
          vStack.push(false);
          
          break;
        } //OP_FALSE
        
        case OP_FIELD: {
          byte operand = frame.nextInstr(); //constants index
          Object constant = frame.getConstant(operand); //field name
          Object value = vStack.peek(); //field value
          LPCObject lpcObject = (LPCObject)vStack.get(vStack.size() - 2); //LPC object

          lpcObject.fields().put((String)constant, value);

          vStack.pop(); //field value

          break;
        } //OP_FIELD
        
        case OP_GET_ARR_ELEM: {
            Object val1 = vStack.pop(); //element index
            Object val2 = vStack.pop(); //LPC array
            
            if (
              !(val1 instanceof Double) ||
              (Double)val1 % 1 != 0
            ) {
              runtimeError("Invalid array element index: " + (Double)val1);

              return InterpretResult.INTERPRET_RUNTIME_ERROR;
            }

            if (!(val2 instanceof LPCArray)) {
              runtimeError("Only arrays have elements.");

              return InterpretResult.INTERPRET_RUNTIME_ERROR;
            }

            LPCArray array = (LPCArray)val2;
            int index = ((Double)val1).intValue();

            if (index < 0 || index > array.size() - 1) {
              runtimeError("Array element index out of bounds: " + index);

              return InterpretResult.INTERPRET_RUNTIME_ERROR;
            }

            vStack.push(array.get(index));

            break;
          } //OP_GET_ELEMENT

        case OP_GET_GLOBAL: {
          byte operand = frame.nextInstr(); //constants index
          Object constant = frame.getConstant(operand); //global or nativeFn name
          String name = (String)constant;

          if (globals.containsKey(name)) {
            Object value = globals.get(name);

            vStack.push(value);
            
            break;
          }
          
          if (nativeFns.containsKey(name)) {
            Object value = nativeFns.get(name);
            
            vStack.push(value);
            
            break;
          }

          return error("Undefined object '" + name + "'.");
        } //OP_GET_GLOBAL

        case OP_GET_LOCAL: {
          byte operand = frame.nextInstr(); //offset from frame base
          Object value = vStack.get(frame.base() + operand); //local value

          vStack.push(value);

          break;
        } //OP_GET_LOCAL
        
        case OP_GET_PROP: {
          Object value = vStack.peek(); //LPC object

          if (!(value instanceof LPCObject)) {
            runtimeError("Only objects have properties.");

            return InterpretResult.INTERPRET_RUNTIME_ERROR;
          }

          byte operand = frame.nextInstr(); //constants index
          Object constant = frame.getConstant(operand); //field or method name
          String name = (String)constant;
          LPCObject lpcObject = (LPCObject)value;

          //Look first for a matching field.
          if (lpcObject.fields().containsKey(name)) {
            Object field = lpcObject.fields().get(name);

            vStack.pop(); // LPC object

            vStack.push(field);

            break;
          }

          //If no field, look for a matching method.
          if (lpcObject.methods().containsKey(name)) {
            Closure method = lpcObject.methods().get(name);

            vStack.pop(); // LPC object

            vStack.push(method);

            break;
          }
          
          //Finally, check native functions
          if(nativeFns.containsKey(name)) {
            NativeFn nativeFn = nativeFns.get(name);
            
            vStack.pop();
            
            vStack.push(nativeFn);
            
            break;
          }

          runtimeError("Undefined property '" + (String)constant + "'.");

          return InterpretResult.INTERPRET_RUNTIME_ERROR;
        } //OP_GET_PROP
        
        case OP_GET_SUPER: {
          break;
        } //OP_GET_SUPER
        
        case OP_GET_UPVAL: {
          byte operand = frame.nextInstr(); //Upvalues slot
          Upvalue upvalue = frame.closure().upvalues()[operand];

          if (upvalue.location() != -1) //i.e., open
            vStack.push(vStack.get(upvalue.location()));
          else //i.e., closed
           vStack.push(upvalue.closedValue());

          break;
        } //OP_GET_UPVAL
        
        case OP_GREATER: {
          if (!twoNumericOperands())
            return errorTwoNumbers();

          binaryOp(Operation.OPERATION_GT);

          break;
        } //OP_GREATER
        
        case OP_INHERIT: {
          Object value = vStack.peek(); //inherited object

          if (!(value instanceof LPCObject)) {
            runtimeError("Inherited object must be an LPCObject.");

            return InterpretResult.INTERPRET_RUNTIME_ERROR;
          }

          LPCObject lpcObject = (LPCObject)value;

          value = vStack.get(vStack.size() - 2); //inheriting object

          if (!(value instanceof LPCObject)) {
            runtimeError("Inheriting object must be an LPCObject.");

            return InterpretResult.INTERPRET_RUNTIME_ERROR;
          }

          LPCObject iSubObject = (LPCObject)value;

          iSubObject.inherit(lpcObject); //copies down fields and methods

          vStack.pop(); // Inheriting object.

          break;
        } //OP_INHERIT
        
        case OP_INVOKE: {
          byte op1 = frame.nextInstr(); //constants index
          byte op2 = frame.nextInstr(); //arg count
          Object constant = frame.getConstant(op1); //method name

          if (!invoke((String)constant, op2))
            return InterpretResult.INTERPRET_RUNTIME_ERROR;

          frame = fStack.peek();

          break;
        } //OP_INVOKE
        
        case OP_JUMP: {
          byte operand = frame.nextInstr(); //offset from frame ip

          frame.setIP(frame.ip() + operand);

          break;
        } //OP_JUMP
        
        case OP_JUMP_IF_FALSE: {
          byte operand = frame.nextInstr(); //offset from frame ip
          Object value = vStack.peek(); //value

          if (isFalsey(value))
            frame.setIP(frame.ip() + operand);

          break;
        } //OP_JUMP_IF_FALSE
        
        case OP_LESS: {
          if (!twoNumericOperands())
            return errorTwoNumbers();

          binaryOp(Operation.OPERATION_LT);

          break;
        } //OP_LESS
        
        case OP_LOOP: {
          byte operand = frame.nextInstr(); //offset from frame ip

          frame.setIP(frame.ip() - operand);

          break;
        } //OP_LOOP
        
        case OP_MAPPING: {
            byte operand = frame.nextInstr(); //entry count
            Map<Object, Object> map = new HashMap<Object, Object>();
            
            for (int i = 0; i < operand; i++) {
              Object val1 = vStack.pop(); //value
              Object val2 = vStack.pop(); //key
              
              map.put(val2, val1);
            }

            vStack.push(new LPCMapping(map));

            break;	
          } //OP_MAPPING

        case OP_METHOD: {
          byte operand = frame.nextInstr(); //constants index
          Object constant = frame.getConstant(operand); //method name
          Closure closure = (Closure)vStack.peek(); //closure
          LPCObject lpcObject = (LPCObject)vStack.get(vStack.size() - 2); //LPC object

          lpcObject.methods().put((String)constant, closure);

          vStack.pop(); //mMethod

          break;
        } //OP_METHOD
        
        case OP_MULTIPLY: {
          if (!twoNumericOperands())
            return errorTwoNumbers();

          binaryOp(Operation.OPERATION_MULT);

          break;
        } //OP_MULTIPLY
        
        case OP_NEGATE: {
          if (!oneNumericOperand())
            return errorOneNumber();

          Object value = vStack.pop();

          vStack.push(-(double)value);

          break;
        } //OP_NEGATE
        
        case OP_NIL: {
          vStack.push(null);
          
          break;
        } //OP_NIL
        
        case OP_NOT: {
          Object value = vStack.pop(); //value

          vStack.push(isFalsey(value));

          break;
        } //OP_NOT
        
        case OP_OBJECT: {
          byte operand = frame.nextInstr(); //constants index
          Object constant = frame.getConstant(operand); //LPC object name
          LPCObject lpcObject = new LPCObject((String)constant);

          vStack.push(lpcObject);

          break;
        } //OP_OBJECT
        
        case OP_POP: {
          vStack.pop();
          
          break;
        } //OP_POP
        
        case OP_RETURN: {
          //We're about to discard the called function's entire
          //stack window, so pop the function's return value but
          //hold onto a reference to it.
          Object value = vStack.pop(); //function return value

          closeUpvalues(frame.base());

          //pop the RunFrame for the returning function
          fStack.pop();

          if (fStack.isEmpty()) { //entire program finished
            vStack.pop();

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
        } //OP_RETURN
        
        case OP_SET_GLOBAL: {
          byte operand = frame.nextInstr(); //constants index
          Object constant = frame.getConstant(operand); //global name
          String name = (String)constant;
          Object value = vStack.peek(); //global value

          if (!globals.containsKey(name))
            return error("Undefined object '" + name + "'.");

          //Peek here, not pop; assignment is an expression,
          //so we leave value vStacked in case the assignment
          //is nested inside a larger expression.
          globals.put(name, value);

          break;
        } //OP_SET_GLOBAL
        
        case OP_SET_LOCAL: {
          byte operand = frame.nextInstr(); //offset from frame base
          Object value = vStack.peek(); //local value

          vStack.set(frame.base() + operand, value);

          break;
        } //OP_SET_LOCAL
        
        case OP_SET_PROP: {
          Object value = vStack.get(vStack.size() - 2); //LPC object

          if (!(value instanceof LPCObject)) {
            runtimeError("Only LPC Objects have fields.");

            return InterpretResult.INTERPRET_RUNTIME_ERROR;
          }

          byte operand = frame.nextInstr(); //constants index
          Object constant = frame.getConstant(operand); //field name
          String name = (String)constant;
          LPCObject lpcObject = (LPCObject)value;

          //Look for a matching field.
          if (!lpcObject.fields().containsKey(name)) {
            runtimeError("Undefined field '" + name + "'.");

            return InterpretResult.INTERPRET_RUNTIME_ERROR;
          }

          //Set the existing field to its new value.
          lpcObject.fields().put(name, vStack.peek());

          value = vStack.pop(); //new field value

          vStack.pop(); //LPCObject

          //Push new field value back on vStack.  Assignment is
          //an expression, so new field value remains stacked in
          //case the assignment is nested inside a larger expression.
          vStack.push(value);

          break;
        } //OP_SET_PROP
        
        case OP_SET_UPVAL: {
          byte operand = frame.nextInstr(); //Upvalues index
          Upvalue upvalue = frame.closure().upvalues()[operand];
          Object value = vStack.peek();

          if (upvalue.location() != -1) //i.e., open
            vStack.set(upvalue.location(), value);
          else //i.e., closed
            upvalue.setClosedValue(value);

          break;
        } //OP_SET_UPVAL
        
        case OP_SUBTRACT: {
          if (!twoNumericOperands())
            return errorTwoNumbers();

          binaryOp(Operation.OPERATION_SUBTRACT);

          break;
        } //OP_SUBTRACT
        
        case OP_SUPER_INVOKE: {
          byte op1 = frame.nextInstr(); //constants index
          byte op2 = frame.nextInstr(); //arg count
          Object constant = frame.getConstant(op1); //inherited method name
          LPCObject lpcObject = (LPCObject)vStack.popMinus(op2); //inheriting LPC object

          if (!invokeFromObject(lpcObject.superObj(), (String)constant, op2))
            return InterpretResult.INTERPRET_RUNTIME_ERROR;

          frame = fStack.peek();

          break;
        } //OP_SUPER_INVOKE
        
        case OP_TRUE: {
          vStack.push(true);
          
          break;
        } //OP_TRUE
        
        default: {
          break;
        } //default
      } //switch
    } //for(;;)
  } //run()

  //reset()
  private void reset() {
    vStack = new ObjStack();
    fStack = new Stack<>();
    openUpvalues = null;
    execCompilation = false;
  }

  //runtimeError(String, String...)
  void runtimeError(String message, String... args) {
    System.err.println("Runtime Error: " + message);

    for (String s : args)
      System.err.println(s);

    //loop through RunFrames on fStack in reverse order
    for (int i = fStack.size() - 1; i >=0; i--) {
      RunFrame frame = fStack.get(i);
      C_Compilation compilation = frame.closure().compilation();
      C_InstrList instrList = compilation.instrList();
      int line = instrList.lines().get(instrList.lines().size() - 2);

      System.err.print("[line " + line + "] in ");

      if (compilation.type() == TYPE_SCRIPT)
        System.err.print("script.\n");
      else
        System.err.print(compilation.name() + "().\n");
    }

    reset(); // vStack, fStack, openUpvalues
  }

  //defineNativeFn(String, NativeFn)
  private void defineNativeFn(String name, NativeFn nativeFn) {
    nativeFns.put(name, nativeFn);
  }

  public C_Compilation getCompilation(String path) {
    String fullPath = getLibPath() + path;
    SourceFile file  = new SourceFile(fullPath);
    C_ObjectCompiler compiler = new C_ObjectCompiler();

    return compiler.compile(
      Paths.get(file.path()),
      file.prefix(),
      file.source()
    );
  }

  //callValue(Object, int)
  private boolean callValue(Object callee, int argCount) {
	//Closure
	if (callee instanceof Closure)
      return frame((Closure)callee, argCount);
    //Native Function
    else if (callee instanceof NativeFn)
      return call((NativeFn)callee, argCount);

    runtimeError("Can only call functions and methods.");

    return false;
  }

  //frame(Closure, int)
  private boolean frame(Closure closure, int argCount) {
    if (!checkArity((C_Function)closure.compilation(), argCount))
      return false;

    int base = vStack.size() - 1 - argCount;

    fStack.push(new RunFrame(closure, base));

    return true;
  }

  //frame(C_Compilation)
  private void frame(C_Compilation compilation) {
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
  //and framing the new Closure with its argCount.
  private boolean invokeFromObject(LPCObject lpcObject, String methodName, int argCount) {
    if (!(lpcObject.methods().containsKey(methodName))) {
      runtimeError("Undefined method '" + methodName + "'.");

      return false;
    }

    Closure closure = lpcObject.methods().get(methodName);

    return frame(closure, argCount);
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
  private boolean checkArity(C_HasArity callee, int argCount) {
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
//    return Prefs.instance().getString("PATH_LIB");
    return "/Users/jonathan/lib";
  }
}
