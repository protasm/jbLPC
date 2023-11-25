package jbLPC.compiler;

public class Instruction {
  private OpCode opCode;
  private Object[] constants;
  private int line;

  //Instruction(OpCode)
  public Instruction(OpCode opCode) {
    this(opCode, new Object[] {});
  }

  //Instruction(OpCode, Object)
  public Instruction(OpCode opCode, Object constant) {
    this(opCode, new Object[] { constant });
  }
  
  //Instruction(OpCode, Object[])
  public Instruction(OpCode opCode, Object[] constants) {
    this.opCode = opCode;
    this.constants = constants;
    line = 0;
  }
  
  //opCode()
  public OpCode opCode() {
    return opCode;
  }
  
  //constants()
  public Object[] constants() {
    return constants;
  }
  
  //line()
  public int line() {
    return line;
  }
  
  //setLine(int)
  public void setLine(int line) {
    this.line = line;
  }

  //toString()
  @Override
  public String toString() {
    if (constants.length == 0)
      return opCode.name();
    else
      return opCode + " " + constants;
  }
}
