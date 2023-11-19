package jbLPC.parser.parselet;

import jbLPC.compiler.LPCCompiler;
import jbLPC.parser.Parser;

public interface Parselet {
  //parse(Parser, LPCCompiler, boolean);
  void parse(Parser parser, LPCCompiler compiler, boolean canAssign);
}
