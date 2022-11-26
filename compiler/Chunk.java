package jbLPC.compiler;

import java.util.ArrayList;
import java.util.List;

public class Chunk {
  private List<Byte> codes;
  private List<Object> constants; //values known at compile time
  private List<Integer> lines;
  private List<String> separators;

  //Chunk()
  public Chunk() {
    codes = new ArrayList<>();
    constants = new ArrayList<>();
    lines = new ArrayList<>();
    separators = new ArrayList<>();
  }

  //codes()
  public List<Byte> codes() {
    return codes;
  }

  //constants()
  public List<Object> constants() {
    return constants;
  }

  //lines()
  public List<Integer> lines() {
    return lines;
  }

  //insertByte(int, byte)
  public void insertByte(int index, byte b) {
    codes.add(index, b);
    lines.add(lines.get(index));
    separators.add(index, ", ");
  }

  //writeByte(byte, int)
  public void writeByte(byte b, int line) {
    codes.add(b);
    lines.add(line);
    separators.add(", ");
  }

  //writeWord(byte, byte, int)
  public void writeWord(byte b1, byte b2, int line) {
    codes.add(b1);
    lines.add(line);
    separators.add("-");

    codes.add(b2);
    lines.add(line);
    separators.add(", ");
  }

  //printCodes()
  public String printCodes() {
    StringBuilder sb = new StringBuilder();

    sb.append("[");

    for (int i = 0; i < codes.size(); i++) {
      sb.append(String.format("%02X", codes.get(i)));

      if (i < codes.size() - 1)
        sb.append(separators.get(i));
    }

    sb.append("]");

    return sb.toString();
  }
}
