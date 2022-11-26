package jbLPC.preprocessor;

public enum PreprocessorCommand {
  PP_DEFINE("define"),
  PP_ELIF("elif"),
  PP_ELSE("else"),
  PP_ENDIF("endif"),
  PP_ERROR("error"),
  PP_IF("if"),
  PP_IFDEF("ifdef"),
  PP_IFNDEF("ifndef"),
  PP_INCLUDE("include"),
  PP_LINE("line"),
  PP_PRAGMA("pragma"),
  PP_UNDEF("undef"),
  PP_WARNING("warning"),
  PP_INCLUDE_NEXT("include_next"),
  PP_IMPORT("import");

  private final String text;

  PreprocessorCommand(String text) {
    this.text = text;
  }

  public static PreprocessorCommand forText( String text) {
    for (PreprocessorCommand ppcmd : PreprocessorCommand.values())
      if (ppcmd.text.equals(text))
        return ppcmd;

    return null;
  }
}
