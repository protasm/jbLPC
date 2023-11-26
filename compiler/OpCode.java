package jbLPC.compiler;

public enum OpCode {
  OP_ADD           ((byte)0,  OpCodeType.TYPE_SIMPLE,  "x"),
  OP_CALL          ((byte)1,  OpCodeType.TYPE_OPERAND, "x"), //arg count
  OP_CLOSE_UPVAL   ((byte)2,  OpCodeType.TYPE_SIMPLE,  "x"),
  OP_CLOSURE       ((byte)3,  OpCodeType.TYPE_CLOSURE, "x"),
  OP_COMPILE       ((byte)4,  OpCodeType.TYPE_CONST,   "x"), //object path
  OP_CONST         ((byte)5,  OpCodeType.TYPE_CONST,   "x"), //constant
  OP_DIVIDE        ((byte)6,  OpCodeType.TYPE_SIMPLE,  "x"),
  OP_EQUAL         ((byte)7,  OpCodeType.TYPE_SIMPLE,  "x"),
  OP_FALSE         ((byte)8,  OpCodeType.TYPE_SIMPLE,  "x"),
  OP_FIELD         ((byte)9,  OpCodeType.TYPE_CONST,   "x"), //field name
  OP_GET_GLOBAL    ((byte)10, OpCodeType.TYPE_CONST,   "x"), //global name
  OP_GET_LOCAL     ((byte)11, OpCodeType.TYPE_OPERAND, "x"), //stack offset
  OP_GET_PROP      ((byte)12, OpCodeType.TYPE_CONST,   "x"), //property name
  OP_GET_SUPER     ((byte)13, OpCodeType.TYPE_CONST,   "x"),
  OP_GET_UPVAL     ((byte)14, OpCodeType.TYPE_OPERAND, "x"),
  OP_GLOBAL        ((byte)15, OpCodeType.TYPE_SIMPLE,  "x"),
  OP_GREATER       ((byte)16, OpCodeType.TYPE_SIMPLE,  "x"),
  OP_INHERIT       ((byte)17, OpCodeType.TYPE_SIMPLE,  "x"),
  OP_INVOKE        ((byte)18, OpCodeType.TYPE_INVOKE,  "x"),
  OP_JUMP          ((byte)19, OpCodeType.TYPE_JUMP,    "x"),
  OP_JUMP_IF_FALSE ((byte)20, OpCodeType.TYPE_JUMP,    "x"),
  OP_LESS          ((byte)21, OpCodeType.TYPE_SIMPLE,  "x"),
  OP_LOOP          ((byte)22, OpCodeType.TYPE_JUMP,    "x"),
  OP_METHOD        ((byte)23, OpCodeType.TYPE_CONST,   "x"), //method name
  OP_MULTIPLY      ((byte)24, OpCodeType.TYPE_SIMPLE,  "x"),
  OP_NEGATE        ((byte)25, OpCodeType.TYPE_SIMPLE,  "x"),
  OP_NIL           ((byte)26, OpCodeType.TYPE_SIMPLE,  "x"),
  OP_NOT           ((byte)27, OpCodeType.TYPE_SIMPLE,  "x"),
  OP_OBJECT        ((byte)28, OpCodeType.TYPE_CONST,   "x"), //object name
  OP_POP           ((byte)29, OpCodeType.TYPE_SIMPLE,  "x"),
  OP_RETURN        ((byte)30, OpCodeType.TYPE_SIMPLE,  "x"),
  OP_SET_GLOBAL    ((byte)31, OpCodeType.TYPE_CONST,   "x"), //global name
  OP_SET_LOCAL     ((byte)32, OpCodeType.TYPE_OPERAND, "x"), //stack offset
  OP_SET_PROP      ((byte)33, OpCodeType.TYPE_CONST,   "x"), //property name
  OP_SET_UPVAL     ((byte)34, OpCodeType.TYPE_OPERAND, "x"),
  OP_SUBTRACT      ((byte)35, OpCodeType.TYPE_SIMPLE,  "x"),
  OP_SUPER_INVOKE  ((byte)36, OpCodeType.TYPE_INVOKE,  "x"),
  OP_TRUE          ((byte)37, OpCodeType.TYPE_SIMPLE,  "x");
 
  public static enum OpCodeType {
	TYPE_CLOSURE,
	TYPE_CONST,
	TYPE_INVOKE,
	TYPE_JUMP,
	TYPE_OPERAND,
	TYPE_SIMPLE,
  }

  private final byte code;
  private final OpCodeType type;
  private final String hint;
  
  OpCode(byte code, OpCodeType type, String hint) {
    this.code = code;
    this.type = type;
    this.hint = hint;
  }

  public byte code() { return code; }
  public OpCodeType type() { return type; }
  public String hint() { return hint; }
}