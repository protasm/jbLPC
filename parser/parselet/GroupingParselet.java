package jbLPC.parser.parselet;

import jbLPC.compiler.Compiler;

import static jbLPC.scanner.TokenType.*;

public class GroupingParselet implements Parselet {
  //parse(jbLPC.compiler.Compiler, boolean)
  public void parse(jbLPC.compiler.Compiler compiler, boolean canAssign) {
    compiler.expression();

    compiler.consume(TOKEN_RIGHT_PAREN, "Expect ')' after expression.");
  }
}
