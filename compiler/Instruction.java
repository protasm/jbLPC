package jbLPC.compiler;

import java.util.Arrays;

public class Instruction {
  private OpCode opCode;
  private Object[] operands;
  private int line;
  
  //Instruction()
  public Instruction() { //necessary?
    this(null, null);
  }

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
  
  //setOpCode(OpCode)
  public void setOpCode(OpCode opCode) {
    this.opCode = opCode;
  }
  
  //operands()
  public Object[] operands() {
    return operands;
  }
  
  //addOperand(Object)
  public void addOperand(Object operand) {
	  Object[] newOperands = Arrays.copyOf(operands, operands.length + 1);
	  
	  newOperands[operands.length - 1] = operand;
	  
	  operands = newOperands;
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
