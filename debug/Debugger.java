package jbLPC.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import jbLPC.compiler.C_Function;
import jbLPC.compiler.C_InstrList;
import jbLPC.compiler.C_Compilation;
import jbLPC.compiler.C_OpCode;
import jbLPC.compiler.C_Scope;
import jbLPC.nativefn.NativeFn;
import jbLPC.util.Prefs;
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
	  List<Byte> instructions = instrList.instructions();

    printBanner(compilation.toString());

    //codes
    if (Prefs.instance().getBoolean("codes")) {
      System.out.print("Codes: ");
      System.out.print(COLOR_MAGENTA);;
      System.out.print(prettyList(instructions));
      System.out.println(COLOR_RESET);
    }

    //locals
    if (Prefs.instance().getBoolean("locals")) {
      System.out.print("Locals: ");
      System.out.println(prettyList(scope.locals()));
    }

    //upvalues
    if (Prefs.instance().getBoolean("upvals")) {
      System.out.println("Upvalues: ");
      System.out.println(prettyList(scope.upvalues()));
    }

    for (int index = 0; index < instructions.size();)
      index = disassembleInstruction(instrList, index);
  }

  //traceExecution(RunFrame, Map<String, Object>, Stack<Object>)
  public void traceExecution(RunFrame frame, Map<String, Object> globals, Stack<Object> vStack) {
    if (!Prefs.instance().getBoolean("exec")) return;

    System.out.print("\n");
    
    //globals
    if (Prefs.instance().getBoolean("globals")) {
      System.out.print(Debugger.COLOR_CYAN);
      System.out.print("Globals: ");
      System.out.print(
    	prettyList(
          globals.entrySet()
            .stream()
            .filter(item -> !(item.getValue() instanceof NativeFn))
            .toArray()
        )
      );
      System.out.print(Debugger.COLOR_RESET);
      System.out.print("\n");
    }

    //vStack
    if (Prefs.instance().getBoolean("stack")) {
      System.out.print(Debugger.COLOR_GREEN + "Stack: ");
      System.out.print(prettyList(vStack));
      System.out.print(Debugger.COLOR_RESET);
      System.out.print("\n");
    }

    //instruction
    disassembleInstruction(frame.compilation().instrList(), frame.nextIndex());
  }

  //disassembleInstruction(C_InstrList, int)
  public int disassembleInstruction(C_InstrList instrList, int index) {
    byte opCode = instrList.instructions().get(index);

    //offset
    System.out.print(String.format("%04d", index));
	    
    System.out.print(COLOR_YELLOW);

    //Line number
    if (
      (index > 0) &&
      (instrList.lines().get(index) == instrList.lines().get(index - 1))
    )
      System.out.print("   | ");
    else
      System.out.print(String.format("%4d ", instrList.lines().get(index)));

    //rawcode
    if (Prefs.instance().getBoolean("rawcode")) {
      System.out.print("(");
      System.out.print(COLOR_MAGENTA);
      System.out.print(String.format("0x%02X", opCode));
      System.out.print(COLOR_YELLOW);
      System.out.print(") ");
    }

    printOpCode(opCode);

    switch (opCode.type()) {
      case TYPE_CLOSURE:
        closureInstruction(instrList); break;
      case TYPE_CONST:
        constantInstruction(instrList); break;
      case TYPE_INVOKE:
          invokeInstruction(instrList); break;
      case TYPE_JUMP:
          jumpInstruction(instrList, 1); break;
      case TYPE_OPERAND:
        operandInstruction(instrList); break;
      case TYPE_SIMPLE:
          simpleInstruction(instrList); break;
      default:
        System.out.print("Unknown opcode: ");
          printOpCode(opCode, true);
        
        break;
    }
    
	System.out.print(COLOR_RESET);
  }

  //closureInstruction(C_OpCode)
  private void closureInstruction(C_OpCode cOpCode) {
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

  //constantInstruction(C_OpCode)
  private void constantInstruction(C_OpCode cOpCode) {
    Object constant = instr.operands()[0];

    System.out.print(COLOR_MAGENTA);
    if (constant instanceof String)
      System.out.println("\"" + constant + "\"");
    else
      System.out.println(constant);
    System.out.print(COLOR_YELLOW);
  }

  //invokeInstruction(C_OpCode)
  private void invokeInstruction(C_OpCode cOpCode) {
    String identifier = (String)instr.operands()[0]; //method name
    int argCount = (int)instr.operands()[1];

    System.out.print("(\"" + identifier + "\")");

    System.out.println(String.format(" (%d args)\n", argCount));
  }

  //jumpInstruction(C_OpCode, int)
  private void jumpInstruction(C_OpCode cOpCode, int sign) {
//	int offset = (int)instr.operands()[0];

    System.out.println("TODO: finish jumpInstruction in Debugger class!");
//    System.out.print(String.format("%-16s %4d -> %d\n",
//      instr.opCode(), offset, offset + 3 + (sign * operand)));
  }
  
  //operandInstruction(C_OpCode)
  private void operandInstruction(C_OpCode cOpCode) {
    System.out.println("TODO: finish operandInstruction in Debugger class!");
  }

  //simpleInstruction(C_OpCode)
  private void simpleInstruction(C_OpCode cOpCode) {
   System.out.println("");
  }
  
  //printOpCode(C_OpCode)
  private void printOpCode(C_OpCode opCode) {
    printOpCode(opCode, false);
  }
  
  //printOpCode(C_OpCode, boolean)
  private void printOpCode(C_OpCode opCode, boolean EOL) {
    System.out.print(String.format("%-16s ", opCode));
    
    if (EOL) System.out.print("\n");
  }
  
  //prettyList(Stack)
  @SuppressWarnings("rawtypes")
  private String prettyList(Stack items) {
    return prettyList(items.toArray());
  }

  //prettyList(List)
  @SuppressWarnings("rawtypes")
  private String prettyList(List items) {
	  return prettyList(items.toArray());
  }
  
  //prettyList(Object[])
  private String prettyList(Object[] items) {
	  StringBuilder sb = new StringBuilder();
	  
	  sb.append("[ ");

	  for (int i = 0; i < items.length; i++) {
	    Object item = items[i];
	    
	    if (item instanceof String)
	      sb.append("\"" + item + "\"");
	    else if (item == null)
	      sb.append("nil");
	    else
	      sb.append(item);
	    
        if (i < items.length - 1)
          sb.append(", ");
	  }
	  
	  sb.append(" ]");
	  
	  return sb.toString();
  }
}
