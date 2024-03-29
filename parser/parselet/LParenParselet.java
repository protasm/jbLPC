package jbLPC.parser.parselet;

import static jbLPC.compiler.C_OpCode.OP_ARRAY;
import static jbLPC.compiler.C_OpCode.OP_MAPPING;

import static jbLPC.scanner.TokenType.TOKEN_LEFT_BRACE;
import static jbLPC.scanner.TokenType.TOKEN_LEFT_BRACKET;
import static jbLPC.scanner.TokenType.TOKEN_RIGHT_PAREN;

import jbLPC.compiler.C_Compiler;
import jbLPC.parser.Parser;

public class LParenParselet implements Parselet {
  //parse(Parser, C_Compiler, boolean)
  public void parse(Parser parser, C_Compiler compiler, boolean canAssign) {
    if (parser.match(TOKEN_LEFT_BRACE)) { //array
      int elementCount = compiler.array();
      
      compiler.emitCode(OP_ARRAY);
      compiler.emitCode(elementCount);
    } else if (parser.match(TOKEN_LEFT_BRACKET)) { //mapping
      int entryCount = compiler.mapping();
      
      compiler.emitCode(OP_MAPPING);
      compiler.emitCode(entryCount);
    } else { //grouping
      compiler.expression();

      parser.consume(TOKEN_RIGHT_PAREN, "Expect ')' after expression.");
    }
  }
}