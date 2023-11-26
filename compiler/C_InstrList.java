package jbLPC.compiler;

import java.util.ArrayList;
import java.util.List;

public class C_InstrList {
  private List<Byte> instructions;
  private List<Object> constants;
  private List<Integer> lines;
  
  //C_InstrList()
  public C_InstrList() {
    instructions = new ArrayList<>();
    constants = new ArrayList<>();
    lines = new ArrayList<>();
  }
  
  //instructions()
  public List<Byte> instructions() {
    return instructions;
  }
  
  //addInstr(OpCode)
  public void addInstr(C_OpCode c_OpCode) {
    addInstr(c_OpCode.code());
  }
  
  //addInstr(Byte)
  public void addInstr(Byte instr) {
    addInstr(instr, -1);
  }

  //addInstr(OpCode, line)
  public void addInstr(C_OpCode c_OpCode, int line) {
    addInstr(c_OpCode.code(), line);
  }

  //addInstr(Byte, int)
  public void addInstr(Byte instr, int line) {
    instructions.add(instr);
    lines.add(line);
  }
  
  //constants()
  public List<Object> constants() {
    return constants;
  }

  //addConstant(Object)
  public int addConstant(Object constant) {
    constants.add(constant);
    
    return constants.size() - 1;
  }
  
  //lines()
  public List<Integer> lines() {
    return lines;
  }
}
