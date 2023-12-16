package jbLPC.parser.parselet;

import jbLPC.compiler.C_Compiler;
import jbLPC.parser.Parser;

public class VariableParselet implements Parselet {
  //parse(Parser, C_Compiler, boolean)
  public void parse(Parser parser, C_Compiler compiler, boolean canAssign) {
    compiler.namedVariable(parser.previous(), canAssign);
  }
}
