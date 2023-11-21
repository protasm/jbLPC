package jbLPC.vm;

import java.util.HashMap;
import java.util.Map;

public class LPCObject {
  private String name;
  private Map<String, Object> fields;
  private Map<String, Closure> methods;
  private LPCObject superObj;

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

  //setSuperObj(LPCObject)
  public void setSuperObj(LPCObject superObj) {
    this.superObj = superObj;
  }

  //inheritFields(Map<String, Object>)
  //public void inheritFields(Map<String, Object> fields) {
  //  this.fields = new HashMap<String, Object>(fields);
  //}

  //inheritMethods(Map<String, Closure>)
  //public void inheritMethods(Map<String, Closure> methods) {
  //  this.methods = new HashMap<String, Closure>(methods);
  //}

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
