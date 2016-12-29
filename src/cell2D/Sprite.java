package cell2D;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.util.Pair;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Vector2f;

public class Sprite implements Animatable {
    
    public static final Sprite BLANK = new Sprite(true, null, null, null, null, null, null, 0, 0);
    
    private final boolean isBlank;
    boolean isLoaded;
    private final Sprite recolorOf;
    private final Filter recolorFilter;
    private final SpriteSheet spriteSheet;
    private final String path;
    private final Color transColor;
    private final Set<Filter> filters;
    private final Map<String,Pair<Image[],BufferedImage>> images = new HashMap<>();
    private Image[] defaultImages = null;
    private BufferedImage bufferedImage = null;
    private final int centerX, centerY;
    private int width = 0;
    private int height = 0;
    private int right = 0;
    private int bottom = 0;
    
    Sprite(boolean isBlank, Sprite recolorOf, Filter recolorFilter, SpriteSheet spriteSheet,
            String path, Color transColor, Set<Filter> filters, int centerX, int centerY) {
        this.isBlank = isBlank;
        isLoaded = isBlank;
        this.recolorOf = recolorOf;
        this.recolorFilter = recolorFilter;
        this.spriteSheet = spriteSheet;
        this.path = path;
        this.transColor = transColor;
        this.filters = filters;
        this.centerX = centerX;
        this.centerY = centerY;
    }
    
    final Sprite getRecolor(Filter recolorFilter) {
        return new Sprite(false, this, recolorFilter, null, null, null, filters, centerX, centerY);
    }
    
    public final boolean isLoaded() {
        return isLoaded;
    }
    
    public final boolean load() throws SlickException {
        if (!isBlank && !isLoaded) {
            isLoaded = true;
            if (spriteSheet == null) {
                Pair<Image,BufferedImage> pair;
                if (recolorOf == null) {
                    pair = CellGame.getTransparentImage(path, transColor);
                } else {
                    recolorOf.load();
                    pair = recolorFilter.getFilteredImage(recolorOf.bufferedImage);
                }
                bufferedImage = pair.getValue();
                loadFilter("", pair.getKey(), bufferedImage);
                for (Filter filter : filters) {
                    Pair<Image,BufferedImage> filteredPair = filter.getFilteredImage(bufferedImage);
                    loadFilter(filter.getName(), filteredPair.getKey(), filteredPair.getValue());
                }
            } else {
                spriteSheet.load();
            }
            return true;
        }
        return false;
    }
    
    final void loadFilter(String filterName, Image image, BufferedImage bufferedImage) {
        Image[] imageArray = new Image[4];
        imageArray[0] = image;
        imageArray[0].setCenterOfRotation(centerX, centerY);
        imageArray[1] = image.getFlippedCopy(true, false);
        imageArray[1].setCenterOfRotation(right, centerY);
        imageArray[2] = image.getFlippedCopy(false, true);
        imageArray[2].setCenterOfRotation(centerX, bottom);
        imageArray[3] = image.getFlippedCopy(true, true);
        imageArray[3].setCenterOfRotation(right, bottom);
        if (filterName.equals("")) {
            defaultImages = imageArray;
            width = image.getWidth();
            height = image.getHeight();
            right = width - centerX;
            bottom = height - centerY;
        }
        images.put(filterName, new Pair<>(imageArray, bufferedImage));
    }
    
    public final boolean unload() {
        if (!isBlank && isLoaded) {
            isLoaded = false;
            if (spriteSheet == null) {
                clear();
            } else {
                spriteSheet.tryUnload();
            }
            return true;
        }
        return false;
    }
    
    final void clear() {
        images.clear();
        defaultImages = null;
        bufferedImage = null;
        width = 0;
        height = 0;
        right = 0;
        bottom = 0;
    }
    
    @Override
    public int getLevel() {
        return 0;
    }
    
    @Override
    public int getLength() {
        return 1;
    }
    
    @Override
    public Animatable getFrame(int index) {
        return (index == 0 ? this : BLANK);
    }
    
    @Override
    public boolean framesAreCompatible(int index1, int index2) {
        return (index1 == 0 && index2 == 0);
    }
    
    @Override
    public double getFrameDuration(int index) {
        return 0;
    }
    
    public final SpriteSheet getSpriteSheet() {
        return spriteSheet;
    }
    
    public final List<String> getFilters() {
        ArrayList<String> list = new ArrayList<>(filters.size());
        for (Filter filter : filters) {
            list.add(filter.getName());
        }
        return list;
    }
    
    public final int getWidth() {
        return width;
    }
    
    public final int getHeight() {
        return height;
    }
    
    public final int getCenterX() {
        return centerX;
    }
    
    public final int getCenterY() {
        return centerY;
    }
    
    public void draw(Graphics g, int x, int y, boolean xFlip, boolean yFlip, double alpha, String filter) {
        if (isBlank || !isLoaded || alpha <= 0) {
            return;
        }
        draw(g, x, y, 0, width, 0, height, 1, xFlip, yFlip, 0, (float)alpha, filter);
    }
    
    public void draw(Graphics g, int x, int y, boolean xFlip,
            boolean yFlip, double angle, double alpha, String filter) {
        if (isBlank || !isLoaded || alpha <= 0) {
            return;
        }
        draw(g, x, y, 0, width, 0, height, 1, xFlip, yFlip, (float)angle, (float)alpha, filter);
    }
    
    public void draw(Graphics g, int x, int y, double scale,
            boolean xFlip, boolean yFlip, double alpha, String filter) {
        if (isBlank || !isLoaded || scale <= 0 || alpha <= 0) {
            return;
        }
        draw(g, x, y, 0, width, 0, height, (float)scale, xFlip, yFlip, 0, (float)alpha, filter);
    }
    
    public void draw(Graphics g, int x, int y, int left, int right, int top,
            int bottom, boolean xFlip, boolean yFlip, double alpha, String filter) {
        if (isBlank || !isLoaded || right <= left || bottom <= top || alpha <= 0) {
            return;
        }
        if (xFlip) {
            int temp = left;
            left = width - right;
            right = width - temp;
        }
        if (yFlip) {
            int temp = bottom;
            bottom = height - top;
            top = height - temp;
        }
        draw(g, x + left, y + top, left, right, top, bottom, 1, xFlip, yFlip, 0, (float)alpha, filter);
    }
    
    public void draw(Graphics g, int x, int y, int left, int right, int top, int bottom,
            boolean xFlip, boolean yFlip, double angle, double alpha, String filter) {
        if (isBlank || !isLoaded || right <= left || bottom <= top || alpha <= 0) {
            return;
        }
        if (xFlip) {
            int temp = left;
            left = width - right;
            right = width - temp;
        }
        if (yFlip) {
            int temp = bottom;
            bottom = height - top;
            top = height - temp;
        }
        Vector2f vector = new Vector2f(left, top).sub(angle);
        draw(g, x + Math.round(vector.getX()), y + Math.round(vector.getY()),
                left, right, top, bottom, 1, xFlip, yFlip, (float)angle, (float)alpha, filter);
    }
    
    public void draw(Graphics g, int x, int y, int left, int right, int top, int bottom,
            double scale, boolean xFlip, boolean yFlip, double alpha, String filter) {
        if (isBlank || !isLoaded || right <= left || bottom <= top || scale <= 0 || alpha <= 0) {
            return;
        }
        if (xFlip) {
            int temp = left;
            left = width - right;
            right = width - temp;
        }
        if (yFlip) {
            int temp = bottom;
            bottom = height - top;
            top = height - temp;
        }
        y += Math.round(top*scale);
        draw(g, (int)(x + Math.round(left*scale)), (int)(y + Math.round(top*scale)),
                left, right, top, bottom, (float)scale, xFlip, yFlip, 0, (float)alpha, filter);
    }
    
    private void draw(Graphics g, int x, int y, int left, int right, int top, int bottom,
            float scale, boolean xFlip, boolean yFlip, float angle, float alpha, String filter) {
        int index = 0;
        float xOffset, yOffset;
        if (xFlip) {
            index += 1;
            xOffset = -this.right;
        } else {
            xOffset = -centerX;
        }
        if (yFlip) {
            index += 2;
            yOffset = -this.bottom;
        } else {
            yOffset = -centerY;
        }
        x += Math.round(xOffset*scale);
        y += Math.round(yOffset*scale);
        Image[] imageArray;
        if (filter == null) {
            imageArray = defaultImages;
        } else {
            Pair<Image[],BufferedImage> pair = images.get(filter);
            if (pair == null) {
                imageArray = defaultImages;
            } else {
                imageArray = pair.getKey();
            }
        }
        imageArray[index].setRotation(-angle);
        imageArray[index].setAlpha(alpha);
        g.drawImage(imageArray[index], x, y, x + (right - left)*scale, y + (bottom - top)*scale, left, top, right, bottom);
    }
    
}