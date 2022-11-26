package jbLPC.preprocessor.source;

import java.io.IOException;
import java.util.List;

import jbLPC.preprocessor.LexerException;
import jbLPC.preprocessor.Token;

@Deprecated
public class TokenSnifferSource extends Source {
  private final List<Token> target;

  TokenSnifferSource(List<Token> target) {
    this.target = target;
  }

  public Token token() throws IOException, LexerException {
    Token tok = getParent().token();

    if (tok.getType() != jbLPC.preprocessor.Token.EOF)
      target.add(tok);

    return tok;
  }

  @Override
  public String toString() {
    return getParent().toString();
  }
}
