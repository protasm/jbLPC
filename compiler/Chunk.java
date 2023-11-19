package jbLPC.compiler;

import java.util.ArrayList;
import java.util.List;

public class Chunk {
  private List<Byte> opCodes;
  private List<Object> constants; //values known at compile time
  private List<Integer> lines;
  private List<String> separators;

  //Chunk()
  public Chunk() {
    opCodes = new ArrayList<>();
    constants = new ArrayList<>();
    lines = new ArrayList<>();
    separators = new ArrayList<>();
  }

  //opCodes()
  public List<Byte> opCodes() {
    return opCodes;
  }

  //constants()
  public List<Object> constants() {
    return constants;
  }

  //lines()
  public List<Integer> lines() {
    return lines;
  }

  //separators()
  public List<String> separators() {
    return separators;
  }

  //insertByte(int, byte)
  public void insertByte(int index, byte b) {
    opCodes.add(index, b);
    lines.add(lines.get(index));
    separators.add(index, ", ");
  }

  //writeByte(byte)
  public void writeByte(byte b) {
    opCodes.add(b);
    lines.add(0);
    separators.add(", ");
  }

  //writeByte(byte, int)
  public void writeByte(byte b, int line) {
    opCodes.add(b);
    lines.add(line);
    separators.add(", ");
  }

  //writeWord(byte, byte)
  public void writeWord(byte b1, byte b2) {
    opCodes.add(b1);
    lines.add(0);
    separators.add("-");

    opCodes.add(b2);
    lines.add(null);
    separators.add(", ");
  }

  //writeWord(byte, byte, int)
  public void writeWord(byte b1, byte b2, int line) {
    opCodes.add(b1);
    lines.add(line);
    separators.add("-");

    opCodes.add(b2);
    lines.add(line);
    separators.add(", ");
  }

  //printCodes()
  public String printCodes() {
    StringBuilder sb = new StringBuilder();

    sb.append("[");

    for (int i = 0; i < opCodes.size(); i++) {
      sb.append(String.format("%02X", opCodes.get(i)));

      if (i < opCodes.size() - 1)
        sb.append(separators.get(i));
    }

    sb.append("]");

    return sb.toString();
  }

  //toString
  @Override
  public String toString() {
    return "<chunk>";
  }
}
