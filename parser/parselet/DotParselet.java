package jbLPC.parser.parselet;

import jbLPC.compiler.Compiler;

import static jbLPC.compiler.OpCode.*;
import static jbLPC.parser.Parser.Precedence.*;
import static jbLPC.scanner.TokenType.*;

public class DotParselet implements Parselet {
  //parse(jbLPC.compiler.Compiler, boolean)
  public void parse(jbLPC.compiler.Compiler compiler, boolean canAssign) {
    compiler.consume(TOKEN_IDENTIFIER, "Expect property name after '.'.");

    int nameIdx = compiler.makeConstant(compiler.parser().previous().lexeme());

    if (canAssign && compiler.match(TOKEN_EQUAL)) {
      compiler.expression();

      compiler.emitByte(OP_SET_PROPERTY);
      compiler.emitWord((short)nameIdx);
    } else if (compiler.match(TOKEN_LEFT_PAREN)) {
      byte argCount = compiler.argumentList();

      compiler.emitByte(OP_INVOKE);
      compiler.emitWord((short)nameIdx);
      compiler.emitByte(argCount);
    } else {
      compiler.emitByte(OP_GET_PROPERTY);
      compiler.emitWord((short)nameIdx);
    }
  }
}
