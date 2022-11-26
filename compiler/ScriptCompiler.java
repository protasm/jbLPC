package jbLPC.compiler;

import jbLPC.debug.Debugger;
import jbLPC.parser.Parser;
import jbLPC.scanner.Scanner;

import static jbLPC.compiler.Function.FunctionType.*;
import static jbLPC.scanner.TokenType.*;

public class ScriptCompiler extends Compiler {
  //compile(String)
  public Function compile(String name, String source) {
    parser = new Parser();
    tokens = new Scanner(source);

    currScope = new Scope(
      null, //enclosing Scope
      TYPE_SCRIPT //FunctionType
    );

    if (debugPrintProgress) Debugger.instance().printProgress("Compiling Script....");

    //advance to the first non-error Token (or EOF)
    advance();

    //loop declarations until EOF
    while (!match(TOKEN_EOF))
      declaration();

    Function function = endCompilation(true);

    return parser.hadError() ? null : function;
  }
}
