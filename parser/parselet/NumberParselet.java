package jbLPC.parser.parselet;

import static jbLPC.compiler.C_OpCode.OP_CONST;

import jbLPC.compiler.C_Compiler;
import jbLPC.parser.Parser;

public class NumberParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, C_Compiler compiler, boolean canAssign) {
    Object number = parser.previous().literal();

    int index = compiler.emitConstant(number);
    
    compiler.emitInstruction(OP_CONST);
    compiler.emitInstruction(index);
  }
}
