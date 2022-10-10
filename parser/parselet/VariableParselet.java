package jbLPC.parser.parselet;

import jbLPC.compiler.Compiler;

public class VariableParselet implements Parselet {
  //parse(jbLPC.compiler.Compiler, boolean)
  public void parse(jbLPC.compiler.Compiler compiler, boolean canAssign) {
    compiler.namedVariable(compiler.parser().previous(), canAssign);
  }
}
