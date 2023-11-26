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
    int index = compiler.emitConstant(parser.previous().lexeme());

    parser.consume(TOKEN_LEFT_PAREN, "Expect left parentheses after method name.");

    int argCount = compiler.argumentList();

    compiler.emitCode(OP_INVOKE);
    compiler.emitCode(index);
    compiler.emitCode(argCount);
  }
}
