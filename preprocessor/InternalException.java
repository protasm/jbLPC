package jbLPC.preprocessor;

/**
 * An internal exception.
 *
 * This exception is thrown when an internal state violation is
 * encountered. This should never happen. If it ever happens, please
 * report it as a bug.
 */
public class InternalException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public InternalException(String msg) {
    super(msg);
  }
}
