package network;

/**
 * The client will connect via Socket TCP sending a binary batch
 *
 * <p><b>Schema: </b>[1 byte: OpCode][8 bytes: Key][50 bytes: Payload]</p>
 *
 */
public class OpCode {
    public static final byte INSERT = 0x01;
    public static final byte SELECT = 0x02;
    public static final byte DELETE = 0x03;
    public static final byte UPDATE = 0x04;
    public static final byte SELECT_ALL = 0x05;

}
