package jbLPC.parser.parselet;

import static jbLPC.compiler.C_OpCode.OP_SUPER_INVOKE;
import static jbLPC.scanner.TokenType.TOKEN_IDENTIFIER;
import static jbLPC.scanner.TokenType.TOKEN_LEFT_PAREN;

import jbLPC.compiler.C_Compiler;
import jbLPC.parser.Parser;

public class SuperParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, C_Compiler compiler, boolean canAssign) {
    //if (compiler.currentClass() == null)
      //parser.error("Can't use 'super' outside of a class.");
    //else if (!(compiler.currentClass().hasSuperclass()))
      //parser.error("Can't use 'super' in a class with no superclass.");

    //parser.consume(TOKEN_DOT, "Expect '.' after 'super'.");

    parser.consume(TOKEN_IDENTIFIER, "Expect inherited function name.");

    int index = compiler.emitConstant(parser.previous().lexeme());

    compiler.namedVariable(compiler.syntheticToken("this"), false);

    parser.consume(TOKEN_LEFT_PAREN, "Expect left parentheses after function name.");

    int argCount = compiler.argumentList();
    
    compiler.namedVariable(compiler.syntheticToken("super"), false);

    compiler.emitCode(OP_SUPER_INVOKE);
    compiler.emitCode(index);
    compiler.emitCode(argCount);
  }
}
