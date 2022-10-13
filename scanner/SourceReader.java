package jbLPC.scanner;

import jbLPC.debug.Debugger;
import jbLPC.main.Props;
import jbLPC.main.PropsObserver;

public abstract class SourceReader extends PropsObserver {
  protected String source;
  protected int head;
  protected int tail;
  protected int line;

  //SourceReader(Props, Debugger)
  public SourceReader(Props properties, Debugger debugger) {
    super(properties, debugger);
  }

  //setSource(String)
  protected void setSource(String source) {
    this.source = source;

    head = 0;
    tail = 0;
    line = 1;
  }

  //match()
  protected boolean match(char expected) {
    if (isAtEnd()) return false;

    if (source.charAt(head) != expected) return false;

    head++;

    return true;
  }

  //peek()
  protected char peek() {
    if (isAtEnd()) return '\0';

    return source.charAt(head);
  }

  //peekNext()
  protected char peekNext() {
    if (head + 1 >= source.length()) return '\0';

    return source.charAt(head + 1);
  }

  //peekPrev()
  protected char peekPrev() {
    if (isAtStart() || isAtEnd()) return '\0';

    return source.charAt(head - 1);
  }

  //isAlpha(char)
  protected boolean isAlpha(char c) {
    return (c >= 'a' && c <= 'z') ||
           (c >= 'A' && c <= 'Z') ||
            c == '_';
  }

  //isAlphaNumeric(char)
  protected boolean isAlphaNumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }

  //isDigit(char)
  protected boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  //isAtStart()
  protected boolean isAtStart() {
    return head == 0;
  }

  //isAtEnd()
  protected boolean isAtEnd() {
    return head >= source.length();
  }

  //peekAndAdvance()
  protected char peekAndAdvance() {
    return source.charAt(head++);
  }
}
