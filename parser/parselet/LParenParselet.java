package jbLPC.parser.parselet;

import static jbLPC.compiler.C_OpCode.OP_ARRAY;

import static jbLPC.scanner.TokenType.TOKEN_LEFT_BRACE;
import static jbLPC.scanner.TokenType.TOKEN_RIGHT_PAREN;

import jbLPC.compiler.C_Compiler;
import jbLPC.parser.Parser;

public class LParenParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, C_Compiler compiler, boolean canAssign) {
    if (parser.match(TOKEN_LEFT_BRACE)) { //array
      int elementCount = compiler.array();
      
      compiler.emitCode(OP_ARRAY);
      compiler.emitCode(elementCount);
    } else { //grouping
      compiler.expression();

      parser.consume(TOKEN_RIGHT_PAREN, "Expect ')' after expression.");
    }
  }
}