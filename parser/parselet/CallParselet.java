package jbLPC.parser.parselet;

import jbLPC.compiler.Compiler;

import static jbLPC.compiler.OpCode.*;
import static jbLPC.parser.Parser.Precedence.*;
import static jbLPC.scanner.TokenType.*;

public class CallParselet implements Parselet {
  //parse(jbLPC.compiler.Compiler, boolean)
  public void parse(jbLPC.compiler.Compiler compiler, boolean canAssign) {
    byte argCount = compiler.argumentList();

    compiler.emitByte(OP_CALL);
    compiler.emitByte(argCount);
  }
}
