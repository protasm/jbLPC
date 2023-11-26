package jbLPC.compiler;

import jbLPC.scanner.Token;

public class C_Local {
  private Token token;
  private int depth;
  private boolean isCaptured;

  //C_Local()
  public C_Local() {
    this(null, -1);
  }

  //C_Local(Token, int)
  public C_Local(Token token, int depth) {
    this.token = token;
    this.depth = depth;

    isCaptured = false;
  }

  //token()
  public Token token() {
    return token;
  }

  //depth()
  public int depth() {
    return depth;
  }

  //setDepth(int)
  public void setDepth(int depth) {
    this.depth = depth;
  }

  //isCaptured()
  public boolean isCaptured() {
    return isCaptured;
  }

  //setIsCaptured(boolean)
  public void setIsCaptured(boolean isCaptured) {
    this.isCaptured = isCaptured;
  }

  //toString()
  @Override
  public String toString() {
    return "[ " + token.lexeme() + " (" + depth + ") ]";
  }
}
