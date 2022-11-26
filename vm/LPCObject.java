package jbLPC.vm;

import java.util.HashMap;
import java.util.Map;

public class LPCObject {
  private String name;
  private Map<String, Object> fields;
  private Map<String, Closure> methods;

  //LPCObject(String)
  public LPCObject(String name) {
    this.name = name;

    fields = new HashMap<>();
    methods = new HashMap<>();
  }

  //name()
  public String name() {
    return name;
  }

  //fields()
  public Map<String, Object> fields() {
    return fields;
  }

  //methods()
  public Map<String, Closure> methods() {
    return methods;
  }

  //inheritFunctions(Map<String, Closure>)
  public void inheritFunctions(Map<String, Closure> methods) {
    this.methods = new HashMap<String, Closure>(methods);
  }

  //toString()
  @Override
  public String toString() {
    return name;
  }
}
