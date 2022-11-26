package jbLPC.nativefn;

import jbLPC.vm.VM;

public class NativeFoo extends NativeFn {
  //NativeFoo(VM, String, int)
  public NativeFoo(VM vm, String fnName, int arity) {
    super(vm, fnName, arity);
  }

  //execute(Object[])
  public Object execute(Object[] args) {
    System.out.println("Did somebody say foo?");
    System.out.println("Foo1: " + args[0]);
    System.out.println("Foo2: " + args[1]);
    System.out.println("Foo3: " + args[2]);

    return null;
  }
}
