package jbLPC;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.SortedSet;

import jbLPC.util.Pair;
import jbLPC.util.Props;
import jbLPC.vm.VM;

public class JBLPC {
  private VM vm;
  private int exitCode;
  private String exitMessage;
  private Map<String, Pair<String>> replCommands;

  //JBLPC()
  public JBLPC() {
    String propsFile = System.getProperty("user.home") + "/eclipse-workspace/jbLPC/src/jbLPC/props";

    Props.instance().open(propsFile);

    vm = new VM();
    replCommands = buildREPLCommands();
  }

  //runFile(String)
  public void runFile(String path) {
    try {
      byte[] source = Files.readAllBytes(Paths.get(path));

      VM.InterpretResult result = vm.interpret(
        null,
        new String(source, Charset.defaultCharset()),
        false
      );

      if (result == VM.InterpretResult.INTERPRET_COMPILE_ERROR)
        shutdown(65, null);

      if (result == VM.InterpretResult.INTERPRET_RUNTIME_ERROR)
        shutdown(70, null);
    } catch (FileNotFoundException f) {
      shutdown(1, "File not found: " + path);
    } catch (IOException i) {
      shutdown(1, "IOException occurred.");
    }
  }

  //repl()
  public void repl() {
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    for (;;) {
      System.out.print("> ");

      try {
        String line = reader.readLine();

        if (line == null) {
          exitCode = 1;
          exitMessage = "Unknown error: null input line.";

          break;
        }

       //treat line prefixed with ':' as a REPL command
       if (line.length() > 0 && line.charAt(0) == ':') {
         if (handleREPLCommand(line.substring(1)))
           continue;
         else
           break;
       }

        //send line to VM for interpreting
        vm.interpret(null, line, false);
      } catch (IOException e) {
        exitCode = 1;
        exitMessage = "IOException reading line.";

        break;
      }
    }

    try {
      input.close();
      reader.close();
    } catch (IOException e) {
      System.err.println(e);
    }

    shutdown(exitCode, exitMessage);
  }

  //handleREPLCommand(String)
  private boolean handleREPLCommand(String command) {
    boolean continueREPL = true; //continue by default

    if (command.equals("debug"))
      printDebugProperties();
    else if (command.equals("lib"))
      System.out.println(vm.getLibPath());
    else if (command.equals("quit") || command.equals("q")) {
      exitCode = 0;
      exitMessage = "Goodbye.";

      continueREPL = false;
    } else if (replCommands.containsKey(command)) {
      Pair<String> pair = replCommands.get(command);
      String newStatus = Props.instance().toggleBool(pair.left()) ? "ON" : "OFF";

      System.out.println(command + ": " + newStatus);
    } else
      System.out.println("Unknown REPL command: '" + command + "'");

    return continueREPL;
  }

  //shutdown(int, String)
  private void shutdown(int code, String message) {
    Props.instance().close();

    if (code == 0) {
      if (message != null)
        System.out.println(message);

        System.exit(code);
    } else {
      if (message != null)
        System.err.println(message);

      System.exit(code);
    }
  }

  //buildREPLCommands()
  private Map<String, Pair<String>> buildREPLCommands() {
    return new HashMap<>() {
      private static final long serialVersionUID = 1L;
	{
      put("master",  new Pair<String>("DEBUG_MASTER",  "Master"));
      put("stack",   new Pair<String>("DEBUG_STACK",   "Print VM Values stack [requires 'exec']"));
      put("exec",    new Pair<String>("DEBUG_EXEC",    "Trace VM execution"));
      put("prog",    new Pair<String>("DEBUG_PROG",    "Show interpret pipeline progress"));
      put("consts",  new Pair<String>("DEBUG_CONSTS",  "Print constants for Chunk in current scope [requires 'comp']"));
      put("globals", new Pair<String>("DEBUG_GLOBALS", "Print VM globals [requires 'exec']"));
      put("locals",  new Pair<String>("DEBUG_LOCALS",  "Print Locals in current scope [requires 'comp']"));
      put("upvals",  new Pair<String>("DEBUG_UPVALS",  "Print Upvalues for function in current scope [requires 'comp']"));
      put("source",  new Pair<String>("DEBUG_SOURCE",  "Print source code being interpreted"));
      put("opcode",  new Pair<String>("DEBUG_OPCODE",  "Prefix hex values of OpCodes"));
      put("comp",    new Pair<String>("DEBUG_COMP",    "Print compiled Function in current scope"));
      put("codes",   new Pair<String>("DEBUG_CODES",   "Print codes for Chunk in current scope [requires 'comp']"));
      put("lines",   new Pair<String>("DEBUG_LINES",   "Print source lines for Chunk codes in current scope [requires 'comp']"));
    }};
  }

  //printDebugProperties()
  private void printDebugProperties() {
    //print "master" key first
    String paddedKey = String.format("%-" + 10 + "s", "master");
    String status = Props.instance().getStatus("DEBUG_MASTER");
    String paddedStatus = String.format("%-" + 3 + "s", status);
    System.out.println(paddedKey + ": " + paddedStatus + " Master");

    SortedSet<String> keys = new TreeSet<>(replCommands.keySet());

    //loop through remaining keys, sorting alphabetically
    for (String key : keys) {
      if (key == "master") continue; //already printed, skip

      Pair<String> pair = replCommands.get(key);
      paddedKey = String.format("%-" + 10 + "s", key); 
      status = Props.instance().getStatus(pair.left());
      paddedStatus = String.format("%-" + 3 + "s", status); 

      System.out.println(paddedKey + ": " + paddedStatus + " " + pair.right());
    } //for (String key : keys)
  }

  //main(String[])
  public static void main(String[] args) throws IOException {
    JBLPC jbLPC = new JBLPC();

    if (args.length > 1)
      jbLPC.shutdown(64, "Usage: jbLPC [script]");
    else if (args.length == 1)
      jbLPC.runFile(args[0]);
    else
      jbLPC.repl();
  }
}
