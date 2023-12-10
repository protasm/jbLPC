package jbLPC.debug;

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
import static jbLPC.compiler.C_OpCode.OP_GET_ARR_ELEM;
import static jbLPC.compiler.C_OpCode.OP_GET_GLOBAL;
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

import java.util.List;
import java.util.Map;

import jbLPC.compiler.C_Compilation;
import jbLPC.compiler.C_Function;
import jbLPC.compiler.C_InstrList;
import jbLPC.compiler.C_Scope;
import jbLPC.util.Prefs;
import jbLPC.util.ObjStack;
import jbLPC.vm.RunFrame;

public class Debugger {
  private static Debugger _instance; //singleton

  private static final String COLOR_RESET = "\033[0m";
//  private static final String COLOR_RED = "\033[31m";
  private static final String COLOR_GREEN = "\033[32m";
  private static final String COLOR_YELLOW = "\033[33m";
//  private static final String COLOR_BLUE = "\033[34m";
  private static final String COLOR_MAGENTA = "\033[35m";
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

  //disassembleScope(C_Scope)
  public void disassembleScope(C_Scope scope) {
	if (!Prefs.instance().getBoolean("comp")) return;

	C_Compilation compilation = scope.compilation();
	C_InstrList instrList = compilation.instrList();
	List<Byte> codes = instrList.codes();

    printBanner(compilation.toString());

    //codes
    if (Prefs.instance().getBoolean("codes")) {
      System.out.print("Codes: ");
      System.out.print(COLOR_MAGENTA);
      System.out.print(instrList.printCodes());
      System.out.println(COLOR_RESET);
    }

    //locals
    if (Prefs.instance().getBoolean("locals")) {
      System.out.print("Locals: ");
      System.out.println(scope.locals());
    }

    //upvalues
    if (Prefs.instance().getBoolean("upvals")) {
      System.out.print("Upvalues: ");
      System.out.println(scope.upvalues());
    }

    for (int index = 0; index < codes.size();)
      index = disassembleInstruction(instrList, index);
  }

  //traceExecution(RunFrame, Map<String, Object>, Stack<Object>)
  public void traceExecution(RunFrame frame, Map<String, Object> globals, ObjStack vStack) {
    if (!Prefs.instance().getBoolean("exec")) return;

    System.out.print("\n");

    //globals
    if (Prefs.instance().getBoolean("globals")) {
      System.out.print(Debugger.COLOR_CYAN);
      System.out.print("Globals: ");
      System.out.print(globals);
      System.out.print(Debugger.COLOR_RESET);
      System.out.print("\n");
    }

    //vStack
    if (Prefs.instance().getBoolean("stack")) {
      System.out.print(Debugger.COLOR_GREEN + "Stack: ");
      System.out.print(vStack);
      System.out.print(Debugger.COLOR_RESET);
      System.out.print("\n");
    }

    //instruction
    disassembleInstruction(frame.closure().compilation().instrList(), frame.ip() - 1);
  }

  //disassembleInstruction(C_InstrList, int)
  public int disassembleInstruction(C_InstrList instrList, int index) {
    byte instruction = getCode(instrList, index);

    System.out.print(COLOR_YELLOW);

    System.out.print(String.format("%04d", index));

    if (
      (index > 0) &&
      (instrList.lines().get(index) == instrList.lines().get(index - 1))
    )
      System.out.print("   | ");
    else
      System.out.print(String.format("%4d ", instrList.lines().get(index)));

    if (Prefs.instance().getBoolean("opcode")) {
      System.out.print(COLOR_MAGENTA);
      System.out.print("(" + String.format("%02X", instruction) + ") ");
      System.out.print(COLOR_YELLOW);
    }

    switch (instruction) {
      case OP_ADD:
        index = simpleInstruction("OP_ADD", index); break;
      case OP_ARRAY:
        index = operandInstruction("OP_ARRAY", instrList, index, "# of elems"); break;
      case OP_CALL:
        index = operandInstruction("OP_CALL", instrList, index, "# of args"); break;
      case OP_CLOSE_UPVAL:
        index = simpleInstruction("OP_CLOSE_UPVAL", index); break;
      case OP_CLOSURE:
        index = closureInstruction("OP_CLOSURE", instrList, index); break;
      case OP_COMPILE:
    	  index = constantInstruction("OP_COMPILE", instrList, index); break;
      case OP_CONSTANT:
        index = constantInstruction("OP_CONSTANT", instrList, index); break;
      case OP_DEF_GLOBAL:
        index = constantInstruction("OP_DEF_GLOBAL", instrList, index); break;
      case OP_DIVIDE:
        index = simpleInstruction("OP_DIVIDE", index); break;
      case OP_EQUAL:
        index = simpleInstruction("OP_EQUAL", index); break;
      case OP_FALSE:
        index = simpleInstruction("OP_FALSE", index); break;
      case OP_FIELD:
        index = constantInstruction("OP_FIELD", instrList, index); break;
      case OP_GET_ARR_ELEM:
          index = simpleInstruction("OP_GET_ARR_ELEM", index); break;
      case OP_GET_GLOBAL:
        index = constantInstruction("OP_GET_GLOBAL", instrList, index); break;
      case OP_GET_LOCAL:
        index = operandInstruction("OP_GET_LOCAL", instrList, index, "offset from base"); break;
      case OP_GET_PROP:
        index = constantInstruction("OP_GET_PROP", instrList, index); break;
      case OP_GET_SUPER:
        index = constantInstruction("OP_GET_SUPER", instrList, index); break;
      case OP_GET_UPVAL:
        index = operandInstruction("OP_GET_UPVAL", instrList, index, ""); break;
      case OP_GREATER:
        index = simpleInstruction("OP_GREATER", index); break;
      case OP_INHERIT:
        index = simpleInstruction("OP_INHERIT", index); break;
      case OP_INVOKE:
        index = invokeInstruction("OP_INVOKE", instrList, index); break;
      case OP_JUMP:
        index = jumpInstruction("OP_JUMP", 1, instrList, index); break;
      case OP_JUMP_IF_FALSE:
        index = jumpInstruction("OP_JUMP_IF_FALSE", 1, instrList, index); break;
      case OP_LESS:
        index = simpleInstruction("OP_LESS", index); break;
      case OP_LOOP:
        index = jumpInstruction("OP_LOOP", -1, instrList, index); break;
      case OP_MAPPING:
          index = operandInstruction("OP_MAPPING", instrList, index, "# of entries"); break;
      case OP_METHOD:
        index = constantInstruction("OP_METHOD", instrList, index); break;
      case OP_MULTIPLY:
        index = simpleInstruction("OP_MULTIPLY", index); break;
      case OP_NEGATE:
        index = simpleInstruction("OP_NEGATE", index); break;
      case OP_NIL:
        index = simpleInstruction("OP_NIL", index); break;
      case OP_NOT:
        index = simpleInstruction("OP_NOT", index); break;
      case OP_OBJECT:
        index = constantInstruction("OP_OBJECT", instrList, index); break;
      case OP_POP:
        index = simpleInstruction("OP_POP", index); break;
      case OP_RETURN:
        index = simpleInstruction("OP_RETURN", index); break;
      case OP_SET_GLOBAL:
        index = constantInstruction("OP_SET_GLOBAL", instrList, index); break;
      case OP_SET_LOCAL:
        index = operandInstruction("OP_SET_LOCAL", instrList, index, "offset from base"); break;
      case OP_SET_PROP:
        index = constantInstruction("OP_SET_PROP", instrList, index); break;
      case OP_SET_UPVAL:
        index = operandInstruction("OP_SET_UPVAL", instrList, index, ""); break;
      case OP_SUBTRACT:
        index = simpleInstruction("OP_SUBTRACT", index); break;
      case OP_SUPER_INVOKE:
        index = invokeInstruction("OP_SUPER_INVOKE", instrList, index); break;
      case OP_TRUE:
        index = simpleInstruction("OP_TRUE", index); break;
      default:
        System.out.println("Unknown opcode: " + instruction);

        index = index + 1;

        break;
    }

    System.out.println(COLOR_RESET);

    return index;
  }

  //closureInstruction(String, C_InstrList, int)
  private int closureInstruction(String name, C_InstrList instrList, int index) {
    byte code = getCode(instrList, index + 1);
    C_Function function = (C_Function)getConstant(instrList, code);

    System.out.print(String.format("%-16s constant: ", name));
    System.out.print(COLOR_MAGENTA);
    System.out.print(String.format("%d ",  code));
    System.out.print(COLOR_YELLOW);
    System.out.print(function);

    index += 2;

    for (int j = 0; j < function.upvalueCount(); j++) {
      boolean isLocal = (getCode(instrList, index++) != 0);
      code = getCode(instrList, index++);

      System.out.print(String.format(
        "%04d      |                     %s %d\n",
        index - 2, isLocal ? "local" : "upvalue", code
      ));
    }

    return index;
  }

  //constantInstruction(String, C_InstrList, int)
  private int constantInstruction(String name, C_InstrList instrList, int index) {
    byte operand = getCode(instrList, index + 1);
    Object constant = getConstant(instrList, operand);

    System.out.print(String.format("%-16s constant: ", name));
    System.out.print(COLOR_MAGENTA);
    System.out.print(String.format("%d ",  operand));
    System.out.print(COLOR_YELLOW);

    if (constant instanceof String)
      System.out.print("(\"" + constant + "\")");
    else
      System.out.print("(" + constant + ")");

    return index + 2;
  }

  //invokeInstruction(String, C_InstrList, int)
  private int invokeInstruction(String name, C_InstrList instrList, int index) {
    byte op1 = getCode(instrList, index + 1);
    byte op2 = getCode(instrList, index + 2); //arg count
    Object constant = getConstant(instrList, op1);

    System.out.print(String.format("%-16s constant: %d ", name, op1));

    if (constant instanceof String)
      System.out.print("(\"" + constant + "\")");
    else
      System.out.print("(" + constant + ")");

    System.out.print(String.format(" (%d args)", op2));

    return index + 3;
  }

  //simpleInstruction(String, int)
  private int simpleInstruction(String name, int index) {
    System.out.print(String.format("%-16s", name));

    return index + 1;
  }

  //operandInstruction(String, C_InstrList, int, String)
  private int operandInstruction(String name, C_InstrList instrList, int index, String hint) {
    byte operand = getCode(instrList, index + 1);

    System.out.print(String.format("%-16s operand: ", name));
    System.out.print(COLOR_MAGENTA);
    System.out.print(String.format("%d ", operand));
    System.out.print(COLOR_YELLOW);
    System.out.print("(" + hint + ")");

    return index + 2;
  }

  //jumpInstruction(String, int, C_InstrList, int)
  private int jumpInstruction(String name, int sign, C_InstrList instrList, int index) {
    byte operand = getCode(instrList, index);

    System.out.print(String.format("%-16s %4d -> %d",
      name, index, index + 3 + (sign * operand)));

    return index + 2;
  }

  //getCode(C_InstrList, int)
  private byte getCode(C_InstrList instrList, int index) {
    return instrList.codes().get(index);
  }

  //getConstant(C_InstrList, int)
  private Object getConstant(C_InstrList instrList, int index) {
    return instrList.constants().get(index);
  }
}
