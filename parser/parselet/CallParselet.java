package jbLPC.parser.parselet;

import static jbLPC.compiler.C_OpCode.OP_CALL;

import jbLPC.compiler.Instruction;
import jbLPC.compiler.C_Compiler;
import jbLPC.parser.Parser;

public class CallParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, C_Compiler compiler, boolean canAssign) {
    int argCount = compiler.argumentList();
    Instruction instr = new Instruction(OP_CALL, argCount);
    
    compiler.emitInstruction(instr);
  }
}
