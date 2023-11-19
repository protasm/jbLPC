package jbLPC.preprocessor;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import jbLPC.preprocessor.source.Source;

/**
 * An Iterator for {@link Source Sources},
 * returning {@link Token Tokens}.
 */
public class SourceIterator implements Iterator<Token> {
  private final Source source;
  private Token tok;

  public SourceIterator( Source s) {
    this.source = s;
    this.tok = null;
  }

  /**
   * Rethrows IOException inside IllegalStateException.
   */
  private void advance() {
    try {
      if (tok == null)
        tok = source.token();
    } catch (LexerException e) {
      throw new IllegalStateException(e);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Returns true if the enclosed Source has more tokens.
   *
   * The EOF token is never returned by the iterator.
   * @throws IllegalStateException if the Source
   *    throws a LexerException or IOException
   */
  @Override
  public boolean hasNext() {
    advance();

    return tok.getType() != jbLPC.preprocessor.Token.EOF;
  }

  /**
   * Returns the next token from the enclosed Source.
   *
   * The EOF token is never returned by the iterator.
   * @throws IllegalStateException if the Source
   *    throws a LexerException or IOException
   */
  @Override
  public Token next() {
    if (!hasNext())
      throw new NoSuchElementException();

    Token t = this.tok;
    this.tok = null;

    return t;
  }

  /**
   * Not supported.
   *
   * @throws UnsupportedOperationException unconditionally.
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
