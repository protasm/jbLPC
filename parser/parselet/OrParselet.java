package jbLPC.parser.parselet;

import static jbLPC.compiler.C_OpCode.OP_JUMP;
import static jbLPC.compiler.C_OpCode.OP_JUMP_IF_FALSE;
import static jbLPC.compiler.C_OpCode.OP_POP;
import static jbLPC.parser.Parser.Precedence.PREC_OR;

import jbLPC.compiler.C_Compiler;
import jbLPC.parser.Parser;

public class OrParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, C_Compiler compiler, boolean canAssign) {
    int elseJump = compiler.emitJump(OP_JUMP_IF_FALSE);
    int endJump = compiler.emitJump(OP_JUMP);

    compiler.patchJump(elseJump);
    
    compiler.emitCode(OP_POP);

    parser.parsePrecedence(PREC_OR);
    
    compiler.patchJump(endJump);
  }
}
