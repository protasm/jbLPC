package jbLPC.compiler;

public class CompilerUpvalue {
  private int index;
  private boolean isLocal;

  //Upvalue(int, boolean)
  public CompilerUpvalue(int index, boolean isLocal) {
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
