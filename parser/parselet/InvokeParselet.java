package jbLPC.parser.parselet;

import static jbLPC.compiler.OpCode.OP_INVOKE;
import static jbLPC.scanner.TokenType.TOKEN_IDENTIFIER;
import static jbLPC.scanner.TokenType.TOKEN_LEFT_PAREN;

import jbLPC.compiler.LPCCompiler;
import jbLPC.parser.Parser;

public class InvokeParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, LPCCompiler compiler, boolean canAssign) {
    parser.consume(TOKEN_IDENTIFIER, "Expect method name after '->'.");

    //method name
    int index = compiler.identifierConstant(parser.previous());

    parser.consume(TOKEN_LEFT_PAREN, "Expect left parentheses after method name.");

    byte argCount = compiler.argumentList();

    compiler.emitByte(OP_INVOKE);
    compiler.emitWord(index); //method name
    compiler.emitByte(argCount);
  }
}
