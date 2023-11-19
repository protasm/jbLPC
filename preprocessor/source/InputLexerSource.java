package jbLPC.preprocessor.source;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

/**
 * A Source which lexes an InputStream.
 *
 * The input is buffered.
 *
 * @see Source
 */
public class InputLexerSource extends LexerSource {
  @Deprecated
  public InputLexerSource(InputStream input) {
    this(input, Charset.defaultCharset());
  }

  /**
   * Creates a new Source for lexing the given Reader.
   *
   * Preprocessor directives are honoured within the file.
   */
  public InputLexerSource(InputStream input, Charset charset) {
    this(new InputStreamReader(input, charset));
  }

  public InputLexerSource(Reader input, boolean ppvalid) {
    super(input, true);
  }

  public InputLexerSource(Reader input) {
    this(input, true);
  }

  @Override
  public String getPath() {
    return "<standard-input>";
  }

  @Override
  public String getName() {
    return "standard input";
  }

  @Override
  public String toString() {
    return String.valueOf(getPath());
  }
}
