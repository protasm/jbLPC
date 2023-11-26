package jbLPC.parser.parselet;

import static jbLPC.compiler.OpCode.OP_CONST;

import jbLPC.compiler.Instruction;
import jbLPC.compiler.LPCCompiler;
import jbLPC.parser.Parser;

public class NumberParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, LPCCompiler compiler, boolean canAssign) {
    Object number = parser.previous().literal();
    Instruction instr = new Instruction(OP_CONST, number);

    compiler.currInstructions().add(instr);
  }
}
