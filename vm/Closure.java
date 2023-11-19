package jbLPC.vm;

import jbLPC.compiler.C_Function;
import jbLPC.compiler.Compilation;

public class Closure {
  private Compilation compilation;
  private Upvalue[] upvalues;

  //Closure()
  public Closure(Compilation compilation) {
    this.compilation = compilation;

    if (compilation instanceof C_Function)
      upvalues = new Upvalue[((C_Function)compilation).upvalueCount()];
  }

  //compilation()
  public Compilation compilation() {
    return compilation;
  }

  //upvalues()
  public Upvalue[] upvalues() {
    return upvalues;
  }

  //upvalueCount()
  public int upvalueCount() {
    if (compilation instanceof C_Function)
      return ((C_Function)compilation).upvalueCount();
    else
      return 0;
  }

  //toString()
  @Override
  public String toString() {
    return compilation.toString();
  }
}
