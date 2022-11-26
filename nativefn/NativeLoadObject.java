package jbLPC.nativefn;

import jbLPC.util.SourceFile;
import jbLPC.vm.VM;

import static jbLPC.compiler.OpCode.*;

public class NativeLoadObject extends NativeFn {
  //NativeLoadObject(VM, String, int)
  public NativeLoadObject(VM vm, String fnName, int arity) {
    super(vm, fnName, arity);
  }

  //execute(Object[])
  public Object execute(Object[] args) {
    String fileName = (String)args[0];
    String libPath = vm.getLibPath();
    String fullPath = libPath + fileName;
    String source = new SourceFile(fullPath).source();

    vm.syntheticInstruction((byte)0); //# args for OP_CALL 
    vm.syntheticInstruction(OP_CALL);

    return vm.loadObject(fileName, source);
  }
}
