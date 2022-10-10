package jbLPC.parser.parselet;

import jbLPC.compiler.Compiler;

public class NumberParselet implements Parselet {
  //parser(jbLPC.compiler.Compiler, boolean)
  public void parse(jbLPC.compiler.Compiler compiler, boolean canAssign) {
    Object value = compiler.parser().previous().literal();

    compiler.emitConstant(value);
  }
}
