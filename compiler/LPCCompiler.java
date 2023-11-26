package jbLPC.compiler;

import static jbLPC.compiler.OpCode.OP_ADD;
import static jbLPC.compiler.OpCode.OP_CLOSE_UPVAL;
import static jbLPC.compiler.OpCode.OP_CLOSURE;
import static jbLPC.compiler.OpCode.OP_CONST;
import static jbLPC.compiler.OpCode.OP_DIVIDE;
import static jbLPC.compiler.OpCode.OP_GET_GLOBAL;
import static jbLPC.compiler.OpCode.OP_GET_LOCAL;
import static jbLPC.compiler.OpCode.OP_GET_UPVAL;
import static jbLPC.compiler.OpCode.OP_GLOBAL;
import static jbLPC.compiler.OpCode.OP_JUMP;
import static jbLPC.compiler.OpCode.OP_JUMP_IF_FALSE;
import static jbLPC.compiler.OpCode.OP_LOOP;
import static jbLPC.compiler.OpCode.OP_MULTIPLY;
import static jbLPC.compiler.OpCode.OP_NIL;
import static jbLPC.compiler.OpCode.OP_POP;
import static jbLPC.compiler.OpCode.OP_RETURN;
import static jbLPC.compiler.OpCode.OP_SET_GLOBAL;
import static jbLPC.compiler.OpCode.OP_SET_LOCAL;
import static jbLPC.compiler.OpCode.OP_SET_UPVAL;
import static jbLPC.compiler.OpCode.OP_SUBTRACT;
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
import jbLPC.util.Prefs;

public class LPCCompiler {
  protected Parser parser;
  protected Scope currScope;
  protected CompilerClass currClass;

  //LPCCompiler()
  public LPCCompiler() {
    Debugger.instance().printProgress("LPCCompiler initialized");
  }
  
  //compile(String, String)
  public Compilation compile(String name, String source) {
	parser = new Parser(this, source);
    currScope = new Scope(
      null, //enclosing Scope
      new C_Script() //compilation
    );

    Debugger.instance().printProgress("Compiling '" + name + "'");

    //advance to the first non-error Token (or EOF)
    parser.advance();

    //loop declarations until EOF
    while (!parser.match(TOKEN_EOF))
      declaration();

    if (parser.hadError())
      return null;
      
    currInstructions().add(new Instruction(OP_NIL)); //return value; always null for a Script
    currInstructions().add(new Instruction(OP_RETURN));

    Debugger.instance().traceCompilation(currScope);
    
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
      currInstructions().add(new Instruction(OP_NIL));

    if (currScope.depth() > 0)
      defineLocal(); //mark local available
    else if (currScope.compilation() instanceof C_Script) {
      currInstructions().add(new Instruction(OP_GLOBAL, token.lexeme()));
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
  protected void parseVariable(String errorMessage) {
    parser.consume(TOKEN_IDENTIFIER, errorMessage);

    Token token = parser.previous();
    
    if (currScope.depth() == 0) { //Global
      instr.addOperand(token.lexeme());
      
      return;
    }
    
    declareLocalVariable(token);
  }

  //declareLocal(Token)
  private void declareLocal(Token token) {
    //In the locals, a variable is "declared" when it is
    //added to the scope.

    //Check for an existing local variable with the same name.
    int currScopeDepth = currScope.depth();
    List<Local> currScopeLocals = currScope.locals()
      .stream()
      .filter(item -> (item.depth() == -1 || item.depth() == currScopeDepth))
      .collect(Collectors.toList());

    for (Local local : currScopeLocals)
      if (identifiersEqual(token, local.token()))
        parser.error("Already a variable with this name in this scope.");

    //Record existence of local variable, with "sentinel" depth for now
    //(i.e. declared but not yet made available for use)
    currScope.locals().push(new Local(token, -1));
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
  private int addUpvalue(Scope scope, Integer index, boolean isLocal) {
    int upvalueCount = scope.compilerUpvalues().size();

    for (int i = 0; i < upvalueCount; i++) {
      CompilerUpvalue compilerUpvalue = scope.getUpvalue(i);

      //isLocal controls whether closure captures a local variable or
      //an upvalue from the surrounding function
      if (compilerUpvalue.index() == index && compilerUpvalue.isLocal() == isLocal)
        return i;
    }

    //Return index of the created upvalue in the currScope's
    //upvalue list.  That index becomes the operand to the
    //OP_GET_UPVALUE and OP_SET_UPVALUE instructions.
    return scope.addUpvalue(new CompilerUpvalue(index, isLocal));
  }

  //argumentList()
  public Integer argumentList() {
    Integer argCount = 0;
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

  //compoundAssignment(OpCode, OpCode, OpCode)
  protected void compoundAssignment(OpCode getOp, OpCode setOp, OpCode assignOp) {
    emitOpCode(getOp);

    expression();

    emitOpCode(assignOp);

    emitOpCode(setOp);
  }

  //instructions()
  public List<Instruction> currInstructions() {
    return currScope.compilation().instructions();
  }

  //currClass()
  public CompilerClass currClass() {
    return currClass;
  }

  //emitInstruction(OpCode)
  public void emitInstruction(OpCode opCode) {
	Instruction instr = new Instruction(opCode);
	
	emitInstruction(instr);
  }
  
  //emitInstruction(Instruction)
  public void emitInstruction(Instruction instr) {
    if (parser.previous() != null) //may be null for "synthetic" operations
      instr.setLine(parser.previous().line());
    
    currInstructions().add(instr);
  }

  //emitJump(OpCode)
  public int emitJump(OpCode opCode) {
    Instruction instr = new Instruction(
      opCode,
      255 //placeholder, later backpatched)
    );
    
    emitInstruction(instr);

    return currInstructions().size() - 1;
  }

  //emitLoop(int)
  private void emitLoop(int loopStart) {
    int offset = currInstructions().size() - loopStart + 2;

    Instruction instr = new Instruction(OP_LOOP, offset);

    emitInstruction(instr);
  }

  //endFunction()
  private C_Function endFunction() {
    emitOpCode(OP_NIL); //return value?
    emitOpCode(OP_RETURN);

    //Extract assembled function from temporary structure.
    C_Function function = (C_Function)currScope.compilation();

    function.setUpvalueCount(currScope.compilerUpvalues().size());

    if (!parser.hadError())
      Debugger.instance().traceCompilation(currScope);

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
          currInstructions().add(new Instruction(OP_CLOSE_UPVAL));
      else
        currInstructions().add(new Instruction(OP_POP));

      currScope.locals().pop();
    }
  }

  //expressionStatement()
  private void expressionStatement() {
    expression();

    parser.consume(TOKEN_SEMICOLON, "Expect ';' after expression.");

    currInstructions().add(new Instruction(OP_POP));
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

    int loopStart = currInstructions().size();

     //Condition clause.
    int exitJump = -1;

    if (!parser.match(TOKEN_SEMICOLON)) {
      expression();

      parser.consume(TOKEN_SEMICOLON, "Expect ';' after loop condition.");

      // Jump out of the loop if the condition is false.
      exitJump = emitJump(OP_JUMP_IF_FALSE);

      currInstructions().add(new Instruction(OP_POP)); // Condition.
    }

    //Increment clause.
    if (!parser.match(TOKEN_RIGHT_PAREN)) {
      int bodyJump = emitJump(OP_JUMP);
      int incrementStart = currInstructions().size();

      expression();

      currInstructions().add(new Instruction(OP_POP));

      parser.consume(TOKEN_RIGHT_PAREN, "Expect ')' after for clauses.");

      emitLoop(loopStart);

      loopStart = incrementStart;

      patchJump(bodyJump);
    }

    statement();

    emitLoop(loopStart);

    if (exitJump != -1) {
      patchJump(exitJump);

      emitOpCode(OP_POP); // Condition.
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
    Scope scope = new Scope(
      currScope, //enclosing Scope
      function
    );
    currScope = scope;

    beginScope();

    parser.consume(TOKEN_LEFT_PAREN, "Expect '(' after function name.");

    if (!parser.check(TOKEN_RIGHT_PAREN))
      do {
        function.setArity(function.arity() + 1);

        parser.consume(TOKEN_PRIMITIVE, "Expect type for parameter.");

        parseVariable("Expect parameter name.");

        currScope.markTopLocalInitialized();
      } while (parser.match(TOKEN_COMMA));

    parser.consume(TOKEN_RIGHT_PAREN, "Expect ')' after parameters.");
    parser.consume(TOKEN_LEFT_BRACE, "Expect '{' before function body.");

    block();

    function = endFunction(); //sets currScope to enclosing

    Instruction instr = new Instruction(
      OP_CLOSURE,
      new Object[] { function, scope.compilerUpvalues() }
    );
    
    currInstructions().add(instr);

    //No endScope() needed because Scope is ended completely
    //at the end of the function body.
  }

  //identifierConstant(Token)
  public void identifierConstant(Token token) {
    emitOperand(token.lexeme());
  }

  //stringConstant(Token)
  public void stringConstant(Token token) {
    emitOperand(token.literal());
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

    currInstructions().add(new Instruction(OP_POP));

    statement();

    int elseJump = emitJump(OP_JUMP);

    patchJump(thenJump);

    currInstructions().add(new Instruction(OP_POP));

    if (parser.match(TOKEN_ELSE)) statement();

    patchJump(elseJump);
  }

  //namedVariable(Token, boolean)
  //generates code to load a variable with the given name onto the vStack.
  public void namedVariable(Token token, boolean canAssign) {
    Instruction getInstr, setInstr;
    
    int arg = resolveLocal(currScope, token); //index of local var, or -1

    if (arg != -1) { //local variable
      getInstr = new Instruction(OP_GET_LOCAL, arg);
      setInstr = new Instruction(OP_SET_LOCAL, arg);
//    } else if ((arg = resolveUpvalue(currScope, token)) != -1) { //upvalue
//      getOp = OP_GET_UPVAL;
//      setOp = OP_SET_UPVAL;
    } else { //global variable
      getInstr = new Instruction(OP_GET_GLOBAL, token.lexeme());
      setInstr = new Instruction(OP_SET_GLOBAL, token.lexeme());
    }

    if (canAssign && parser.match(TOKEN_EQUAL)) { //assignment
      expression();

      currInstructions().add(setInstr);
//    } else if (canAssign && parser.match(TOKEN_MINUS_EQUAL))
//      compoundAssignment(getOp, setOp, OP_SUBTRACT, arg);
//    else if (canAssign && parser.match(TOKEN_PLUS_EQUAL))
//      compoundAssignment(getOp, setOp, OP_ADD, arg);
//    else if (canAssign && parser.match(TOKEN_SLASH_EQUAL))
//      compoundAssignment(getOp, setOp, OP_DIVIDE, arg);
//    else if (canAssign && parser.match(TOKEN_STAR_EQUAL))
//      compoundAssignment(getOp, setOp, OP_MULTIPLY, arg);
    } else { //retrieval
      currInstructions().add(getInstr);
    }
  }

  //parser()
  public Parser parser() {
    return parser;
  }

  //patchJump(int)
  public void patchJump(int offset) {
    // -1 to adjust for the jump offset itself.
    int jump = currInstructions().size() - offset - 1;

    Instruction instr = new Instruction(OP_JUMP, jump);

    currInstructions().set(offset, instr);
  }

  //resolveLocal(Scope, Token)
  protected int resolveLocal(Scope scope, Token token) {
    //traverse locals backward, looking for a match
	for (int i = scope.locals().size() - 1; i >= 0; i--) {
	    Local local = scope.locals().get(i);
	  
	    if (identifiersEqual(token, local.token())) {  //found match
	      if (local.depth() == -1) //"sentinel" depth
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
    if (currScope.compilation() instanceof C_Script)
      parser.error("Can't return from top-level code.");

    if (parser.match(TOKEN_SEMICOLON)) //no return value provided
      currInstructions().add(new Instruction(OP_NIL));
    else { //handle return value
      expression();

      parser.consume(TOKEN_SEMICOLON, "Expect ';' after return value.");
    }

    currInstructions().add(new Instruction(OP_RETURN));
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
    int loopStart = currInstructions().size();

    parser.consume(TOKEN_LEFT_PAREN, "Expect '(' after 'while'.");

    expression();

    parser.consume(TOKEN_RIGHT_PAREN, "Expect ')' after condition.");

    int exitJump = emitJump(OP_JUMP_IF_FALSE);

    currInstructions().add(new Instruction(OP_POP));

    statement();

    emitLoop(loopStart);

    patchJump(exitJump);

    currInstructions().add(new Instruction(OP_POP));
  }
}
