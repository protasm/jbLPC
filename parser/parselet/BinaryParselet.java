package jbLPC.parser.parselet;

import static jbLPC.compiler.C_OpCode.OP_ADD;
import static jbLPC.compiler.C_OpCode.OP_DIVIDE;
import static jbLPC.compiler.C_OpCode.OP_EQUAL;
import static jbLPC.compiler.C_OpCode.OP_GREATER;
import static jbLPC.compiler.C_OpCode.OP_LESS;
import static jbLPC.compiler.C_OpCode.OP_MULTIPLY;
import static jbLPC.compiler.C_OpCode.OP_NOT;
import static jbLPC.compiler.C_OpCode.OP_SUBTRACT;

import jbLPC.compiler.C_Compiler;
import jbLPC.parser.ParseRule;
import jbLPC.parser.Parser;
import jbLPC.scanner.TokenType;

public class BinaryParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, C_Compiler compiler, boolean canAssign) {
    TokenType operatorType = parser.previous().type();
    ParseRule rule = parser.getRule(operatorType);

    parser.parsePrecedence(rule.precedence() + 1);

    switch (operatorType) {
      case TOKEN_BANG_EQUAL:
        compiler.emitCode(OP_EQUAL);
        compiler.emitCode(OP_NOT);

        break;
      case TOKEN_EQUAL_EQUAL:
        compiler.emitCode(OP_EQUAL);

        break;
      case TOKEN_GREATER:
        compiler.emitCode(OP_GREATER);

        break;
      case TOKEN_GREATER_EQUAL:
        compiler.emitCode(OP_LESS);
        compiler.emitCode(OP_NOT);

        break;
      case TOKEN_LESS:
        compiler.emitCode(OP_LESS);

        break;
      case TOKEN_LESS_EQUAL:
        compiler.emitCode(OP_GREATER);
        compiler.emitCode(OP_NOT);

        break;
      case TOKEN_PLUS:
        compiler.emitCode(OP_ADD);

        break;
      case TOKEN_MINUS:
        compiler.emitCode(OP_SUBTRACT);

        break;
      case TOKEN_STAR:
        compiler.emitCode(OP_MULTIPLY);

        break;
      case TOKEN_SLASH:
        compiler.emitCode(OP_DIVIDE);

        break;
      case TOKEN_PLUS_EQUAL:
        break;
      default:  //Unreachable
        return;
    }
  }
}
