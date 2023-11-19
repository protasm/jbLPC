package jbLPC.parser.parselet;

import jbLPC.compiler.LPCCompiler;


import jbLPC.parser.Parser;

import static jbLPC.compiler.OpCode.*;
import static jbLPC.scanner.TokenType.*;

public class SuperParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, LPCCompiler compiler, boolean canAssign) {
    //if (compiler.currentClass() == null)
      //parser.error("Can't use 'super' outside of a class.");
    //else if (!(compiler.currentClass().hasSuperclass()))
      //parser.error("Can't use 'super' in a class with no superclass.");

    //parser.consume(TOKEN_DOT, "Expect '.' after 'super'.");

    parser.consume(TOKEN_IDENTIFIER, "Expect inherited function name.");

    int nameIdx = compiler.identifierConstant(parser.previous());

    compiler.namedVariable(compiler.syntheticToken("this"), false);

    parser.consume(TOKEN_LEFT_PAREN, "Expect left parentheses after function name.");

    byte argCount = compiler.argumentList();
    compiler.namedVariable(compiler.syntheticToken("super"), false);

    compiler.emitByte(OP_SUPER_INVOKE);
    compiler.emitWord((short)nameIdx);
    compiler.emitByte(argCount);
  }
}
