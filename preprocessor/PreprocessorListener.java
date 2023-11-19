package jbLPC.preprocessor;

import jbLPC.preprocessor.source.Source;

/**
 * A handler for preprocessor events, primarily errors and warnings.
 *
 * If no PreprocessorListener is installed in a Preprocessor, all
 * error and warning events will throw an exception. Installing a
 * listener allows more intelligent handling of these events.
 */

public interface PreprocessorListener {
  /**
   * Handles a warning.
   *
   * The behaviour of this method is defined by the
   * implementation. It may simply record the error message, or
   * it may throw an exception.
   */
  public void handleWarning(Source source, int line, int column,
    String msg) throws LexerException;

  /**
   * Handles an error.
   *
   * The behaviour of this method is defined by the
   * implementation. It may simply record the error message, or
   * it may throw an exception.
   */
  public void handleError(Source source, int line, int column,
    String msg) throws LexerException;

  public enum SourceChangeEvent {
    SUSPEND, PUSH, POP, RESUME;
  }

  public void handleSourceChange( Source source,  SourceChangeEvent event);
}
