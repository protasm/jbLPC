package jbLPC.parser.parselet;

import jbLPC.compiler.LPCCompiler;
import jbLPC.parser.Parser;

public class VariableParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, LPCCompiler compiler, boolean canAssign) {
    compiler.namedVariable(parser.previous(), canAssign);
  }
}
