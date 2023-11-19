package jbLPC.compiler;

public class C_LPCObject extends Compilation {
  //C_LPCObject(String)
  public C_LPCObject(String name) {
    super(name);
  }

  //toString
  @Override
  public String toString() {
    return "<cObj: " + name + ">";
  }
}
