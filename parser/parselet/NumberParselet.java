package jbLPC.parser.parselet;

import jbLPC.compiler.LPCCompiler;
import jbLPC.parser.Parser;

public class NumberParselet implements Parselet {
  //parser(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, LPCCompiler compiler, boolean canAssign) {
    Object value = parser.previous().literal();

    compiler.emitConstant(value);
  }
}
