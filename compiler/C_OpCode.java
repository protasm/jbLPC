package jbLPC.compiler;

public final class C_OpCode {
  public static final byte OP_ADD           = 0x00; //simple
  public static final byte OP_CALL          = 0x01; //operand (arg count)
  public static final byte OP_CLOSE_UPVALUE = 0x02; //simple
  public static final byte OP_CLOSURE       = 0x03; //closure
  public static final byte OP_COMPILE       = 0x04; //const (object path)
  public static final byte OP_CONST         = 0x05; //const (constant)
  public static final byte OP_DIVIDE        = 0x06; //simple
  public static final byte OP_EQUAL         = 0x07; //simple
  public static final byte OP_FALSE         = 0x08; //simple
  public static final byte OP_FIELD         = 0x09; //const (field name)
  public static final byte OP_GET_GLOBAL    = 0x0A; //const (global name)
  public static final byte OP_GET_LOCAL     = 0x0B; //operand (stack offset)
  public static final byte OP_GET_PROP      = 0x0C; //const (prop name)
  public static final byte OP_GET_SUPER     = 0x0D; //const
  public static final byte OP_GET_UPVALUE   = 0x0E; //operand
  public static final byte OP_GLOBAL        = 0x0F; //simple
  public static final byte OP_GREATER       = 0x10; //simple
  public static final byte OP_INHERIT       = 0x11; //simple
  public static final byte OP_INVOKE        = 0x12; //invoke
  public static final byte OP_JUMP          = 0x13; //jump
  public static final byte OP_JUMP_IF_FALSE = 0x14; //jump
  public static final byte OP_LESS          = 0x15; //simple
  public static final byte OP_LOOP          = 0x16; //jump
  public static final byte OP_METHOD        = 0x17; //const (method name)
  public static final byte OP_MULTIPLY      = 0x18; //simple
  public static final byte OP_NEGATE        = 0x19; //simple
  public static final byte OP_NIL           = 0x1A; //simple
  public static final byte OP_NOT           = 0x1B; //simple
  public static final byte OP_OBJECT        = 0x1C; //const (object name)
  public static final byte OP_POP           = 0x1D; //simple
  public static final byte OP_RETURN        = 0x1E; //simple
  public static final byte OP_SET_GLOBAL    = 0x1F; //const (global name)
  public static final byte OP_SET_LOCAL     = 0x20; //operand (stack offset)
  public static final byte OP_SET_PROP      = 0x21; //const (prop name)
  public static final byte OP_SET_UPVALUE   = 0x22; //operand
  public static final byte OP_SUBTRACT      = 0x23; //simple
  public static final byte OP_SUPER_INVOKE  = 0x24; //invoke
  public static final byte OP_TRUE          = 0x25; //simple

  //OpCode()
  private OpCode() {}
}

/*
  public static enum C_OpCodeType {
	  TYPE_CLOSURE,
	  TYPE_CONST,
	  TYPE_INVOKE,
	  TYPE_JUMP,
	  TYPE_OPERAND,
	  TYPE_SIMPLE,
  }
*/
