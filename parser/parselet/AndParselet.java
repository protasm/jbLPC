package jbLPC.parser.parselet;

import jbLPC.compiler.Compiler;

import static jbLPC.compiler.OpCode.*;
import static jbLPC.parser.Parser.Precedence.*;

public class AndParselet implements Parselet {
  //parse(jbLPC.compiler.Compiler, boolean)
  public void parse(jbLPC.compiler.Compiler compiler, boolean canAssign) {
    int endJump = compiler.emitJump(OP_JUMP_IF_FALSE);

    compiler.emitByte(OP_POP);
    compiler.parsePrecedence(PREC_AND);

    compiler.patchJump(endJump);
  }
}
