package jbLPC.nativefn;

import jbLPC.compiler.Compilation;
import jbLPC.compiler.LPCObjectCompiler;
import jbLPC.util.SourceFile;
import jbLPC.vm.VM;

public class NativeCompileLPCObject extends NativeFn {
  //NativeCompile(VM, String, int)
  public NativeCompileLPCObject(VM vm, String fnName, int arity) {
    super(vm, fnName, arity);
  }

  //execute(Object[])
  @Override
  public Object execute(Object[] args) {
    String path = (String)args[0];
    Compilation compilation = vm.compilation(path);

    return compilation;
  }
}
