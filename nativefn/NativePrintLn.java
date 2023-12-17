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
      vm.writeLn("");
    else if (args.length == 1) {
      new NativePrint(vm, fnName, arity).execute(args);
      
      vm.write("\n");
    }

    return null;
  }
}
