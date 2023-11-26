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
        compiler.emitInstruction(OP_EQUAL);
        compiler.emitInstruction(OP_NOT);

        break;
      case TOKEN_EQUAL_EQUAL:
        compiler.emitInstruction(OP_EQUAL);

        break;
      case TOKEN_GREATER:
        compiler.emitInstruction(OP_GREATER);

        break;
      case TOKEN_GREATER_EQUAL:
        compiler.emitInstruction(OP_LESS);
        compiler.emitInstruction(OP_NOT);

        break;
      case TOKEN_LESS:
        compiler.emitInstruction(OP_LESS);

        break;
      case TOKEN_LESS_EQUAL:
        compiler.emitInstruction(OP_GREATER);
        compiler.emitInstruction(OP_NOT);

        break;
      case TOKEN_PLUS:
        compiler.emitInstruction(OP_ADD);

        break;
      case TOKEN_MINUS:
        compiler.emitInstruction(OP_SUBTRACT);

        break;
      case TOKEN_STAR:
        compiler.emitInstruction(OP_MULTIPLY);

        break;
      case TOKEN_SLASH:
        compiler.emitInstruction(OP_DIVIDE);

        break;
      case TOKEN_PLUS_EQUAL:
        break;
      default:  //Unreachable
        return;
    }
  }
}
