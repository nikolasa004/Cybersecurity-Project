import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RandomPositionGenerator {

    public List<Integer> generatePositions(
            BufferedImage image,
            String password
    ) {
        int totalPositions =
                (int) CapacityCalculator.getTotalRgbLsbBits(image);

        List<Integer> positions =
                new ArrayList<>(totalPositions);

        for (int i = 0; i < totalPositions; i++) {
            positions.add(i);
        }

        Collections.shuffle(
                positions,
                new Random(createSeed(password))
        );

        return positions;
    }

    private long createSeed(String password) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            password.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return ByteBuffer.wrap(hash)
                    .getLong();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create seed.",
                    e
            );
        }
    }
}