package jbLPC.vm;

import java.util.HashMap;
import java.util.Map;

public class LPCObject {
  private String name;
  private LPCObject superObj;
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

  //superObj()
  public LPCObject superObj() {
    return superObj;
  }

  //inherit(LPCObject)
  public void inherit(LPCObject superObj) {
    this.superObj = superObj;
    this.fields = superObj.fields();
    this.methods = superObj.methods();
  }

  //toString()
  @Override
  public String toString() {
    String str = "<obj: " + name;

    if (superObj != null)
      str = str + " [" + superObj.name() + "]";

    str = str + ">";

    return str;
  }
}
