package jbLPC.nativefn;

import jbLPC.vm.VM;

public class NativePrint extends NativeFn {
  //NativePrint(VM, String, int)
  public NativePrint(VM vm, String fnName, int arity) {
    super(vm, fnName, arity);
  }

  //execute(Object[])
  public Object execute(Object[] args) {
    if (args.length == 0)
      vm.write("");
    else if (args.length == 1) {
      Object o = args[0];

      if (o instanceof Number) {
        double d = (double)o;

        if (d == (long)d)
          vm.write(String.format("%d", (long)d));
        else
      	  vm.write(String.format("%s", d));
      } else
        vm.write(o.toString());
    }

    return null;
  }
}
