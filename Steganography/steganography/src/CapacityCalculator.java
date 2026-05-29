import java.awt.image.BufferedImage;

public class CapacityCalculator {

    private static final int HEADER_BITS = 32;

    private CapacityCalculator() {
    }

    public static long getTotalRgbLsbBits(BufferedImage image) {
        return (long) image.getWidth() * image.getHeight() * 3L;
    }

    public static long getUsableBits(BufferedImage image) {
        long total = getTotalRgbLsbBits(image);
        return Math.max(0, total - HEADER_BITS);
    }

    public static long getMaxMessageBytes(BufferedImage image) {
        return getUsableBits(image) / 8L;
    }

    public static void ensureMessageFits(BufferedImage image, byte[] messageBytes) {
        long maxBytes = getMaxMessageBytes(image);
        if (messageBytes.length > maxBytes) {
            throw new IllegalArgumentException(
                    "Message is too large for this image. Maximum is " + maxBytes + " bytes, but got " + messageBytes.length + "."
            );
        }
    }
}