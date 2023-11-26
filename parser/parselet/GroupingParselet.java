package jbLPC.parser.parselet;

import static jbLPC.scanner.TokenType.TOKEN_RIGHT_PAREN;

import jbLPC.compiler.C_Compiler;
import jbLPC.parser.Parser;

public class GroupingParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, C_Compiler compiler, boolean canAssign) {
    compiler.expression();

    parser.consume(TOKEN_RIGHT_PAREN, "Expect ')' after expression.");
  }
}
