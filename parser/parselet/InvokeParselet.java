package jbLPC.parser.parselet;

import jbLPC.compiler.Compiler;

import static jbLPC.compiler.OpCode.*;
import static jbLPC.parser.Parser.Precedence.*;
import static jbLPC.scanner.TokenType.*;

public class InvokeParselet implements Parselet {
  //parse(jbLPC.compiler.Compiler, boolean)
  public void parse(jbLPC.compiler.Compiler compiler, boolean canAssign) {
    compiler.consume(TOKEN_IDENTIFIER, "Expect function name after '->'.");

    int index = compiler.makeConstant(compiler.parser().previous().lexeme());

    compiler.consume(TOKEN_LEFT_PAREN, "Expect left parentheses after function name.");

    byte argCount = compiler.argumentList();

    compiler.emitByte(OP_INVOKE);
    compiler.emitWord((short)index);
    compiler.emitByte(argCount);
  }
}
