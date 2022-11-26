package jbLPC.nativefn;

import jbLPC.vm.VM;

public class NativeClock extends NativeFn {
  //NativeClock(VM, String, int)
  public NativeClock(VM vm, String fnName, int arity) {
    super(vm, fnName, arity);
  }

  //execute(Object[])
  public Object execute(Object[] args) {
    Long clock = System.currentTimeMillis();

    return (double)clock / 1000;
  }
}
