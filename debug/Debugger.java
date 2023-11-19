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
    System.out.println(message);
  }

  //printSource(String)
  public void printSource(String source) {
    printBanner("source");

    System.out.println((source.length() == 0) ? "[ no source ]" : source);
  }

  //traceExecution(CallFrame, Map<String, Object>, Object[])
  public void traceExecution(CallFrame frame, Map<String, Object> globals, Object[] stackValues) {
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

    if (printStack) {
      System.out.print("          ");

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
    System.out.println("== " + text + " ==");
  }

  //disassembleInstruction(Chunk, int)
  public int disassembleInstruction(Chunk chunk, int offset) {
    byte instruction = getCode(chunk, offset);

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
        return constantInstruction("OP_CONSTANT", chunk, offset);
      case OP_NIL:
        return simpleInstruction("OP_NIL", offset);
      case OP_TRUE:
        return simpleInstruction("OP_TRUE", offset);
      case OP_FALSE:
        return simpleInstruction("OP_FALSE", offset);
      case OP_POP:
        return simpleInstruction("OP_POP", offset);
      case OP_GET_LOCAL:
        return wordOperandInstruction("OP_GET_LOCAL", chunk, offset);
      case OP_SET_LOCAL:
        return wordOperandInstruction("OP_SET_LOCAL", chunk, offset);
      case OP_DEFINE_GLOBAL:
        return constantInstruction("OP_DEFINE_GLOBAL", chunk, offset);
      case OP_GET_GLOBAL:
        return constantInstruction("OP_GET_GLOBAL", chunk, offset);
      case OP_SET_GLOBAL:
        return constantInstruction("OP_SET_GLOBAL", chunk, offset);
      case OP_GET_UPVALUE:
        return wordOperandInstruction("OP_GET_UPVALUE", chunk, offset);
      case OP_SET_UPVALUE:
        return wordOperandInstruction("OP_SET_UPVALUE", chunk, offset);
      case OP_GET_PROPERTY:
        return constantInstruction("OP_GET_PROPERTY", chunk, offset);
      case OP_SET_PROPERTY:
        return constantInstruction("OP_SET_PROPERTY", chunk, offset);
      case OP_GET_SUPER:
        return constantInstruction("OP_GET_SUPER", chunk, offset);
      case OP_EQUAL:
        return simpleInstruction("OP_EQUAL", offset);
      case OP_GREATER:
        return simpleInstruction("OP_GREATER", offset);
      case OP_LESS:
        return simpleInstruction("OP_LESS", offset);
      case OP_ADD:
        return simpleInstruction("OP_ADD", offset);
      case OP_SUBTRACT:
        return simpleInstruction("OP_SUBTRACT", offset);
      case OP_MULTIPLY:
        return simpleInstruction("OP_MULTIPLY", offset);
      case OP_DIVIDE:
        return simpleInstruction("OP_DIVIDE", offset);
      case OP_NOT:
        return simpleInstruction("OP_NOT", offset);
      case OP_NEGATE:
        return simpleInstruction("OP_NEGATE", offset);
      case OP_JUMP:
        return jumpInstruction("OP_JUMP", 1, chunk, offset);
      case OP_JUMP_IF_FALSE:
        return jumpInstruction("OP_JUMP_IF_FALSE", 1, chunk, offset);
      case OP_LOOP:
        return jumpInstruction("OP_LOOP", -1, chunk, offset);
      case OP_CALL:
        return byteOperandInstruction("OP_CALL", chunk, offset, "# args:");
      case OP_INVOKE:
        return invokeInstruction("OP_INVOKE", chunk, offset);
      case OP_SUPER_INVOKE:
        return invokeInstruction("OP_SUPER_INVOKE", chunk, offset);
      case OP_CLOSURE:
        return closureInstruction("OP_CLOSURE", chunk, offset);
      case OP_CLOSE_UPVALUE:
        return simpleInstruction("OP_CLOSE_UPVALUE", offset);
      case OP_RETURN:
        return simpleInstruction("OP_RETURN", offset);
      case OP_INHERIT:
        return simpleInstruction("OP_INHERIT", offset);
      case OP_OBJECT:
        return constantInstruction("OP_OBJECT", chunk, offset);
      case OP_FIELD:
        return constantInstruction("OP_FIELD", chunk, offset);
      case OP_METHOD:
        return constantInstruction("OP_METHOD", chunk, offset);
      default:
        System.out.println("Unknown opcode: " + instruction);

        return offset + 1;
    }
  }

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
