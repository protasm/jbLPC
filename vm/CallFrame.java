package jbLPC.vm;

public class CallFrame {
  private Closure closure;
  private int base; //index of bottom-most vStack value in this frame
  private int ip; //index of next Chunk code to execute

  //CallFrame(Closure, int)
  CallFrame(Closure closure, int base) {
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

  //getAndIncrementIP()
  public int getAndIncrementIP() {
    return ip++;
  }

  //ip()
  public int ip() {
    return ip;
  }

  //incrementIP()
  public void incrementIP() {
    ip++;
  }

  //setIP(int)
  public void setIP(int ip) {
    this.ip = ip;
  }

  //toString()
  @Override
  public String toString() {
    return "@Frame: " + closure.compilation().toString() + "@";
  }
}
