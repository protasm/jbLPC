package jbLPC.preprocessor.source;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import jbLPC.preprocessor.LexerException;
import jbLPC.preprocessor.Token;

public class FixedTokenSource extends Source {
  private static final Token EOF = new Token(Token.EOF, "<ts-eof>");
  private final List<Token> tokens;
  private int idx;

   public FixedTokenSource(Token... tokens) {
    this.tokens = Arrays.asList(tokens);
    this.idx = 0;
  }

   public FixedTokenSource(List<Token> tokens) {
    this.tokens = tokens;
    this.idx = 0;
  }

  @Override
  public Token token() throws IOException, LexerException {
    if (idx >= tokens.size())
      return EOF;

    return tokens.get(idx++);
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();

    buf.append("constant token stream ").append(tokens);

    Source parent = getParent();

    if (parent != null)
      buf.append(" in ").append(String.valueOf(parent));

    return buf.toString();
  }
}
