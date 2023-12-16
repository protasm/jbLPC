package jbLPC.parser.parselet;

import static jbLPC.compiler.C_OpCode.OP_GET_ITEM;
import static jbLPC.compiler.C_OpCode.OP_SET_ITEM;
import static jbLPC.scanner.TokenType.TOKEN_EQUAL;
import static jbLPC.scanner.TokenType.TOKEN_RIGHT_BRACKET;

import jbLPC.compiler.C_Compiler;
import jbLPC.parser.Parser;

public class IndexParselet implements Parselet {
  //parse(Parser, C_Compiler, boolean)
  public void parse(Parser parser, C_Compiler compiler, boolean canAssign) {
    compiler.expression(); //element index
    
    parser.consume(TOKEN_RIGHT_BRACKET, "Expect ']' after array element index.");
    
    if (parser.match(TOKEN_EQUAL)) { //assignment
      compiler.expression();

      compiler.emitCode(OP_SET_ITEM);
    } else
      compiler.emitCode(OP_GET_ITEM);
  }
}
