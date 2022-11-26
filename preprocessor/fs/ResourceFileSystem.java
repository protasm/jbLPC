package jbLPC.preprocessor.fs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import jbLPC.preprocessor.source.Source;
import jbLPC.preprocessor.source.InputLexerSource;

public class ResourceFileSystem implements VirtualFileSystem {
  private final ClassLoader loader;
  private final Charset charset;

  public ResourceFileSystem( ClassLoader loader,  Charset charset) {
    this.loader = loader;
    this.charset = charset;
  }

  @Override
  public VirtualFile getFile(String path) {
    return new ResourceFile(loader, path);
  }

  @Override
  public VirtualFile getFile(String dir, String name) {
    return getFile(dir + "/" + name);
  }

  private class ResourceFile implements VirtualFile {
    private final ClassLoader loader;
    private final String path;

    public ResourceFile(ClassLoader loader, String path) {
      this.loader = loader;
      this.path = path;
    }

    @Override
    public boolean isFile() {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getPath() {
      return path;
    }

    @Override
    public String getName() {
      return path.substring(path.lastIndexOf('/') + 1);
    }

    @Override
    public ResourceFile getParentFile() {
      int idx = path.lastIndexOf('/');

      if (idx < 1)
        return null;

      return new ResourceFile(loader, path.substring(0, idx));
    }

    @Override
    public ResourceFile getChildFile(String name) {
      return new ResourceFile(loader, path + "/" + name);
    }

    @Override
    public Source getSource() throws IOException {
      InputStream stream = loader.getResourceAsStream(path);

      return new InputLexerSource(stream, charset);
    }
  } //class ResourceFile
}
