package jbLPC.vm;

import java.util.Iterator;
import java.util.ListIterator;

import jbLPC.compiler.Compilation;
import jbLPC.compiler.Instruction;

public class RunFrame implements Iterator<Instruction> {
  private Compilation compilation;
  private int base; //index of bottom-most vStack value in this frame
//  private int ip; //index of next instruction to execute
  ListIterator<Instruction> iterator;

  //RunFrame(Compilation, int)
  RunFrame(Compilation compilation, int base) {
    this.compilation = compilation;
    this.base = base;

//    ip = 0;
    iterator = compilation.instructions().listIterator();
  }

  //compilation()
  public Compilation compilation() {
    return compilation;
  }

  //base()
  public int base() {
    return base;
  }

  //getAndIncrementIP()
//  public int getAndIncrementIP() {
//    return ip++;
//  }

  //ip()
//  public int ip() {
//    return ip;
//  }

  //incrementIP()
//  public void incrementIP() {
//    ip++;
//  }

  //setIP(int)
//  public void setIP(int ip) {
//    this.ip = ip;
//  }

  //hasNext()
  public boolean hasNext() {
    return iterator.hasNext();
  }

  //next()
  public Instruction next() {
    return iterator.next();
  }
  
  //hasPrevious()
  public boolean hasPrevious() {
    return iterator.hasPrevious();
  }
  
  //previous()
  public Instruction previous() {
    return iterator.previous();
  }

  //remove()
//  @Override
//  public void remove() {
//    throw new UnsupportedOperationException();
//  }

  //toString()
  @Override
  public String toString() {
    return "@Frame: " + compilation + "@";
  }
}
