package jbLPC.compiler;

public class C_Method extends C_Function {
  public C_Method(String name) {
    super(name);
  }

  @Override
  public String toString() {
    return "<cMethod: " + name + ">";
  }
}
