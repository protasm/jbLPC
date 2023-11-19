package jbLPC.parser.parselet;

import static jbLPC.scanner.TokenType.TOKEN_RIGHT_PAREN;

import jbLPC.compiler.LPCCompiler;
import jbLPC.parser.Parser;

public class GroupingParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, LPCCompiler compiler, boolean canAssign) {
    compiler.expression();

    parser.consume(TOKEN_RIGHT_PAREN, "Expect ')' after expression.");
  }
}
