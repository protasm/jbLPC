package jbLPC.vm;

import jbLPC.compiler.C_Function;

public class Closure {
  private C_Function function;
  private Upvalue[] upvalues;

  //Closure()
  public Closure(C_Function function) {
    this.function = function;

    upvalues = new Upvalue[function.upvalueCount()];
  }

  //cFunction()
  public C_Function function() {
    return function;
  }

  //upvalues()
  public Upvalue[] upvalues() {
    return upvalues;
  }

  //toString()
  @Override
  public String toString() {
    return "<closure: " + function.name() + ">";
  }
}
