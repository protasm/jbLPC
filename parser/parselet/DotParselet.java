package jbLPC.parser.parselet;

import static jbLPC.compiler.OpCode.OP_GET_PROPERTY;
import static jbLPC.compiler.OpCode.OP_INVOKE;
import static jbLPC.compiler.OpCode.OP_SET_PROPERTY;
import static jbLPC.scanner.TokenType.TOKEN_EQUAL;
import static jbLPC.scanner.TokenType.TOKEN_IDENTIFIER;
import static jbLPC.scanner.TokenType.TOKEN_LEFT_PAREN;

import jbLPC.compiler.LPCCompiler;
import jbLPC.parser.Parser;

public class DotParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, LPCCompiler compiler, boolean canAssign) {
    parser.consume(TOKEN_IDENTIFIER, "Expect property name after '.'.");

    int nameIdx = compiler.identifierConstant(parser.previous());

    if (canAssign && parser.match(TOKEN_EQUAL)) {
      compiler.expression();

      compiler.emitByte(OP_SET_PROPERTY);
      compiler.emitWord((short)nameIdx);
    } else if (parser.match(TOKEN_LEFT_PAREN)) {
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
