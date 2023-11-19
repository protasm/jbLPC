package jbLPC.util;

public class Pair<E> {
  private E left;
  private E right;

  //Pair(E, E)
  public Pair(E left, E right) {
    this.left = left;
    this.right = right;
  }

  //left()
  public E left() {
    return left;
  }

  //right()
  public E right() {
    return right;
  }
}
