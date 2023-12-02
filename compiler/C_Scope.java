package jbLPC.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import jbLPC.scanner.Token;

import static jbLPC.compiler.C_Compilation.C_CompilationType.TYPE_OBJECT;
import static jbLPC.scanner.TokenType.TOKEN_THIS;

public class C_Scope {
  private C_Scope enclosing;
  private C_Compilation compilation;
  private Stack<C_Local> locals; //simulate runtime vStack
  private List<C_Upvalue> upvalues;
  private int depth; //the number of surrounding blocks

  //Scope(Scope, Compilation)
  public C_Scope(C_Scope enclosing, C_Compilation compilation) {
    this.enclosing = enclosing;

    this.compilation = compilation;
    locals = new Stack<>();
    upvalues = new ArrayList<>();
    depth = 0;

    //Block out stack slot zero for the compilation being compiled.
    Token token;

    if (compilation.type() == TYPE_OBJECT)
      token = new Token(TOKEN_THIS, "this", null, -1);
    else
      token = new Token(null, "", null, -1);

    locals.push(new C_Local(token, 0));
  }

  //enclosing()
  public C_Scope enclosing() {
    return enclosing;
  }

  //compilation()
  public C_Compilation compilation() {
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
  public Stack<C_Local> locals() {
    return locals;
  }

  //upvalues()
  public List<C_Upvalue> upvalues() {
    return upvalues;
  }

  //markTopLocalInitialized()
  public void markTopLocalInitialized() {
    locals.peek().setDepth(depth);
  }

  //addUpvalue(C_Upvalue)
  public int addUpvalue(C_Upvalue upvalue) {
    upvalues.add(upvalue);

    //return index of newly-added upvalue
    return upvalues.size() - 1;
  }

  //getUpvalue(int)
  public C_Upvalue getUpvalue(int index) {
    return upvalues.get(index);
  }
}
