import java.awt.image.BufferedImage;

public class SequentialLsbEncoder {

    public BufferedImage encode(BufferedImage original, String message) {
        byte[] messageBytes = MessageConverter.stringToBytes(message);
        CapacityCalculator.ensureMessageFits(original, messageBytes);

        int[] headerBits = MessageConverter.intToBits(messageBytes.length);
        int[] messageBits = MessageConverter.bytesToBits(messageBytes);

        BufferedImage copy = deepCopy(original);

        int totalBitsToWrite = headerBits.length + messageBits.length;
        int writtenBits = 0;

        int headerIndex = 0;
        int messageIndex = 0;

        outer:
        for (int y = 0; y < copy.getHeight(); y++) {
            for (int x = 0; x < copy.getWidth(); x++) {
                int argb = copy.getRGB(x, y);

                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;

                int[] channels = {r, g, b};

                for (int i = 0; i < 3; i++) {
                    if (writtenBits >= totalBitsToWrite) {
                        break outer;
                    }

                    int bit;
                    if (headerIndex < headerBits.length) {
                        bit = headerBits[headerIndex++];
                    } else {
                        bit = messageBits[messageIndex++];
                    }

                    channels[i] = (channels[i] & 0xFE) | bit;
                    writtenBits++;
                }

                int newArgb =
                        (a << 24) |
                        (channels[0] << 16) |
                        (channels[1] << 8) |
                        channels[2];

                copy.setRGB(x, y, newArgb);
            }
        }

        return copy;
    }

    private BufferedImage deepCopy(BufferedImage original) {
        BufferedImage copy = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                copy.setRGB(x, y, original.getRGB(x, y));
            }
        }

        return copy;
    }
}