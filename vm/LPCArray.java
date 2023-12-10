package jbLPC.vm;

import java.util.ArrayList;
import java.util.List;

public class LPCArray {
  private List<Object> array;
  
  //LPCArray()
  public LPCArray() {
    array = new ArrayList<>();
  }

  //LPCArray(List<Object>)
  public LPCArray(List<Object> array) {
    this.array = array;
  }

  //get(int)
  public Object get(int index) {
    return array.get(index);
  }

  //size()
  public int size() {
    return array.size();
  }
  
  //toString()
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("({");

    for (int i = 0; i < array.size(); i++) {
      sb.append(array.get(i));
      
      if (i < array.size() - 1) {
        sb.append(", ");
      }
    }

    return sb.toString() + "})";
  }
}