package jbLPC.scanner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jbLPC.debug.Debugger;
import jbLPC.main.Props;

import static jbLPC.scanner.TokenType.*;

public class Scanner extends SourceReader {
  private static final Map<String, TokenType> reservedWords;
  private static final Map<Character, TokenType> singleCharLexemes;
  private Map<String, String> defines;
  private List<Token> tokens;
  private int nextTokenIdx;
  private boolean noTokensYetThisLine;

  //Cached properties
  private boolean debugMaster;
  private boolean debugPrintProgress;
  private boolean debugPrintSource;

  static {
    reservedWords = new HashMap<>() {{
      // Keywords.
      put("else",   TOKEN_ELSE);
      put("false",  TOKEN_FALSE);
      put("for",    TOKEN_FOR);
      put("if",     TOKEN_IF);
      put("nil",    TOKEN_NIL);
      put("return", TOKEN_RETURN);
      put("super",  TOKEN_SUPER);
      put("this",   TOKEN_THIS);
      put("true",   TOKEN_TRUE);
      put("while",  TOKEN_WHILE);

      // Types.
      put("int",    TOKEN_T_INT);
      put("object", TOKEN_T_OBJECT);
      put("status", TOKEN_T_STATUS);
      put("string", TOKEN_T_STRING);
      put("void",   TOKEN_T_VOID);
    }};

    singleCharLexemes = new HashMap<>() {{
      put('(', TOKEN_LEFT_PAREN);
      put(')', TOKEN_RIGHT_PAREN);
      put('{', TOKEN_LEFT_BRACE);
      put('}', TOKEN_RIGHT_BRACE);
      put(',', TOKEN_COMMA);
      put('.', TOKEN_DOT);
      put('+', TOKEN_PLUS);
      put(';', TOKEN_SEMICOLON);
      put('*', TOKEN_STAR); 
    }};
  }

  //Scanner(Props, Debugger)
  public Scanner(Props properties, Debugger debugger) {
    super(properties, debugger);

    if (debugPrintProgress) debugger.printProgress("Initializing scanner....");
  }

  //scan(String)
  public void scan(String source) {
    setSource(source); //resets head, tail, line

    defines = new HashMap<>();
    tokens = new ArrayList<>();
    nextTokenIdx = 0;
    noTokensYetThisLine = true;

    if (debugPrintSource) debugger.printSource(source);
    if (debugPrintProgress) debugger.printProgress("Scanning....");

    // Scan tokens.
    while (!isAtEnd()) {
      // We are at the beginning of the next lexeme.
      tail = head;

      lexToken();
    }
  }

  //lexToken()
  private void lexToken() {
    char c = peekAndAdvance();
    TokenType type = singleCharLexemes.get(c);

    if (type != null) addToken(type);
    else switch (c) {
      case '&':
        if (match('&')) addToken(TOKEN_DBL_AMP);
        else unexpectedChar();

        break;
      case '|':
        if (match('|')) addToken(TOKEN_DBL_PIPE);
        else unexpectedChar();

        break;
      case '-':
        addToken(match('>') ? TOKEN_INVOKE : TOKEN_MINUS); break;
      case '!':
        addToken(match('=') ? TOKEN_BANG_EQUAL : TOKEN_BANG); break;
      case '=':
        addToken(match('=') ? TOKEN_EQUAL_EQUAL : TOKEN_EQUAL); break;
      case '<':
        addToken(match('=') ? TOKEN_LESS_EQUAL : TOKEN_LESS); break;
      case '>':
        addToken(match('=') ? TOKEN_GREATER_EQUAL : TOKEN_GREATER); break;
      case '/':
        if (match('/')) singleLineComment();
        else if (match('*')) multiLineComment();
        else addToken(TOKEN_SLASH);

        break;
      case ' ': case '\r': case '\t': break; // Ignore whitespace.
      case '\n': newLine(); break;
      case '"': string(); break;
      case '#':
        if (noTokensYetThisLine) directive();
        else unexpectedChar();

        break;
      default:
        if (isDigit(c)) number();
        else if (isAlpha(c)) identifier();
        else unexpectedChar();

        break;
    }
  }

  //newLine()
  private void newLine() {
    line++;

    noTokensYetThisLine = true; //reset
  }

  //singleLineComment()
  private void singleLineComment() {
    while (peek() != '\n' && !isAtEnd())
      head++;
  }

  //multiLineComment()
  private void multiLineComment() {
    if (!seek('*')) {
      errorToken("Unterminated multiline comment.");

      return;
    }

    //Consume the '*'.
    head++;

    if (peek() != '/')
      //Continue seeking end of comment ("*/").
      multiLineComment();
    else
      //Consume the '/'.
      head++;
  }

  //directive()
  private void directive() {
    singleLineComment(); //temp
  }

  //identifier()
  private void identifier() {
    while (isAlphaNumeric(peek())) head++;

    String text = source.substring(tail, head);
    TokenType type = reservedWords.get(text);

    if (type == null) type = TOKEN_IDENTIFIER;

    addToken(type);
  }

  //number()
  private void number() {
    while (isDigit(peek())) head++;

    // Look for a fractional part.
    if (peek() == '.' && isDigit(peekNext())) {
      // Consume the '.'
      head++;

      while (isDigit(peek())) head++;
    }

    addToken(
      TOKEN_NUMBER,
      Double.parseDouble(source.substring(tail, head))
    );
  }

  //string()
  private void string() {
    if (!seek('"')) {
      errorToken("Unterminated string.");

      return;
    }

    // Consume the closing '"'.
    head++;

    // Trim the surrounding quotes.
    String value = source.substring(tail + 1, head - 1);

    addToken(TOKEN_STRING, value);
  }

  //seek(char)
  private boolean seek(char c) {
    while (peek() != c && !isAtEnd()) {
      if (peek() == '\n')
        newLine();

      head++;
    }

    return !isAtEnd();
  }

  //unexpectedChar()
  private void unexpectedChar() {
    errorToken("Unexpected character: '" + peekPrev() + "'.");
  }

  //errorToken(String)
  private void errorToken(String errorText) {
    tokens.add(new Token(TOKEN_ERROR, errorText, null, line));
  }

  //addToken(TokenType)
  private void addToken(TokenType type) {
    addToken(type, null);
  }

  //addToken(TokenType, Object)
  private void addToken(TokenType type, Object literal) {
    String text = source.substring(tail, head);

    tokens.add(new Token(type, text, literal, line));

    noTokensYetThisLine = false;
  }

  //getNextToken()
  public Token getNextToken() {
    if (nextTokenIdx > (tokens.size() - 1))
      return (new Token(TOKEN_EOF, "", null, line));
    else
      return tokens.get(nextTokenIdx++);
  }

  //updateCachedProperties()
  @Override
  protected void updateCachedProperties() {
    debugMaster = properties.getBool("DEBUG_MASTER");
    debugPrintProgress = debugMaster && properties.getBool("DEBUG_PRINT_PROGRESS");
    debugPrintSource = debugMaster && properties.getBool("DEBUG_PRINT_SOURCE");
  }
}
