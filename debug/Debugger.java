package jbLPC.debug;

import java.util.Map;
import java.util.stream.Collectors;

import jbLPC.compiler.C_Function;
import jbLPC.compiler.Chunk;
import jbLPC.compiler.Instruction;
import jbLPC.compiler.OpCode;
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
  private boolean printCode;
  private boolean printStack;

  private static Debugger _instance; //singleton

  private static final String COLOR_RESET = "\033[0m";
  private static final String COLOR_RED = "\033[31m";
  private static final String COLOR_GREEN = "\033[32m";
  private static final String COLOR_CYAN = "\033[36m";

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
      System.out.print(Debugger.COLOR_CYAN + "Globals: ");

      System.out.print(
        globals.entrySet()
          .stream()
          .filter(item -> !(item.getValue() instanceof NativeFn))
          .collect(Collectors.toList())
      );

      System.out.print(Debugger.COLOR_RESET + "\n");
    }

    //Print Stack values array
    if (printStack) {
      System.out.print(Debugger.COLOR_GREEN + "Stack: ");

      for (int i = 0; i < stackValues.length; i++) {
        Object stackValue = stackValues[i];

        if (stackValue instanceof String)
          System.out.print("[ \"" + stackValue + "\" ]");
        else
          System.out.print("[ " + stackValue + " ]");

      }

      System.out.print(Debugger.COLOR_RESET + "\n");
    } //if (printStack)

    disassembleInstruction(frame.closure().compilation().chunk(), frame.ip());
  }

  //disassembleScope(Scope)
  public void disassembleScope(Scope scope) {
    Chunk chunk = scope.compilation().chunk();

    printBanner(scope.compilation().toString());

    if (printCodes) {
      System.out.println("Codes: " + COLOR_RED + chunk.instructions() + COLOR_RESET);

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

    for (Instruction instr : chunk.instructions())
      disassembleInstruction(instr);
  }

  public void printBanner(String text) {
    System.out.print("\n");
    System.out.println("== " + text + " ==");
  }

  //disassembleInstruction(Instruction)
  public void disassembleInstruction(Instruction instr) {
    //OpCode offset
//    System.out.print(String.format("%04d", offset));

    //Line number
//    if (
//      (offset > 0) &&
//      (chunk.lines().get(offset) == chunk.lines().get(offset - 1))
//    )
//      System.out.print("   | ");
//    else
//      System.out.print(String.format("%4d ", chunk.lines().get(offset)));
    System.out.print(String.format("%4d", instr.line()));

    //Code
    if (printCode)
      System.out.print("(" + COLOR_RED + String.format("%02d", instr.opCode().code()) + COLOR_RESET + ") ");

    switch (instr.opCode()) {
      case OP_CONST:
        constantInstruction(instr); break;
      case OP_NIL:
        simpleInstruction(opCode, offset); break;
      case OP_TRUE:
        simpleInstruction(opCode, offset); break;
      case OP_FALSE:
        simpleInstruction(opCode, offset); break;
      case OP_POP:
        simpleInstruction(opCode, offset); break;
      case OP_GET_LOCAL:
        wordOperandInstruction(opCode, chunk, offset); break;
      case OP_SET_LOCAL:
        wordOperandInstruction(opCode, chunk, offset); break;
      case OP_GLOBAL:
        constantInstruction(opCode, chunk, offset); break;
      case OP_GET_GLOBAL:
        constantInstruction(opCode, chunk, offset); break;
      case OP_SET_GLOBAL:
        constantInstruction(opCode, chunk, offset); break;
      case OP_GET_UPVAL:
        wordOperandInstruction(opCode, chunk, offset); break;
      case OP_SET_UPVAL:
        wordOperandInstruction(opCode, chunk, offset); break;
      case OP_GET_PROP:
        constantInstruction(opCode, chunk, offset); break;
      case OP_SET_PROP:
        constantInstruction(opCode, chunk, offset); break;
      case OP_GET_SUPER:
        constantInstruction(opCode, chunk, offset); break;
      case OP_EQUAL:
        simpleInstruction(opCode, offset); break;
      case OP_GREATER:
        simpleInstruction(opCode, offset); break;
      case OP_LESS:
        simpleInstruction(opCode, offset); break;
      case OP_ADD:
        simpleInstruction(opCode, offset); break;
      case OP_SUBTRACT:
        simpleInstruction(opCode, offset); break;
      case OP_MULTIPLY:
        simpleInstruction(opCode, offset); break;
      case OP_DIVIDE:
        simpleInstruction(opCode, offset); break;
      case OP_NOT:
        simpleInstruction(opCode, offset); break;
      case OP_NEGATE:
        simpleInstruction(opCode, offset); break;
      case OP_JUMP:
        jumpInstruction(opCode, 1, chunk, offset); break;
      case OP_JUMP_IF_FALSE:
        jumpInstruction(opCode, 1, chunk, offset); break;
      case OP_LOOP:
        jumpInstruction(opCode, -1, chunk, offset); break;
      case OP_CALL:
        byteOperandInstruction(opCode, chunk, offset); break;
      case OP_INVOKE:
        invokeInstruction(opCode, chunk, offset); break;
      case OP_SUPER_INVOKE:
        invokeInstruction(opCode, chunk, offset); break;
      case OP_CLOSURE:
        closureInstruction(opCode, chunk, offset); break;
      case OP_CLOSE_UPVAL:
        simpleInstruction(opCode, offset); break;
      case OP_RETURN:
        simpleInstruction(opCode, offset); break;
      case OP_INHERIT:
        simpleInstruction(opCode, offset); break;
      case OP_OBJECT:
        constantInstruction(opCode, chunk, offset); break;
      case OP_FIELD:
        constantInstruction(opCode, chunk, offset); break;
      case OP_METHOD:
        constantInstruction(opCode, chunk, offset); break;
      case OP_COMPILE:
        constantInstruction(opCode, chunk, offset); break;
      default:
        System.out.println("Unknown opcode: " + String.format("0x%02X", opCode));
        
        offset + 1;
        
        break;
    }
  }

  //closureInstruction(OpCode, Chunk, int)
  private int closureInstruction(OpCode opCode, Chunk chunk, int offset) {
    Integer operand = getOperand(chunk, offset);

    System.out.print(String.format("%-16s constant: %d ", opCode, operand));

    C_Function function = (C_Function)chunk.constants().get(operand);

    System.out.println(function);

    offset += 3;

    for (int j = 0; j < function.upvalueCount(); j++) {
      boolean isLocal = (getCode(chunk, offset++) != 0);
      Integer index = getCode(chunk, offset++);

      System.out.print(String.format(
        "%04d      |                     %s %d\n",
        offset - 2, isLocal ? "local" : "upvalue", index
      ));
    }

    return offset;
  }

  //constantInstruction(OpCode, Chunk, int)
  private int constantInstruction(OpCode opCode, Chunk chunk, int offset) {
    Integer operand = getOperand(chunk, offset);

    Object constant = chunk.constants().get(operand);

    System.out.print(String.format("%-16s ", opCode));

    if (constant instanceof String)
      System.out.print("\"" + constant + "\" ");
    else
      System.out.print("" + constant + " ");

    System.out.print(String.format("<==slot: " + COLOR_RED + "%02d" + COLOR_RESET + "\n", operand));

    return offset + 3;
  }

  //invokeInstruction(OpCode, Chunk, int)
  private int invokeInstruction(OpCode opCode, Chunk chunk, int offset) {
    Integer operand = getOperand(chunk, offset);
    Object constant = chunk.constants().get(operand); //method name
    Integer argCount = getCode(chunk, offset + 3);

    //instruction name, constant index
    System.out.print(
      String.format("%-16s constant: %d ",
      opCode,
      operand)
    );

    //method name
    System.out.print("(\"" + constant + "\")");

    //argument count
    System.out.print(String.format(" (%d args)\n", argCount));

    return offset + 4;
  }

  //simpleInstruction(OpCode, int)
  private int simpleInstruction(OpCode opCode, int offset) {
    System.out.println(String.format("%-16s", opCode));

    return offset + 1;
  }

  //byteOperandInstruction(OpCode, Chunk, int, String)
  private int byteOperandInstruction(OpCode opCode, Chunk chunk, int offset) {
    Integer operand = getOperand(chunk, offset);

    System.out.println(String.format("%-16s %s %d", opCode, operand));

    return offset + 2;
  }

  //wordOperandInstruction(OpCode, Chunk, int)
  private int wordOperandInstruction(OpCode opCode, Chunk chunk, int offset) {
    Integer operand = getOperand(chunk, offset);

    System.out.print(String.format("%-16s %4d\n", opCode, operand));

    return offset + 3;
  }

  //jumpInstruction(OpCode, int, Chunk, int)
  private int jumpInstruction(OpCode opCode, int sign, Chunk chunk, int offset) {
    Integer operand = getOperand(chunk, offset);

    System.out.print(String.format("%-16s %4d -> %d\n",
      opCode, offset, offset + 3 + (sign * operand)));

    return offset + 3;
  }

  //getCode(Chunk, int)
  private Instruction getCode(Chunk chunk, int offset) {
    return chunk.instructions().get(offset);
  }

  //getOperand(Chunk, int)
  private Instruction getOperand(Chunk chunk, int offset) {
    return chunk.instructions().get(offset + 1);
  }

  //updateCachedProperties()
  private void updateCachedProperties() {
    printCodes = Props.instance().getBool("DEBUG_CODES");
    printConstants = Props.instance().getBool("DEBUG_CONSTS");
    printGlobals = Props.instance().getBool("DEBUG_GLOBALS");
    printLines = Props.instance().getBool("DEBUG_LINES");
    printLocals = Props.instance().getBool("DEBUG_LOCALS");
    printUpvalues = Props.instance().getBool("DEBUG_UPVALS");
    printCode = Props.instance().getBool("DEBUG_CODE");
    printStack = Props.instance().getBool("DEBUG_STACK");
  }

  //notifyPropertiesChanged()
  public void notifyPropertiesChanged() {
    updateCachedProperties();
  }
}
