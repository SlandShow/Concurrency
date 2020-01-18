package Painter;

import java.awt.image.BufferedImage;

/**
 * Class, which change all white pixels to red.
 */
public class ImagePainter {

    /**
     * This method iterates over all image pixels and recolor each white pixel to red.
     *
     * @param originalImage original image
     * @param resultImage   recolored result image
     * @param leftCorner    left corner pixel
     * @param topCorner     top corner pixel
     * @param width         width of image
     * @param height        height of image
     */
    public void recolorImage(BufferedImage originalImage, BufferedImage resultImage, int leftCorner, int topCorner,
                             int width, int height) {
        for (int x = leftCorner; x < leftCorner + width && x < originalImage.getWidth(); x++) {
            for (int y = topCorner; y < topCorner + height && y < originalImage.getHeight(); y++) {
                recolorPixel(originalImage, resultImage, x, y);
            }
        }
    }

    private void recolorPixel(BufferedImage originalImage, BufferedImage resultImage, int x, int y) {
        int rgb = originalImage.getRGB(x, y);

        int red = getRed(rgb);
        int green = getGreen(rgb);
        int blue = getBlue(rgb);

        int newRed;
        int newGreen;
        int newBlue;

        if (isShadeOfGray(red, green, blue)) {
            newRed = Math.min(255, red + 10);
            newGreen = Math.max(0, green - 80);
            newBlue = Math.max(0, blue - 20);
        } else {
            newRed = red;
            newGreen = green;
            newBlue = blue;
        }
        int newRGB = createRGBFromColors(newRed, newGreen, newBlue);
        setRGB(resultImage, x, y, newRGB);
    }

    private void setRGB(BufferedImage image, int x, int y, int rgb) {
        image.getRaster().setDataElements(x, y, image.getColorModel().getDataElements(rgb, null));
    }

    private boolean isShadeOfGray(int red, int green, int blue) {
        return Math.abs(red - green) < 30 && Math.abs(red - blue) < 30 && Math.abs(green - blue) < 30;
    }

    private int createRGBFromColors(int red, int green, int blue) {
        int rgb = 0;

        rgb |= blue;
        rgb |= green << 8;
        rgb |= red << 16;

        rgb |= 0xFF000000;

        return rgb;
    }

    private int getRed(int rgb) {
        return (rgb & 0x00FF0000) >> 16;
    }

    private int getGreen(int rgb) {
        return (rgb & 0x0000FF00) >> 8;
    }

    private int getBlue(int rgb) {
        return rgb & 0x000000FF;
    }
}
