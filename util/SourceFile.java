package jbLPC.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SourceFile {
  private String source;

  //SourceFile(String)
  public SourceFile(String fileName) {
    source = "";

    try {
      byte[] bytes = Files.readAllBytes(Paths.get(fileName));

      source = new String(bytes, Charset.defaultCharset());
    } catch (IOException e) {
      System.err.println("IOException occurred: '" + fileName + "'");
    }
  }

  //source()
  public String source() {
    return source;
  }
}
