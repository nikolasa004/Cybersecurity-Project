import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ImageLoader {

    public BufferedImage loadPng(String path) throws Exception {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Image path cannot be empty.");
        }

        File file = new File(path);

        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + path);
        }

        BufferedImage image = ImageIO.read(file);

        if (image == null) {
            throw new IllegalArgumentException("Failed to read image. Make sure it is a valid PNG file.");
        }

        String lower = file.getName().toLowerCase();
        if (!lower.endsWith(".png")) {
            throw new IllegalArgumentException("Only PNG files are supported in this demo.");
        }

        return image;
    }

    public void savePng(BufferedImage image, String path) throws Exception {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Output path cannot be empty.");
        }

        File file = new File(path);

        String lower = file.getName().toLowerCase();
        if (!lower.endsWith(".png")) {
            throw new IllegalArgumentException("Output file must have .png extension.");
        }

        boolean success = ImageIO.write(image, "png", file);
        if (!success) {
            throw new IllegalStateException("Failed to save PNG image.");
        }
    }
}