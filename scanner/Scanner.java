package jbLPC.scanner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jbLPC.debug.Debugger;
import jbLPC.main.Props;
import jbLPC.main.PropsObserver;
import jbLPC.preprocessor.Preprocessor;
import jbLPC.preprocessor.source.StringLexerSource;
import static jbLPC.scanner.TokenType.*;

public class Scanner implements PropsObserver, Iterator<Token> {
  private static final char EOL = '\n';
  private static final Map<String, TokenType> reservedWords;
  private static final Map<Character, TokenType> oneCharLexemes;
  private ScannableSource ss;

  //Cached properties
  private boolean debugMaster;
  private boolean debugPrintProgress;
  private boolean debugPrintSource;

  static {
    reservedWords = new HashMap<>() {{
      // Keywords.
      put("else",    TOKEN_ELSE);
      put("false",   TOKEN_FALSE);
      put("for",     TOKEN_FOR);
      put("if",      TOKEN_IF);
      put("inherit", TOKEN_INHERIT);
      put("nil",     TOKEN_NIL);
      put("return",  TOKEN_RETURN);
      put("this",    TOKEN_THIS);
      put("true",    TOKEN_TRUE);
      put("while",   TOKEN_WHILE);

      // LPC Types.
      put("int",    TOKEN_TYPE);
      put("object", TOKEN_TYPE);
      put("status", TOKEN_TYPE);
      put("string", TOKEN_TYPE);
      put("void",   TOKEN_TYPE);
    }};

    oneCharLexemes = new HashMap<>() {{
      put('(', TOKEN_LEFT_PAREN);
      put(')', TOKEN_RIGHT_PAREN);
      put('{', TOKEN_LEFT_BRACE);
      put('}', TOKEN_RIGHT_BRACE);
      put('.', TOKEN_DOT);
      put(',', TOKEN_COMMA);
      put(';', TOKEN_SEMICOLON);
    }};
  }

  //Scanner(String)
  public Scanner(String source) {
    Props.instance().registerObserver(this);

    Preprocessor pp = new Preprocessor();

    pp.addInput(new StringLexerSource(source, true));
    pp.getSystemIncludePath().add(".");

    ss = new ScannableSource(pp.preprocess());

    if (debugPrintSource) Debugger.instance().printSource(ss.toString());
    if (debugPrintProgress) Debugger.instance().printProgress("Scanner initialized.");
  }

  //lexToken()
  public Token lexToken() {
    if (ss.atEnd())
      return new Token(TOKEN_EOF, "", null, ss.line());

    ss.sync(); // reset (tail = head)

    char c = ss.peekAndAdvance();

    if (oneCharLexemes.containsKey(c))
      return makeToken(oneCharLexemes.get(c));
    if (isDigit(c)) return number();
    if (isAlpha(c)) return identifier();

    switch (c) {
      case EOL: return null;
      case '"': return string();
      case '&':
        if (ss.match('&')) return makeToken(TOKEN_DBL_AMP);
        else return unexpectedChar(c);
      case '|':
        if (ss.match('|')) return makeToken(TOKEN_DBL_PIPE);
        else return unexpectedChar(c);
      case ':':
        if (ss.match(':')) return makeToken(TOKEN_SUPER);
        else return unexpectedChar(c);
      case '-':
        if (ss.match('-'))
          return makeToken(TOKEN_MINUS_MINUS);
        else if (ss.match('='))
          return makeToken(TOKEN_MINUS_EQUAL);
        else if (ss.match('>'))
          return makeToken(TOKEN_INVOKE);
        else
          return makeToken(TOKEN_MINUS);
      case '+':
        if (ss.match('+'))
          return makeToken(TOKEN_PLUS_PLUS);
        else if (ss.match('='))
          return makeToken(TOKEN_PLUS_EQUAL);
        else
          return makeToken(TOKEN_PLUS);
      case '!':
        return makeToken(ss.match('=') ? TOKEN_BANG_EQUAL : TOKEN_BANG);
      case '=':
        return makeToken(ss.match('=') ? TOKEN_EQUAL_EQUAL : TOKEN_EQUAL);
      case '<':
        return makeToken(ss.match('=') ? TOKEN_LESS_EQUAL : TOKEN_LESS);
      case '>':
        return makeToken(ss.match('=') ? TOKEN_GREATER_EQUAL : TOKEN_GREATER);
      case '/':
        if (ss.match('/'))
          return lineComment();
        else if (ss.match('*'))
          return blockComment();
        else if (ss.match('='))
          return makeToken(TOKEN_SLASH_EQUAL);
        else
          return makeToken(TOKEN_SLASH);
      case '*':
        return makeToken(ss.match('=') ? TOKEN_STAR_EQUAL : TOKEN_STAR);
      case ' ':
      case '\r':
      case '\t':
        while (isWhitespace(ss.peek())) ss.advance(); //fast-forward through whitespace

        return null;
      default:
        return unexpectedChar(c);
    } //switch
  }

  //lineComment()
  private Token lineComment() {
    ss.seek(EOL);

    return null;
  }

  //blockComment()
  private Token blockComment() {
    while (!ss.atEnd()) {
      ss.seek('*');

      if (ss.peekPrev() == '/')
        return errorToken("Nested block comment");

      ss.advance();

      if (ss.match('/')) return null;
    } //while

    // Error if we get here.
    return errorToken("Unterminated block comment.");
  }

  //identifier()
  private Token identifier() {
    while (isAlphaNumeric(ss.peek())) ss.advance();

    TokenType type = reservedWords.get(ss.read());

    if (type == null) type = TOKEN_IDENTIFIER;

    return makeToken(type);
  }

  //number()
  private Token number() {
    while (isDigit(ss.peek())) ss.advance();

    // Look for a fractional part.
    if (ss.peek() == '.' && isDigit(ss.peekNext())) {
      ss.advance(); //consume the '.'

      while (isDigit(ss.peek())) ss.advance();
    }

    return makeToken(TOKEN_NUMBER, Double.parseDouble(ss.read()));
  }

  //string()
  private Token string() {
    if (!ss.seek('"'))
      return errorToken("Unterminated string.");

    ss.advance(); //consume the closing '"'

    return makeToken(TOKEN_STRING, ss.readTrimmed());
  }

  //isWhitespace(char)
  private boolean isWhitespace(char c) {
    return (c == ' ') || (c == '\r') || (c == '\t');
  }

  //isAlpha(char)
  private boolean isAlpha(char c) {
    return (c >= 'a' && c <= 'z') ||
           (c >= 'A' && c <= 'Z') ||
            c == '_';
  }

  //isAlphaNumeric(char)
  private boolean isAlphaNumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }

  //isDigit(char)
  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  //unexpectedChar(char)
  private Token unexpectedChar(char c) {
    return errorToken("Unexpected character: '" + c + "'.");
  }

  //errorToken(String)
  private Token errorToken(String message) {
    return new Token(TOKEN_ERROR, message, null, ss.line());
  }

  //makeToken(TokenType)
  private Token makeToken(TokenType type) {
    return makeToken(type, null);
  }

  //makeToken(TokenType, Object)
  private Token makeToken(TokenType type, Object literal) {
    return new Token(type, ss.read(), literal, ss.line());
  }

  //hasNext()
  @Override
  public boolean hasNext() {
    return true;
  }

  //next()
  @Override
  public Token next() {
    Token token;

    do {
      token = lexToken();
    } while (token == null);

    return token;
  }

  //remove()
  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  //updateCachedProperties()
  private void updateCachedProperties() {
    debugMaster = Props.instance().getBool("DEBUG_MASTER");
    debugPrintProgress = debugMaster && Props.instance().getBool("DEBUG_PROG");
    debugPrintSource = debugMaster && Props.instance().getBool("DEBUG_SOURCE");
  }

  //notifyPropertiesChanged()
  public void notifyPropertiesChanged() {
    updateCachedProperties();
  }
}
