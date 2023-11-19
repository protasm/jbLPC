package jbLPC.nativefn;

import jbLPC.vm.VM;

public class NativePrint extends NativeFn {
  //NativePrint(VM, String, int)
  public NativePrint(VM vm, String fnName, int arity) {
    super(vm, fnName, arity);
  }

  //execute(Object[])
  public Object execute(Object[] args) {
    Object o = args[0];

    if (o instanceof Number) {
      double d = (double)o;

      if (d == (long)d)
        System.out.print(String.format("%d", (long)d));
      else
        System.out.print(String.format("%s", d));
    } else
      System.out.print(o);

    return null;
  }
}
