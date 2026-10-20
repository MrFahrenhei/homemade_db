package storage;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Tuple {
    public static final int TUPLE_SIZE = 64;
    public static final int PAYLOAD_SIZE = 48;

    private long id;
    private long timestamp;
    private final byte[] payload;

    public Tuple(long id, long timestamp, String payloadStr){
        this.id = id;
        this.timestamp = timestamp;
        this.payload = new byte[PAYLOAD_SIZE];

        byte[] stringBytes = payloadStr.getBytes();
        int lengthToCopy = Math.min(stringBytes.length, PAYLOAD_SIZE); // truncate if cross the limit
        System.arraycopy(stringBytes, 0, this.payload, 0, lengthToCopy);
    }

    public Tuple(){
        this.payload = new byte[PAYLOAD_SIZE];
    }

    /**
     * This won't convert to json, unload the bytes in the buffer
     */
    public void serialize(ByteBuffer buffer, int offset){
        buffer.position(offset);
        buffer.putLong(id);
        buffer.putLong(timestamp);
        buffer.put(payload);
    }

    public void deserialize(ByteBuffer buffer, int offset){
        buffer.position(offset);
        this.id = buffer.getLong();
        this.timestamp = buffer.getLong();
        buffer.get(this.payload);
    }
    public long getId(){
        return id;
    }

    public String getPayloadAsString(){
        return new String(payload, StandardCharsets.UTF_8).trim(); // trim clean
    }

}
