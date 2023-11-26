package jbLPC.compiler;

public enum C_OpCode {
  OP_ADD           ((byte)0,  C_OpCodeType.TYPE_SIMPLE,  "x"),
  OP_CALL          ((byte)1,  C_OpCodeType.TYPE_OPERAND, "x"), //arg count
  OP_CLOSE_UPVAL   ((byte)2,  C_OpCodeType.TYPE_SIMPLE,  "x"),
  OP_CLOSURE       ((byte)3,  C_OpCodeType.TYPE_CLOSURE, "x"),
  OP_COMPILE       ((byte)4,  C_OpCodeType.TYPE_CONST,   "x"), //object path
  OP_CONST         ((byte)5,  C_OpCodeType.TYPE_CONST,   "x"), //constant
  OP_DIVIDE        ((byte)6,  C_OpCodeType.TYPE_SIMPLE,  "x"),
  OP_EQUAL         ((byte)7,  C_OpCodeType.TYPE_SIMPLE,  "x"),
  OP_FALSE         ((byte)8,  C_OpCodeType.TYPE_SIMPLE,  "x"),
  OP_FIELD         ((byte)9,  C_OpCodeType.TYPE_CONST,   "x"), //field name
  OP_GET_GLOBAL    ((byte)10, C_OpCodeType.TYPE_CONST,   "x"), //global name
  OP_GET_LOCAL     ((byte)11, C_OpCodeType.TYPE_OPERAND, "x"), //stack offset
  OP_GET_PROP      ((byte)12, C_OpCodeType.TYPE_CONST,   "x"), //property name
  OP_GET_SUPER     ((byte)13, C_OpCodeType.TYPE_CONST,   "x"),
  OP_GET_UPVAL     ((byte)14, C_OpCodeType.TYPE_OPERAND, "x"),
  OP_GLOBAL        ((byte)15, C_OpCodeType.TYPE_SIMPLE,  "x"),
  OP_GREATER       ((byte)16, C_OpCodeType.TYPE_SIMPLE,  "x"),
  OP_INHERIT       ((byte)17, C_OpCodeType.TYPE_SIMPLE,  "x"),
  OP_INVOKE        ((byte)18, C_OpCodeType.TYPE_INVOKE,  "x"),
  OP_JUMP          ((byte)19, C_OpCodeType.TYPE_JUMP,    "x"),
  OP_JUMP_IF_FALSE ((byte)20, C_OpCodeType.TYPE_JUMP,    "x"),
  OP_LESS          ((byte)21, C_OpCodeType.TYPE_SIMPLE,  "x"),
  OP_LOOP          ((byte)22, C_OpCodeType.TYPE_JUMP,    "x"),
  OP_METHOD        ((byte)23, C_OpCodeType.TYPE_CONST,   "x"), //method name
  OP_MULTIPLY      ((byte)24, C_OpCodeType.TYPE_SIMPLE,  "x"),
  OP_NEGATE        ((byte)25, C_OpCodeType.TYPE_SIMPLE,  "x"),
  OP_NIL           ((byte)26, C_OpCodeType.TYPE_SIMPLE,  "x"),
  OP_NOT           ((byte)27, C_OpCodeType.TYPE_SIMPLE,  "x"),
  OP_OBJECT        ((byte)28, C_OpCodeType.TYPE_CONST,   "x"), //object name
  OP_POP           ((byte)29, C_OpCodeType.TYPE_SIMPLE,  "x"),
  OP_RETURN        ((byte)30, C_OpCodeType.TYPE_SIMPLE,  "x"),
  OP_SET_GLOBAL    ((byte)31, C_OpCodeType.TYPE_CONST,   "x"), //global name
  OP_SET_LOCAL     ((byte)32, C_OpCodeType.TYPE_OPERAND, "x"), //stack offset
  OP_SET_PROP      ((byte)33, C_OpCodeType.TYPE_CONST,   "x"), //property name
  OP_SET_UPVAL     ((byte)34, C_OpCodeType.TYPE_OPERAND, "x"),
  OP_SUBTRACT      ((byte)35, C_OpCodeType.TYPE_SIMPLE,  "x"),
  OP_SUPER_INVOKE  ((byte)36, C_OpCodeType.TYPE_INVOKE,  "x"),
  OP_TRUE          ((byte)37, C_OpCodeType.TYPE_SIMPLE,  "x");
 
  public static enum C_OpCodeType {
	  TYPE_CLOSURE,
	  TYPE_CONST,
	  TYPE_INVOKE,
	  TYPE_JUMP,
	  TYPE_OPERAND,
	  TYPE_SIMPLE,
  }

  private final byte code;
  private final C_OpCodeType type;
  private final String hint;
  
  C_OpCode(byte code, C_OpCodeType type, String hint) {
    this.code = code;
    this.type = type;
    this.hint = hint;
  }

  public byte code() { return code; }
  public C_OpCodeType type() { return type; }
  public String hint() { return hint; }
}