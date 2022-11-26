package jbLPC.nativefn;

import jbLPC.vm.VM;

public class NativePrintLn extends NativeFn {
  //NativePrintLn(VM, String, int)
  public NativePrintLn(VM vm, String fnName, int arity) {
    super(vm, fnName, arity);
  }

  //execute(Object[])
  public Object execute(Object[] args) {
    if (args.length == 0)
      System.out.println();
    else if (args.length == 1) {
      Object o = args[0];

      if (o instanceof Number) {
        double d = (double)o;

        if (d == (long)d)
          System.out.println(String.format("%d", (long)d));
        else
          System.out.println(String.format("%s", d));
      } else
        System.out.println(o);
    }

    return null;
  }
}
