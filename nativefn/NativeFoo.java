package jbLPC.nativefn;

import jbLPC.vm.VM;

public class NativeFoo extends NativeFn {
  //NativeFoo(VM, String, int)
  public NativeFoo(VM vm, String fnName, int arity) {
    super(vm, fnName, arity);
  }

  //execute(Object[])
  public Object execute(Object[] args) {
    vm.write("Did somebody say foo?");
    vm.write("Foo1: " + args[0]);
    vm.write("Foo2: " + args[1]);
    vm.write("Foo3: " + args[2]);

    return null;
  }
}
