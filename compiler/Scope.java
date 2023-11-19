package jbLPC.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import jbLPC.scanner.Token;

public class Scope {
  private Scope enclosing;
  private Compilation compilation;
  private Stack<Local> locals;
  private List<Upvalue> upvalues;
  private int depth; //the number of surrounding blocks

  //Scope(Scope, Compilation)
  public Scope(Scope enclosing, Compilation compilation) {
    this.enclosing = enclosing;

    this.compilation = compilation;
    locals = new Stack<>();
    upvalues = new ArrayList<>();
    depth = 0;

    //Block out stack slot zero for the compilation being called.
    Token token;

    if (compilation instanceof C_Function)
      token = new Token(null, "", null, -1);
    else
      token = new Token(null, "this", null, -1);

    locals.push(new Local(token, 0));
  }

  //enclosing()
  public Scope enclosing() {
    return enclosing;
  }

  //compilation()
  public Compilation compilation() {
    return compilation;
  }

  //depth()
  public int depth() {
    return depth;
  }

  //setDepth(int)
  public void setDepth(int depth) {
    this.depth = depth;
  }

  //locals()
  public Stack<Local> locals() {
    return locals;
  }

  //upvalues()
  public List<Upvalue> upvalues() {
    return upvalues;
  }

  //markTopLocalInitialized()
  public void markTopLocalInitialized() {
    locals.peek().setDepth(depth);
  }

  //addUpvalue(Upvalue)
  public int addUpvalue(Upvalue upvalue) {
    upvalues.add(upvalue);

    //return index of newly-added upvalue
    return upvalues.size() - 1;
  }

  //getUpvalue(int)
  public Upvalue getUpvalue(int index) {
    return upvalues.get(index);
  }
}
