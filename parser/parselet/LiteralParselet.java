package jbLPC.parser.parselet;

import static jbLPC.compiler.OpCode.OP_FALSE;
import static jbLPC.compiler.OpCode.OP_NIL;
import static jbLPC.compiler.OpCode.OP_TRUE;

import jbLPC.compiler.LPCCompiler;
import jbLPC.parser.Parser;

public class LiteralParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, LPCCompiler compiler, boolean canAssign) {
    switch (parser.previous().type()) {
      case TOKEN_FALSE:
        compiler.emitInstruction(OP_FALSE);

        break;
      case TOKEN_NIL:
        compiler.emitInstruction(OP_NIL);

        break;
      case TOKEN_TRUE:
        compiler.emitInstruction(OP_TRUE);

        break;
      default: //Unreachable
        return;
    }
  }
}
