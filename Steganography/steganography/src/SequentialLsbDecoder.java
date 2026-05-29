import java.awt.image.BufferedImage;

public class SequentialLsbDecoder {

    public String decode(BufferedImage image) {
        int totalBits = (int) CapacityCalculator.getTotalRgbLsbBits(image);
        if (totalBits < 32) {
            throw new IllegalArgumentException("Image is too small to contain even the message header.");
        }

        int[] allBits = extractAllBits(image);

        int[] headerBits = new int[32];
        System.arraycopy(allBits, 0, headerBits, 0, 32);

        int messageLengthBytes = MessageConverter.bitsToInt(headerBits);

        if (messageLengthBytes < 0) {
            throw new IllegalArgumentException("Invalid embedded message length.");
        }

        long requiredBits = 32L + (long) messageLengthBytes * 8L;
        if (requiredBits > totalBits) {
            throw new IllegalArgumentException("Embedded message length is invalid or image does not contain a valid message.");
        }

        int[] messageBits = new int[messageLengthBytes * 8];
        System.arraycopy(allBits, 32, messageBits, 0, messageBits.length);

        byte[] messageBytes = MessageConverter.bitsToBytes(messageBits);
        return MessageConverter.bytesToString(messageBytes);
    }

    private int[] extractAllBits(BufferedImage image) {
        int[] bits = new int[(int) CapacityCalculator.getTotalRgbLsbBits(image)];
        int index = 0;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);

                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;

                bits[index++] = r & 1;
                bits[index++] = g & 1;
                bits[index++] = b & 1;
            }
        }

        return bits;
    }
}