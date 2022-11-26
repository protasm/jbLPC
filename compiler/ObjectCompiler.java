package jbLPC.compiler;

import jbLPC.debug.Debugger;
import jbLPC.parser.Parser;
import jbLPC.scanner.Scanner;
import jbLPC.scanner.Token;

import static jbLPC.compiler.Function.FunctionType.*;
import static jbLPC.compiler.OpCode.*;
import static jbLPC.scanner.TokenType.*;

public class ObjectCompiler extends Compiler {
  //compile(String, String)
  public Function compile(String name, String source) {
    parser = new Parser();
    tokens = new Scanner(source);

    currScope = new Scope(
      null, //enclosing Scope
      TYPE_OBJECT //FunctionType
    );

    if (debugPrintProgress) Debugger.instance().printProgress("Compiling Object....");

    //Add object name to current Chunk's constants
    int constantIdx = makeConstant(name);

    //VM: create and push new LoxClass
    emitByte(OP_OBJECT);
    emitWord(constantIdx);

    //advance to the first non-error Token (or EOF)
    advance();

    while (!match(TOKEN_EOF))
      declaration();

    Function function = endCompilation(false);

    return parser.hadError() ? null : function;
  }

  //declaration()
  @Override
  protected void declaration() {
    consume(TOKEN_TYPE, "Expect method or field type.");
    consume(TOKEN_IDENTIFIER, "Expect method or field name.");

    int constantsIdx = makeConstant(parser.previous().lexeme());

    if (check(TOKEN_LEFT_PAREN))
      method(constantsIdx);
    else
      field(constantsIdx);
    //declareVariable();

    //defineVariable(nameConstantIdx);

    //emitByte(OP_DEFINE_GLOBAL);
    //emitWord(constantIdx);

    //currClass =
      //new CompilerClass(
        //currClass, //enclosing
        //false         //hasSuperClass
      //);

    // Handle inheritance.
/*
    if (match(TOKEN_LESS)) {
      consume(TOKEN_IDENTIFIER, "Expect superclass name.");

      new VariableParselet().parse(this, false);

      if (identifiersEqual(classToken, parser.previous()))
        error("A class can't inherit from itself.");

      beginScope();

      addLocal(syntheticToken("super"));

      defineVariable(0x00);

      namedVariable(classToken, false);

      emitByte(OP_INHERIT);

      currClass.setHasSuperclass(true);
    } //if (match(TOKEN_LESS))
*/

    //load class back onto the stack
    //namedVariable(objToken, false);

    //emitByte(OP_POP);

    //if (currClass.hasSuperclass())
      //endScope();

    //currClass = currClass.enclosing();
  }

  //method()
  private void method(int index) {
    function(TYPE_OBJ_METHOD);

    emitByte(OP_DEFINE_METHOD);
    emitWord(index);
  }

  //field()
  private void field(int index) {
    if (match(TOKEN_EQUAL))
      expression();
    else
      emitByte(OP_NIL);

    emitByte(OP_DEFINE_FIELD);
    emitWord(index);

    //handle variable declarations of the form:
    //var x = 99, y, z = "hello";
    if (match(TOKEN_COMMA)) {
      consume(TOKEN_IDENTIFIER, "Expect field name.");

      int nextIndex = makeConstant(parser.previous().lexeme());

      field(nextIndex);

      return;
    }

    consume(TOKEN_SEMICOLON, "Expect ';' after variable declaration(s).");
  }
}
