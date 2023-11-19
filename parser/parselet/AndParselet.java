package jbLPC.parser.parselet;

import static jbLPC.compiler.OpCode.OP_JUMP_IF_FALSE;
import static jbLPC.compiler.OpCode.OP_POP;
import static jbLPC.parser.Parser.Precedence.PREC_AND;

import jbLPC.compiler.LPCCompiler;
import jbLPC.parser.Parser;

public class AndParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, LPCCompiler compiler, boolean canAssign) {
    int endJump = compiler.emitJump(OP_JUMP_IF_FALSE);

    compiler.emitByte(OP_POP);
    parser.parsePrecedence(PREC_AND);

    compiler.patchJump(endJump);
  }
}
