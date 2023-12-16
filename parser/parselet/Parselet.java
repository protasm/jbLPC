package jbLPC.parser.parselet;

import jbLPC.compiler.C_Compiler;
import jbLPC.parser.Parser;

public interface Parselet {
  //parse(Parser, C_Compiler, boolean);
  void parse(Parser parser, C_Compiler compiler, boolean canAssign);
}
