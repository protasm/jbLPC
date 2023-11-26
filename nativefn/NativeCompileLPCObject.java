package jbLPC.nativefn;

import jbLPC.compiler.Compilation;
import jbLPC.vm.VM;

public class NativeCompileLPCObject extends NativeFn {
  //NativeCompile(VM, String, int)
  public NativeCompileLPCObject(VM vm, String fnName, int arity) {
    super(vm, fnName, arity);
  }

  //execute(Object[])
  @Override
  public Object execute(Object[] args) {
    String path = (String)args[0]; //object path
    Compilation compilation = vm.compilation(path);

    return compilation;
  }
}
