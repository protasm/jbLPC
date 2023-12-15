package jbLPC.compiler;

public final class C_OpCode {
  public static final byte OP_ADD           = 0x00; //simple
  public static final byte OP_ARRAY         = 0x01; //operand (element count)
  public static final byte OP_CALL          = 0x02; //operand (arg count)
  public static final byte OP_CLOSE_UPVAL   = 0x03; //simple
  public static final byte OP_CLOSURE       = 0x04; //closure
  public static final byte OP_COMPILE       = 0x05; //const (object path)
  public static final byte OP_CONSTANT      = 0x06; //const (constant)
  public static final byte OP_DIVIDE        = 0x07; //simple
  public static final byte OP_EQUAL         = 0x08; //simple
  public static final byte OP_FALSE         = 0x09; //simple
  public static final byte OP_FIELD         = 0x0A; //const (field name)
  public static final byte OP_GET_GLOBAL    = 0x0B; //const (global name)
  public static final byte OP_GET_ITEM      = 0x0C; //simple
  public static final byte OP_GET_LOCAL     = 0x0D; //operand (stack offset)
  public static final byte OP_GET_PROP      = 0x0E; //const (prop name)
  public static final byte OP_GET_SUPER     = 0x0F; //const
  public static final byte OP_GET_UPVAL     = 0x10; //operand
  public static final byte OP_DEF_GLOBAL    = 0x11; //simple
  public static final byte OP_GREATER       = 0x12; //simple
  public static final byte OP_INHERIT       = 0x13; //simple
  public static final byte OP_INVOKE        = 0x14; //invoke
  public static final byte OP_JUMP          = 0x15; //jump
  public static final byte OP_JUMP_IF_FALSE = 0x16; //jump
  public static final byte OP_LESS          = 0x17; //simple
  public static final byte OP_LOOP          = 0x18; //jump
  public static final byte OP_MAPPING       = 0x19; //operand (element count)
  public static final byte OP_METHOD        = 0x1A; //const (method name)
  public static final byte OP_MULTIPLY      = 0x1B; //simple
  public static final byte OP_NEGATE        = 0x1C; //simple
  public static final byte OP_NIL           = 0x1D; //simple
  public static final byte OP_NOT           = 0x1E; //simple
  public static final byte OP_OBJECT        = 0x1F; //const (object name)
  public static final byte OP_POP           = 0x20; //simple
  public static final byte OP_RETURN        = 0x21; //simple
  public static final byte OP_SET_GLOBAL    = 0x22; //const (global name)
  public static final byte OP_SET_LOCAL     = 0x23; //operand (stack offset)
  public static final byte OP_SET_PROP      = 0x24; //const (prop name)
  public static final byte OP_SET_UPVAL     = 0x25; //operand
  public static final byte OP_SUBTRACT      = 0x26; //simple
  public static final byte OP_SUPER_INVOKE  = 0x27; //invoke
  public static final byte OP_TRUE          = 0x28; //simple

  //C_OpCode()
  private C_OpCode() {}
}
