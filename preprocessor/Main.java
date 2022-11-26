package jbLPC.preprocessor;

import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import jbLPC.preprocessor.source.FileLexerSource;
import jbLPC.preprocessor.source.InputLexerSource;
import jbLPC.preprocessor.source.Source;
import jbLPC.preprocessor.source.StringLexerSource;

/**
 * (Currently a simple test class).
 */
public class Main {
  private static CharSequence getWarnings() {
    StringBuilder buf = new StringBuilder();

    for (Warning w : Warning.values()) {
      if (buf.length() > 0)
        buf.append(", ");

      String name = w.name().toLowerCase();

      buf.append(name.replace('_', '-'));
    }

    return buf;
  }

  public static void main(String[] args) throws Exception {
    (new Main()).run(args);
  }

  public void run(String[] args) throws Exception {
    OptionParser parser = new OptionParser();

    OptionSpec<?> helpOption = parser.accepts("help",
      "Displays command-line help.")
      .forHelp();

    OptionSpec<?> debugOption = parser.acceptsAll(Arrays.asList("debug"),
      "Enables debug output.");

    OptionSpec<String> defineOption = parser.acceptsAll(Arrays.asList("define", "D"),
      "Defines the given macro.")
      .withRequiredArg().ofType(String.class).describedAs("name[=definition]");

    OptionSpec<String> undefineOption = parser.acceptsAll(Arrays.asList("undefine", "U"),
      "Undefines the given macro, previously either builtin or defined using -D.")
      .withRequiredArg().describedAs("name");

    OptionSpec<File> includeOption = parser.accepts("include",
      "Process file as if \"#" + "include \"file\"\" appeared as the first line of the primary source file.")
      .withRequiredArg().ofType(File.class).describedAs("file");

    OptionSpec<File> incdirOption = parser.acceptsAll(Arrays.asList("incdir", "I"),
      "Adds the directory dir to the list of directories to be searched for header files.")
      .withRequiredArg().ofType(File.class).describedAs("dir");

    OptionSpec<File> iquoteOption = parser.acceptsAll(Arrays.asList("iquote"),
      "Adds the directory dir to the list of directories to be searched for header files included using \"\".")
      .withRequiredArg().ofType(File.class).describedAs("dir");

    OptionSpec<String> warningOption = parser.acceptsAll(Arrays.asList("warning", "W"),
      "Enables the named warning class (" + getWarnings() + ").")
      .withRequiredArg().ofType(String.class).describedAs("warning");

    OptionSpec<Void> noWarningOption = parser.acceptsAll(Arrays.asList("no-warnings", "w"),
      "Disables ALL warnings.");

    OptionSpec<File> inputsOption = parser.nonOptions()
      .ofType(File.class).describedAs("Files to process.");

    OptionSet options = parser.parse(args);

    if (options.has(helpOption)) {
      parser.printHelpOn(System.out);

      return;
    }

    Preprocessor pp = new Preprocessor();

    pp.addFeature(Feature.DIGRAPHS);
    pp.addFeature(Feature.TRIGRAPHS);
    //pp.addFeature(Feature.LINEMARKERS);
    pp.addWarning(Warning.IMPORT);
    pp.setListener(new DefaultPreprocessorListener());
    pp.addMacro("__JCPP__");
    pp.getSystemIncludePath().add(".");

    if (options.has(debugOption))
      pp.addFeature(Feature.DEBUG);

    if (options.has(noWarningOption))
      pp.getWarnings().clear();

    for (String warning : options.valuesOf(warningOption)) {
      warning = warning.toUpperCase();
      warning = warning.replace('-', '_');

      if (warning.equals("ALL"))
        pp.addWarnings(EnumSet.allOf(Warning.class));
      else
        pp.addWarning(Enum.valueOf(Warning.class, warning));
    }

    for (String arg : options.valuesOf(defineOption)) {
      int idx = arg.indexOf('=');

      if (idx == -1)
        pp.addMacro(arg);
      else
        pp.addMacro(arg.substring(0, idx), arg.substring(idx + 1));
    }

    for (String arg : options.valuesOf(undefineOption)) {
      pp.getMacros().remove(arg);
    }

    for (File dir : options.valuesOf(incdirOption))
      pp.getSystemIncludePath().add(dir.getAbsolutePath());

    for (File dir : options.valuesOf(iquoteOption))
      pp.getQuoteIncludePath().add(dir.getAbsolutePath());

    for (File file : options.valuesOf(includeOption))
      // Comply exactly with spec.
      pp.addInput(new StringLexerSource("#" + "include \"" + file + "\"\n"));

    List<File> inputs = options.valuesOf(inputsOption);

    if (inputs.isEmpty()) {
      pp.addInput(new InputLexerSource(System.in));
    } else {
      for (File input : inputs)
        pp.addInput(new FileLexerSource(input));
    }

    if (pp.getFeature(Feature.DEBUG)) {
      System.out.println("#" + "include \"...\" search starts here:");

      for (String dir : pp.getQuoteIncludePath())
        System.out.println("  " + dir);

      System.out.println("#" + "include <...> search starts here:");

      for (String dir : pp.getSystemIncludePath())
        System.out.println("  " + dir);

      System.out.println("End of search list.");
    }

    try {
      for (;;) {
        Token tok = pp.token();

        if (tok == null || tok.getType() == Token.EOF)
          break;

        System.out.print(tok.getText());
      }
    } catch (Exception e) {
      StringBuilder buf = new StringBuilder("Preprocessor failed:\n");
      Source s = pp.getSource();

      while (s != null) {
        buf.append(" -> ").append(s).append("\n");

        s = s.getParent();
      }

      System.err.println(buf.toString());
      System.err.println(e);
    }
  }
}
