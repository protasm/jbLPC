package jbLPC.compiler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jbLPC.debug.Debugger;
import jbLPC.main.Props;
import jbLPC.main.PropsObserver;
import jbLPC.parser.ParseRule;
import jbLPC.parser.Parser;
import jbLPC.parser.parselet.*;
import jbLPC.scanner.Scanner;
import jbLPC.scanner.Token;
import jbLPC.scanner.TokenType;

import static jbLPC.compiler.Function.FunctionType.*;
import static jbLPC.compiler.OpCode.*;
import static jbLPC.parser.Parser.Precedence.*;
import static jbLPC.scanner.TokenType.*;

public abstract class Compiler implements PropsObserver {
  public abstract Function compile(String name, String source);

  protected Iterator<Token> tokens;
  private Map<TokenType, ParseRule> tokenTypeToRule;
  protected Parser parser;
  protected Scope currScope;

  //the current, innermost class being compiled
  private CompilerClass currClass;

  //Cached properties
  protected boolean debugMaster;
  protected boolean debugPrintProgress;
  protected boolean debugPrintComp;

  //Compiler()
  public Compiler() {
    Props.instance().registerObserver(this);

    tokenTypeToRule = new HashMap<>();

    registerTokenTypesToRules();

    if (debugPrintProgress) Debugger.instance().printProgress("Compiler initialized.");
  }

  //parser()
  public Parser parser() {
    return parser;
  }

  //currClass()
  public CompilerClass currClass() {
    return currClass;
  }

  //advance()
  protected void advance() {
    parser.setPrevious(parser.current());

    for (;;) {
      parser.setCurrent(tokens.next());

      if (parser.current().type() != TOKEN_ERROR)
        break;

      errorAtCurrent(parser.current().lexeme());
    }
  }

  //consume(TokenType, String)
  public void consume(TokenType type, String message) {
    if (parser.current().type() == type) {
      advance();

      return;
    }

    errorAtCurrent(message);
  }

  //check(TokenType)
  protected boolean check(TokenType type) {
    return parser.current().type() == type;
  }

  //match(TokenType)
  public boolean match(TokenType type) {
    if (!check(type)) return false;

    advance();

    return true;
  }

  //endCompilation(boolean)
  protected Function endCompilation(boolean emitNil) {
    if (emitNil)
      emitByte(OP_NIL);

    emitByte(OP_RETURN);

    //Extract assembled function from temporary structure.
    Function function = currScope.function();

    function.setUpvalueCount(currScope.upvalues().size());

    if (!parser.hadError() && debugPrintComp)
      Debugger.instance().disassembleScope(currScope);

    //Step up to higher scope.
    currScope = currScope.enclosing();

    return function;
  }

  //emitByte(byte)
  public void emitByte(byte b) {
    currChunk().writeByte(b, parser.previous().line());
  }

  //emitWord(int)
  public void emitWord(int i) {
    emitWord((short)i);
  }

  //emitWord(short)
  public void emitWord(short s) {
    byte b1 = highByte(s);
    byte b2 = lowByte(s);

    currChunk().writeWord(b1, b2, parser.previous().line());
  }

  //highByte(short)
  private byte highByte(short s) {
    return (byte)((s >> 8) & 0xFF);
  }

  //lowByte(short)
  private byte lowByte(short s) {
    return (byte)(s & 0xFF);
  }

  //emitLoop(int)
  private void emitLoop(int loopStart) {
    int maxLoop = Props.instance().getInt("MAX_LOOP");

    emitByte(OP_LOOP);

    int offset = currChunk().codes().size() - loopStart + 2;

    if (offset > maxLoop) error("Loop body too large.");

    emitWord(offset);
  }

  //emitJump(byte)
  public int emitJump(byte instruction) {
    emitByte(instruction);

    //placeholders, later backpatched.
    emitByte((byte)0xFF);
    emitByte((byte)0xFF);

    return currChunk().codes().size() - 2;
  }

  //makeConstant(Object)
  public int makeConstant(Object value) {
    currChunk().constants().add(value);

    int index = currChunk().constants().size() - 1;

    if (index > Props.instance().getInt("MAX_SIGNED_SHORT")) {
      error("Too many constants in one chunk.");

      return 0;
    }

    //Return the index of the constant added.
    return index;
  }

  //emitConstant(Object)
  public void emitConstant(Object value) {
    int index = makeConstant(value);

    emitByte(OP_GET_CONSTANT);
    emitWord(index);
  }

  //patchJump(int)
  public void patchJump(int offset) {
    int maxJump = Props.instance().getInt("MAX_JUMP");
    // -2 to adjust for the bytecode for the jump offset itself.
    int jump = currChunk().codes().size() - offset - 2;

    if (jump > maxJump)
      error("Too much code to jump over.");

    byte hi = highByte((short)jump);
    byte lo = lowByte((short)jump);

    currChunk().codes().set(offset, hi);
    currChunk().codes().set(offset + 1, lo);
  }

  //currChunk()
  private Chunk currChunk() {
    return currScope.function().chunk();
  }

  //beginScope()
  private void beginScope() {
    currScope.setDepth(currScope.depth() + 1);
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

  //getRule(TokenType)
  public ParseRule getRule(TokenType type) {
    return tokenTypeToRule.get(type);
  }

  //compoundAssignment(OpCode, int)
  private void compoundAssignment(byte getOp, byte setOp, byte assignOp, int index) {
    emitByte(getOp);
    emitWord(index);

    expression();

    emitByte(assignOp);

    emitByte(setOp);
    emitWord(index);
  }

  //namedVariable(Token, boolean)
  //generates code to load a variable with the given name onto the stack.
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
      arg = makeConstant(token.lexeme());

      getOp = OP_GET_GLOBAL;
      setOp = OP_SET_GLOBAL;
    }

    if (canAssign && match(TOKEN_EQUAL)) { //assignment
      expression();

      emitByte(setOp);
      emitWord(arg);
    } else if (canAssign && match(TOKEN_MINUS_EQUAL))
      compoundAssignment(getOp, setOp, OP_SUBTRACT, arg);
    else if (canAssign && match(TOKEN_PLUS_EQUAL))
      compoundAssignment(getOp, setOp, OP_ADD, arg);
    else if (canAssign && match(TOKEN_SLASH_EQUAL))
      compoundAssignment(getOp, setOp, OP_DIVIDE, arg);
    else if (canAssign && match(TOKEN_STAR_EQUAL))
      compoundAssignment(getOp, setOp, OP_MULTIPLY, arg);
    else { //retrieval
      emitByte(getOp);
      emitWord(arg);
    }
  }

  //parsePrecedence(int)
  public void parsePrecedence(int precedence) {
    advance();

    // Look up the Parselet to use where the previous token's type
    // is an expression prefix.
    Parselet prefixRule = getRule(parser.previous().type()).prefix();

    if (prefixRule == null) {
      error("Expect expression.");

      return;
    }

    boolean canAssign = (precedence <= PREC_ASSIGNMENT);

    prefixRule.parse(this, canAssign);

    //infix parsing loop
    while (precedence <= getRule(parser.current().type()).precedence()) {
      advance();

      Parselet infixRule = getRule(parser.previous().type()).infix();

      infixRule.parse(this, canAssign);
    }

    if (canAssign)
      if (match(TOKEN_EQUAL) || match(TOKEN_PLUS_EQUAL))
        //If the = doesn't get consumed as part of the expression, nothing
        //else is going to consume it. It's an error and we should report it.
        error("Invalid assignment target.");
  }

  //identifiersEqual(Token, Token)
  private boolean identifiersEqual(Token a, Token b) {
    return a.lexeme().equals(b.lexeme());
  }

  //resolveLocal(Scope, Token)
  private int resolveLocal(Scope scope, Token token) {
    for (int i = scope.locals().size() - 1; i >= 0; i--) {
      Local local = scope.locals().get(i);

      if (identifiersEqual(token, local.token())) {
        if (local.depth() == -1) //"sentinel" depth
          error("Can't read local variable in its own initializer.");

        return i;
      }
    }

    //No variable with the given name, therefore not a local.
    return -1;
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
      error("Too many closure variables in function.");

      return 0;
    }

    //Return index of the created upvalue in the currScope's
    //upvalue list.  That index becomes the operand to the
    //OP_GET_UPVALUE and OP_SET_UPVALUE instructions.
    return scope.addUpvalue(new Upvalue(index, isLocal));
  }

  //resolveUpvalue(Scope, Token)
  private int resolveUpvalue(Scope scope, Token token) {
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

  //addLocal(Token)
  private void addLocal(Token token) {
    if (currScope.locals().size() >= Props.instance().getInt("MAX_SIGNED_BYTE")) {
      error("Too many local variables in function.");

      return;
    }

    currScope.locals().push(new Local(token, -1));
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
        error("Already a variable with this name in this scope.");
    }

    //Record existence of local variable.
    addLocal(parser.previous());
  }

  //parseVariable(String)
  private int parseVariable(String errorMessage) {
    consume(TOKEN_IDENTIFIER, errorMessage);

    declareVariable();

    //Exit the function if we're in a local scope,
    //returning a dummy table index.
    if (currScope.depth() > 0) return 0;

    return makeConstant(parser.previous().lexeme());
  }

  //markInitialized()
  private void markInitialized() {
    if (currScope.depth() == 0) return;

    currScope.markTopLocalInitialized();
  }

  //defineVariable(int)
  private void defineVariable(int index) {
    if (currScope.depth() > 0) {
      //In the locals, a variable is "defined" when it
      //becomes available for use.
      markInitialized();

      //No code needed to create a local variable at
      //runtime; it's on top of the stack.

      return;
    }

    emitByte(OP_DEFINE_GLOBAL);
    emitWord(index);
  }

  //syntheticToken(String)
  public Token syntheticToken(String text) {
    return new Token(text);
  }

  //argumentList()
  public byte argumentList() {
    byte argCount = 0;
    int maxSignedByte = Props.instance().getInt("MAX_SIGNED_BYTE");

    if (!check(TOKEN_RIGHT_PAREN))
      do {
        expression();

        if (argCount == maxSignedByte)
          error("Can't have more than " + maxSignedByte + " arguments.");

        argCount++;
      } while (match(TOKEN_COMMA));

    consume(TOKEN_RIGHT_PAREN, "Expect ')' after arguments.");

    return argCount;
  }

  //expression()
  public void expression() {
    parsePrecedence(PREC_ASSIGNMENT);
  }

  //block()
  private void block() {
    while (!check(TOKEN_RIGHT_BRACE) && !check(TOKEN_EOF))
      declaration();

    consume(TOKEN_RIGHT_BRACE, "Expect '}' after block.");
  }

  //function(Function.FunctionType)
  protected void function(Function.FunctionType type) {
    int maxSignedByte = Props.instance().getInt("MAX_SIGNED_BYTE");
    Scope scope = new Scope(
      currScope, //enclosing Scope
      type, //FunctionType
      parser.previous().lexeme() //Function name
    );

    currScope = scope;

    beginScope();

    consume(TOKEN_LEFT_PAREN, "Expect '(' after function name.");

    if (!check(TOKEN_RIGHT_PAREN))
      do {
       scope.function().setArity(scope.function().arity() + 1);

        if (scope.function().arity() > maxSignedByte)
          errorAtCurrent("Can't have more than " + maxSignedByte + " parameters.");

        consume(TOKEN_TYPE, "Expect type for parameter.");

        int index = parseVariable("Expect parameter name.");

        defineVariable(index);
      } while (match(TOKEN_COMMA));

    consume(TOKEN_RIGHT_PAREN, "Expect ')' after parameters.");
    consume(TOKEN_LEFT_BRACE, "Expect '{' before function body.");

    block();

    Function function = endCompilation(true); //sets currScope to enclosing

    //We're emitting into the Chunk in the enclosing scope now.
    //Store the compiled function in the enclosing scope Chunk's
    //constant table and emit the OpCode for the VM to build a Closure
    //around it at runtime.
    emitByte(OP_CLOSURE);
    emitWord(makeConstant(function));

    for (Upvalue upvalue : scope.upvalues()) {
      emitByte((byte)(upvalue.isLocal() ? 1 : 0));
      emitByte((byte)(upvalue.index()));
    }

    //No endScope() needed because Scope is ended completely
    //at the end of the function body.
  }

  //inherit()
  private void inherit() {
  }

  //typedDeclaration()
  private void typedDeclaration() {
    int index = parseVariable("Expect function or variable name.");

    if (check(TOKEN_LEFT_PAREN))
      funDeclaration(index);
    else
      varDeclaration(index);
  }

  //funDeclaration(int)
  private void funDeclaration(int index) {
    //Function declaration's variable is marked "initialized"
    //before compiling the body so that the name can be
    //referenced inside the body without generating an error.
    markInitialized();

    function(TYPE_FUNCTION);

    defineVariable(index);
  }

  //varDeclaration(int)
  private void varDeclaration(int index) {
    if (match(TOKEN_EQUAL))
      expression();
    else
      emitByte(OP_NIL);

    defineVariable(index);

    //handle variable declarations of the form:
    //var x = 99, y, z = "hello";
    if (match(TOKEN_COMMA)) {
      int nextVarIndex = parseVariable("Expect variable name.");

      varDeclaration(nextVarIndex);

      return;
    }

    consume(TOKEN_SEMICOLON, "Expect ';' after variable declaration(s).");
  }

  //expressionStatement()
  private void expressionStatement() {
    expression();

    consume(TOKEN_SEMICOLON, "Expect ';' after expression.");

    emitByte(OP_POP);
  }

  //forStatement()
  private void forStatement() {
    beginScope();

    consume(TOKEN_LEFT_PAREN, "Expect '(' after 'for'.");

    //Initializer clause.
    if (match(TOKEN_SEMICOLON)) {
      // No initializer.
    } else if (match(TOKEN_TYPE)) {
      int index = parseVariable("Expect variable name.");

      varDeclaration(index);
    } else
      expressionStatement();

    int loopStart = currChunk().codes().size();

     //Condition clause.
    int exitJump = -1;

    if (!match(TOKEN_SEMICOLON)) {
      expression();

      consume(TOKEN_SEMICOLON, "Expect ';' after loop condition.");

      // Jump out of the loop if the condition is false.
      exitJump = emitJump(OP_JUMP_IF_FALSE);

      emitByte(OP_POP); // Condition.
    }

    //Increment clause.
    if (!match(TOKEN_RIGHT_PAREN)) {
      int bodyJump = emitJump(OP_JUMP);
      int incrementStart = currChunk().codes().size();

      expression();

      emitByte(OP_POP);

      consume(TOKEN_RIGHT_PAREN, "Expect ')' after for clauses.");

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

  //ifStatement()
  private void ifStatement() {
    consume(TOKEN_LEFT_PAREN, "Expect '(' after 'if'.");

    expression();

    consume(TOKEN_RIGHT_PAREN, "Expect ')' after condition."); 

    int thenJump = emitJump(OP_JUMP_IF_FALSE);

    emitByte(OP_POP);

    statement();

    int elseJump = emitJump(OP_JUMP);

    patchJump(thenJump);

    emitByte(OP_POP);

    if (match(TOKEN_ELSE)) statement();

    patchJump(elseJump);
  }

  //returnStatement()
  private void returnStatement() {
    if (currScope.function().type() == TYPE_SCRIPT)
      error("Can't return from top-level code.");

    if (match(TOKEN_SEMICOLON)) //no return value provided
      emitByte(OP_NIL);
    else { //handle return value
      expression();

      consume(TOKEN_SEMICOLON, "Expect ';' after return value.");
    }

    emitByte(OP_RETURN);
  }

  //whileStatement()
  private void whileStatement() {
    int loopStart = currChunk().codes().size();

    consume(TOKEN_LEFT_PAREN, "Expect '(' after 'while'.");

    expression();

    consume(TOKEN_RIGHT_PAREN, "Expect ')' after condition.");

    int exitJump = emitJump(OP_JUMP_IF_FALSE);

    emitByte(OP_POP);

    statement();

    emitLoop(loopStart);

    patchJump(exitJump);

    emitByte(OP_POP);
  }

  //synchronize()
  private void synchronize() {
    parser.setPanicMode(false);

    while (parser.current().type() != TOKEN_EOF) {
      if (parser.previous().type() == TOKEN_SEMICOLON) return;

      switch (parser.current().type()) {
        case TOKEN_TYPE:
        case TOKEN_FOR:
        case TOKEN_IF:
        case TOKEN_WHILE:
        case TOKEN_RETURN:
          return;

        default:
          break; // Do nothing.
      } //switch

      advance();
    } //while
  }

  //declaration()
  protected void declaration() {
    if (match(TOKEN_INHERIT))
      inherit();
    else if (match(TOKEN_TYPE))
      typedDeclaration();
    else
      statement();

    if (parser.panicMode())
      synchronize();
  }

  //statement()
  private void statement() {
    if (match(TOKEN_FOR))
      forStatement();
    else if (match(TOKEN_IF))
      ifStatement();
    else if (match(TOKEN_RETURN))
      returnStatement();
    else if (match(TOKEN_WHILE))
      whileStatement();
    else if (match(TOKEN_LEFT_BRACE)) {
      beginScope();

      block();

      endScope();
    } else
      expressionStatement();
  }

  //errorAtCurrent(String)
  private void errorAtCurrent(String message) {
    errorAt(parser.current(), message);
  }

  //error(String)
  public void error(String message) {
    errorAt(parser.previous(), message);
  }

  //errorAt(Token, String)
  private void errorAt(Token token, String message) {
    if (parser.panicMode()) return;

    parser.setPanicMode(true);

    System.err.print("[line " + token.line() + "] Error");

    if (token.type() == TOKEN_EOF)
      System.err.print(" at end");
    else if (token.type() == TOKEN_ERROR) {
      // Nothing.
    } else
      System.err.print(" at '" + token.lexeme() + "'");

    System.err.print(": " + message + "\n");

    parser.setHadError(true);
  }

  //register(TokenType, Parselet, Parselet, int)
  private void register(TokenType type, Parselet prefix, Parselet infix, int precedence) {
    tokenTypeToRule.put(type, new ParseRule(prefix, infix, precedence));
  }

  //registerTokenTypesToRules()
  private void registerTokenTypesToRules() {
    //Column 1: TokenType
    //Column 2: Parselet to use when TokenType appears as expression prefix
    //Column 3: Parselet to use when TokenType appears as expression infix
    //Column 4: Precedence to use when TokenType appears as expression infix
    register(TOKEN_LEFT_PAREN,    new GroupingParselet(), new CallParselet(),   PREC_CALL);
    register(TOKEN_RIGHT_PAREN,   null,                   null,                 PREC_NONE);
    register(TOKEN_LEFT_BRACE,    null,                   null,                 PREC_NONE);
    register(TOKEN_RIGHT_BRACE,   null,                   null,                 PREC_NONE);
    register(TOKEN_COMMA,         null,                   null,                 PREC_NONE);
    register(TOKEN_DOT,           null,                   new DotParselet(),    PREC_CALL);
    register(TOKEN_INVOKE,        null,                   new InvokeParselet(), PREC_CALL);
    register(TOKEN_MINUS,         new UnaryParselet(),    new BinaryParselet(), PREC_TERM);
    register(TOKEN_MINUS_EQUAL,   null,                   null,                 PREC_NONE);
    register(TOKEN_MINUS_MINUS,   null,                   null,                 PREC_NONE);
    register(TOKEN_PLUS,          null,                   new BinaryParselet(), PREC_TERM);
    register(TOKEN_PLUS_EQUAL,    null,                   null,                 PREC_NONE);
    register(TOKEN_PLUS_PLUS,     null,                   null,                 PREC_NONE);
    register(TOKEN_SEMICOLON,     null,                   null,                 PREC_NONE);
    register(TOKEN_SLASH,         null,                   new BinaryParselet(), PREC_FACTOR);
    register(TOKEN_SLASH_EQUAL,   null,                   null,                 PREC_NONE);
    register(TOKEN_STAR,          null,                   new BinaryParselet(), PREC_FACTOR);
    register(TOKEN_STAR_EQUAL,    null,                   null,                 PREC_NONE);
    register(TOKEN_BANG,          new UnaryParselet(),    null,                 PREC_NONE);
    register(TOKEN_BANG_EQUAL,    null,                   new BinaryParselet(), PREC_EQUALITY);
    register(TOKEN_EQUAL,         null,                   null,                 PREC_NONE);
    register(TOKEN_EQUAL_EQUAL,   null,                   new BinaryParselet(), PREC_EQUALITY);
    register(TOKEN_GREATER,       null,                   new BinaryParselet(), PREC_COMPARISON);
    register(TOKEN_GREATER_EQUAL, null,                   new BinaryParselet(), PREC_COMPARISON);
    register(TOKEN_LESS,          null,                   new BinaryParselet(), PREC_COMPARISON);
    register(TOKEN_LESS_EQUAL,    null,                   new BinaryParselet(), PREC_COMPARISON);
    register(TOKEN_IDENTIFIER,    new VariableParselet(), null,                 PREC_NONE);
    register(TOKEN_STRING,        new StringParselet(),   null,                 PREC_NONE);
    register(TOKEN_NUMBER,        new NumberParselet(),   null,                 PREC_NONE);
    register(TOKEN_DBL_AMP,       null,                   new AndParselet(),    PREC_AND);
    register(TOKEN_ELSE,          null,                   null,                 PREC_NONE);
    register(TOKEN_FALSE,         new LiteralParselet(),  null,                 PREC_NONE);
    register(TOKEN_FOR,           null,                   null,                 PREC_NONE);
    register(TOKEN_IF,            null,                   null,                 PREC_NONE);
    register(TOKEN_INHERIT,       null,                   null,                 PREC_NONE);
    register(TOKEN_NIL,           new LiteralParselet(),  null,                 PREC_NONE);
    register(TOKEN_DBL_PIPE,      null,                   new OrParselet(),     PREC_OR);
    register(TOKEN_RETURN,        null,                   null,                 PREC_NONE);
    register(TOKEN_SUPER,         new SuperParselet(),    null,                 PREC_NONE);
    register(TOKEN_THIS,          new ThisParselet(),     null,                 PREC_NONE);
    register(TOKEN_TRUE,          new LiteralParselet(),  null,                 PREC_NONE);
    register(TOKEN_TYPE,          null,                   null,                 PREC_NONE);
    register(TOKEN_WHILE,         null,                   null,                 PREC_NONE);
    register(TOKEN_ERROR,         null,                   null,                 PREC_NONE);
    register(TOKEN_EOF,           null,                   null,                 PREC_NONE);
  }

  //updateCachedProperties()
  private void updateCachedProperties() {
    debugMaster = Props.instance().getBool("DEBUG_MASTER");
    debugPrintProgress = debugMaster && Props.instance().getBool("DEBUG_PROG");
    debugPrintComp = debugMaster && Props.instance().getBool("DEBUG_COMP");
  }

  //notifyPropertiesChanged()
  public void notifyPropertiesChanged() {
    updateCachedProperties();
  }
}
