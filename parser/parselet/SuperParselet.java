package jbLPC.parser.parselet;

import static jbLPC.compiler.OpCode.OP_SUPER_INVOKE;
import static jbLPC.scanner.TokenType.TOKEN_IDENTIFIER;
import static jbLPC.scanner.TokenType.TOKEN_LEFT_PAREN;

import jbLPC.compiler.Instruction;
import jbLPC.compiler.LPCCompiler;
import jbLPC.parser.Parser;

public class SuperParselet implements Parselet {
  //parse(Parser, LPCCompiler, boolean)
  public void parse(Parser parser, LPCCompiler compiler, boolean canAssign) {
    //if (compiler.currentClass() == null)
      //parser.error("Can't use 'super' outside of a class.");
    //else if (!(compiler.currentClass().hasSuperclass()))
      //parser.error("Can't use 'super' in a class with no superclass.");

    //parser.consume(TOKEN_DOT, "Expect '.' after 'super'.");

    parser.consume(TOKEN_IDENTIFIER, "Expect inherited function name.");

    int index = compiler.identifierConstant(parser.previous());

    compiler.namedVariable(compiler.syntheticToken("this"), false);

    parser.consume(TOKEN_LEFT_PAREN, "Expect left parentheses after function name.");

    int argCount = compiler.argumentList();
    
    compiler.namedVariable(compiler.syntheticToken("super"), false);

    Instruction instr = new Instruction(
      OP_SUPER_INVOKE,
      new Object[] { index, argCount }
    );
    
    compiler.emitInstruction(instr);
  }
}
