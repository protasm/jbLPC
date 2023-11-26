package jbLPC.vm;

import java.util.Iterator;
import java.util.ListIterator;

import jbLPC.compiler.C_Compilation;

public class RunFrame implements Iterator<Byte> {
  private C_Compilation cCompilation;
  private int base; //index of bottom-most vStack value in this frame
  Iterator<Byte> iterator;

  //RunFrame(Compilation, int)
  RunFrame(C_Compilation cCompilation, int base) {
    this.cCompilation = cCompilation;
    this.base = base;

    iterator = cCompilation.instrList().instructions().listIterator();
  }

  //cCompilation()
  public C_Compilation cCompilation() {
    return cCompilation;
  }

  //base()
  public int base() {
    return base;
  }

  //hasNext()
  public boolean hasNext() {
    return iterator.hasNext();
  }

  //next()
  public Byte next() {
    return iterator.next();
  }
  
  //hasPrevious()
  public boolean hasPrevious() {
    return ((ListIterator<Byte>)iterator).hasPrevious();
  }
  
  //previous()
  public Byte previous() {
    return ((ListIterator<Byte>)iterator).previous();
  }

  //toString()
  @Override
  public String toString() {
    return "@Frame: " + cCompilation + "@";
  }
}
