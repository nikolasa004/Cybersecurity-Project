import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class MessageConverter {

    private MessageConverter() {
    }

    public static byte[] stringToBytes(String message) {
        return message.getBytes(StandardCharsets.UTF_8);
    }

    public static String bytesToString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static int[] intToBits(int value) {
        int[] bits = new int[32];
        for (int i = 31; i >= 0; i--) {
            bits[31 - i] = (value >> i) & 1;
        }
        return bits;
    }

    public static int bitsToInt(int[] bits) {
        if (bits.length != 32) {
            throw new IllegalArgumentException("Exactly 32 bits are required.");
        }

        int value = 0;
        for (int bit : bits) {
            value = (value << 1) | bit;
        }
        return value;
    }

    public static int[] bytesToBits(byte[] data) {
        int[] bits = new int[data.length * 8];
        int index = 0;

        for (byte b : data) {
            for (int i = 7; i >= 0; i--) {
                bits[index++] = (b >> i) & 1;
            }
        }

        return bits;
    }

    public static byte[] bitsToBytes(int[] bits) {
        if (bits.length % 8 != 0) {
            throw new IllegalArgumentException("Number of bits must be divisible by 8.");
        }

        byte[] result = new byte[bits.length / 8];
        int bitIndex = 0;

        for (int i = 0; i < result.length; i++) {
            int value = 0;
            for (int j = 0; j < 8; j++) {
                value = (value << 1) | bits[bitIndex++];
            }
            result[i] = (byte) value;
        }

        return result;
    }

    public static byte[] intToBytes(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    public static int bytesToInt(byte[] bytes) {
        if (bytes.length != 4) {
            throw new IllegalArgumentException("Exactly 4 bytes are required.");
        }
        return ByteBuffer.wrap(bytes).getInt();
    }
}