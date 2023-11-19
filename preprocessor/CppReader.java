package jbLPC.preprocessor;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;

import jbLPC.preprocessor.source.LexerSource;

/**
 * A Reader wrapper around the Preprocessor.
 *
 * This is a utility class to provide a transparent {@link Reader}
 * which preprocesses the input text.
 *
 * @see Preprocessor
 * @see Reader
 */
public class CppReader extends Reader implements Closeable {
  private final Preprocessor cpp;
  private String token;
  private int idx;

  public CppReader(final Reader r) {
    cpp = new Preprocessor(new LexerSource(r, true) {
      @Override
      public String getName() {
        return "<CppReader Input@"
          + System.identityHashCode(r) + ">";
      }
    });

    token = "";
    idx = 0;
  }

  public CppReader(Preprocessor p) {
    cpp = p;
    token = "";
    idx = 0;
  }

  /**
   * Returns the Preprocessor used by this CppReader.
   */
  
  public Preprocessor getPreprocessor() {
    return cpp;
  }

  /**
   * Defines the given name as a macro.
   *
   * This is a convnience method.
   */
  public void addMacro(String name) throws LexerException {
    cpp.addMacro(name);
  }

  /**
   * Defines the given name as a macro.
   *
   * This is a convenience method.
   */
  public void addMacro(String name, String value)
    throws LexerException {
    cpp.addMacro(name, value);
  }

  private boolean refill()
    throws IOException {
    try {
      assert cpp != null : "cpp is null : was it closed?";

      if (token == null)
        return false;

      while (idx >= token.length()) {
        Token tok = cpp.token();

        switch (tok.getType()) {
          case jbLPC.preprocessor.Token.EOF:
            token = null;

            return false;
          case jbLPC.preprocessor.Token.CCOMMENT:
          case jbLPC.preprocessor.Token.CPPCOMMENT:
            if (!cpp.getFeature(Feature.KEEPCOMMENTS)) {
              token = " ";

              break;
            }
          default:
            token = tok.getText();

            break;
        } //switch (tok.getType())

        idx = 0;
      }

      return true;
    } catch (LexerException e) {
      // new IOException(String, Throwable) is since 1.6
      IOException _e = new IOException(String.valueOf(e));

      _e.initCause(e);

      throw _e;
    }
  }

  @Override
  public int read() throws IOException {
    if (!refill())
      return -1;

    return token.charAt(idx++);
  }

  @Override
  /* XXX Very slow and inefficient. */
  public int read(char cbuf[], int off, int len) throws IOException {
    if (token == null)
      return -1;

    for (int i = 0; i < len; i++) {
      int ch = read();

      if (ch == -1)
        return i;

      cbuf[off + i] = (char) ch;
    }

    return len;
  }

  @Override
  public void close() throws IOException {
    cpp.close();

    token = null;
  }
}
