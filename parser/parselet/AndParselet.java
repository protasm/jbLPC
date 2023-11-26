package jbLPC.parser.parselet;

import static jbLPC.compiler.C_OpCode.OP_JUMP_IF_FALSE;
import static jbLPC.compiler.C_OpCode.OP_POP;
import static jbLPC.parser.Parser.Precedence.PREC_AND;

import jbLPC.compiler.C_Compiler;
import jbLPC.parser.Parser;

public class AndParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, C_Compiler compiler, boolean canAssign) {
    int endJump = compiler.emitJump(OP_JUMP_IF_FALSE);

    compiler.emitCode(OP_POP);
    
    parser.parsePrecedence(PREC_AND);
    
    compiler.patchJump(endJump);
  }
}
