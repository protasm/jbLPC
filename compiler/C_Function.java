package jbLPC.compiler;

public class C_Function extends Compilation implements HasArity {
  protected int arity;
  protected int upvalueCount;

  //C_Function(String)
  public C_Function(String name) {
    super(name);

    arity = 0;
    upvalueCount = 0;
  }

  //arity()
  public int arity() {
    return arity;
  }

  //setArity(int)
  public void setArity(int arity) {
    this.arity = arity;
  }

  //upvalueCount()
  public int upvalueCount() {
    return upvalueCount;
  }

  //setUpvalueCount(int)
  public void setUpvalueCount(int upvalueCount) {
    this.upvalueCount = upvalueCount;
  }

  //toString()
  @Override
  public String toString() {
    return "<cFn: " + name + ">";
  }
}
