package jbLPC.parser.parselet;

import jbLPC.compiler.C_Compiler;
import jbLPC.parser.Parser;

public class ThisParselet implements Parselet {
  //parse(Parser, C_Compiler, boolean)
  public void parse(Parser parser, C_Compiler compiler, boolean canAssign) {
//    if (compiler.currClass() == null) {
//      parser.error("Can't use 'this' outside of an object.");
//
//      return;
//    }

    compiler.namedVariable(compiler.syntheticToken("this"), false);
  }
}
