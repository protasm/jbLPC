package jbLPC.preprocessor.fs;

import java.io.File;
import java.io.IOException;

import jbLPC.preprocessor.source.Source;
import jbLPC.preprocessor.source.FileLexerSource;

/**
 * A virtual filesystem implementation using java.io in a virtual
 * chroot.
 */
public class ChrootFileSystem implements VirtualFileSystem {
  private File root;

  public ChrootFileSystem(File root) {
    this.root = root;
  }

  @Override
  public VirtualFile getFile(String path) {
    return new ChrootFile(path);
  }

  @Override
  public VirtualFile getFile(String dir, String name) {
    return new ChrootFile(dir, name);
  }

  private class ChrootFile extends File implements VirtualFile {
    private static final long serialVersionUID = 1L;
//	private File rfile;

    public ChrootFile(String path) {
      super(path);
    }

    public ChrootFile(String dir, String name) {
      super(dir, name);
    }

    /* private */
    public ChrootFile(File dir, String name) {
      super(dir, name);
    }

    @Override
    public ChrootFile getParentFile() {
      return new ChrootFile(getParent());
    }

    @Override
    public ChrootFile getChildFile(String name) {
      return new ChrootFile(this, name);
    }

    @Override
    public boolean isFile() {
      File real = new File(root, getPath());

      return real.isFile();
    }

    @Override
    public Source getSource() throws IOException {
      return new FileLexerSource(new File(root, getPath()),
        getPath());
    }
  } //class ChrootFile
}
