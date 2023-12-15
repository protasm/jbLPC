package jbLPC.vm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class LPCMapping {
  private Map<Object, Object> map;
  
  //LPCMapping()
  public LPCMapping() {
    map = new HashMap<>();
  }

  //LPCMapping(Map<Object, Object>)
  public LPCMapping(Map<Object, Object> map) {
    this.map = map;
  }

  //get(Object)
  public Object get(Object key) {
    return map.get(key);
  }

  //containsKey(Object)
  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  //toString()
  @Override
  public String toString() {
    StringBuilder result = new StringBuilder("([");
    Iterator<Map.Entry<Object, Object>> iterator = map.entrySet().iterator(); 
      
    while(iterator.hasNext()) { 
      Map.Entry<Object, Object> entry = iterator.next(); 
      
      result.append(entry.getKey() +  ":" + entry.getValue()); 
      
      if (iterator.hasNext())
        result.append(", ");
    }

    return result.toString() + "])";
  }
}