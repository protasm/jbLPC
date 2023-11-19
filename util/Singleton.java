package jbLPC.util;

public class Singleton {
  // methods and attributes for Singleton pattern
  static private Singleton _instance;

  //Singleton()
  private Singleton() {}

  //instance()
  public static Singleton instance() {
    if (_instance == null) {
      //critical section
      synchronized(Singleton.class) {
        if (_instance == null)
          _instance = new Singleton();
      }
    }

    return _instance;
  }

  // methods and attributes for global data
}
