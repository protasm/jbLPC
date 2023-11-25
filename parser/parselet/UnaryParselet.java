package jbLPC.parser.parselet;

import static jbLPC.compiler.OpCode.OP_NEGATE;
import static jbLPC.compiler.OpCode.OP_NOT;
import static jbLPC.parser.Parser.Precedence.PREC_UNARY;

import jbLPC.compiler.LPCCompiler;
import jbLPC.parser.Parser;
import jbLPC.scanner.TokenType;

public class UnaryParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, LPCCompiler compiler, boolean canAssign) {
    TokenType operatorType = parser.previous().type();

    // Compile the operand.
    parser.parsePrecedence(PREC_UNARY);

    // Emit the operator instruction.
    switch (operatorType) {
      case TOKEN_BANG:
        compiler.emitInstruction(OP_NOT);

        break;
      case TOKEN_MINUS:
        compiler.emitInstruction(OP_NEGATE);

        break;
      default: //Unreachable
        return;
    }
  }
}
