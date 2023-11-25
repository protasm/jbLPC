package jbLPC.parser.parselet;

import static jbLPC.compiler.OpCode.OP_JUMP;
import static jbLPC.compiler.OpCode.OP_JUMP_IF_FALSE;
import static jbLPC.compiler.OpCode.OP_POP;
import static jbLPC.parser.Parser.Precedence.PREC_OR;

import jbLPC.compiler.LPCCompiler;
import jbLPC.parser.Parser;

public class OrParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, LPCCompiler compiler, boolean canAssign) {
    int elseJump = compiler.emitJump(OP_JUMP_IF_FALSE);
    int endJump = compiler.emitJump(OP_JUMP);

    compiler.patchJump(elseJump);
    
    compiler.emitInstruction(OP_POP);

    parser.parsePrecedence(PREC_OR);
    
    compiler.patchJump(endJump);
  }
}
