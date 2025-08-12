package vn.hoidanit.jobhunter.config.codec;

import io.lettuce.core.codec.RedisCodec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class StringByteArrayCodec implements RedisCodec<String, byte[]> {

    @Override
    public String decodeKey(ByteBuffer bytes) {
        byte[] b = new byte[bytes.remaining()];
        bytes.get(b);
        return new String(b, StandardCharsets.UTF_8);
    }

    @Override
    public byte[] decodeValue(ByteBuffer bytes) {
        byte[] b = new byte[bytes.remaining()];
        bytes.get(b);
        return b;
    }

    @Override
    public ByteBuffer encodeKey(String key) {
        return StandardCharsets.UTF_8.encode(key);
    }

    @Override
    public ByteBuffer encodeValue(byte[] value) {
        return ByteBuffer.wrap(value);
    }
}
