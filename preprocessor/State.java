package jbLPC.preprocessor;

public class State {
  boolean parent;
  boolean active;
  boolean sawElse;

  State() {
    this.parent = true;
    this.active = true;
    this.sawElse = false;
  }

  State(State parent) {
    this.parent = parent.isParentActive() && parent.isActive();
    this.active = true;
    this.sawElse = false;
  }

  /* Required for #elif */
  void setParentActive(boolean b) {
    this.parent = b;
  }

  boolean isParentActive() {
    return parent;
  }

  void setActive(boolean b) {
    this.active = b;
  }

  boolean isActive() {
    return active;
  }

  void setSawElse() {
    sawElse = true;
  }

  boolean sawElse() {
    return sawElse;
  }

  @Override
  public String toString() {
    return "parent=" + parent
      + ", active=" + active
      + ", sawelse=" + sawElse;
  }
}
