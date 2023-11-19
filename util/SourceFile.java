package jbLPC.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class SourceFile {
  private File file;

  //SourceFile(String)
  public SourceFile(String path) {
    file = new File(path);
  }

  //source()
  public String source() {
    String source = "";

    try {
      source = Files.readString(file.toPath(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return source;
  }

  //getName()
  public String getName() {
    return file.getName();
  }

  //getNameNoExt()
  public String getNameNoExt() {
    String name = getName();

    if (name.indexOf(".") > 0)
      return name.substring(0, name.lastIndexOf("."));
    else
     return name;
  }
}
