package jbLPC.preprocessor.fs;

import java.io.File;
import java.io.IOException;

import jbLPC.preprocessor.source.FileLexerSource;
import jbLPC.preprocessor.source.Source;

/**
 * A virtual filesystem implementation using java.io.
 */
public class JavaFileSystem implements VirtualFileSystem {
  @Override
  public VirtualFile getFile(String path) {
    return new JavaFile(path);
  }

  @Override
  public VirtualFile getFile(String dir, String name) {
    return new JavaFile(dir, name);
  }

  private class JavaFile extends File implements VirtualFile {
    private static final long serialVersionUID = 1L;

	public JavaFile(String path) {
      super(path);
    }

    public JavaFile(String dir, String name) {
      super(dir, name);
    }

    /* private */
    public JavaFile(File dir, String name) {
      super(dir, name);
    }

    /*
     @Override
     public String getPath() {
     return getCanonicalPath();
     }
     */

    @Override
    public JavaFile getParentFile() {
      String parent = getParent();

      if (parent != null)
        return new JavaFile(parent);

      File absolute = getAbsoluteFile();
      parent = absolute.getParent();

      /*
       if (parent == null)
       return null;
       */

      return new JavaFile(parent);
    }

    @Override
    public JavaFile getChildFile(String name) {
      return new JavaFile(this, name);
    }

    @SuppressWarnings("deprecation")
	@Override
    public Source getSource() throws IOException {
      return new FileLexerSource(this);
    }
  } //class JavaFile
}
