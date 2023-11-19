package jbLPC.preprocessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A macro argument.
 *
 * This encapsulates a raw and preprocessed token stream.
 */
public class Argument extends ArrayList<Token> {
  private static final long serialVersionUID = 1L;
  private List<Token> expansion;

  public Argument() {
    this.expansion = null;
  }

  public void addToken(Token tok) {
    add(tok);
  }

  void expand(Preprocessor p)
    throws IOException, LexerException {
    /* Cache expansion. */
    if (expansion == null) {
      this.expansion = p.expand(this);
      // System.out.println("Expanded arg " + this);
    }
  }

  public Iterator<Token> expansion() {
    return expansion.iterator();
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();

    buf.append("Argument(");
    // buf.append(super.toString());
    buf.append("raw=[ ");

    for (int i = 0; i < size(); i++)
      buf.append(get(i).getText());

    buf.append(" ];expansion=[ ");

    if (expansion == null)
      buf.append("null");
    else
      for (Token token : expansion)
        buf.append(token.getText());

    buf.append(" ])");

    return buf.toString();
  }
}
