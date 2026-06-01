import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Scanner;

public class SteganographyApp {

    private final Scanner scanner = new Scanner(System.in);
    private final ImageLoader imageLoader = new ImageLoader();
    private final SequentialLsbEncoder sequentialEncoder = new SequentialLsbEncoder();
    private final SequentialLsbDecoder sequentialDecoder = new SequentialLsbDecoder();
    private final RandomLsbEncoder randomEncoder = new RandomLsbEncoder();

    private final RandomLsbDecoder randomDecoder = new RandomLsbDecoder();

    public void run() {
        boolean running = true;

        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> embedSequential();
                    case "2" -> extractSequential();
                    case "3" -> showCapacity();
                    case "4" -> embedRandom();
                    case "5" -> extractRandom();
                    case "0" -> {
                        running = false;
                        System.out.println("Exiting application.");
                    }
                    default -> System.out.println("Invalid option. Try again.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }

            System.out.println();
        }

        scanner.close();
    }

    private void printMenu() {
        System.out.println("====================================");
        System.out.println("      LSB Steganography Demo");
        System.out.println("====================================");
        System.out.println("1. Embed message using Sequential LSB");
        System.out.println("2. Extract message using Sequential LSB");
        System.out.println("3. Show image capacity");
        System.out.println("4. Embed message using Random LSB");
        System.out.println("5. Extract message using Random LSB");
        System.out.println("0. Exit");
        System.out.print("Choose an option: ");
    }

    private void embedSequential() throws Exception {
        System.out.print("Enter input PNG image path: ");
        String inputPath = scanner.nextLine().trim();

        BufferedImage inputImage = imageLoader.loadPng(inputPath);

        long maxChars = CapacityCalculator.getMaxMessageBytes(inputImage);
        System.out.println("Maximum message size for this image: " + maxChars + " bytes/characters (approx. ASCII text)");

        System.out.print("Enter message to hide: ");
        String message = scanner.nextLine();

        if (message.isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty.");
        }

        System.out.print("Enter output PNG image path: ");
        String outputPath = scanner.nextLine().trim();

        BufferedImage stegoImage = sequentialEncoder.encode(inputImage, message);
        imageLoader.savePng(stegoImage, outputPath);

        System.out.println("Message successfully embedded.");
        System.out.println("Stego image saved to: " + Path.of(outputPath).toAbsolutePath());
    }

    private void extractSequential() throws Exception {
        System.out.print("Enter stego PNG image path: ");
        String inputPath = scanner.nextLine().trim();

        BufferedImage image = imageLoader.loadPng(inputPath);
        String message = sequentialDecoder.decode(image);

        System.out.println("Extracted message:");
        System.out.println("------------------------------------");
        System.out.println(message);
        System.out.println("------------------------------------");
    }

    private void showCapacity() throws Exception {
        System.out.print("Enter PNG image path: ");
        String inputPath = scanner.nextLine().trim();

        BufferedImage image = imageLoader.loadPng(inputPath);

        int width = image.getWidth();
        int height = image.getHeight();
        long pixels = (long) width * height;
        long totalBits = CapacityCalculator.getTotalRgbLsbBits(image);
        long usableBits = CapacityCalculator.getUsableBits(image);
        long maxBytes = CapacityCalculator.getMaxMessageBytes(image);

        System.out.println("Image dimensions: " + width + " x " + height);
        System.out.println("Total pixels: " + pixels);
        System.out.println("Total LSB bits (RGB only): " + totalBits);
        System.out.println("Usable bits (after 32-bit header): " + usableBits);
        System.out.println("Maximum message bytes/characters (approx. ASCII): " + maxBytes);
    }

    private void embedRandom() throws Exception {

        System.out.print("Enter input PNG image path: ");
        String inputPath = scanner.nextLine().trim();

        BufferedImage inputImage =
                imageLoader.loadPng(inputPath);

        long maxChars =
                CapacityCalculator.getMaxMessageBytes(
                        inputImage
                );

        System.out.println(
                "Maximum message size for this image: "
                        + maxChars
                        + " bytes/characters"
        );

        System.out.print("Enter message to hide: ");
        String message = scanner.nextLine();

        if (message.isEmpty()) {
            throw new IllegalArgumentException(
                    "Message cannot be empty."
            );
        }

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        if (password.isBlank()) {
            throw new IllegalArgumentException(
                    "Password cannot be empty."
            );
        }

        System.out.print("Enter output PNG image path: ");
        String outputPath = scanner.nextLine().trim();

        BufferedImage stegoImage =
                randomEncoder.encode(
                        inputImage,
                        message,
                        password
                );

        imageLoader.savePng(
                stegoImage,
                outputPath
        );

        System.out.println(
                "Message successfully embedded."
        );

        System.out.println(
                "Stego image saved to: "
                        + Path.of(outputPath)
                        .toAbsolutePath()
        );
    }
    private void extractRandom() throws Exception {

        System.out.print(
                "Enter stego PNG image path: "
        );

        String inputPath =
                scanner.nextLine().trim();

        BufferedImage image =
                imageLoader.loadPng(inputPath);

        System.out.print("Enter password: ");
        String password =
                scanner.nextLine();

        String message =
                randomDecoder.decode(
                        image,
                        password
                );

        System.out.println(
                "Extracted message:"
        );

        System.out.println(
                "------------------------------------"
        );

        System.out.println(message);

        System.out.println(
                "------------------------------------"
        );
    }
}