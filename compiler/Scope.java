package jbLPC.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import jbLPC.compiler.Function.FunctionType;
import jbLPC.main.Props;
import jbLPC.scanner.Token;

import static jbLPC.compiler.Function.FunctionType.*;
import static jbLPC.scanner.TokenType.*;

public class Scope {
  private Scope enclosing;
  private Function function;
  private Stack<Local> locals;
  private List<Upvalue> upvalues;
  private int depth; //the number of surrounding blocks

  //Scope(Scope, FunctionType)
  public Scope(Scope enclosing, FunctionType type) {
    this(enclosing, type, null);
  }

  //Scope(Scope, FunctionType, String)
  public Scope(Scope enclosing, FunctionType type, String name) {
    this.enclosing = enclosing;

    function = new Function(type, name);
    locals = new Stack<>();
    upvalues = new ArrayList<>();
    depth = 0;

    //Block out stack slot zero for the function being called.
    Token token;

    if (type == FunctionType.TYPE_FUNCTION)
      token = new Token(null, "", null, -1);
    else
      token = new Token(null, "this", null, -1);

    locals.push(new Local(token, 0));
  }

  //enclosing()
  public Scope enclosing() {
    return enclosing;
  }

  //function()
  public Function function() {
    return function;
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
