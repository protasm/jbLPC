package jbLPC.parser.parselet;

import jbLPC.compiler.Compiler;

import static jbLPC.compiler.OpCode.*;
import static jbLPC.scanner.TokenType.*;

public class SuperParselet implements Parselet {
  //parse(jbLPC.compiler.Compiler, boolean)
  public void parse(jbLPC.compiler.Compiler compiler, boolean canAssign) {
    //if (compiler.currentClass() == null)
      //compiler.error("Can't use 'super' outside of a class.");
    //else if (!(compiler.currentClass().hasSuperclass()))
      //compiler.error("Can't use 'super' in a class with no superclass.");

    //compiler.consume(TOKEN_DOT, "Expect '.' after 'super'.");

    compiler.consume(TOKEN_IDENTIFIER, "Expect inherited function name.");

    int nameIdx = compiler.makeConstant(compiler.parser().previous().lexeme());

    compiler.namedVariable(compiler.syntheticToken("this"), false);

    compiler.consume(TOKEN_LEFT_PAREN, "Expect left parentheses after function name.");

    byte argCount = compiler.argumentList();
    compiler.namedVariable(compiler.syntheticToken("super"), false);

    compiler.emitByte(OP_SUPER_INVOKE);
    compiler.emitWord((short)nameIdx);
    compiler.emitByte(argCount);
  }
}
