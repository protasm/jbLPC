package jbLPC.parser.parselet;

import jbLPC.compiler.LPCCompiler;
import jbLPC.parser.Parser;

import static jbLPC.compiler.OpCode.*;
import static jbLPC.parser.Parser.Precedence.*;

public class OrParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, LPCCompiler compiler, boolean canAssign) {
    int elseJump = compiler.emitJump(OP_JUMP_IF_FALSE);
    int endJump = compiler.emitJump(OP_JUMP);

    compiler.patchJump(elseJump);
    compiler.emitByte(OP_POP);

    parser.parsePrecedence(PREC_OR);
    compiler.patchJump(endJump);
  }
}
