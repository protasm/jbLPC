package jbLPC.parser.parselet;

import static jbLPC.compiler.C_OpCode.OP_GET_PROP;
import static jbLPC.compiler.C_OpCode.OP_INVOKE;
import static jbLPC.compiler.C_OpCode.OP_SET_PROP;
import static jbLPC.scanner.TokenType.TOKEN_EQUAL;
import static jbLPC.scanner.TokenType.TOKEN_IDENTIFIER;
import static jbLPC.scanner.TokenType.TOKEN_LEFT_PAREN;

import jbLPC.compiler.C_Compiler;
import jbLPC.parser.Parser;

public class DotParselet implements Parselet {
  //parse(Parser, C_Compiler, boolean)
  public void parse(Parser parser, C_Compiler compiler, boolean canAssign) {
    parser.consume(TOKEN_IDENTIFIER, "Expect property name after '.'.");

    int index = compiler.emitConstant(parser.previous().lexeme());

    if (canAssign && parser.match(TOKEN_EQUAL)) {
      compiler.expression();
      
      compiler.emitCode(OP_SET_PROP);
      compiler.emitCode(index);
    } else if (parser.match(TOKEN_LEFT_PAREN)) {
      int argCount = compiler.argumentList();

      compiler.emitCode(OP_INVOKE);
      compiler.emitCode(index);
      compiler.emitCode(argCount);
    } else {
      compiler.emitCode(OP_GET_PROP);
      compiler.emitCode(index);
    }
  }
}
