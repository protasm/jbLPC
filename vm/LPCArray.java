package jbLPC.vm;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

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
  
  //set(int, Object)
  public void set(int index, Object value) {
    array.set(index, value);
  }

  //size()
  public int size() {
    return array.size();
  }
  
  //toString()
  @Override
  public String toString() {
	StringBuilder result = new StringBuilder("({");
    ListIterator<Object> iterator = array.listIterator();

    while (iterator.hasNext()) {
      result.append(iterator.next());
      
      if (iterator.hasNext())
        result.append(", ");
    }

      return result.toString() + "})";
    }
}