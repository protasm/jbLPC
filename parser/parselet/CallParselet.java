package jbLPC.parser.parselet;

import static jbLPC.compiler.C_OpCode.OP_CALL;

import jbLPC.compiler.C_Compiler;
import jbLPC.parser.Parser;

public class CallParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, C_Compiler compiler, boolean canAssign) {
    int argCount = compiler.argumentList();
    
    compiler.emitCode(OP_CALL);
    compiler.emitCode(argCount);
  }
}
