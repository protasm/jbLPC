package jbLPC.preprocessor;

/**
 * A preprocessor exception.
 *
 * Note to users: I don't really like the name of this class. S.
 */
public class LexerException extends Exception {
  private static final long serialVersionUID = 1L;

  public LexerException(String msg) {
    super(msg);
  }

  public LexerException(Throwable cause) {
    super(cause);
  }
}
