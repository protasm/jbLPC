package jbLPC.util;

import java.util.ArrayList;
import java.util.List;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public final class Props {
  private String propsFile;
  private Properties properties;
  private List<PropsObserver> observers;

  static private Props _instance; //singleton

  //Props()
  private Props() {
    observers = new ArrayList<>();
  }

  //instance()
  public static Props instance() {
    if (_instance == null) {
      //critical section
      synchronized(Props.class) {
        if (_instance == null)
          _instance = new Props();
      }
    }

    return _instance;
  }

  //open(String)
  public void open(String propsFile) {
    this.propsFile = propsFile;

    try (InputStream input = new FileInputStream(propsFile)) {
      properties = new Properties();

      properties.load(input);

      input.close();
    } catch (IOException e) {
      System.err.println("Failed to load properties file '" + propsFile + "'.");
    }
  }

  //close()
  public void close() {
    try (OutputStream out = new FileOutputStream(propsFile)) {
      properties.store(out, "---No Comment---");

      out.close();
    } catch (IOException e) {
      System.err.println("Failed to store properties file '" + propsFile + "'.");
    }
  }

  //getStatus(String)
  public String getStatus(String key) {
    return getBool(key) ? "ON" : "OFF";
  }

  //getBool(String)
  public boolean getBool(String key) {
    String property = properties.getProperty(key);

    return Boolean.parseBoolean(property);
  }

  //toggleBool(String)
  public boolean toggleBool(String key) {
    boolean newState = !getBool(key);

    properties.setProperty(key, String.valueOf(newState));

    close();

    for (PropsObserver observer : observers)
      observer.notifyPropertiesChanged();

    return newState;
  }

  //getInt(String)
  public int getInt(String key) {
    String property = properties.getProperty(key);

    if (property == null) return -1;

    try {
      return Integer.parseInt(property);
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  //getString(String)
  public String getString(String key) {
    return properties.getProperty(key);
  }

  //registerObserver(PropsObserver)
  public void registerObserver(PropsObserver observer) {
    observers.add(observer);

    observer.notifyPropertiesChanged();
  }
}
