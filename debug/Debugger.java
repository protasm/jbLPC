package jbLPC.debug;

import static jbLPC.compiler.OpCode.OP_ADD;
import static jbLPC.compiler.OpCode.OP_CALL;
import static jbLPC.compiler.OpCode.OP_CLOSE_UPVALUE;
import static jbLPC.compiler.OpCode.OP_CLOSURE;
import static jbLPC.compiler.OpCode.OP_CONSTANT;
import static jbLPC.compiler.OpCode.OP_DEFINE_GLOBAL;
import static jbLPC.compiler.OpCode.OP_DIVIDE;
import static jbLPC.compiler.OpCode.OP_EQUAL;
import static jbLPC.compiler.OpCode.OP_FALSE;
import static jbLPC.compiler.OpCode.OP_FIELD;
import static jbLPC.compiler.OpCode.OP_GET_GLOBAL;
import static jbLPC.compiler.OpCode.OP_GET_LOCAL;
import static jbLPC.compiler.OpCode.OP_GET_PROPERTY;
import static jbLPC.compiler.OpCode.OP_GET_SUPER;
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

import java.util.Map;
import java.util.stream.Collectors;

import jbLPC.compiler.C_Function;
import jbLPC.compiler.Chunk;
import jbLPC.compiler.Scope;
import jbLPC.nativefn.NativeFn;
import jbLPC.util.Props;
import jbLPC.util.PropsObserver;
import jbLPC.vm.CallFrame;

public class Debugger implements PropsObserver {
  //Cached properties
  private boolean printCodes;
  private boolean printConstants;
  private boolean printGlobals;
  private boolean printLines;
  private boolean printLocals;
  private boolean printUpvalues;
  private boolean printOpCode;
  private boolean printStack;

  static private Debugger _instance; //singleton

  //Debugger()
  private Debugger() {
    Props.instance().registerObserver(this);
  }

  //instance()
  public static Debugger instance() {
    if (_instance == null) {
      //critical section
      synchronized(Debugger.class) {
        if (_instance == null)
          _instance = new Debugger();
      }
    }

    return _instance;
  }

  //printProgress(String)
  public void printProgress(String message) {
    System.out.print("\n===");
    System.out.print(message.toUpperCase());
    System.out.print("===\n");
  }

  //printSource(String)
  public void printSource(String source) {
    printBanner("source");

    System.out.println((source.length() == 0) ? "[ no source ]" : source);
  }

  //traceExecution(CallFrame, Map<String, Object>, Object[])
  public void traceExecution(CallFrame frame, Map<String, Object> globals, Object[] stackValues) {
    System.out.print("\n");
    
    //Print Globals array
    if (printGlobals) {
      System.out.print("Globals: ");

      System.out.print(
        globals.entrySet()
               .stream()
               .filter(item -> !(item.getValue() instanceof NativeFn))
               .collect(Collectors.toList())
      );

      System.out.print("\n");
    }

    //Print Stack values array
    if (printStack) {
      System.out.print("Stack: ");

      for (int i = 0; i < stackValues.length; i++) {
        Object stackValue = stackValues[i];

        if (stackValue instanceof String)
          System.out.print("[ \"" + stackValue + "\" ]");
        else
          System.out.print("[ " + stackValue + " ]");

      }

      System.out.print("\n");
    } //if (printStack)

    disassembleInstruction(frame.closure().compilation().chunk(), frame.ip());
  }

  //disassembleScope(Scope)
  public void disassembleScope(Scope scope) {
    Chunk chunk = scope.compilation().chunk();

    printBanner(scope.compilation().toString());

    if (printCodes) {
      System.out.println("Codes: " + chunk.printCodes());

      if (printLines)
        System.out.println("Lines: " + chunk.lines());
    }

    if (printConstants) {
      System.out.print("Constants: [ ");

      Object[] constantValues = chunk.constants().toArray();

      for (int i = 0; i < constantValues.length; i++) {
        Object constantValue = constantValues[i];

        if (constantValue instanceof String)
          System.out.print("\"" + constantValue + "\"");
        else
          System.out.print(constantValue);

        if (i < constantValues.length - 1)
          System.out.print(", ");
      }

      System.out.print(" ]\n");
    }

    if (printLocals)
      System.out.println("Locals: " + scope.locals());

    if (printUpvalues)
      System.out.println("Upvalues: " + scope.upvalues());

    for (int offset = 0; offset < chunk.opCodes().size();)
      offset = disassembleInstruction(chunk, offset);
  }

  public void printBanner(String text) {
    System.out.print("\n");
    System.out.println("== " + text + " ==");
  }

  //disassembleInstruction(Chunk, int)
  public int disassembleInstruction(Chunk chunk, int offset) {
    byte instruction = getCode(chunk, offset);
    int result;
    
    System.out.print("Instr: ");

    System.out.print(String.format("%04d", offset));

    if (
      (offset > 0) &&
      (chunk.lines().get(offset) == chunk.lines().get(offset - 1))
    )
      System.out.print("   | ");
    else
      System.out.print(String.format("%4d ", chunk.lines().get(offset)));

    if (printOpCode)
      System.out.print("(" + String.format("0x%02X", instruction) + ") ");

    switch (instruction) {
      case OP_CONSTANT:
        result = constantInstruction("OP_CONSTANT", chunk, offset); break;
      case OP_NIL:
        result = simpleInstruction("OP_NIL", offset); break;
      case OP_TRUE:
        result = simpleInstruction("OP_TRUE", offset); break;
      case OP_FALSE:
        result = simpleInstruction("OP_FALSE", offset); break;
      case OP_POP:
        result = simpleInstruction("OP_POP", offset); break;
      case OP_GET_LOCAL:
        result = wordOperandInstruction("OP_GET_LOCAL", chunk, offset); break;
      case OP_SET_LOCAL:
        result = wordOperandInstruction("OP_SET_LOCAL", chunk, offset); break;
      case OP_DEFINE_GLOBAL:
        result = constantInstruction("OP_DEFINE_GLOBAL", chunk, offset); break;
      case OP_GET_GLOBAL:
        result = constantInstruction("OP_GET_GLOBAL", chunk, offset); break;
      case OP_SET_GLOBAL:
        result = constantInstruction("OP_SET_GLOBAL", chunk, offset); break;
      case OP_GET_UPVALUE:
        result = wordOperandInstruction("OP_GET_UPVALUE", chunk, offset); break;
      case OP_SET_UPVALUE:
        result = wordOperandInstruction("OP_SET_UPVALUE", chunk, offset); break;
      case OP_GET_PROPERTY:
        result = constantInstruction("OP_GET_PROPERTY", chunk, offset); break;
      case OP_SET_PROPERTY:
        result = constantInstruction("OP_SET_PROPERTY", chunk, offset); break;
      case OP_GET_SUPER:
        result = constantInstruction("OP_GET_SUPER", chunk, offset); break;
      case OP_EQUAL:
        result = simpleInstruction("OP_EQUAL", offset); break;
      case OP_GREATER:
        result = simpleInstruction("OP_GREATER", offset); break;
      case OP_LESS:
        result = simpleInstruction("OP_LESS", offset); break;
      case OP_ADD:
        result = simpleInstruction("OP_ADD", offset); break;
      case OP_SUBTRACT:
        result = simpleInstruction("OP_SUBTRACT", offset); break;
      case OP_MULTIPLY:
        result = simpleInstruction("OP_MULTIPLY", offset); break;
      case OP_DIVIDE:
        result = simpleInstruction("OP_DIVIDE", offset); break;
      case OP_NOT:
        result = simpleInstruction("OP_NOT", offset); break;
      case OP_NEGATE:
        result = simpleInstruction("OP_NEGATE", offset); break;
      case OP_JUMP:
        result = jumpInstruction("OP_JUMP", 1, chunk, offset); break;
      case OP_JUMP_IF_FALSE:
        result = jumpInstruction("OP_JUMP_IF_FALSE", 1, chunk, offset); break;
      case OP_LOOP:
        result = jumpInstruction("OP_LOOP", -1, chunk, offset); break;
      case OP_CALL:
        result = byteOperandInstruction("OP_CALL", chunk, offset, "# args:"); break;
      case OP_INVOKE:
        result = invokeInstruction("OP_INVOKE", chunk, offset); break;
      case OP_SUPER_INVOKE:
        result = invokeInstruction("OP_SUPER_INVOKE", chunk, offset); break;
      case OP_CLOSURE:
        result = closureInstruction("OP_CLOSURE", chunk, offset); break;
      case OP_CLOSE_UPVALUE:
        result = simpleInstruction("OP_CLOSE_UPVALUE", offset); break;
      case OP_RETURN:
        result = simpleInstruction("OP_RETURN", offset); break;
      case OP_INHERIT:
        result = simpleInstruction("OP_INHERIT", offset); break;
      case OP_OBJECT:
        result = constantInstruction("OP_OBJECT", chunk, offset); break;
      case OP_FIELD:
        result = constantInstruction("OP_FIELD", chunk, offset); break;
      case OP_METHOD:
        result = constantInstruction("OP_METHOD", chunk, offset); break;
      default:
        System.out.println("Unknown opcode: " + instruction);
        
        result = offset + 1;
        
        break;
    }
    
    return result;
  } //disassembleInstruction(Chunk, int)

  //closureInstruction(String, Chunk, int)
  private int closureInstruction(String name, Chunk chunk, int offset) {
    short operand = getWordOperand(chunk, offset);

    System.out.print(String.format("%-16s constant: %d ", name, operand));

    C_Function function = (C_Function)chunk.constants().get(operand);

    System.out.println(function);

    offset += 3;

    for (int j = 0; j < function.upvalueCount(); j++) {
      boolean isLocal = (getCode(chunk, offset++) != 0);
      byte index = getCode(chunk, offset++);

      System.out.print(String.format(
        "%04d      |                     %s %d\n",
        offset - 2, isLocal ? "local" : "upvalue", index
      ));
    }

    return offset;
  }

  //constantInstruction(String, Chunk, int)
  private int constantInstruction(String name, Chunk chunk, int offset) {
    short operand = getWordOperand(chunk, offset);

    System.out.print(String.format("%-16s constant: %d ", name, operand));

    Object constant = chunk.constants().get(operand);

    if (constant instanceof String)
      System.out.print("(\"" + constant + "\")\n");
    else
      System.out.print("(" + constant + ")\n");

    return offset + 3;
  }

  //invokeInstruction(String, Chunk, int)
  private int invokeInstruction(String name, Chunk chunk, int offset) {
    short operand = getWordOperand(chunk, offset);
    Object constant = chunk.constants().get(operand);
    byte argCount = getCode(chunk, offset + 3);

    System.out.print(String.format("%-16s constant: %d ", name, operand));

    if (constant instanceof String)
      System.out.print("(\"" + constant + "\")");
    else
      System.out.print("(" + constant + ")");

    System.out.print(String.format(" (%d args)\n", argCount));

    return offset + 4;
  }

  //simpleInstruction(String, int)
  private int simpleInstruction(String name, int offset) {
    System.out.println(String.format("%-16s", name));

    return offset + 1;
  }

  //byteOperandInstruction(String, Chunk, int, String)
  private int byteOperandInstruction(String name, Chunk chunk, int offset, String hint) {
    byte operand = getByteOperand(chunk, offset);

    System.out.print(String.format("%-16s %s %d ", name, hint, operand));
    System.out.print("\n");

    return offset + 2;
  }

  //wordOperandInstruction(String, Chunk, int)
  private int wordOperandInstruction(String name, Chunk chunk, int offset) {
    short operand = getWordOperand(chunk, offset);

    System.out.print(String.format("%-16s %4d\n", name, operand));

    return offset + 3;
  }

  //jumpInstruction(String, int, Chunk, int)
  private int jumpInstruction(String name, int sign, Chunk chunk, int offset) {
    short operand = getWordOperand(chunk, offset);

    System.out.print(String.format("%-16s %4d -> %d\n",
      name, offset, offset + 3 + (sign * operand)));

    return offset + 3;
  }

  //getCode(Chunk, int)
  private byte getCode(Chunk chunk, int offset) {
    return chunk.opCodes().get(offset);
  }

  //getByteOperand(Chunk, int)
  private byte getByteOperand(Chunk chunk, int offset) {
    return chunk.opCodes().get(offset + 1);
  }

  //getWordOperand(Chunk, int)
  private short getWordOperand(Chunk chunk, int offset) {
    byte hi = (byte)chunk.opCodes().get(offset + 1);
    byte lo = (byte)chunk.opCodes().get(offset + 2);

    return (short)(((hi & 0xFF) << 8) | (lo & 0xFF));
  }

  //updateCachedProperties()
  private void updateCachedProperties() {
    printCodes = Props.instance().getBool("DEBUG_CODES");
    printConstants = Props.instance().getBool("DEBUG_CONSTS");
    printGlobals = Props.instance().getBool("DEBUG_GLOBALS");
    printLines = Props.instance().getBool("DEBUG_LINES");
    printLocals = Props.instance().getBool("DEBUG_LOCALS");
    printUpvalues = Props.instance().getBool("DEBUG_UPVALS");
    printOpCode = Props.instance().getBool("DEBUG_OPCODE");
    printStack = Props.instance().getBool("DEBUG_STACK");
  }

  //notifyPropertiesChanged()
  public void notifyPropertiesChanged() {
    updateCachedProperties();
  }
}
