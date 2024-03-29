package jbLPC.compiler;

import static jbLPC.compiler.C_Compilation.C_CompilationType.TYPE_OBJECT;
import static jbLPC.compiler.C_OpCode.OP_ADD;
import static jbLPC.compiler.C_OpCode.OP_COMPILE;
import static jbLPC.compiler.C_OpCode.OP_DIVIDE;
import static jbLPC.compiler.C_OpCode.OP_FIELD;
import static jbLPC.compiler.C_OpCode.OP_GET_LOCAL;
import static jbLPC.compiler.C_OpCode.OP_GET_PROP;
import static jbLPC.compiler.C_OpCode.OP_GET_UPVAL;
import static jbLPC.compiler.C_OpCode.OP_INHERIT;
import static jbLPC.compiler.C_OpCode.OP_METHOD;
import static jbLPC.compiler.C_OpCode.OP_MULTIPLY;
import static jbLPC.compiler.C_OpCode.OP_NIL;
import static jbLPC.compiler.C_OpCode.OP_OBJECT;
import static jbLPC.compiler.C_OpCode.OP_RETURN;
import static jbLPC.compiler.C_OpCode.OP_SET_LOCAL;
import static jbLPC.compiler.C_OpCode.OP_SET_PROP;
import static jbLPC.compiler.C_OpCode.OP_SET_UPVAL;
import static jbLPC.compiler.C_OpCode.OP_SUBTRACT;
import static jbLPC.scanner.TokenType.TOKEN_COMMA;
import static jbLPC.scanner.TokenType.TOKEN_EOF;
import static jbLPC.scanner.TokenType.TOKEN_EQUAL;
//import static jbLPC.scanner.TokenType.TOKEN_IDENTIFIER;
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

public class C_ObjectCompiler extends C_Compiler {
  public static Map<Path, C_Compilation> compiledObjects = new HashMap<>();
  
  //C_ObjectCompiler()
  public C_ObjectCompiler(Debugger debugger) {
    super(debugger);
  }

  //compile(Path, String, String)
  public C_Compilation compile(Path path, String prefix, String source) {
    if (C_ObjectCompiler.compiledObjects.containsKey(path))
      return C_ObjectCompiler.compiledObjects.get(path);

    parser = new Parser(this, debugger, source);
    currScope = new C_Scope(
      null, //enclosing Scope
      new C_Compilation(prefix, TYPE_OBJECT) //compilation
    );

    debugger.printProgress("Compiling LPCObject '" + prefix + "'");

    int index = emitConstant(prefix);

    emitCode(OP_OBJECT);
    emitCode(index);

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
    emitCode(OP_RETURN);

    debugger.disassembleScope(currScope);

    C_Compilation compiledObject = currScope.compilation();
    
    //store this compilation to avoid future recompilation
    C_ObjectCompiler.compiledObjects.put(path, compiledObject);

    return compiledObject;
  }

  //fieldDeclaration(int)
  private void fieldDeclaration(int index) {
    if (parser.match(TOKEN_EQUAL))
      expression();
    else
      emitCode(OP_NIL);

    defineVariable(index);

    emitCode(OP_FIELD);
    emitCode(index);

    //handle variable declarations of the form:
    //var x = 99, y, z = "hello";
    if (parser.match(TOKEN_COMMA)) {
      index = parseVariable("Expect field name.");
      
      fieldDeclaration(index);

      return;
    }

    parser.consume(TOKEN_SEMICOLON, "Expect ';' after variable declaration(s).");
  }

  protected void methodDeclaration(int index) {
	  funDeclaration(index);
	  
	  emitCode(OP_METHOD);
	  emitCode(index);
  }

  //inherit()
  protected void inherit() {
    parser.consume(TOKEN_STRING, "Expect inherited object name.");
    
    int index = emitConstant(parser.previous().literal());

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
      
    emitCode(OP_COMPILE);
    emitCode(index);

    emitCode(OP_INHERIT);

//    currentClass.setHasSuperclass(true);
  }

  //namedVariable(Token, boolean)
  //generates code to load a variable, whose name equals the
  //given Token's lexeme, onto the vStack.
  @Override
  public void namedVariable(Token token, boolean canAssign) {
    byte getOp, setOp;

    int index = resolveLocal(currScope, token);

    if (index != -1) { //local variable
      getOp = OP_GET_LOCAL;
      setOp = OP_SET_LOCAL;
    } else if ((index = resolveUpvalue(currScope, token)) != -1) { //upvalue
      getOp = OP_GET_UPVAL;
      setOp = OP_SET_UPVAL;
    } else { //property
      index = emitConstant(token.lexeme()); //property name
      
      //load LPC object in which property lives
      emitCode(OP_GET_LOCAL);
      emitCode(0x00); //Controversial

      getOp = OP_GET_PROP;
      setOp = OP_SET_PROP;
    }

    if (canAssign && parser.match(TOKEN_EQUAL)) { //assignment
      expression();

      emitCode(setOp);
      emitCode(index);
    } else if (canAssign && parser.match(TOKEN_MINUS_EQUAL))
      compoundAssignment(getOp, setOp, OP_SUBTRACT, index);
    else if (canAssign && parser.match(TOKEN_PLUS_EQUAL))
      compoundAssignment(getOp, setOp, OP_ADD, index);
    else if (canAssign && parser.match(TOKEN_SLASH_EQUAL))
      compoundAssignment(getOp, setOp, OP_DIVIDE, index);
    else if (canAssign && parser.match(TOKEN_STAR_EQUAL))
      compoundAssignment(getOp, setOp, OP_MULTIPLY, index);
    else { //retrieval
      emitCode(getOp);
      emitCode(index);
    }
  }

  //typedDeclaration()
  @Override
  protected void typedDeclaration() {
	int index = parseVariable("Expect field or method name.");
	
    if (!parser.check(TOKEN_LEFT_PAREN))
      fieldDeclaration(index);
    else {
      methodDeclaration(index);
    }
  }
}
