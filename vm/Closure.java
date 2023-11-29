package jbLPC.vm;

import jbLPC.compiler.C_Compilation;
import jbLPC.compiler.C_Function;

import static jbLPC.compiler.C_Compilation.C_CompilationType.TYPE_FUNCTION;

public class Closure {
  private C_Compilation compilation;
  private Upvalue[] upvalues;

  //Closure(C_Compilation)
  public Closure(C_Compilation compilation) {
    this.compilation = compilation;

    if (compilation.type() == TYPE_FUNCTION) {
      C_Function function = (C_Function)compilation;

      upvalues = new Upvalue[function.upvalueCount()];
    }
  }

  //compilation()
  public C_Compilation compilation() {
    return compilation;
  }

  //upvalues()
  public Upvalue[] upvalues() {
    return upvalues;
  }

  //toString()
  @Override
  public String toString() {
    return "<closure: " + compilation.name() + ">";
  }
}
