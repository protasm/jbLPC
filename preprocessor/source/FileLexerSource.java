package jbLPC.preprocessor.source;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * A {@link Source} which lexes a file.
 *
 * The input is buffered.
 *
 * @see Source
 */
public class FileLexerSource extends InputLexerSource {
  private final String path;
  private final File file;

  /**
   * Creates a new Source for lexing the given File.
   *
   * Preprocessor directives are honoured within the file.
   */
  public FileLexerSource(File file, Charset charset, String path)
    throws IOException {
    super(new FileInputStream(file), charset);

    this.file = file;
    this.path = path;
  }

  public FileLexerSource(File file, String path)
    throws IOException {
    this(file, Charset.defaultCharset(), path);
  }

  public FileLexerSource(File file, Charset charset)
    throws IOException {
    this(file, charset, file.getPath());
  }

  @Deprecated
  public FileLexerSource(File file)
    throws IOException {
    this(file, Charset.defaultCharset());
  }

  public FileLexerSource(String path, Charset charset)
    throws IOException {
    this(new File(path), charset, path);
  }

  @Deprecated
  public FileLexerSource(String path)
      throws IOException {
    this(path, Charset.defaultCharset());
  }

  public File getFile() {
    return file;
  }

  /**
   * This is not necessarily the same as getFile().getPath() in case we are in a chroot.
   */
  @Override
  public String getPath() {
    return path;
  }

  @Override
  public String getName() {
    return getPath();
  }

  @Override
  public String toString() {
    return "file " + getPath();
  }
}
