package jbLPC.parser.parselet;

import static jbLPC.compiler.C_OpCode.OP_INVOKE;
import static jbLPC.scanner.TokenType.TOKEN_IDENTIFIER;
import static jbLPC.scanner.TokenType.TOKEN_LEFT_PAREN;

import jbLPC.compiler.C_Compiler;
import jbLPC.parser.Parser;

public class InvokeParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, C_Compiler compiler, boolean canAssign) {
    parser.consume(TOKEN_IDENTIFIER, "Expect method name after '->'.");

    //method name
    int op1 = compiler.emitConstant(parser.previous().lexeme());

    parser.consume(TOKEN_LEFT_PAREN, "Expect left parentheses after method name.");

    //arg count
    int op2 = compiler.argumentList();

    compiler.emitCode(OP_INVOKE);
    compiler.emitCode(op1);
    compiler.emitCode(op2);
  }
}
