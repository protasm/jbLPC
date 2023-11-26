package jbLPC.compiler;

public class C_CompilerClass {
  private C_CompilerClass enclosing;
  private boolean hasSuperclass;

  //CompilerClass(CompilerClass, boolean)
  public C_CompilerClass(C_CompilerClass enclosing, boolean hasSuperclass) {
    this.enclosing = enclosing;
    this.hasSuperclass = hasSuperclass;
  }

  //enclosing()
  public C_CompilerClass enclosing() {
    return enclosing;
  }

  //hasSuperclass()
  public boolean hasSuperclass() {
    return hasSuperclass;
  }

  //setHasSuperclass(boolean)
  public void setHasSuperclass(boolean hasSuperclass) {
    this.hasSuperclass = hasSuperclass;
  }
}
