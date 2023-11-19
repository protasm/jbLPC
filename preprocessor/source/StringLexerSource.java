package jbLPC.preprocessor.source;

import java.io.StringReader;

/**
 * A Source for lexing a String.
 *
 * This class is used by token pasting, but can be used by user
 * code.
 */
public class StringLexerSource extends LexerSource {
  /**
   * Creates a new Source for lexing the given String.
   *
   * @param string The input string to lex.
   * @param ppvalid true if preprocessor directives are to be
   *  honored within the string.
   */
  public StringLexerSource(String string, boolean ppvalid) {
    super(new StringReader(string), ppvalid);
  }

  /**
   * Creates a new Source for lexing the given String.
   *
   * Equivalent to calling <code>new StringLexerSource(string, false)</code>.
   *
   * By default, preprocessor directives are not honored within
   * the string.
   *
   * @param string The input string to lex.
   */
  public StringLexerSource(String string) {
    this(string, false);
  }

  @Override
  public String toString() {
    return "string literal";
  }
}
