package jbLPC.parser.parselet;

import jbLPC.compiler.Compiler;
import jbLPC.scanner.TokenType;

import static jbLPC.compiler.OpCode.*;
import static jbLPC.parser.Parser.Precedence.*;
import static jbLPC.scanner.TokenType.*;

public class UnaryParselet implements Parselet {
  //parse(jbLPC.compiler.Compiler, boolean)
  public void parse(jbLPC.compiler.Compiler compiler, boolean canAssign) {
    TokenType operatorType = compiler.parser().previous().type();

    // Compile the operand.
    compiler.parsePrecedence(PREC_UNARY);

    // Emit the operator instruction.
    switch (operatorType) {
      case TOKEN_BANG:
        compiler.emitByte(OP_NOT);

        break;
      case TOKEN_MINUS:
        compiler.emitByte(OP_NEGATE);

        break;
      default: //Unreachable
        return;
    }
  }
}
