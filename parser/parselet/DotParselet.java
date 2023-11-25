package jbLPC.parser.parselet;

import static jbLPC.compiler.OpCode.OP_GET_PROP;
import static jbLPC.compiler.OpCode.OP_INVOKE;
import static jbLPC.compiler.OpCode.OP_SET_PROP;
import static jbLPC.scanner.TokenType.TOKEN_EQUAL;
import static jbLPC.scanner.TokenType.TOKEN_IDENTIFIER;
import static jbLPC.scanner.TokenType.TOKEN_LEFT_PAREN;

import jbLPC.compiler.Instruction;
import jbLPC.compiler.LPCCompiler;
import jbLPC.parser.Parser;

public class DotParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, LPCCompiler compiler, boolean canAssign) {
    parser.consume(TOKEN_IDENTIFIER, "Expect property name after '.'.");

    int index = compiler.identifierConstant(parser.previous());
    Instruction instr;

    if (canAssign && parser.match(TOKEN_EQUAL)) {
      compiler.expression();
      
      instr = new Instruction(OP_SET_PROP, index);
    } else if (parser.match(TOKEN_LEFT_PAREN)) {
      Integer argCount = compiler.argumentList();

      instr = new Instruction(
        OP_INVOKE,
        new Object[] { index, argCount }
      );
    } else
      instr = new Instruction(OP_GET_PROP, index);
    
    compiler.emitInstruction(instr);
  }
}
