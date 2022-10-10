package jbLPC.parser.parselet;

import jbLPC.compiler.Compiler;
import jbLPC.parser.Parser;
import jbLPC.scanner.Token;

import static jbLPC.compiler.OpCode.*;
import static jbLPC.scanner.TokenType.*;

public class LiteralParselet implements Parselet {
  //parse(jbLPC.compiler.Compiler, boolean)
  public void parse(jbLPC.compiler.Compiler compiler, boolean canAssign) {
    switch (compiler.parser().previous().type()) {
      case TOKEN_FALSE:
        compiler.emitByte(OP_FALSE);

        break;
      case TOKEN_NIL:
        compiler.emitByte(OP_NIL);

        break;
      case TOKEN_TRUE:
        compiler.emitByte(OP_TRUE);

        break;
      default: //Unreachable
        return;
    }
  }
}
