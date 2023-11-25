package jbLPC.util;

import java.util.prefs.Preferences;

public class Prefs {
  private static Prefs _instance;
  private static Preferences prefs;

  private Prefs() {
    prefs = Preferences.systemNodeForPackage(jbLPC.JBLPC.class);
  }

  public static Prefs instance() {
    if (_instance == null) {
	  synchronized(Prefs.class) {
	    if (_instance == null)
	      _instance = new Prefs();
	    }
    }
	    
    return _instance;
  }

  public boolean getBoolean(String pref) {
    return prefs.getBoolean("master", true) && prefs.getBoolean(pref, true);
  }
  
  public void putBoolean(String pref, boolean value) {
    prefs.putBoolean(pref, value);
  }
  
  public String getString(String pref) {
    return prefs.get(pref, null);
  }
  
//  public void putString(String pref, String value) {
//    prefs.put(pref, value);
//  }
}
