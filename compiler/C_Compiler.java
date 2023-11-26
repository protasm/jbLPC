package jbLPC.compiler;

import static jbLPC.compiler.C_Compilation.C_CompilationType.TYPE_SCRIPT;
import static jbLPC.compiler.C_OpCode.OP_ADD;
import static jbLPC.compiler.C_OpCode.OP_CLOSE_UPVAL;
import static jbLPC.compiler.C_OpCode.OP_CLOSURE;
import static jbLPC.compiler.C_OpCode.OP_DIVIDE;
import static jbLPC.compiler.C_OpCode.OP_GET_GLOBAL;
import static jbLPC.compiler.C_OpCode.OP_GET_LOCAL;
import static jbLPC.compiler.C_OpCode.OP_GLOBAL;
import static jbLPC.compiler.C_OpCode.OP_JUMP;
import static jbLPC.compiler.C_OpCode.OP_JUMP_IF_FALSE;
import static jbLPC.compiler.C_OpCode.OP_LOOP;
import static jbLPC.compiler.C_OpCode.OP_MULTIPLY;
import static jbLPC.compiler.C_OpCode.OP_NIL;
import static jbLPC.compiler.C_OpCode.OP_POP;
import static jbLPC.compiler.C_OpCode.OP_RETURN;
import static jbLPC.compiler.C_OpCode.OP_SET_GLOBAL;
import static jbLPC.compiler.C_OpCode.OP_SET_LOCAL;
import static jbLPC.compiler.C_OpCode.OP_SUBTRACT;
import static jbLPC.parser.Parser.Precedence.PREC_ASSIGNMENT;
import static jbLPC.scanner.TokenType.TOKEN_COMMA;
import static jbLPC.scanner.TokenType.TOKEN_ELSE;
import static jbLPC.scanner.TokenType.TOKEN_EOF;
import static jbLPC.scanner.TokenType.TOKEN_EQUAL;
import static jbLPC.scanner.TokenType.TOKEN_FOR;
import static jbLPC.scanner.TokenType.TOKEN_IDENTIFIER;
import static jbLPC.scanner.TokenType.TOKEN_IF;
import static jbLPC.scanner.TokenType.TOKEN_LEFT_BRACE;
import static jbLPC.scanner.TokenType.TOKEN_LEFT_PAREN;
import static jbLPC.scanner.TokenType.TOKEN_MINUS_EQUAL;
import static jbLPC.scanner.TokenType.TOKEN_PLUS_EQUAL;
import static jbLPC.scanner.TokenType.TOKEN_PRIMITIVE;
import static jbLPC.scanner.TokenType.TOKEN_RETURN;
import static jbLPC.scanner.TokenType.TOKEN_RIGHT_BRACE;
import static jbLPC.scanner.TokenType.TOKEN_RIGHT_PAREN;
import static jbLPC.scanner.TokenType.TOKEN_SEMICOLON;
import static jbLPC.scanner.TokenType.TOKEN_SLASH_EQUAL;
import static jbLPC.scanner.TokenType.TOKEN_STAR_EQUAL;
import static jbLPC.scanner.TokenType.TOKEN_WHILE;

import java.util.List;
import java.util.stream.Collectors;

import jbLPC.debug.Debugger;
import jbLPC.parser.Parser;
import jbLPC.scanner.Token;

public class C_Compiler {
  protected Parser parser;
  protected C_Scope currScope;
  protected C_CompilerClass currClass;

  //C_Compiler()
  public C_Compiler() {
    Debugger.instance().printProgress("Compiler initialized");
  }

  //parser()
  public Parser parser() {
    return parser;
  }

  //currInstrList()
  public C_InstrList currInstrList() {
    return currScope.compilation().instrList();
  }

  //currClass()
  public C_CompilerClass currClass() {
    return currClass;
  }

  //compile(String, String)
  public C_Compilation compile(String name, String source) {
	  parser = new Parser(this, source);
    currScope = new C_Scope(
      null, //enclosing Scope
      new C_Compilation(name, TYPE_SCRIPT)
    );

    Debugger.instance().printProgress("Compiling '" + name + "'");

    //advance to the first non-error Token (or EOF)
    parser.advance();

    //loop declarations until EOF
    while (!parser.match(TOKEN_EOF))
      declaration();

    if (parser.hadError())
      return null;
      
    currInstrList().addInstr(OP_NIL); //return value; always null for a Script
    currInstrList().addInstr(OP_RETURN);

    Debugger.instance().disassembleScope(currScope);
    
    return currScope.compilation();
  }

  //declaration()
  protected void declaration() {
    if (parser.match(TOKEN_PRIMITIVE))
      typedDeclaration();
    else
      statement();

    if (parser.panicMode())
      parser.synchronize();
  }
  
  //typedDeclaration()
  protected void typedDeclaration() {
  if (!parser.check(TOKEN_LEFT_PAREN))
      varDeclaration();
    else
      funDeclaration();
  }

  //varDeclaration()
  protected void varDeclaration() {
	parser.consume(TOKEN_IDENTIFIER, "Expect variable name.");
	
	Token token = parser.previous();
	
	if (currScope.depth() > 0)
      declareLocal(token); //add new local

    if (parser.match(TOKEN_EQUAL))
      expression();
    else
      currInstrList().addInstr(OP_NIL);

    if (currScope.depth() > 0)
      defineLocal(); //mark local available
    else if (currScope.compilation().type() == TYPE_SCRIPT) {
      int index = currInstrList().addConstant(token.lexeme());
      
      currInstrList().addInstr(OP_GLOBAL);
      currInstrList().addInstr((byte)index);
    }

    //handle variable declarations of the form:
    //int x = 99, y, z = "hello";
    if (parser.match(TOKEN_COMMA)) {
      varDeclaration(); //recursive loop

      return;
    }

    parser.consume(TOKEN_SEMICOLON, "Expect ';' after variable declaration(s).");
  }

  //parseVariable(String)
//  protected void parseVariable(String errorMessage) {
//    parser.consume(TOKEN_IDENTIFIER, errorMessage);
//
//    Token token = parser.previous();
//    
//    if (currScope.depth() == 0) { //Global
//      instr.addOperand(token.lexeme());
//      
//      return;
//    }
//    
//    declareLocalVariable(token);
//  }

  //declareLocal(Token)
  private void declareLocal(Token token) {
    //In the locals, a variable is "declared" when it is
    //added to the scope.

    //Guard against an existing local variable with the same name.
    int currScopeDepth = currScope.depth();
    List<C_Local> currScopeLocals = currScope.locals()
      .stream()
      .filter(item -> (item.depth() == -1 || item.depth() == currScopeDepth))
      .collect(Collectors.toList());

    for (C_Local c_Local : currScopeLocals)
      if (identifiersEqual(token, c_Local.token()))
        parser.error("Already a variable with this name in this scope.");

    //Record existence of local variable, with "sentinel" depth for now
    //(i.e. declared but not yet made available for use)
    currScope.locals().push(new C_Local(token, -1));
  }

  //expression()
  public void expression() {
    parser.parsePrecedence(PREC_ASSIGNMENT);
  }

  //defineLocal()
  protected void defineLocal() {
    //In the locals, a variable is "defined" when it
    //becomes available for use.
    currScope.markTopLocalInitialized();

    //No code needed to create a local variable at
    //runtime; it's on top of the stack.
  }

  //addLocal(Token)
//  private void addLocal(Token token) {
//  }

  //addUpvalue(Scope, byte, boolean)
  private int addUpvalue(C_Scope c_Scope, Integer index, boolean isLocal) {
    int upvalueCount = c_Scope.upvalues().size();

    for (int i = 0; i < upvalueCount; i++) {
      C_Upvalue c_Upvalue = c_Scope.getUpvalue(i);

      //isLocal controls whether closure captures a local variable or
      //an upvalue from the surrounding function
      if (c_Upvalue.index() == index && c_Upvalue.isLocal() == isLocal)
        return i;
    }

    //Return index of the created upvalue in the currScope's
    //upvalue list.  That index becomes the operand to the
    //OP_GET_UPVALUE and OP_SET_UPVALUE instructions.
    return c_Scope.addUpvalue(new C_Upvalue(index, isLocal));
  }

  //argumentList()
  public int argumentList() {
    int argCount = 0;

    if (!parser.check(TOKEN_RIGHT_PAREN))
      do {
        expression();

        argCount++;
      } while (parser.match(TOKEN_COMMA));

    parser.consume(TOKEN_RIGHT_PAREN, "Expect ')' after arguments.");

    return argCount;
  }

  //beginScope()
  private void beginScope() {
    currScope.setDepth(currScope.depth() + 1);
  }

  //block()
  protected void block() {
    while (!parser.check(TOKEN_RIGHT_BRACE) && !parser.check(TOKEN_EOF))
      declaration();

    parser.consume(TOKEN_RIGHT_BRACE, "Expect '}' after block.");
  }

  //compoundAssignment(OpCode, OpCode, OpCode, int index)
  protected void compoundAssignment(C_OpCode getOp, C_OpCode setOp, C_OpCode assignOp, int index) {
    currInstrList().addInstr(getOp);
    currInstrList().addInstr((byte)index);

    expression();

    currInstrList().addInstr(assignOp);

    currInstrList().addInstr(setOp);
    currInstrList().addInstr((byte)index);
  }

  //emitInstruction(int)
  public void emitInstruction(int code) {
    emitInstruction((byte) code);
  }

  //emitInstruction(OpCode)
  public void emitInstruction(C_OpCode c_OpCode) {
    emitInstruction(c_OpCode.code());
  }

  //emitInstruction(byte code)
  public void emitInstruction(byte code) {
    if (parser.previous() == null) //may be null for "synthetic" operations
      currInstrList().addInstr(code);
    else
      currInstrList().addInstr(code, parser.previous().line());
  }
  
  //emitConstant(Object)
  public int emitConstant(Object constant) {
    return currInstrList().addConstant(constant);
  }

  //emitJump(OpCode)
  public int emitJump(C_OpCode c_OpCode) {
    emitInstruction(c_OpCode);
    emitInstruction((byte)0xFF); //placeholder, later backpatched

    return currInstrList().instructions().size() - 1;
  }

  //emitLoop(int)
  private void emitLoop(int loopStart) {
    int offset = currInstrList().instructions().size() - loopStart + 2;

    emitInstruction(OP_LOOP);
    emitInstruction(offset);
  }

  //endFunction()
  private C_Function endFunction() {
    emitInstruction(OP_NIL); //return value?
    emitInstruction(OP_RETURN);

    //Extract assembled function from temporary structure.
    C_Function function = (C_Function)currScope.compilation();

    function.setUpvalueCount(currScope.upvalues().size());

    if (!parser.hadError())
      Debugger.instance().disassembleScope(currScope);

    //Step up to higher scope.
    currScope = currScope.enclosing();

    return function;
  }

  //endScope()
  private void endScope() {
    currScope.setDepth(currScope.depth() - 1);

    while (
      !(currScope.locals().isEmpty()) &&
      currScope.locals().peek().depth() > currScope.depth()
    ) {
      if (currScope.locals().get(currScope.locals().size() - 1).isCaptured())
          emitInstruction(OP_CLOSE_UPVAL);
      else
        emitInstruction(OP_POP);

      currScope.locals().pop();
    }
  }

  //expressionStatement()
  private void expressionStatement() {
    expression();

    parser.consume(TOKEN_SEMICOLON, "Expect ';' after expression.");

    emitInstruction(OP_POP);
  }

  //forStatement()
  private void forStatement() {
    beginScope();

    parser.consume(TOKEN_LEFT_PAREN, "Expect '(' after 'for'.");

    //Initializer clause.
    if (parser.match(TOKEN_SEMICOLON)) {
      // No initializer.
    } else if (parser.match(TOKEN_PRIMITIVE))
      varDeclaration();
    else
      expressionStatement();

    int loopStart = currInstrList().instructions().size();

     //Condition clause.
    int exitJump = -1;

    if (!parser.match(TOKEN_SEMICOLON)) {
      expression();

      parser.consume(TOKEN_SEMICOLON, "Expect ';' after loop condition.");

      // Jump out of the loop if the condition is false.
      exitJump = emitJump(OP_JUMP_IF_FALSE);

      emitInstruction(OP_POP); // Condition.
    }

    //Increment clause.
    if (!parser.match(TOKEN_RIGHT_PAREN)) {
      int bodyJump = emitJump(OP_JUMP);
      int incrementStart = currInstrList().instructions().size();

      expression();

      emitInstruction(OP_POP);

      parser.consume(TOKEN_RIGHT_PAREN, "Expect ')' after for clauses.");

      emitLoop(loopStart);

      loopStart = incrementStart;

      patchJump(bodyJump);
    }

    statement();

    emitLoop(loopStart);

    if (exitJump != -1) {
      patchJump(exitJump);

      emitInstruction(OP_POP); // Condition.
    }

    endScope();
  }

  //funDeclaration()
  protected void funDeclaration() {
	  parser.consume(TOKEN_IDENTIFIER, "Expect function name.");
	
    //Function declaration's variable is marked "initialized"
    //before compiling the body so that the name can be
    //referenced inside the body without generating an error.
    currScope.markTopLocalInitialized();

    function();

    currScope.markTopLocalInitialized();
  }

  //function()
  private void function() {
    C_Function function = new C_Function(parser.previous().lexeme());
    C_Scope c_Scope = new C_Scope(
      currScope, //enclosing Scope
      function
    );
    currScope = c_Scope;

    beginScope();

    parser.consume(TOKEN_LEFT_PAREN, "Expect '(' after function name.");

    if (!parser.check(TOKEN_RIGHT_PAREN))
      do {
        function.setArity(function.arity() + 1);

        parser.consume(TOKEN_PRIMITIVE, "Expect type for parameter.");

        parser.consume(TOKEN_IDENTIFIER, "Expect parameter name.");

        currScope.markTopLocalInitialized();
      } while (parser.match(TOKEN_COMMA));

    parser.consume(TOKEN_RIGHT_PAREN, "Expect ')' after parameters.");
    parser.consume(TOKEN_LEFT_BRACE, "Expect '{' before function body.");

    block();

    function = endFunction(); //sets currScope to enclosing

    //TODO: fix
    int index = emitConstant(new Object[] { function, c_Scope.upvalues() });

    emitInstruction(OP_CLOSURE);
    emitInstruction(index);

    //No endScope() needed because Scope is ended completely
    //at the end of the function body.
  }

  //identifiersEqual(Token, Token)
  private boolean identifiersEqual(Token a, Token b) {
    return a.lexeme().equals(b.lexeme());
  }

  //ifStatement()
  private void ifStatement() {
    parser.consume(TOKEN_LEFT_PAREN, "Expect '(' after 'if'.");

    expression();

    parser.consume(TOKEN_RIGHT_PAREN, "Expect ')' after condition.");

    int thenJump = emitJump(OP_JUMP_IF_FALSE);

    emitInstruction(OP_POP);

    statement();

    int elseJump = emitJump(OP_JUMP);

    patchJump(thenJump);

    emitInstruction(OP_POP);

    if (parser.match(TOKEN_ELSE)) statement();

    patchJump(elseJump);
  }

  //namedVariable(Token, boolean)
  //generates code to load a variable with the given name onto the vStack.
  public void namedVariable(Token token, boolean canAssign) {
    C_OpCode getOp, setOp;
    
    int index = resolveLocal(currScope, token);

    if (index != -1) { //local variable
      getOp = OP_GET_LOCAL;
      setOp = OP_SET_LOCAL;
//    } else if ((index = resolveUpvalue(currScope, token)) != -1) { //upvalue
//      getOp = OP_GET_UPVAL;
//      setOp = OP_SET_UPVAL;
    } else { //global variable
      index = emitConstant(token.lexeme());

      getOp = OP_GET_GLOBAL;
      setOp = OP_SET_GLOBAL;
    }

    if (canAssign && parser.match(TOKEN_EQUAL)) { //assignment
      expression();

      emitInstruction(setOp);
      emitInstruction(index);
    } else if (canAssign && parser.match(TOKEN_MINUS_EQUAL))
      compoundAssignment(getOp, setOp, OP_SUBTRACT, index);
    else if (canAssign && parser.match(TOKEN_PLUS_EQUAL))
      compoundAssignment(getOp, setOp, OP_ADD, index);
    else if (canAssign && parser.match(TOKEN_SLASH_EQUAL))
      compoundAssignment(getOp, setOp, OP_DIVIDE, index);
    else if (canAssign && parser.match(TOKEN_STAR_EQUAL))
      compoundAssignment(getOp, setOp, OP_MULTIPLY, index);
    else { //retrieval
      emitInstruction(getOp);
      emitInstruction(index);
    }
  }

  //patchJump(int)
  public void patchJump(int offset) {
    // -1 to adjust for the jump offset itself.
    int jump = currInstrList().instructions().size() - offset - 1;

    currInstrList().instructions().set(offset, (byte)jump);
  }

  //resolveLocal(Scope, Token)
  protected int resolveLocal(C_Scope c_Scope, Token token) {
    //traverse locals backward, looking for a match
	for (int i = c_Scope.locals().size() - 1; i >= 0; i--) {
	    C_Local c_Local = c_Scope.locals().get(i);
	  
	    if (identifiersEqual(token, c_Local.token())) {  //found match
	      if (c_Local.depth() == -1) //"sentinel" depth
	        parser.error("Can't read local variable in its own initializer.");
	    
	      return i;
	    }
	  } 
	    
    //No match, therefore not a local.
    return -1;
  }

  //resolveUpvalue(Scope, Token)
//  protected boolean resolveUpvalue(Scope scope, Token token) {
//    Scope enclosing = scope.enclosing();
//
//    if (enclosing == null) return false;
//
//    boolean local = resolveLocal(enclosing, token);
//
//    if (!local) {
//      enclosing.locals().get(local).setIsCaptured(true);
//
//      //Return index of newly-added Upvalue.
//      addUpvalue(scope, index, true);
//      
//      return true;
//    }
//
//    int upvalue = resolveUpvalue(enclosing, token);
//
//    if (upvalue != -1)
//      return addUpvalue(scope, upvalue, false);
//
//    return false;
//  }

  //returnStatement()
  private void returnStatement() {
    if (currScope.compilation().type() == TYPE_SCRIPT)
      parser.error("Can't return from top-level code.");

    if (parser.match(TOKEN_SEMICOLON)) //no return value provided
      emitInstruction(OP_NIL);
    else { //handle return value
      expression();

      parser.consume(TOKEN_SEMICOLON, "Expect ';' after return value.");
    }

    emitInstruction(OP_RETURN);
  }

  //statement()
  private void statement() {
    if (parser.match(TOKEN_FOR))
      forStatement();
    else if (parser.match(TOKEN_IF))
      ifStatement();
    else if (parser.match(TOKEN_RETURN))
      returnStatement();
    else if (parser.match(TOKEN_WHILE))
      whileStatement();
    else if (parser.match(TOKEN_LEFT_BRACE)) {
      beginScope();

      block();

      endScope();
    } else
      expressionStatement();
  }

  //syntheticToken(String)
  public Token syntheticToken(String text) {
    return new Token(text);
  }

  //whileStatement()
  private void whileStatement() {
    int loopStart = currInstrList().instructions().size();

    parser.consume(TOKEN_LEFT_PAREN, "Expect '(' after 'while'.");

    expression();

    parser.consume(TOKEN_RIGHT_PAREN, "Expect ')' after condition.");

    int exitJump = emitJump(OP_JUMP_IF_FALSE);

    emitInstruction(OP_POP);

    statement();

    emitLoop(loopStart);

    patchJump(exitJump);

    emitInstruction(OP_POP);
  }
}
