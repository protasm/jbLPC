package jbLPC.parser.parselet;

import static jbLPC.compiler.C_OpCode.OP_CONST;

import jbLPC.compiler.Instruction;
import jbLPC.compiler.C_Compiler;
import jbLPC.parser.Parser;

public class StringParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, C_Compiler compiler, boolean canAssign) {
    Object value = parser.previous().literal();
    Instruction instr = new Instruction(OP_CONST, value);

    compiler.currInstrList().add(instr);
  }
}
