package org.cell2d;

import java.util.Objects;
import org.cell2d.celick.Graphics;
import org.cell2d.celick.Image;
import org.cell2d.celick.SlickException;

/**
 * <p>A ColorFilter is a Filter that blends the RGB value of each pixel in the
 * original image with that of a single Color that it uses. The alpha value of
 * the ColorFilter's Color is proportional to the strength of its influence on
 * the filtered image's RGB values. An alpha value of 0 has no effect, and an
 * alpha value of 1 completely replaces the original image's RGB values, thus
 * turning the filtered image into a colored silhouette. The alpha values of the
 * original image's pixels are left unchanged in the filtered image.</p>
 * @see Color
 * @author Alex Heyman
 */
public class ColorFilter implements Filter {
    
    private final Color color;
    
    /**
     * Constructs a ColorFilter that uses the specified Color.
     * @param color The used Color
     */
    public ColorFilter(Color color) {
        this.color = color;
    }
    
    @Override
    public final int hashCode() {
        return Objects.hash("ColorFilter", color);
    }
    
    /**
     * Returns whether the specified object is a ColorFilter that is equal to
     * this ColorFilter. Two ColorFilters are equal if and only if the Colors
     * they use are equal.
     * @param obj The object to be compared with this ColorFilter
     * @return Whether the specified object is a ColorFilter that is equal to
     * this ColorFilter
     */
    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof ColorFilter) {
            return color.equals(((ColorFilter)obj).color);
        }
        return false;
    }
    
    /**
     * Returns the Color that this ColorFilter uses.
     * @return The Color that this ColorFilter uses
     */
    public final Color getColor() {
        return color;
    }
    
    @Override
    public final Image getFilteredImage(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();
        Image newImage;
        Graphics newGraphics;
        try {
            newImage = image.getBlankCopy();
            newGraphics = newImage.getGraphics();
        } catch (SlickException e) {
            throw new RuntimeException(e);
        }
        newGraphics.setDrawMode(Graphics.MODE_SCREEN);
        float blendAlpha = color.getA();
        if (blendAlpha == 1) {
            float newR = color.getR();
            float newG = color.getG();
            float newB = color.getB();
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    Color pixelColor = image.getColor(x, y);
                    pixelColor = new Color(newR, newG, newB, pixelColor.getA());
                    newGraphics.setColor(pixelColor);
                    newGraphics.fillRect(x, y, 1, 1);
                }
            }
        } else {
            float blendR = color.getR()*blendAlpha;
            float blendG = color.getG()*blendAlpha;
            float blendB = color.getB()*blendAlpha;
            float remainder = 1 - blendAlpha;
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    Color pixelColor = image.getColor(x, y);
                    pixelColor = new Color(
                            blendR + pixelColor.getR()*remainder, blendG + pixelColor.getG()*remainder,
                            blendB + pixelColor.getB()*remainder, pixelColor.getA());
                    newGraphics.setColor(pixelColor);
                    newGraphics.fillRect(x, y, 1, 1);
                }
            }
        }
        newGraphics.flush();
        return newImage;
    }
    
}
