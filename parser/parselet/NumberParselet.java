package jbLPC.parser.parselet;

import static jbLPC.compiler.C_OpCode.OP_CONSTANT;

import jbLPC.compiler.C_Compiler;
import jbLPC.parser.Parser;

public class NumberParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, C_Compiler compiler, boolean canAssign) {
    Object obj = parser.previous().literal();

    int index = compiler.emitConstant(obj);
    
    compiler.emitCode(OP_CONSTANT);
    compiler.emitCode(index);
  }
}
