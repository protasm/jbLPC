package jbLPC.compiler;

import static jbLPC.compiler.OpCode.OP_ADD;
import static jbLPC.compiler.OpCode.OP_COMPILE_OBJ;
import static jbLPC.compiler.OpCode.OP_DIVIDE;
import static jbLPC.compiler.OpCode.OP_FIELD;
import static jbLPC.compiler.OpCode.OP_GET_LOCAL;
import static jbLPC.compiler.OpCode.OP_GET_PROPERTY;
import static jbLPC.compiler.OpCode.OP_GET_UPVALUE;
import static jbLPC.compiler.OpCode.OP_INHERIT;
import static jbLPC.compiler.OpCode.OP_METHOD;
import static jbLPC.compiler.OpCode.OP_MULTIPLY;
import static jbLPC.compiler.OpCode.OP_NIL;
import static jbLPC.compiler.OpCode.OP_OBJECT;
import static jbLPC.compiler.OpCode.OP_RETURN;
import static jbLPC.compiler.OpCode.OP_SET_LOCAL;
import static jbLPC.compiler.OpCode.OP_SET_PROPERTY;
import static jbLPC.compiler.OpCode.OP_SET_UPVALUE;
import static jbLPC.compiler.OpCode.OP_SUBTRACT;
import static jbLPC.scanner.TokenType.TOKEN_COMMA;
import static jbLPC.scanner.TokenType.TOKEN_EOF;
import static jbLPC.scanner.TokenType.TOKEN_EQUAL;
import static jbLPC.scanner.TokenType.TOKEN_IDENTIFIER;
import static jbLPC.scanner.TokenType.TOKEN_INHERIT;
import static jbLPC.scanner.TokenType.TOKEN_LEFT_PAREN;
import static jbLPC.scanner.TokenType.TOKEN_MINUS_EQUAL;
import static jbLPC.scanner.TokenType.TOKEN_PLUS_EQUAL;
import static jbLPC.scanner.TokenType.TOKEN_SEMICOLON;
import static jbLPC.scanner.TokenType.TOKEN_SLASH_EQUAL;
import static jbLPC.scanner.TokenType.TOKEN_STAR_EQUAL;
import static jbLPC.scanner.TokenType.TOKEN_STRING;

import jbLPC.debug.Debugger;
import jbLPC.parser.Parser;
import jbLPC.scanner.Token;

public class LPCObjectCompiler extends LPCCompiler {
  //compile(String, String)
  @Override
  public Compilation compile(String name, String source) {
    parser = new Parser(this, source);
    currScope = new Scope(
      null, //enclosing Scope
      new C_LPCObject(name) //compilation
    );

    if (debugPrintProgress) Debugger.instance().printProgress("Compiling LPCObject '" + name + "'");

    int index = makeConstant(name);

    emitByte(OP_OBJECT);
    emitWord(index);

    //advance to the first non-error Token (or EOF)
    parser.advance();

    //loop inherit declarations until exhausted
    while(parser.match(TOKEN_INHERIT))
      inherit();

    //loop all remaining declarations until EOF
    while (!parser.match(TOKEN_EOF))
      declaration();
    
    if (parser.hadError())
      return null;
    
    emitByte(OP_RETURN);

    if (debugPrintComp)
      Debugger.instance().disassembleScope(currScope);

    return currScope.compilation();
  }

  //field()
  private void field(int index) {
    if (parser.match(TOKEN_EQUAL))
      expression();
    else
      emitByte(OP_NIL);
    
    defineVariable(index);

    emitByte(OP_FIELD);
    emitWord(index);

    //handle variable declarations of the form:
    //var x = 99, y, z = "hello";
    if (parser.match(TOKEN_COMMA)) {
      parser.consume(TOKEN_IDENTIFIER, "Expect field name.");

      int nextIndex = makeConstant(parser.previous().lexeme());

      field(nextIndex);

      return;
    }

    parser.consume(TOKEN_SEMICOLON, "Expect ';' after variable declaration(s).");
  }

  //inherit()
  protected void inherit() {
    parser.consume(TOKEN_STRING, "Expect inherited object name.");

    //add inherited object name to chunk constants
    int index = stringConstant(parser.previous());
    
    parser.consume(TOKEN_SEMICOLON, "Expect semicolon after inherited object name.");

    //TODO
//    if (identifiersEqual(classToken, parser.previous()))
//      error("A class can't inherit from itself.");

//    beginScope();

//    addLocal(syntheticToken("super"));

//    defineVariable(0x00);

//    namedVariable(classToken, false);
    emitByte(OP_COMPILE_OBJ);
    emitWord(index);

    emitByte(OP_INHERIT);

//    currentClass.setHasSuperclass(true);
  }

  //method(int)
  private void method(int index) {
    funDeclaration(index);

    emitByte(OP_METHOD);
    emitWord(index);
  }

  //namedVariable(Token, boolean)
  //generates code to load a variable with the given name onto the vStack.
  @Override
  public void namedVariable(Token token, boolean canAssign) {
    byte getOp;
    byte setOp;

    int arg = resolveLocal(currScope, token);

    if (arg != -1) { //local variable
      getOp = OP_GET_LOCAL;
      setOp = OP_SET_LOCAL;
    } else if ((arg = resolveUpvalue(currScope, token)) != -1) { //upvalue
      getOp = OP_GET_UPVALUE;
      setOp = OP_SET_UPVALUE;
    } else { //field
//      emitByte(OP_GET_LOCAL);
//      emitWord(0); //correct way to do this?

      arg = identifierConstant(token);

      getOp = OP_GET_PROPERTY;
      setOp = OP_SET_PROPERTY;
    }

    if (canAssign && parser.match(TOKEN_EQUAL)) { //assignment
      expression();

      emitByte(setOp);
      emitWord(arg);
    } else if (canAssign && parser.match(TOKEN_MINUS_EQUAL))
      compoundAssignment(getOp, setOp, OP_SUBTRACT, arg);
    else if (canAssign && parser.match(TOKEN_PLUS_EQUAL))
      compoundAssignment(getOp, setOp, OP_ADD, arg);
    else if (canAssign && parser.match(TOKEN_SLASH_EQUAL))
      compoundAssignment(getOp, setOp, OP_DIVIDE, arg);
    else if (canAssign && parser.match(TOKEN_STAR_EQUAL))
      compoundAssignment(getOp, setOp, OP_MULTIPLY, arg);
    else { //retrieval
      emitByte(getOp);
      emitWord(arg);
    }
  }

  //typedDeclaration()
  @Override
  protected void typedDeclaration() {
    int index = parseVariable("Expect method or field name.");

    if (parser.check(TOKEN_LEFT_PAREN))
      method(index);
    else
      field(index);
  }
}
