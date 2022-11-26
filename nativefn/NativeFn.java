package jbLPC.nativefn;

import jbLPC.compiler.HasArity;
import jbLPC.vm.VM;

public abstract class NativeFn implements HasArity {
  protected VM vm;
  protected String fnName;
  protected int arity = 0;

  //NativeFn(VM, String, int)
  public NativeFn(VM vm, String fnName, int arity) {
    this.vm = vm;
    this.fnName = fnName;
    this.arity = arity;
  }

  //vm()
  public VM vm() {
    return vm;
  }

  //fnName()
  public String fnName() {
    return fnName;
  }

  //arity()
  @Override
  public int arity() {
    return arity;
  }

  public abstract Object execute(Object[] args);

  //toString()
  @Override
  public String toString() {
    return "<nativefn " + fnName + ">";
  }
}
