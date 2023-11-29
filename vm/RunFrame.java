package jbLPC.vm;

import jbLPC.compiler.C_Compilation;

public class RunFrame {
  private Closure closure;
  private int base; //index of bottom-most vStack value in this frame
  private int ip; //instruction pointer

  //RunFrame(Compilation, int)
  RunFrame(C_Compilation compilation, int base) {
    this(new Closure(compilation), base);
  }
  
  //RunFrame(Closure, int)
  RunFrame(Closure closure, int base) {
    this.closure = closure;
	this.base = base;
	  
	ip = 0;
  }

  //closure()
  public Closure closure() {
    return closure;
  }

  //base()
  public int base() {
    return base;
  }

  //ip()
  public int ip() {
	  return ip;
  }
  
  //nextInstr()
  public Byte nextInstr() {
    return closure.compilation().instrList().codes().get(ip++);
  }
  
  //getConstant(byte)
  public Object getConstant(byte index) {
	  return closure.compilation().instrList().constants().get(index);
  }
  
  //setIP(int)
  public void setIP(int ip) {
	  this.ip = ip;
  }
  
  //toString()
  @Override
  public String toString() {
    return "@RunFrame: " + closure + "@";
  }
}
