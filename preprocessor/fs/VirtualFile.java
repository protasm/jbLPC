package jbLPC.preprocessor.fs;

import java.io.IOException;

import jbLPC.preprocessor.source.Source;

/**
 * An extremely lightweight virtual file interface.
 */
public interface VirtualFile {
  // public String getParent();
  public boolean isFile();
  public String getPath();
  public String getName();
  public VirtualFile getParentFile();
  public VirtualFile getChildFile(String name);
  public Source getSource() throws IOException;
}
