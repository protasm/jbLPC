package jbLPC.debug;

import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

import jbLPC.compiler.C_Function;
import jbLPC.compiler.Compilation;
import jbLPC.compiler.Instruction;
import jbLPC.compiler.Scope;
import jbLPC.nativefn.NativeFn;
import jbLPC.util.Prefs;

public class Debugger {
  private static Debugger _instance; //singleton

  private static final String COLOR_RESET = "\033[0m";
  private static final String COLOR_RED = "\033[31m";
  private static final String COLOR_GREEN = "\033[32m";
  private static final String COLOR_CYAN = "\033[36m";

  //instance()
  public static Debugger instance() {
    if (_instance == null) {
      synchronized(Debugger.class) {
        if (_instance == null)
          _instance = new Debugger();
      }
    }

    return _instance;
  }

  //printBanner(String)
  public void printBanner(String text) {
    System.out.print("\n");
    System.out.println("== " + text + " ==");
  }

  //printProgress(String)
  public void printProgress(String message) {
	if (!Prefs.instance().getBoolean("prog")) return;

    System.out.print("\n===");
    System.out.print(message.toUpperCase());
    System.out.print("===\n");
  }

  //printSource(String)
  public void printSource(String source) {
    if (!Prefs.instance().getBoolean("source")) return;

    printBanner("source");

    System.out.println((source.length() == 0) ? "[ no source ]" : source);
  }

  //traceCompilation(Scope)
  public void traceCompilation(Scope scope) {
	if (!Prefs.instance().getBoolean("comp")) return;
	
	Compilation compilation = scope.compilation();

    printBanner(compilation.toString());

    //codes
    if (Prefs.instance().getBoolean("codes")) {
      System.out.println("Codes: " + COLOR_RED + compilation.instructions() + COLOR_RESET);
    }

    //locals
    if (Prefs.instance().getBoolean("locals"))
      System.out.println("Locals: " + scope.locals());

//    if (Prefs.instance().getBoolean("upvals"))
//      System.out.println("Upvalues: " + scope.upvalues());

    for (Instruction instr : compilation.instructions())
      disassembleInstruction(instr);
  }

  //traceExecution(Instruction, Map<String, Object>, Stack<Object>)
  public void traceExecution(Instruction instr, Map<String, Object> globals, Stack<Object> vStack) {
    if (!Prefs.instance().getBoolean("exec")) return;

    System.out.print("\n");
    
    //globals
    if (Prefs.instance().getBoolean("globals")) {
      System.out.print(Debugger.COLOR_CYAN);
      System.out.print("Globals: ");

      System.out.print(
        globals.entrySet()
          .stream()
          .filter(item -> !(item.getValue() instanceof NativeFn))
          .collect(Collectors.toList())
      );

      System.out.print(Debugger.COLOR_RESET + "\n");
    }

    //vStack
    if (Prefs.instance().getBoolean("stack")) {
      System.out.print(Debugger.COLOR_GREEN + "Stack: ");

//      for (Object value : vStack) {
//        if (value instanceof String)
//          System.out.print("[ \"" + value + "\" ]");
//        else
//          System.out.print("[ " + value + " ]");
//
//      }
      System.out.print(vStack);

      System.out.print(Debugger.COLOR_RESET);
      System.out.println("\n");
    }

    //disassemble instruction
    disassembleInstruction(instr);
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
    System.out.print(String.format("%4d ", instr.line()));

    //integer value of opCode
    if (Prefs.instance().getBoolean("rawcode"))
      System.out.print("(" + COLOR_RED + String.format("%02d", instr.opCode().code()) + COLOR_RESET + ") ");
    
    printOpCode(instr);

    switch (instr.opCode()) {
      case OP_CONST:
        constantInstruction(instr); break;
      case OP_NIL:
        simpleInstruction(instr); break;
      case OP_TRUE:
        simpleInstruction(instr); break;
      case OP_FALSE:
        simpleInstruction(instr); break;
      case OP_POP:
        simpleInstruction(instr); break;
      case OP_GET_LOCAL:
        oneOperandInstruction(instr); break;
      case OP_SET_LOCAL:
        oneOperandInstruction(instr); break;
      case OP_GLOBAL:
        constantInstruction(instr); break;
      case OP_GET_GLOBAL:
        constantInstruction(instr); break;
      case OP_SET_GLOBAL:
        constantInstruction(instr); break;
      case OP_GET_UPVAL:
        oneOperandInstruction(instr); break;
      case OP_SET_UPVAL:
        oneOperandInstruction(instr); break;
      case OP_GET_PROP:
        constantInstruction(instr); break;
      case OP_SET_PROP:
        constantInstruction(instr); break;
      case OP_GET_SUPER:
        constantInstruction(instr); break;
      case OP_EQUAL:
        simpleInstruction(instr); break;
      case OP_GREATER:
        simpleInstruction(instr); break;
      case OP_LESS:
        simpleInstruction(instr); break;
      case OP_ADD:
        simpleInstruction(instr); break;
      case OP_SUBTRACT:
        simpleInstruction(instr); break;
      case OP_MULTIPLY:
        simpleInstruction(instr); break;
      case OP_DIVIDE:
        simpleInstruction(instr); break;
      case OP_NOT:
        simpleInstruction(instr); break;
      case OP_NEGATE:
        simpleInstruction(instr); break;
      case OP_JUMP:
        jumpInstruction(instr, 1); break;
      case OP_JUMP_IF_FALSE:
        jumpInstruction(instr, 1); break;
      case OP_LOOP:
        jumpInstruction(instr, -1); break;
      case OP_CALL:
        oneOperandInstruction(instr); break;
      case OP_INVOKE:
        invokeInstruction(instr); break;
      case OP_SUPER_INVOKE:
        invokeInstruction(instr); break;
      case OP_CLOSURE:
        closureInstruction(instr); break;
      case OP_CLOSE_UPVAL:
        simpleInstruction(instr); break;
      case OP_RETURN:
        simpleInstruction(instr); break;
      case OP_INHERIT:
        simpleInstruction(instr); break;
      case OP_OBJECT:
        constantInstruction(instr); break;
      case OP_FIELD:
        constantInstruction(instr); break;
      case OP_METHOD:
        constantInstruction(instr); break;
      case OP_COMPILE:
        constantInstruction(instr); break;
      default:
        System.out.print("Unknown opcode: ");
          printOpCode(instr, true);
        
        break;
    }
  }

  //closureInstruction(Instruction)
  private void closureInstruction(Instruction instr) {
	C_Function function = (C_Function)instr.operands()[0];

    System.out.println(function);

//    for (int j = 0; j < function.upvalueCount(); j++) {
//      boolean isLocal = (getCode(chunk, offset++) != 0);
//      Integer index = getCode(chunk, offset++);
//
//      System.out.print(String.format(
//        "%04d      |                     %s %d\n",
//        offset - 2, isLocal ? "local" : "upvalue", index
//      ));
//    }
  }

  //constantInstruction(Instruction)
  private void constantInstruction(Instruction instr) {
    Object constant = instr.operands()[0];

    if (constant instanceof String)
      System.out.println("\"" + constant + "\"");
    else
      System.out.println("\"" + constant + "\"");
  }

  //invokeInstruction(Instruction)
  private void invokeInstruction(Instruction instr) {
    String identifier = (String)instr.operands()[0]; //method name
    int argCount = (int)instr.operands()[1];

    //method name
    System.out.print("(\"" + identifier + "\")");

    //argument count
    System.out.println(String.format(" (%d args)\n", argCount));
  }

  //simpleInstruction(Instruction)
  private void simpleInstruction(Instruction instr) {
   System.out.println("");
  }
  
  //oneOperandInstruction(Instruction)
  private void oneOperandInstruction(Instruction instr) {
    System.out.println("TODO: finish oneOperandInstruction in Debugger class!");
  }

  //jumpInstruction(Instruction, int)
  private void jumpInstruction(Instruction instr, int sign) {
//	int offset = (int)instr.operands()[0];

    System.out.println("TODO: finish jumpInstruction in Debugger class!");
//    System.out.print(String.format("%-16s %4d -> %d\n",
//      instr.opCode(), offset, offset + 3 + (sign * operand)));
  }
  
  //printOpCode(Instruction)
  private void printOpCode(Instruction instr) {
    printOpCode(instr, false);
  }
  
  //printOpCode(Instruction, boolean)
  private void printOpCode(Instruction instr, boolean EOL) {
    System.out.print(String.format("%-16s ", instr.opCode()));
    
    if (EOL) System.out.print("\n");
  }
}
