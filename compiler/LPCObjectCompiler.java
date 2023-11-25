package jbLPC.compiler;

import static jbLPC.compiler.OpCode.OP_ADD;
import static jbLPC.compiler.OpCode.OP_COMPILE;
import static jbLPC.compiler.OpCode.OP_DIVIDE;
import static jbLPC.compiler.OpCode.OP_FIELD;
import static jbLPC.compiler.OpCode.OP_GET_LOCAL;
import static jbLPC.compiler.OpCode.OP_GET_PROP;
import static jbLPC.compiler.OpCode.OP_GET_UPVAL;
import static jbLPC.compiler.OpCode.OP_INHERIT;
import static jbLPC.compiler.OpCode.OP_METHOD;
import static jbLPC.compiler.OpCode.OP_MULTIPLY;
import static jbLPC.compiler.OpCode.OP_NIL;
import static jbLPC.compiler.OpCode.OP_OBJECT;
import static jbLPC.compiler.OpCode.OP_RETURN;
import static jbLPC.compiler.OpCode.OP_SET_LOCAL;
import static jbLPC.compiler.OpCode.OP_SET_PROP;
import static jbLPC.compiler.OpCode.OP_SET_UPVAL;
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

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import jbLPC.debug.Debugger;
import jbLPC.parser.Parser;
import jbLPC.scanner.Token;
import jbLPC.util.Props;

public class LPCObjectCompiler extends LPCCompiler {
  public static Map<Path, C_LPCObject> compiledObjects = new HashMap<>();
  
  //compile(String, String)
  public C_LPCObject compile(Path path, String prefix, String source) {
    if (LPCObjectCompiler.compiledObjects.containsKey(path))
      return LPCObjectCompiler.compiledObjects.get(path);

    parser = new Parser(this, source);
    currScope = new Scope(
      null, //enclosing Scope
      new C_LPCObject(prefix) //compilation
    );

    if (debugPrintProgress) Debugger.instance().printProgress("Compiling LPCObject '" + prefix + "'");

    int index = makeConstant(prefix);

    emitOpCode(OP_OBJECT);
    emitArgCode(index);

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

    //end compilation
    emitOpCode(OP_RETURN);

    if (debugPrintComp)
      Debugger.instance().disassembleScope(currScope);

    C_LPCObject compiledObject = (C_LPCObject)currScope.compilation();
    
    //store this compilation to avoid future recompilation
    LPCObjectCompiler.compiledObjects.put(path, compiledObject);

    return compiledObject;
  }

  //field()
  private void field(int index) {
    if (parser.match(TOKEN_EQUAL))
      expression();
    else
      emitOpCode(OP_NIL);

    defineVariable(index);

    emitOpCode(OP_FIELD);
    emitArgCode(index);

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
    
    int index = stringConstant(parser.previous());

    parser.consume(TOKEN_SEMICOLON, "Expect semicolon after inherited object name.");

    //TODO
//    if (identifiersEqual(classToken, parser.previous()))
//      error("A class can't inherit from itself.");

//    beginScope();

//    if (currScope.locals().size() >= Props.instance().getInt("MAX_SIGNED_BYTE")) {
//      parser.error("Too many local variables in function.");
//
//      return;
//    }

    //Record existence of local variable.
    //Token token = syntheticToken("super"));
//    currScope.locals().push(new Local(token, -1));

//    defineVariable(0x00);

//    namedVariable(classToken, false);
      
    emitOpCode(OP_COMPILE);
    emitArgCode(index);

    emitOpCode(OP_INHERIT);

//    currentClass.setHasSuperclass(true);
  }

  //objectConstant(Object)
  public int objectConstant(Object object) {
    return makeConstant(object);
  }

  //method(int)
  private void method(int index) {
    funDeclaration(index);

    emitOpCode(OP_METHOD);
    emitArgCode(index);
  }

  //namedVariable(Token, boolean)
  //generates code to load a variable, whose name equals the
  //given Token's lexeme, onto the vStack.
  @Override
  public void namedVariable(Token token, boolean canAssign) {
    OpCode getOp;
    OpCode setOp;

    int arg = resolveLocal(currScope, token); //index of local var, or -1

    if (arg != -1) { //local variable
      getOp = OP_GET_LOCAL;
      setOp = OP_SET_LOCAL;
    } else if ((arg = resolveUpvalue(currScope, token)) != -1) { //upvalue
      getOp = OP_GET_UPVAL;
      setOp = OP_SET_UPVAL;
    } else { //field
      emitOpCode(OP_GET_LOCAL);
      emitArgCode(0); //correct way to do this?

      arg = identifierConstant(token);

      getOp = OP_GET_PROP;
      setOp = OP_SET_PROP;
    }

    if (canAssign && parser.match(TOKEN_EQUAL)) { //assignment
      expression();

      emitOpCode(setOp);
      emitArgCode(arg);
    } else if (canAssign && parser.match(TOKEN_MINUS_EQUAL))
      compoundAssignment(getOp, setOp, OP_SUBTRACT, arg);
    else if (canAssign && parser.match(TOKEN_PLUS_EQUAL))
      compoundAssignment(getOp, setOp, OP_ADD, arg);
    else if (canAssign && parser.match(TOKEN_SLASH_EQUAL))
      compoundAssignment(getOp, setOp, OP_DIVIDE, arg);
    else if (canAssign && parser.match(TOKEN_STAR_EQUAL))
      compoundAssignment(getOp, setOp, OP_MULTIPLY, arg);
    else { //retrieval
      emitOpCode(getOp);
      emitArgCode(arg);
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
