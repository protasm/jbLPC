package jbLPC.compiler;

import java.util.Arrays;

public class Instruction {
  private OpCode opCode;
  private Object[] operands;
  private int line;

  //Instruction(OpCode)
  public Instruction(OpCode opCode) {
    this(opCode, null);
  }

  //Instruction(OpCode, Object)
  public Instruction(OpCode opCode, Object operand) {
    this(opCode, new Object[] { operand });
  }
  
  //Instruction(OpCode, Object[])
  public Instruction(OpCode opCode, Object[] operands) {
    this.opCode = opCode;
    this.operands = operands;
    line = 0;
  }
  
  //opCode()
  public OpCode opCode() {
    return opCode;
  }
  
  //operands()
  public Object[] operands() {
    return operands;
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
    if (operands == null)
      return String.valueOf(opCode.code());
    else
      return opCode.code() + " " + Arrays.toString(operands);
  }
}
