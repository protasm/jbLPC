package jbLPC.preprocessor.fs;

/**
 * An extremely lightweight virtual file system interface.
 */
public interface VirtualFileSystem {
  public VirtualFile getFile(String path);
  public VirtualFile getFile(String dir, String name);
}
