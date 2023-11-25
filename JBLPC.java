package jbLPC;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import jbLPC.util.Prefs;
import jbLPC.vm.VM;

public class JBLPC {
  private VM vm;
  private int exitCode;
  private String exitMessage;
  private Map<String, String> debugCommands;

  //JBLPC()
  public JBLPC() {
    vm = new VM();
    debugCommands = buildDebugCommands();
  }

  //runFile(String)
  public void runFile(String path) {
    try {
      byte[] source = Files.readAllBytes(Paths.get(path));

      VM.InterpretResult result = vm.interpret(
        path,
        new String(source, Charset.defaultCharset())
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
        vm.interpret(line, line);
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
      printDebugStatus();
    else if (command.equals("lib"))
      System.out.println(vm.getLibPath());
    else if (command.equals("quit") || command.equals("q")) {
      exitCode = 0;
      exitMessage = "Goodbye.";

      continueREPL = false;
    } else if (debugCommands.containsKey(command)) {
      boolean status = Prefs.instance().getBoolean(command);

      Prefs.instance().putBoolean(command, !status); //flip

      System.out.println(command + ": " + (!(status) ? "ON" : "OFF"));
    } else
      System.out.println("Unknown REPL command: '" + command + "'");

    return continueREPL;
  }

  //shutdown(int, String)
  private void shutdown(int code, String message) {
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

  //buildDebugCommands()
  private Map<String, String> buildDebugCommands() {
    return new LinkedHashMap<>() {
      private static final long serialVersionUID = 1L;
	{
      put("master",  "Master");
      put("prog",    "Show interpret pipeline progress");
      put("source",  "Print source code being interpreted");
      put("comp",    "Print compiled Compilation in current scope");
      put("instrs",  "--Print all instructions in current scope [requires 'comp']");
      put("locals",  "--Print Locals in current scope [requires 'comp']");
      put("upvals",  "--Print Upvalues for function in current scope [requires 'comp']");
      put("exec",    "Trace VM execution");
      put("rawcode", "--Print int value of code [requires 'exec'?]");
      put("stack",   "--Print VM Values stack [requires 'exec']");
      put("globals", "--Print VM globals [requires 'exec']");
    }};
  }

  //printDebugStatus()
  private void printDebugStatus() {
    //print "master" key first
    printStatus("master", "Master");

    //loop through remaining keys
    for (String key : debugCommands.keySet()) {
      if (key == "master") continue; //already printed, skip

      printStatus(key, debugCommands.get(key));
    }
  }
  
  //printStatus(String, String)
  private void printStatus(String key, String label) {
    String paddedKey = String.format("%-10s", key);
    boolean status = Prefs.instance().getBoolean(key);
    String paddedStatus = String.format("%-5s", status ? "ON" : "OFF");
    
    System.out.println(paddedKey + ": " + paddedStatus + " " + label);
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
