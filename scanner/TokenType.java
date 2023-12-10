package jbLPC.scanner;

public enum TokenType {
  // One character tokens.
  TOKEN_LEFT_PAREN, TOKEN_RIGHT_PAREN,
  TOKEN_LEFT_BRACE, TOKEN_RIGHT_BRACE,
  TOKEN_LEFT_BRACKET, TOKEN_RIGHT_BRACKET,
  TOKEN_DOT, TOKEN_COMMA, TOKEN_SEMICOLON,
  TOKEN_SLASH, TOKEN_STAR, TOKEN_BANG,
  TOKEN_EQUAL, TOKEN_GREATER,
  TOKEN_LESS, TOKEN_MINUS, TOKEN_PLUS,

  // Two character tokens
  TOKEN_BANG_EQUAL,
  TOKEN_DBL_AMP, TOKEN_DBL_PIPE,
  TOKEN_EQUAL_EQUAL,
  TOKEN_GREATER_EQUAL,
  TOKEN_INVOKE, TOKEN_LESS_EQUAL,
  TOKEN_MINUS_EQUAL, TOKEN_PLUS_EQUAL,
  TOKEN_SLASH_EQUAL, TOKEN_STAR_EQUAL,
  TOKEN_PLUS_PLUS, TOKEN_MINUS_MINUS,

  // Literals.
  TOKEN_IDENTIFIER, TOKEN_STRING, TOKEN_NUMBER,

  // Keywords.
  TOKEN_ELSE, TOKEN_FALSE, TOKEN_FOR,
  TOKEN_IF, TOKEN_INHERIT, TOKEN_NIL,
  TOKEN_RETURN, TOKEN_SUPER, //TOKEN_THIS,
  TOKEN_TRUE, TOKEN_WHILE,

  // LPC Type tokens.
  TOKEN_PRIMITIVE,

  // Special tokens.
  TOKEN_ERROR, TOKEN_EOF
};
