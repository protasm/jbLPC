package jbLPC.compiler;

public class Function implements HasArity {
  public enum FunctionType {
    TYPE_SCRIPT,
    TYPE_OBJECT,
    TYPE_FUNCTION,
    TYPE_INITIALIZER,
    TYPE_METHOD,
    TYPE_OBJ_METHOD,
  }

  private FunctionType type;
  private String name;
  private int arity;
  private int upvalueCount;
  private Chunk chunk;

  //Function(FunctionType, String)
  public Function(FunctionType type, String name) {
    this.type = type;
    this.name = name;

    arity = 0;
    upvalueCount = 0;
    chunk = new Chunk();
  }

  //type()
  public FunctionType type() {
    return type;
  }

  //name()
  public String name() {
    return name;
  }

  //arity()
  @Override
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

  //chunk()
  public Chunk chunk() {
    return chunk;
  }

  //toString()
  @Override
  public String toString() {
    if (type == FunctionType.TYPE_SCRIPT)
      return "<script>";
    else if (type == FunctionType.TYPE_OBJECT)
      return "<object>";
    else
      return "<fn " + name + ">";
  }
}
