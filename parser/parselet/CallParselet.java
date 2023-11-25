package jbLPC.parser.parselet;

import static jbLPC.compiler.OpCode.OP_CALL;

import jbLPC.compiler.Instruction;
import jbLPC.compiler.LPCCompiler;
import jbLPC.parser.Parser;

public class CallParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, LPCCompiler compiler, boolean canAssign) {
    int argCount = compiler.argumentList();
    Instruction instr = new Instruction(OP_CALL, argCount);
    
    compiler.emitInstruction(instr);
  }
}
