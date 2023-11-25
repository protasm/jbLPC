package jbLPC.compiler;

import static jbLPC.compiler.OpCode.OP_ADD;
import static jbLPC.compiler.OpCode.OP_CLOSE_UPVAL;
import static jbLPC.compiler.OpCode.OP_CLOSURE;
import static jbLPC.compiler.OpCode.OP_CONST;
import static jbLPC.compiler.OpCode.OP_GLOBAL;
import static jbLPC.compiler.OpCode.OP_DIVIDE;
import static jbLPC.compiler.OpCode.OP_GET_GLOBAL;
import static jbLPC.compiler.OpCode.OP_GET_LOCAL;
import static jbLPC.compiler.OpCode.OP_GET_UPVAL;
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
import java.util.Stack;
import java.util.stream.Collectors;

import jbLPC.debug.Debugger;
import jbLPC.nativefn.NativeFn;
import jbLPC.parser.Parser;
import jbLPC.scanner.Token;
import jbLPC.util.Pair;
import jbLPC.util.Props;
import jbLPC.util.PropsObserver;

public class LPCCompiler implements PropsObserver {
  protected Parser parser;
  protected Scope currScope;
  //the current, innermost class being compiled
  protected CompilerClass currClass;

  //Cached properties
  protected boolean debugMaster;
  protected boolean debugPrintProgress;
  protected boolean debugPrintComp;

  //LPCCompiler()
  public LPCCompiler() {
    Props.instance().registerObserver(this);
    
    if (debugPrintProgress) Debugger.instance().printProgress("LPCCompiler initialized");
  }

  //addLocal(Token)
  private void addLocal(Token token) {
  }

  //addUpvalue(Scope, byte, boolean)
  private int addUpvalue(Scope scope, Integer index, boolean isLocal) {
    int maxClosureVariables = Props.instance().getInt("MAX_SIGNED_BYTE");

    int upvalueCount = scope.compilerUpvalues().size();

    for (int i = 0; i < upvalueCount; i++) {
      CompilerUpvalue compilerUpvalue = scope.getUpvalue(i);

      //isLocal controls whether closure captures a local variable or
      //an upvalue from the surrounding function
      if (compilerUpvalue.index() == index && compilerUpvalue.isLocal() == isLocal)
        return i;
    }

    if (upvalueCount == maxClosureVariables) {
      parser.error("Too many closure variables in function.");

      return 0;
    }

    //Return index of the created upvalue in the currScope's
    //upvalue list.  That index becomes the operand to the
    //OP_GET_UPVALUE and OP_SET_UPVALUE instructions.
    return scope.addUpvalue(new CompilerUpvalue(index, isLocal));
  }

  //argumentList()
  public Integer argumentList() {
    Integer argCount = 0;
    int maxSignedByte = Props.instance().getInt("MAX_SIGNED_BYTE");

    if (!parser.check(TOKEN_RIGHT_PAREN))
      do {
        expression();

        if (argCount == maxSignedByte)
          parser.error("Can't have more than " + maxSignedByte + " arguments.");

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

  //compile(String, String)
  public Compilation compile(String name, String source) {
	parser = new Parser(this, source);
    currScope = new Scope(
      null, //enclosing Scope
      new C_Script() //compilation
    );

    if (debugPrintProgress) Debugger.instance().printProgress("Compiling '" + name + "'");

    //advance to the first non-error Token (or EOF)
    parser.advance();

    //loop declarations until EOF
    while (!parser.match(TOKEN_EOF))
      declaration();

    if (parser.hadError())
      return null;
      
    emitInstruction(OP_NIL); //return value; always null for a Script
    emitInstruction(OP_RETURN);

    if (debugPrintComp)
      Debugger.instance().disassembleScope(currScope);
    
    return currScope.compilation();
  }

  //compoundAssignment(OpCode, OpCode, OpCode)
  protected void compoundAssignment(OpCode getOp, OpCode setOp, OpCode assignOp) {
    emitInstruction(getOp);

    expression();

    emitInstruction(assignOp);

    emitInstruction(setOp);
  }

  //instructions()
  protected List<Instruction> currInstructions() {
    return currScope.compilation().instructions();
  }

  //currClass()
  public CompilerClass currClass() {
    return currClass;
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
  
  //parseVariable(String)
  protected void parseVariable(String errorMessage) {
    parser.consume(TOKEN_IDENTIFIER, errorMessage);

    Token token = parser.previous();
    
    if (currScope.depth() == 0) {
      identifierConstant(token); //Global
      
      return;
    }

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

    if (currScope.locals().size() >= Props.instance().getInt("MAX_SIGNED_BYTE")) {
      parser.error("Too many local variables in function.");

      return;
    }

    //Record existence of local variable.
    currScope.locals().push(new Local(token, -1));
  }

  //defineVariable()
  protected void defineVariable() {
    if (currScope.depth() > 0) {
      //In the locals, a variable is "defined" when it
      //becomes available for use.
      currScope.markTopLocalInitialized();

      //No code needed to create a local variable at
      //runtime; it's on top of the stack.

      return;
    }

    if (currScope.compilation() instanceof C_Script) {
      Instruction instr = new Instruction(OP_GLOBAL);
      
      emitInstruction(instr);
    }
  }

  //emitInstruction(OpCode)
  public void emitInstruction(OpCode opCode) {
    emitInstruction(new Instruction(opCode));
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

    if (offset > Props.instance().getInt("MAX_LOOP"))
      parser.error("Loop body too large.");
    
    Instruction instr = new Instruction(OP_LOOP, offset);

    emitInstruction(instr);
  }

  //endFunction()
  private C_Function endFunction() {
    emitInstruction(OP_NIL); //return value?
    emitInstruction(OP_RETURN);

    //Extract assembled function from temporary structure.
    C_Function function = (C_Function)currScope.compilation();

    function.setUpvalueCount(currScope.compilerUpvalues().size());

    if (!parser.hadError() && debugPrintComp)
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

  //expression()
  public void expression() {
    parser.parsePrecedence(PREC_ASSIGNMENT);
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
    } else if (parser.match(TOKEN_PRIMITIVE)) {
      parseVariable("Expect variable name.");

      varDeclaration();
    } else
      expressionStatement();

    int loopStart = currInstructions().size();

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
      int incrementStart = currInstructions().size();

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

        int maxSignedByte = Props.instance().getInt("MAX_SIGNED_BYTE");

        if (function.arity() > maxSignedByte)
          parser.errorAtCurrent("Can't have more than " + maxSignedByte + " parameters.");

        parser.consume(TOKEN_PRIMITIVE, "Expect type for parameter.");

        parseVariable("Expect parameter name.");

        defineVariable();
      } while (parser.match(TOKEN_COMMA));

    parser.consume(TOKEN_RIGHT_PAREN, "Expect ')' after parameters.");
    parser.consume(TOKEN_LEFT_BRACE, "Expect '{' before function body.");

    block();

    function = endFunction(); //sets currScope to enclosing

    Instruction instr = new Instruction(
      OP_CLOSURE,
      new Object[] { function, scope.compilerUpvalues() }
    );
    
    emitInstruction(instr);

    //No endScope() needed because Scope is ended completely
    //at the end of the function body.
  }

  //funDeclaration()
  protected void funDeclaration() {
    //Function declaration's variable is marked "initialized"
    //before compiling the body so that the name can be
    //referenced inside the body without generating an error.
    currScope.markTopLocalInitialized();

    function();

    defineVariable();
  }

  //identifierConstant(Token)
  public void identifierConstant(Token token) {
    Instruction instr = new Instruction(
      OP_CONST,
      token.lexeme()
    );
    
    emitInstruction(instr);
  }

  //stringConstant(Token)
  public void stringConstant(Token token) {
    Instruction instr = new Instruction(
      OP_CONST,
      token.literal()
    );
      
    emitInstruction(instr);
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
    OpCode getOp;
    OpCode setOp;

    if (resolveLocal(currScope, token)) { //local variable
      getOp = OP_GET_LOCAL;
      setOp = OP_SET_LOCAL;
    } else if (resolveUpvalue(currScope, token)) { //upvalue
      getOp = OP_GET_UPVAL;
      setOp = OP_SET_UPVAL;
    } else { //global variable
      //add token to constants
      identifierConstant(token);

      getOp = OP_GET_GLOBAL;
      setOp = OP_SET_GLOBAL;
    }

    if (canAssign && parser.match(TOKEN_EQUAL)) { //assignment
      expression();

      emitInstruction(setOp);
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
      emitInstruction(getOp);
      emitArgCode(arg);
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

    if (jump > Props.instance().getInt("MAX_JUMP"))
      parser.error("Too much code to jump over.");
    
    Instruction instr = new Instruction(OP_JUMP, jump);

    currInstructions().set(offset, instr);
  }

  //resolveLocal(Scope, Token)
  protected boolean resolveLocal(Scope scope, Token token) {
    for (Local local : currScope.locals()) {
      if (identifiersEqual(token, local.token())) { //found
        if (local.depth() == -1) //prevent 'var a = a;'
          parser.error("Can't read local variable in its own initializer.");

        return true;
      }
    }

    //No variable with the given name, therefore not a local.
    return false;
  }

  //resolveUpvalue(Scope, Token)
  protected boolean resolveUpvalue(Scope scope, Token token) {
    Scope enclosing = scope.enclosing();

    if (enclosing == null) return false;

    boolean local = resolveLocal(enclosing, token);

    if (!local) {
      enclosing.locals().get(local).setIsCaptured(true);

      //Return index of newly-added Upvalue.
      addUpvalue(scope, index, true);
      
      return true;
    }

    int upvalue = resolveUpvalue(enclosing, token);

    if (upvalue != -1)
      return addUpvalue(scope, upvalue, false);

    return false;
  }

  //returnStatement()
  private void returnStatement() {
    if (currScope.compilation() instanceof C_Script)
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

  //typedDeclaration()
  protected void typedDeclaration() {
    parseVariable("Expect function or variable name.");

    if (parser.check(TOKEN_LEFT_PAREN))
      funDeclaration();
    else
      varDeclaration();
  }

  //varDeclaration()
  protected void varDeclaration() {
    if (parser.match(TOKEN_EQUAL))
      expression();
    else
      emitInstruction(OP_NIL);

    defineVariable();

    //handle variable declarations of the form:
    //int x = 99, y, z = "hello";
    if (parser.match(TOKEN_COMMA)) {
      parseVariable("Expect variable name.");

      varDeclaration();

      return;
    }

    parser.consume(TOKEN_SEMICOLON, "Expect ';' after variable declaration(s).");
  }

  //whileStatement()
  private void whileStatement() {
    int loopStart = currInstructions().size();

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

  //updateCachedProperties()
  protected void updateCachedProperties() {
    debugMaster = Props.instance().getBool("DEBUG_MASTER");
    debugPrintProgress = debugMaster && Props.instance().getBool("DEBUG_PROG");
    debugPrintComp = debugMaster && Props.instance().getBool("DEBUG_COMP");
  }

  //notifyPropertiesChanged()
  @Override
  public void notifyPropertiesChanged() {
    updateCachedProperties();
  }
}
