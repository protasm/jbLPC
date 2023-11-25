package jbLPC.vm;

import jbLPC.compiler.C_Function;

public class Closure {
  private C_Function cFunction;
  private Upvalue[] upvalues;

  //Closure()
  public Closure(C_Function cFunction) {
    this.cFunction = cFunction;

    upvalues = new Upvalue[cFunction.upvalueCount()];
  }

  //cFunction()
  public C_Function cFunction() {
    return cFunction;
  }

  //upvalues()
  public Upvalue[] upvalues() {
    return upvalues;
  }

  //toString()
  @Override
  public String toString() {
    return cFunction.toString();
  }
}
