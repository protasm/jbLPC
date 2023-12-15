package jbLPC.util;

import java.util.Iterator;
import java.util.List;
import java.util.Stack;


public final class ObjStack {
  private static final String COLOR_RESET = "\033[0m";
  private static final String COLOR_GREEN = "\033[32m";

  private Stack<Object> values;
  
  public ObjStack() {
    this.values = new Stack<Object>();
  }
  
  public void push(Object value) {
    this.values.push(value);
  }
  
  public Object pop() {
    return values.pop();
  }
  
  public Object popMinus(int offset) {
    return get(size() - 1  - offset);
  }
  
  public Object peek() {
    return values.peek();
  }
  
  public Object peekN(int offset) {
	if (values.size() > 0)
      return values.get(values.size() - 1 - offset);
	else
	  return null;
  }
  
  public Object get(int index) {
    return values.get(index);
  }
  
  public void set(int index, Object value) {
    this.values.set(index, value);
  }
  
  public int size() {
    return values.size();
  }
  
  public List<Object> subList(int fromIndex, int toIndex) {
    return values.subList(fromIndex, toIndex);
  }

  //toString()
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(COLOR_RESET + "[");
    Iterator<Object> iterator = this.values.iterator();
    
    while (iterator.hasNext()) {
      Object value = iterator.next();
      
      sb.append(COLOR_GREEN);
      
      if (value instanceof String) {
        sb.append("\"" + value + "\"");
      } else {
        sb.append(value);
      }

      sb.append(COLOR_RESET);

      if (iterator.hasNext())
        sb.append(" | ");
    }
    
    sb.append("]");
    
    return sb.toString();
  }
}
