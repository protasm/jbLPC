package jbLPC.parser.parselet;

import jbLPC.compiler.LPCCompiler;
import jbLPC.parser.Parser;

public class ThisParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, LPCCompiler compiler, boolean canAssign) {
    if (compiler.currClass() == null) {
      parser.error("Can't use 'this' outside of an object.");

      return;
    }

    new VariableParselet().parse(parser, compiler, false);
  }
}
