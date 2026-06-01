import java.awt.image.BufferedImage;
import java.util.List;

public class RandomLsbDecoder {

    private final RandomPositionGenerator generator =
            new RandomPositionGenerator();

    public String decode(
            BufferedImage image,
            String password
    ) {

        List<Integer> positions =
                generator.generatePositions(
                        image,
                        password
                );

        int positionIndex = 0;

        int[] headerBits =
                new int[32];

        for (int i = 0; i < 32; i++) {

            headerBits[i] =
                    readBit(
                            image,
                            positions.get(positionIndex++)
                    );
        }

        int messageLengthBytes =
                MessageConverter.bitsToInt(
                        headerBits
                );

        if (messageLengthBytes < 0) {
            throw new IllegalArgumentException(
                    "Invalid embedded message length."
            );
        }

        long requiredBits =
                32L +
                        ((long) messageLengthBytes * 8L);

        if (
                requiredBits >
                        CapacityCalculator.getTotalRgbLsbBits(
                                image
                        )
        ) {
            throw new IllegalArgumentException(
                    "Wrong password or image does not contain a valid message."
            );
        }

        int[] messageBits =
                new int[
                        messageLengthBytes * 8
                        ];

        for (
                int i = 0;
                i < messageBits.length;
                i++
        ) {

            messageBits[i] =
                    readBit(
                            image,
                            positions.get(positionIndex++)
                    );
        }

        byte[] messageBytes =
                MessageConverter.bitsToBytes(
                        messageBits
                );

        return MessageConverter.bytesToString(
                messageBytes
        );
    }

    private int readBit(
            BufferedImage image,
            int position
    ) {

        int width =
                image.getWidth();

        int pixelIndex =
                position / 3;

        int channel =
                position % 3;

        int x =
                pixelIndex % width;

        int y =
                pixelIndex / width;

        int argb =
                image.getRGB(x, y);

        int r =
                (argb >> 16) & 0xFF;

        int g =
                (argb >> 8) & 0xFF;

        int b =
                argb & 0xFF;

        return switch (channel) {

            case 0 -> r & 1;
            case 1 -> g & 1;
            default -> b & 1;
        };
    }
}