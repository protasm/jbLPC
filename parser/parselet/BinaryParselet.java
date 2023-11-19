package jbLPC.parser.parselet;

import static jbLPC.compiler.OpCode.OP_ADD;
import static jbLPC.compiler.OpCode.OP_DIVIDE;
import static jbLPC.compiler.OpCode.OP_EQUAL;
import static jbLPC.compiler.OpCode.OP_GREATER;
import static jbLPC.compiler.OpCode.OP_LESS;
import static jbLPC.compiler.OpCode.OP_MULTIPLY;
import static jbLPC.compiler.OpCode.OP_NOT;
import static jbLPC.compiler.OpCode.OP_SUBTRACT;

import jbLPC.compiler.LPCCompiler;
import jbLPC.parser.ParseRule;
import jbLPC.parser.Parser;
import jbLPC.scanner.TokenType;

public class BinaryParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, LPCCompiler compiler, boolean canAssign) {
    TokenType operatorType = parser.previous().type();
    ParseRule rule = parser.getRule(operatorType);

    parser.parsePrecedence(rule.precedence() + 1);

    switch (operatorType) {
      case TOKEN_BANG_EQUAL:
        compiler.emitByte(OP_EQUAL);
        compiler.emitByte(OP_NOT);

        break;
      case TOKEN_EQUAL_EQUAL:
        compiler.emitByte(OP_EQUAL);

        break;
      case TOKEN_GREATER:
        compiler.emitByte(OP_GREATER);

        break;
      case TOKEN_GREATER_EQUAL:
        compiler.emitByte(OP_LESS);
        compiler.emitByte(OP_NOT);

        break;
      case TOKEN_LESS:
        compiler.emitByte(OP_LESS);

        break;
      case TOKEN_LESS_EQUAL:
        compiler.emitByte(OP_GREATER);
        compiler.emitByte(OP_NOT);

        break;
      case TOKEN_PLUS:
        compiler.emitByte(OP_ADD);

        break;
      case TOKEN_MINUS:
        compiler.emitByte(OP_SUBTRACT);

        break;
      case TOKEN_STAR:
        compiler.emitByte(OP_MULTIPLY);

        break;
      case TOKEN_SLASH:
        compiler.emitByte(OP_DIVIDE);

        break;
      case TOKEN_PLUS_EQUAL:
        break;
      default:  //Unreachable
        return;
    }
  }
}
