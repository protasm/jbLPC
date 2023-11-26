package jbLPC.vm;

import java.util.ListIterator;

import jbLPC.compiler.C_Compilation;

public class RunFrame implements ListIterator<Byte> {
  private C_Compilation compilation;
  private int base; //index of bottom-most vStack value in this frame
  ListIterator<Byte> iterator;

  //RunFrame(Compilation, int)
  RunFrame(C_Compilation compilation, int base) {
    this.compilation = compilation;
    this.base = base;

    iterator = compilation.instrList().instructions().listIterator();
  }

  //compilation()
  public C_Compilation compilation() {
    return compilation;
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
    return iterator.hasPrevious();
  }
  
  //previous()
  public Byte previous() {
    return iterator.previous();
  }

  //toString()
  @Override
  public String toString() {
    return "@Frame: " + compilation + "@";
  }

 
  @Override
  public int nextIndex() {
    return iterator.nextIndex();
  }

  
  @Override
  public int previousIndex() {
    return iterator.previousIndex();
  }

  
  @Override
  public void remove() {
    iterator.remove();
  }

  
  @Override
  public void set(Byte e) {
    iterator.set(e);
  }

  
  @Override
  public void add(Byte e) {
    iterator.add(e);
  }
}
