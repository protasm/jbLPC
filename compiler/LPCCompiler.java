package jbLPC.compiler;

import static jbLPC.compiler.OpCode.OP_ADD;
import static jbLPC.compiler.OpCode.OP_CLOSE_UPVALUE;
import static jbLPC.compiler.OpCode.OP_CLOSURE;
import static jbLPC.compiler.OpCode.OP_CONSTANT;
import static jbLPC.compiler.OpCode.OP_DEFINE_GLOBAL;
import static jbLPC.compiler.OpCode.OP_DIVIDE;
import static jbLPC.compiler.OpCode.OP_GET_GLOBAL;
import static jbLPC.compiler.OpCode.OP_GET_LOCAL;
import static jbLPC.compiler.OpCode.OP_GET_UPVALUE;
import static jbLPC.compiler.OpCode.OP_JUMP;
import static jbLPC.compiler.OpCode.OP_JUMP_IF_FALSE;
import static jbLPC.compiler.OpCode.OP_LOOP;
import static jbLPC.compiler.OpCode.OP_MULTIPLY;
import static jbLPC.compiler.OpCode.OP_NIL;
import static jbLPC.compiler.OpCode.OP_POP;
import static jbLPC.compiler.OpCode.OP_RETURN;
import static jbLPC.compiler.OpCode.OP_SET_GLOBAL;
import static jbLPC.compiler.OpCode.OP_SET_LOCAL;
import static jbLPC.compiler.OpCode.OP_SET_UPVALUE;
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

import jbLPC.debug.Debugger;
import jbLPC.parser.Parser;
import jbLPC.scanner.Token;
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
    if (currScope.locals().size() >= Props.instance().getInt("MAX_SIGNED_BYTE")) {
      parser.error("Too many local variables in function.");

      return;
    }

    currScope.locals().push(new Local(token, -1));
  }

  //addUpvalue(Scope, byte, boolean)
  private int addUpvalue(Scope scope, byte index, boolean isLocal) {
    int maxClosureVariables = Props.instance().getInt("MAX_SIGNED_BYTE");

    int upvalueCount = scope.upvalues().size();

    for (int i = 0; i < upvalueCount; i++) {
      Upvalue upvalue = scope.getUpvalue(i);

      //isLocal controls whether closure captures a local variable or
      //an upvalue from the surrounding function
      if (upvalue.index() == index && upvalue.isLocal() == isLocal)
        return i;
    }

    if (upvalueCount == maxClosureVariables) {
      parser.error("Too many closure variables in function.");

      return 0;
    }

    //Return index of the created upvalue in the currScope's
    //upvalue list.  That index becomes the operand to the
    //OP_GET_UPVALUE and OP_SET_UPVALUE instructions.
    return scope.addUpvalue(new Upvalue(index, isLocal));
  }

  //argumentList()
  public byte argumentList() {
    byte argCount = 0;
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
      
    emitByte(OP_NIL); //return value; always null for a Script
    emitByte(OP_RETURN);

    if (debugPrintComp)
      Debugger.instance().disassembleScope(currScope);
    
    return currScope.compilation();
  }

  //compoundAssignment(OpCode, int)
  protected void compoundAssignment(byte getOp, byte setOp, byte assignOp, int index) {
    emitByte(getOp);
    emitWord(index);

    expression();

    emitByte(assignOp);

    emitByte(setOp);
    emitWord(index);
  }

  //currChunk()
  protected Chunk currChunk() {
    return currScope.compilation().chunk();
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

  //declareVariable()
  private void declareVariable() {
    if (currScope.depth() == 0)
      return;

    //In the locals, a variable is "declared" when it is
    //added to the scope.
    Token token = parser.previous();

    //Start at the end of the locals array and work backward,
    //looking for an existing variable with the same name.
    for (int i = currScope.locals().size() - 1; i >= 0; i--) {
      Local local = currScope.locals().get(i);

      if (local.depth() != -1 && local.depth() < currScope.depth())
        break;

      if (identifiersEqual(token, local.token()))
        parser.error("Already a variable with this name in this scope.");
    }

    //Record existence of local variable.
    addLocal(parser.previous());
  }

  //defineVariable(int)
  protected void defineVariable(int index) {
    if (currScope.depth() > 0) {
      //In the locals, a variable is "defined" when it
      //becomes available for use.
      markInitialized();

      //No code needed to create a local variable at
      //runtime; it's on top of the stack.

      return;
    }

    if (currScope.compilation() instanceof C_Script) {
      emitByte(OP_DEFINE_GLOBAL);
      emitWord(index);
    }
  }

  //emitByte(byte)
  public void emitByte(byte b) {
    if (parser.previous() == null) //may occur for "synthetic" operations
      currChunk().writeByte(b);
    else
      currChunk().writeByte(b, parser.previous().line());
  }

  //emitConstant(Object)
  public void emitConstant(Object value) {
    int index = makeConstant(value);

    emitByte(OP_CONSTANT);
    emitWord(index);
  }

  //emitJump(byte)
  public int emitJump(byte instruction) {
    emitByte(instruction);

    //placeholders, later backpatched.
    emitByte((byte)0xFF);
    emitByte((byte)0xFF);

    return currChunk().opCodes().size() - 2;
  }

  //emitLoop(int)
  private void emitLoop(int loopStart) {
    int maxLoop = Props.instance().getInt("MAX_LOOP");

    emitByte(OP_LOOP);

    int offset = currChunk().opCodes().size() - loopStart + 2;

    if (offset > maxLoop) parser.error("Loop body too large.");

    emitWord(offset);
  }

  //emitWord(int)
  public void emitWord(int i) {
    emitWord((short)i);
  }

  //emitWord(short)
  public void emitWord(short s) {
    byte b1 = highByte(s);
    byte b2 = lowByte(s);

    if (parser.previous() == null) //may occur for "synthetic" operations
      currChunk().writeWord(b1, b2);
    else
      currChunk().writeWord(b1, b2, parser.previous().line());
  }

  //endFunction()
  private C_Function endFunction() {
    emitByte(OP_NIL); //return value?
    emitByte(OP_RETURN);

    //Extract assembled function from temporary structure.
    C_Function function = (C_Function)currScope.compilation();

    function.setUpvalueCount(currScope.upvalues().size());

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
        emitByte(OP_CLOSE_UPVALUE);
      else
        emitByte(OP_POP);

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

    emitByte(OP_POP);
  }

  //forStatement()
  private void forStatement() {
    beginScope();

    parser.consume(TOKEN_LEFT_PAREN, "Expect '(' after 'for'.");

    //Initializer clause.
    if (parser.match(TOKEN_SEMICOLON)) {
      // No initializer.
    } else if (parser.match(TOKEN_PRIMITIVE)) {
      int index = parseVariable("Expect variable name.");

      varDeclaration(index);
    } else
      expressionStatement();

    int loopStart = currChunk().opCodes().size();

     //Condition clause.
    int exitJump = -1;

    if (!parser.match(TOKEN_SEMICOLON)) {
      expression();

      parser.consume(TOKEN_SEMICOLON, "Expect ';' after loop condition.");

      // Jump out of the loop if the condition is false.
      exitJump = emitJump(OP_JUMP_IF_FALSE);

      emitByte(OP_POP); // Condition.
    }

    //Increment clause.
    if (!parser.match(TOKEN_RIGHT_PAREN)) {
      int bodyJump = emitJump(OP_JUMP);
      int incrementStart = currChunk().opCodes().size();

      expression();

      emitByte(OP_POP);

      parser.consume(TOKEN_RIGHT_PAREN, "Expect ')' after for clauses.");

      emitLoop(loopStart);

      loopStart = incrementStart;

      patchJump(bodyJump);
    }

    statement();

    emitLoop(loopStart);

    if (exitJump != -1) {
      patchJump(exitJump);

      emitByte(OP_POP); // Condition.
    }

    endScope();
  }

  //function()
  private void function() {
    int maxSignedByte = Props.instance().getInt("MAX_SIGNED_BYTE");
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

        if (function.arity() > maxSignedByte)
          parser.errorAtCurrent("Can't have more than " + maxSignedByte + " parameters.");

        parser.consume(TOKEN_PRIMITIVE, "Expect type for parameter.");

        int index = parseVariable("Expect parameter name.");

        defineVariable(index);
      } while (parser.match(TOKEN_COMMA));

    parser.consume(TOKEN_RIGHT_PAREN, "Expect ')' after parameters.");
    parser.consume(TOKEN_LEFT_BRACE, "Expect '{' before function body.");

    block();

    function = endFunction(); //sets currScope to enclosing

    //We're emitting into the Chunk in the enclosing scope now.
    //Store the compiled function in the enclosing scope Chunk's
    //constant table and emit the OpCode for the VM to build a Closure
    //around it at runtime.
    emitByte(OP_CLOSURE);
    emitWord(makeConstant(function));

    for (Upvalue upvalue : scope.upvalues()) {
      emitByte((byte)(upvalue.isLocal() ? 1 : 0));
      emitByte((upvalue.index()));
    }

    //No endScope() needed because Scope is ended completely
    //at the end of the function body.
  }

  //funDeclaration(int)
  protected void funDeclaration(int index) {
    //Function declaration's variable is marked "initialized"
    //before compiling the body so that the name can be
    //referenced inside the body without generating an error.
    markInitialized();

    function();

    defineVariable(index);
  }

  //highByte(short)
  private byte highByte(short s) {
    return (byte)((s >> 8) & 0xFF);
  }

  //identifierConstant(Token)
  public int identifierConstant(Token token) {
    //return index of newly added constant
    return makeConstant(token.lexeme());
  }

  public int stringConstant(Token token) {
    return makeConstant(token.literal());
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

    emitByte(OP_POP);

    statement();

    int elseJump = emitJump(OP_JUMP);

    patchJump(thenJump);

    emitByte(OP_POP);

    if (parser.match(TOKEN_ELSE)) statement();

    patchJump(elseJump);
  }

  //lowByte(short)
  private byte lowByte(short s) {
    return (byte)(s & 0xFF);
  }

  //makeConstant(Object)
  protected int makeConstant(Object value) {
    currChunk().constants().add(value);

    int index = currChunk().constants().size() - 1;

    if (index > Props.instance().getInt("MAX_SIGNED_SHORT")) {
      parser.error("Too many constants in one chunk.");

      return 0;
    }

    //Return the index of the constant added.
    return index;
  }

  //markInitialized()
  private void markInitialized() {
    if (currScope.depth() == 0) return;

    currScope.markTopLocalInitialized();
  }

  //namedVariable(Token, boolean)
  //generates code to load a variable with the given name onto the vStack.
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
    } else { //global variable
      //add token to constants and store index in arg
      arg = identifierConstant(token);

      getOp = OP_GET_GLOBAL;
      setOp = OP_SET_GLOBAL;
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

  //parser()
  public Parser parser() {
    return parser;
  }

  //parseVariable(String)
  protected int parseVariable(String errorMessage) {
    parser.consume(TOKEN_IDENTIFIER, errorMessage);

    declareVariable();

    //Exit the function if we're in a local scope,
    //returning a dummy table index.
    if (currScope.depth() > 0) return 0;

    return identifierConstant(parser.previous());
  }

  //patchJump(int)
  public void patchJump(int offset) {
    int maxJump = Props.instance().getInt("MAX_JUMP");
    // -2 to adjust for the bytecode for the jump offset itself.
    int jump = currChunk().opCodes().size() - offset - 2;

    if (jump > maxJump)
      parser.error("Too much code to jump over.");

    byte hi = highByte((short)jump);
    byte lo = lowByte((short)jump);

    currChunk().opCodes().set(offset, hi);
    currChunk().opCodes().set(offset + 1, lo);
  }

  //resolveLocal(Scope, Token)
  protected int resolveLocal(Scope scope, Token token) {
    for (int i = scope.locals().size() - 1; i >= 0; i--) {
      Local local = scope.locals().get(i);

      if (identifiersEqual(token, local.token())) { //found
        //prevent 'var a = a;'
        if (local.depth() == -1) //"sentinel" depth
          parser.error("Can't read local variable in its own initializer.");

        return i;
      }
    }

    //No variable with the given name, therefore not a local.
    return -1;
  }

  //resolveUpvalue(Scope, Token)
  protected int resolveUpvalue(Scope scope, Token token) {
    Scope enclosing = scope.enclosing();

    if (enclosing == null) return -1;

    int local = resolveLocal(enclosing, token);

    if (local != -1) {
      enclosing.locals().get(local).setIsCaptured(true);

      //Return index of newly-added Upvalue.
      return addUpvalue(scope, (byte)local, true);
    }

    int upvalue = resolveUpvalue(enclosing, token);

    if (upvalue != -1)
      return addUpvalue(scope, (byte)upvalue, false);

    return -1;
  }

  //returnStatement()
  private void returnStatement() {
    if (currScope.compilation() instanceof C_Script)
      parser.error("Can't return from top-level code.");

    if (parser.match(TOKEN_SEMICOLON)) //no return value provided
      emitByte(OP_NIL);
    else { //handle return value
      expression();

      parser.consume(TOKEN_SEMICOLON, "Expect ';' after return value.");
    }

    emitByte(OP_RETURN);
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
    int index = parseVariable("Expect function or variable name.");

    if (parser.check(TOKEN_LEFT_PAREN))
      funDeclaration(index);
    else
      varDeclaration(index);
  }

  //varDeclaration(int)
  protected void varDeclaration(int index) {
    if (parser.match(TOKEN_EQUAL))
      expression();
    else
      emitByte(OP_NIL);

    defineVariable(index);

    //handle variable declarations of the form:
    //var x = 99, y, z = "hello";
    if (parser.match(TOKEN_COMMA)) {
      int nextVarIndex = parseVariable("Expect variable name.");

      varDeclaration(nextVarIndex);

      return;
    }

    parser.consume(TOKEN_SEMICOLON, "Expect ';' after variable declaration(s).");
  }

  //whileStatement()
  private void whileStatement() {
    int loopStart = currChunk().opCodes().size();

    parser.consume(TOKEN_LEFT_PAREN, "Expect '(' after 'while'.");

    expression();

    parser.consume(TOKEN_RIGHT_PAREN, "Expect ')' after condition.");

    int exitJump = emitJump(OP_JUMP_IF_FALSE);

    emitByte(OP_POP);

    statement();

    emitLoop(loopStart);

    patchJump(exitJump);

    emitByte(OP_POP);
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
