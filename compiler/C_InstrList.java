package jbLPC.compiler;

import java.util.ArrayList;
import java.util.List;

public class C_InstrList {
  private List<Byte> codes;
  private List<Object> constants;
  private List<Integer> lines;
  
  //C_InstrList()
  public C_InstrList() {
    codes = new ArrayList<>();
    constants = new ArrayList<>();
    lines = new ArrayList<>();
  }
  
  //instructions()
  public List<Byte> codes() {
    return codes;
  }
  
  //addCode(int)
  public void addCode(int code) {
    addCode((byte)code);
  }
  
  //addCode(byte)
  public void addCode(byte code) {
    addCode(code, -1);
  }
  
  //addCode(int, int)
  public void addCode(int code, int line) {
    addCode((byte)code, line);
  }

  //addCode(byte, int)
  public void addCode(byte code, int line) {
    codes.add(code);
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
