package jbLPC.debug;

import java.util.List;
import java.util.Map;
import java.util.Stack;

import jbLPC.compiler.C_Function;
import jbLPC.compiler.Compilation;
import jbLPC.compiler.Instruction;
import jbLPC.compiler.Scope;
import jbLPC.nativefn.NativeFn;
import jbLPC.util.Prefs;

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
  
  private static int lastLine = 0;

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
      System.out.print("Codes: ");
      System.out.print(COLOR_MAGENTA);;
      System.out.print(prettyList(compilation.instructions()));
      System.out.println(COLOR_RESET);
    }

    //locals
    if (Prefs.instance().getBoolean("locals")) {
      System.out.print("Locals: ");
      System.out.println(prettyList(scope.locals()));
    }

//    if (Prefs.instance().getBoolean("upvals"))
//      System.out.println("Upvalues: " + scope.upvalues());

    for (Instruction instr : compilation.instructions())
      disassembleInstruction(instr);

    lastLine = 0;
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
    disassembleInstruction(instr);
  }

  //disassembleInstruction(Instruction)
  public void disassembleInstruction(Instruction instr) {
    //OpCode offset
//    System.out.print(String.format("%04d", offset));
	int line = instr.line();
	    
    System.out.print(COLOR_YELLOW);

    //Line number
    if (line == lastLine)
      System.out.print("   | ");
    else
      System.out.print(String.format("%4d ", line));
    
    lastLine = line;

    //rawcode
    if (Prefs.instance().getBoolean("rawcode")) {
      System.out.print("(");
      System.out.print(COLOR_MAGENTA);
      System.out.print(String.format("%02d", instr.opCode().code()));
      System.out.print(COLOR_YELLOW);
      System.out.print(") ");
    }

    printOpCode(instr);

    switch (instr.opCode().type()) {
      case TYPE_CLOSURE:
        closureInstruction(instr); break;
      case TYPE_CONST:
        constantInstruction(instr); break;
      case TYPE_INVOKE:
          invokeInstruction(instr); break;
      case TYPE_JUMP:
          jumpInstruction(instr, 1); break;
      case TYPE_OPERAND:
        operandInstruction(instr); break;
      case TYPE_SIMPLE:
          simpleInstruction(instr); break;
      default:
        System.out.print("Unknown opcode: ");
          printOpCode(instr, true);
        
        break;
    }
    
	System.out.print(COLOR_RESET);
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

    System.out.print(COLOR_MAGENTA);
    if (constant instanceof String)
      System.out.println("\"" + constant + "\"");
    else
      System.out.println(constant);
    System.out.print(COLOR_YELLOW);
  }

  //invokeInstruction(Instruction)
  private void invokeInstruction(Instruction instr) {
    String identifier = (String)instr.operands()[0]; //method name
    int argCount = (int)instr.operands()[1];

    System.out.print("(\"" + identifier + "\")");

    System.out.println(String.format(" (%d args)\n", argCount));
  }

  //jumpInstruction(Instruction, int)
  private void jumpInstruction(Instruction instr, int sign) {
//	int offset = (int)instr.operands()[0];

    System.out.println("TODO: finish jumpInstruction in Debugger class!");
//    System.out.print(String.format("%-16s %4d -> %d\n",
//      instr.opCode(), offset, offset + 3 + (sign * operand)));
  }
  
  //operandInstruction(Instruction)
  private void operandInstruction(Instruction instr) {
    System.out.println("TODO: finish operandInstruction in Debugger class!");
  }

  //simpleInstruction(Instruction)
  private void simpleInstruction(Instruction instr) {
   System.out.println("");
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
