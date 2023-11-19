package jbLPC.preprocessor;

import jbLPC.preprocessor.source.Source;

/**
 * A handler for preprocessor events, primarily errors and warnings.
 *
 * If no PreprocessorListener is installed in a Preprocessor, all
 * error and warning events will throw an exception. Installing a
 * listener allows more intelligent handling of these events.
 */
public class DefaultPreprocessorListener implements PreprocessorListener {
  private int errors;
  private int warnings;

  public DefaultPreprocessorListener() {
    clear();
  }

  public void clear() {
    errors = 0;
    warnings = 0;
  }

  public int getErrors() {
    return errors;
  }

  public int getWarnings() {
    return warnings;
  }

  protected void print(String msg) {
    System.out.println(msg);
  }

  /**
   * Handles a warning.
   *
   * The behaviour of this method is defined by the
   * implementation. It may simply record the error message, or
   * it may throw an exception.
   */
  @Override
  public void handleWarning(Source source, int line, int column,
    String msg) throws LexerException {
    warnings++;

    print(source.getName() + ":" + line + ":" + column
      + ": warning: " + msg);
  }

  /**
   * Handles an error.
   *
   * The behaviour of this method is defined by the
   * implementation. It may simply record the error message, or
   * it may throw an exception.
   */
  @Override
  public void handleError(Source source, int line, int column,
    String msg) throws LexerException {
    errors++;

    print(source.getName() + ":" + line + ":" + column
      + ": error: " + msg);
  }

  @Override
  public void handleSourceChange(Source source, SourceChangeEvent event) {
  }
}
