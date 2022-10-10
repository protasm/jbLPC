package jbLPC.parser.parselet;

import jbLPC.compiler.Compiler;

public interface Parselet {
  //parse(jbLPC.compiler.Compiler, boolean);
  void parse(jbLPC.compiler.Compiler compiler, boolean canAssign);
}
