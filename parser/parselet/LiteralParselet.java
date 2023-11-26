package jbLPC.parser.parselet;

import static jbLPC.compiler.C_OpCode.OP_FALSE;
import static jbLPC.compiler.C_OpCode.OP_NIL;
import static jbLPC.compiler.C_OpCode.OP_TRUE;

import jbLPC.compiler.C_Compiler;
import jbLPC.parser.Parser;

public class LiteralParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, C_Compiler compiler, boolean canAssign) {
    switch (parser.previous().type()) {
      case TOKEN_FALSE:
        compiler.emitInstruction(OP_FALSE);

        break;
      case TOKEN_NIL:
        compiler.emitInstruction(OP_NIL);

        break;
      case TOKEN_TRUE:
        compiler.emitInstruction(OP_TRUE);

        break;
      default: //Unreachable
        return;
    }
  }
}
