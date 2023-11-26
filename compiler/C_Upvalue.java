package jbLPC.compiler;

public class C_Upvalue {
  private int index;
  private boolean isLocal;

  //C_Upvalue(int, boolean)
  public C_Upvalue(int index, boolean isLocal) {
    this.index = index;
    this.isLocal = isLocal;
  }

  //index()
  public int index() {
    return index;
  }

  //isLocal()
  public boolean isLocal() {
    return isLocal;
  }

  //toString()
  @Override
  public String toString() {
    return "[ index " + index + " (isLocal = " + isLocal + ") ]";
  }
}
