package jbLPC.compiler;

public abstract class Compilation {
  protected String name;
  private Chunk chunk;

  //Compilation(String)
  public Compilation(String name) {
    this.name = name;
    chunk = new Chunk();
  }

  //name()
  public String name() {
    return name;
  }

  //chunk()
  public Chunk chunk() {
    return chunk;
  }

  //toString()
  @Override
  public String toString() {
    return "<compilation: " + name + ">";
  }
}
