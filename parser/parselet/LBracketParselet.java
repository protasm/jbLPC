package jbLPC.parser.parselet;

import static jbLPC.compiler.C_OpCode.OP_GET_ELEMENT;
import static jbLPC.scanner.TokenType.TOKEN_RIGHT_BRACKET;

import jbLPC.compiler.C_Compiler;
import jbLPC.parser.Parser;

public class LBracketParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, C_Compiler compiler, boolean canAssign) {
    compiler.expression(); //element index
    
    parser.consume(TOKEN_RIGHT_BRACKET, "Expect ']' after array element index.");

    compiler.emitCode(OP_GET_ELEMENT);
  }
}
