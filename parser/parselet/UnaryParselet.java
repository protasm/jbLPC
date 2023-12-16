package jbLPC.parser.parselet;

import static jbLPC.compiler.C_OpCode.OP_NEGATE;
import static jbLPC.compiler.C_OpCode.OP_NOT;
import static jbLPC.parser.Parser.Precedence.PREC_UNARY;

import jbLPC.compiler.C_Compiler;
import jbLPC.parser.Parser;
import jbLPC.scanner.TokenType;

public class UnaryParselet implements Parselet {
  //parse(Parser, C_Compiler, boolean)
  public void parse(Parser parser, C_Compiler compiler, boolean canAssign) {
    TokenType operatorType = parser.previous().type();

    // Compile the operand.
    parser.parsePrecedence(PREC_UNARY);

    // Emit the operator instruction.
    switch (operatorType) {
      case TOKEN_BANG:
        compiler.emitCode(OP_NOT);

        break;
      case TOKEN_MINUS:
        compiler.emitCode(OP_NEGATE);

        break;
      default: //Unreachable
        return;
    }
  }
}
