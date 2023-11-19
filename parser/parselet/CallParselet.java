package jbLPC.parser.parselet;

import static jbLPC.compiler.OpCode.OP_CALL;

import jbLPC.compiler.LPCCompiler;
import jbLPC.parser.Parser;

public class CallParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, LPCCompiler compiler, boolean canAssign) {
    byte argCount = compiler.argumentList();

    compiler.emitByte(OP_CALL);
    compiler.emitByte(argCount);
  }
}
