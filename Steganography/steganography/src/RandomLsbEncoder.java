import java.awt.image.BufferedImage;
import java.util.List;

public class RandomLsbEncoder {

    private final RandomPositionGenerator generator =
            new RandomPositionGenerator();

    public BufferedImage encode(
            BufferedImage original,
            String message,
            String password
    ) {

        byte[] messageBytes =
                MessageConverter.stringToBytes(message);

        CapacityCalculator.ensureMessageFits(
                original,
                messageBytes
        );

        int[] headerBits =
                MessageConverter.intToBits(
                        messageBytes.length
                );

        int[] messageBits =
                MessageConverter.bytesToBits(
                        messageBytes
                );

        BufferedImage copy =
                deepCopy(original);

        List<Integer> positions =
                generator.generatePositions(
                        copy,
                        password
                );

        int bitIndex = 0;

        for (int bit : headerBits) {
            writeBit(
                    copy,
                    positions.get(bitIndex++),
                    bit
            );
        }

        for (int bit : messageBits) {
            writeBit(
                    copy,
                    positions.get(bitIndex++),
                    bit
            );
        }

        return copy;
    }

    private void writeBit(
            BufferedImage image,
            int position,
            int bit
    ) {

        int width = image.getWidth();

        int pixelIndex = position / 3;
        int channel = position % 3;

        int x = pixelIndex % width;
        int y = pixelIndex / width;

        int argb =
                image.getRGB(x, y);

        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        switch (channel) {

            case 0 ->
                    r = (r & 0xFE) | bit;

            case 1 ->
                    g = (g & 0xFE) | bit;

            case 2 ->
                    b = (b & 0xFE) | bit;
        }

        int newArgb =
                (a << 24)
                        | (r << 16)
                        | (g << 8)
                        | b;

        image.setRGB(
                x,
                y,
                newArgb
        );
    }

    private BufferedImage deepCopy(
            BufferedImage original
    ) {

        BufferedImage copy =
                new BufferedImage(
                        original.getWidth(),
                        original.getHeight(),
                        BufferedImage.TYPE_INT_ARGB
                );

        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {

                copy.setRGB(
                        x,
                        y,
                        original.getRGB(x, y)
                );
            }
        }

        return copy;
    }
}