package jbLPC.compiler;

//public final class OpCode {
// public static final byte  OP_LOAD_NEW_COMPILATION = 0x28;
// public static final byte  OP_INHERIT = 0x22;
// public static final byte  OP_LOAD_NEW_OBJECT = 0x27;
// public static final byte  OP_ADD_NEW_FIELD = 0x25;
// public static final byte  OP_ADD_NEW_METHOD = 0x26;
// public static final byte  OP_POP_AND_ADD_AS_NEW_GLOBAL = 0x07;
// public static final byte  OP_LOAD_NIL = 0x01;
// public static final byte  OP_LOAD_TRUE = 0x02;
// public static final byte  OP_LOAD_FALSE = 0x03;
// public static final byte  OP_LOAD_CONST = 0x00;
// public static final byte  OP_LOAD_LOCAL = 0x05;
// public static final byte  OP_LOAD_UPVAL = 0x0A;
// public static final byte  OP_LOAD_FIELD_OR_METHOD = 0x0C;
// public static final byte  OP_LOAD_GLOBAL = 0x08;
// public static final byte  OP_LOAD_SUPER = 0x0E;
// public static final byte  OP_SET_LOCAL = 0x06;
// public static final byte  OP_SET_UPVAL = 0x0B;
// public static final byte  OP_SET_AND_PUSH_FIELD = 0x0D;
// public static final byte  OP_SET_GLOBAL = 0x09;
// public static final byte  OP_CALL_WITH_ARGS = 0x1C;
// public static final byte  OP_INVOKE = 0x1D;
// public static final byte  OP_SUPER_INVOKE = 0x1E;
// public static final byte  OP_LOAD_NEW_CLOSURE = 0x1F;
// public static final byte  OP_CLOSE_AND_POP_UPVAL = 0x20;
// public static final byte  OP_RETURN = 0x21;
// public static final byte  OP_POP = 0x04;
// public static final byte  OP_EQUAL = 0x0F;
// public static final byte  OP_GREATER = 0x10;
// public static final byte  OP_LESS = 0x11;
// public static final byte  OP_ADD = 0x12;
// public static final byte  OP_SUBTRACT = 0x13;
// public static final byte  OP_MULTIPLY = 0x14;
// public static final byte  OP_DIVIDE = 0x15;
// public static final byte  OP_NOT = 0x16;
// public static final byte  OP_POP_AND_LOAD_NEGATIVE = 0x17;
// public static final byte  OP_JUMP = 0x19;
// public static final byte  OP_JUMP_IF_FALSE = 0x1A;
// public static final byte  OP_LOOP = 0x1B;
//
// //OpCode()
// private OpCode() {}
//}

public enum OpCode {
  OP_COMPILE (0, "Compile '' and push it on the vStack."),
  OP_INHERIT (1, "x"),
  OP_OBJECT (2, "x"),
  OP_FIELD (3, "x"),
  OP_METHOD (4, "x"),
  OP_GLOBAL (5, "x"),
  OP_NIL (6, "x"),
  OP_TRUE (7, "x"),
  OP_FALSE (8, "x"),
  OP_CONST (9, "x"),
  OP_GET_LOCAL (10, "x"),
  OP_GET_UPVAL (11, "x"),
  OP_GET_PROP (12, "x"),
  OP_GET_GLOBAL (13, "x"),
  OP_GET_SUPER (14, "x"),
  OP_SET_LOCAL (15, "x"),
  OP_SET_UPVAL (16, "x"),
  OP_SET_PROP (17, "x"),
  OP_SET_GLOBAL (18, "x"),
  OP_CALL (19, "x"),
  OP_INVOKE (20, "x"),
  OP_SUPER_INVOKE (21, "x"),
  OP_CLOSURE (22, "x"),
  OP_CLOSE_UPVAL (23, "x"),
  OP_RETURN (24, "x"),
  OP_POP (25, "x"),
  OP_EQUAL (26, "x"),
  OP_GREATER (27, "x"),
  OP_LESS (28, "x"),
  OP_ADD (29, "x"),
  OP_SUBTRACT (30, "x"),
  OP_MULTIPLY (31, "x"),
  OP_DIVIDE (32, "x"),
  OP_NOT (33, "x"),
  OP_NEGATE (34, "x"),
  OP_JUMP (35, "x"),
  OP_JUMP_IF_FALSE (36, "x"),
  OP_LOOP (37, "x");
 
  private final int code;
  private final String explanation;
  
  OpCode(int code, String explanation) {
    this.code = code;
    this.explanation = explanation;
  }

  public int code() { return code; }
  public String explanation() { return explanation; }
}