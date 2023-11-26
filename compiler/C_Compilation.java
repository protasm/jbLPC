package jbLPC.compiler;

public class C_Compilation {
  public static enum C_CompilationType {
    TYPE_SCRIPT,
    TYPE_OBJECT,
    TYPE_FUNCTION,
    TYPE_METHOD
  }
  
  protected String name;
  private C_CompilationType type;
  private C_InstrList instrList;

  //Compilation(String, CompilationType)
  public C_Compilation(String name, C_CompilationType type) {
    this.type = type;
    this.name = name;
    instrList = new C_InstrList();
  }

  //name()
  public String name() {
    return name;
  }
  
  //type()
  public C_CompilationType type() {
    return type;
  }

  //instrList()
  public C_InstrList instrList() {
    return instrList;
  }

  //toString()
  @Override
  public String toString() {
    switch (type) {
      case TYPE_SCRIPT:
        return "<cScript>";
      case TYPE_OBJECT:
        return "<cObj: " + name + ">";
      case TYPE_FUNCTION:
        return "<cFn: " + name + ">";
      default:
        return "<cComp: " + name + ">";
    }
  }
}
