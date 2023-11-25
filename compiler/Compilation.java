package jbLPC.compiler;

import java.util.ArrayList;
import java.util.List;

public abstract class Compilation {
  protected String name;
  private List<Instruction> instructions;

  //Compilation(String)
  public Compilation(String name) {
    this.name = name;
    instructions = new ArrayList<>();
  }

  //name()
  public String name() {
    return name;
  }

  //instructions()
  public List<Instruction> instructions() {
    return instructions;
  }

  //toString()
  @Override
  public String toString() {
    return "<compilation: " + name + ">";
  }
}
