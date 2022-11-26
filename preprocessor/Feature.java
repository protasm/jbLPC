package jbLPC.preprocessor;

/**
 * Features of the Preprocessor, which may be enabled or disabled.
 */
public enum Feature {
  DIGRAPHS, //Suppors ANSI digraphs.
  TRIGRAPHS, //Supports ANSI trigraphs.
  LINEMARKERS, //Outputs linemarker tokens.
  CSYNTAX, //Reports tokens of type INVALID as errors.

  /** Preserves comments in the lexed output. Like cpp -C */
  KEEPCOMMENTS,

  /** Preserves comments in the lexed output, even when inactive. */
  KEEPALLCOMMENTS,

  DEBUG,
  OBJCSYNTAX, //Supports lexing of objective-C.
  INCLUDENEXT,
  PRAGMA_ONCE //Random extensions.
}
