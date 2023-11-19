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
    String libPath = vm.getLibPath();
    String fullPath = libPath + path;
    SourceFile file  = new SourceFile(fullPath);
    LPCObjectCompiler compiler = new LPCObjectCompiler();
    Compilation compilation = compiler.compile(file.getNameNoExt(), file.source());

    return compilation;
  }
}
