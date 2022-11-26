package jbLPC.parser.parselet;

import jbLPC.compiler.Compiler;

public class ThisParselet implements Parselet {
  //parse(jbLPC.compiler.Compiler, boolean)
  public void parse(jbLPC.compiler.Compiler compiler, boolean canAssign) {
    if (compiler.currClass() == null) {
      compiler.error("Can't use 'this' outside of a class.");

      return;
    }

    new VariableParselet().parse(compiler, false);
  }
}
