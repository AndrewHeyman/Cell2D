package org.cell2d;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.cell2d.celick.Image;
import org.cell2d.celick.SlickException;

/**
 * <p>A SpriteSheet is a rectangular grid of Sprites. Each Sprite has an
 * x-coordinate in the grid that starts at 0 for the leftmost column and
 * increases to the right, as well as a y-coordinate that starts at 0 for the
 * topmost row and increases below. Like other Loadables, SpriteSheets can be
 * manually loaded and unloaded into and out of memory. Loading may take a
 * moment, but a SpriteSheet's Sprites cannot be loaded and drawn if the
 * SpriteSheet itself is not loaded. Loading a SpriteSheet will also load all of
 * its Sprites, and loading a Sprite that is part of a SpriteSheet will also
 * load that SpriteSheet.</p>
 * 
 * <p>A SpriteSheet has a fixed set of Filters, specified upon its creation,
 * that can have an effect on its Sprites when applied to them with their draw()
 * methods. The SpriteSheet makes this possible by making a copy of its image
 * data with each Filter applied upon being loaded. Thus, the amount of memory
 * that a loaded SpriteSheet occupies is proportional to its number of
 * applicable Filters plus 1.</p>
 * @see Sprite
 * @see Filter
 * @author Alex Heyman
 */
public class SpriteSheet implements Iterable<Sprite>, Loadable {
    
    private boolean loaded = false;
    private final SpriteSheet basedOn;
    private final Filter basedFilter;
    private final String path;
    private final Color transColor;
    private Image defaultImage = null;
    private final Map<Filter,Image> filterImages;
    private final int width, height, spriteWidth, spriteHeight, spacing, margin, originX, originY;
    private final Sprite[] sprites;
    private int numSpritesLoaded = 0;
    
    /**
     * Constructs a SpriteSheet from an image file.
     * @param path The relative path to the image file
     * @param width The width in Sprites of this SpriteSheet
     * @param height The height in Sprites of this SpriteSheet
     * @param spriteWidth The width in pixels of each Sprite
     * @param spriteHeight The height in pixels of each Sprite
     * @param spacing The horizontal and vertical spacing in pixels between
     * Sprites
     * @param margin The horizontal and vertical spacing in pixels between the
     * grid of Sprites and the edges of the image
     * @param originX The x-coordinate in pixels on each Sprite of that Sprite's
     * origin
     * @param originY The y-coordinate in pixels on each Sprite of that Sprite's
     * origin
     * @param load Whether this SpriteSheet should load upon creation
     * @param filters The Filters that will have an effect on this SpriteSheet's
     * Sprites when applied to them with draw()
     */
    public SpriteSheet(String path, int width, int height, int spriteWidth, int spriteHeight,
            int spacing, int margin, int originX, int originY, boolean load, Filter... filters) {
        this(path, width, height, spriteWidth, spriteHeight, spacing, margin, originX, originY,
                null, load, filters);
    }
    
    /**
     * Constructs a SpriteSheet from an image file.
     * @param path The relative path to the image file
     * @param width The width in Sprites of this SpriteSheet
     * @param height The height in Sprites of this SpriteSheet
     * @param spriteWidth The width in pixels of each Sprite
     * @param spriteHeight The height in pixels of each Sprite
     * @param spacing The horizontal and vertical spacing in pixels between
     * Sprites
     * @param margin The horizontal and vertical spacing in pixels between the
     * grid of Sprites and the edges of the image
     * @param originX The x-coordinate in pixels on each Sprite of that Sprite's
     * origin
     * @param originY The y-coordinate in pixels on each Sprite of that Sprite's
     * origin
     * @param transColor The transparent color of this SpriteSheet's Sprites, or
     * null if there should be none
     * @param load Whether this SpriteSheet should load upon creation
     * @param filters The Filters that will have an effect on this SpriteSheet's
     * Sprites when applied to them with draw()
     */
    public SpriteSheet(String path, int width, int height,
            int spriteWidth, int spriteHeight, int spacing, int margin,
            int originX, int originY, Color transColor, boolean load, Filter... filters) {
        this(null, null, path, transColor, Arrays.asList(filters),
                width, height, spriteWidth, spriteHeight, spacing, margin, originX, originY, load);
    }
    
    /**
     * Constructs a SpriteSheet from an image file.
     * @param path The relative path to the image file
     * @param width The width in Sprites of this SpriteSheet
     * @param height The height in Sprites of this SpriteSheet
     * @param spriteWidth The width in pixels of each Sprite
     * @param spriteHeight The height in pixels of each Sprite
     * @param spacing The horizontal and vertical spacing in pixels between
     * Sprites
     * @param margin The horizontal and vertical spacing in pixels between the
     * grid of Sprites and the edges of the image
     * @param originX The x-coordinate in pixels on each Sprite of that Sprite's
     * origin
     * @param originY The y-coordinate in pixels on each Sprite of that Sprite's
     * origin
     * @param transR The red value (0-255) of this SpriteSheet's Sprites'
     * transparent color
     * @param transG The green value (0-255) of this SpriteSheet's Sprites'
     * transparent color
     * @param transB The blue value (0-255) of this SpriteSheet's Sprites'
     * transparent color
     * @param load Whether this SpriteSheet should load upon creation
     * @param filters The Filters that will have an effect on this SpriteSheet's
     * Sprites when applied to them with draw()
     */
    public SpriteSheet(String path, int width, int height,
            int spriteWidth, int spriteHeight, int spacing, int margin, int originX, int originY,
            int transR, int transG, int transB, boolean load, Filter... filters) {
        this(path, width, height, spriteWidth, spriteHeight,
                spacing, margin, originX, originY, new Color(transR, transG, transB), load, filters);
    }
    
    /**
     * Constructs a SpriteSheet from an existing SpriteSheet with a Filter
     * applied to it. The new SpriteSheet will have the same set of Filters that
     * are usable with its Sprites' draw() methods as the existing Sprite.
     * @param spriteSheet The SpriteSheet to create this SpriteSheet from
     * @param filter The Filter to apply to the existing SpriteSheet
     * @param load Whether this SpriteSheet should load upon creation
     */
    public SpriteSheet(SpriteSheet spriteSheet, Filter filter, boolean load) {
        this(spriteSheet, filter, null, null, spriteSheet.filterImages.keySet(),
                spriteSheet.width, spriteSheet.height, spriteSheet.spriteWidth, spriteSheet.spriteHeight,
                spriteSheet.spacing, spriteSheet.margin, spriteSheet.originX, spriteSheet.originY, load);
    }
    
    private SpriteSheet(SpriteSheet basedOn, Filter basedFilter, String path, Color transColor,
            Collection<Filter> filters, int width, int height, int spriteWidth, int spriteHeight,
            int spacing, int margin, int originX, int originY, boolean load) {
        if (width <= 0) {
            throw new RuntimeException("Attempted to construct a SpriteSheet with non-positive width "
                    + width);
        }
        if (height <= 0) {
            throw new RuntimeException("Attempted to construct a SpriteSheet with non-positive height "
                    + height);
        }
        if (spriteWidth <= 0) {
            throw new RuntimeException("Attempted to construct a SpriteSheet with non-positive sprite width "
                    + spriteWidth);
        }
        if (spriteHeight <= 0) {
            throw new RuntimeException("Attempted to construct a SpriteSheet with non-positive sprite"
                    + " height " + spriteHeight);
        }
        if (spacing < 0) {
            throw new RuntimeException("Attempted to construct a SpriteSheet with negative spacing "
                    + spacing);
        }
        if (margin < 0) {
            throw new RuntimeException("Attempted to construct a SpriteSheet with negative margin "
                    + margin);
        }
        this.basedOn = basedOn;
        this.basedFilter = basedFilter;
        this.path = path;
        this.transColor = transColor;
        if (filters.isEmpty()) {
            filterImages = Collections.emptyMap();
        } else {
            filterImages = new HashMap<>();
            for (Filter filter : filters) {
                filterImages.put(filter, null);
            }
        }
        this.width = width;
        this.height = height;
        this.spriteWidth = spriteWidth;
        this.spriteHeight = spriteHeight;
        this.spacing = spacing;
        this.margin = margin;
        this.originX = originX;
        this.originY = originY;
        sprites = new Sprite[width*height];
        for (int i = 0; i < sprites.length; i++) {
            sprites[i] = new Sprite(this);
        }
        if (load) {
            load();
        }
    }
    
    private class SpriteIterator implements Iterator<Sprite> {
        
        private int i = 0;
        
        @Override
        public boolean hasNext() {
            return (i < sprites.length);
        }
        
        @Override
        public Sprite next() {
            Sprite next = sprites[i];
            i++;
            return next;
        }
        
    }
    
    /**
     * Returns an Iterator over this SpriteSheet's Sprites. The Iterator does
     * not support the remove operation.
     * @return An Iterator over this SpriteSheet's Sprites
     */
    @Override
    public final Iterator<Sprite> iterator() {
        return new SpriteIterator();
    }
    
    @Override
    public final boolean isLoaded() {
        return loaded;
    }
    
    /**
     * Loads this SpriteSheet, along with all of its Sprites, if it is not
     * already loaded.
     * @return Whether the loading occurred
     */
    @Override
    public final boolean load() {
        if (!loaded) {
            loaded = true;
            Image image;
            if (path != null) {
                try {
                    image = new Image(path, false, Image.FILTER_NEAREST, transColor);
                } catch (SlickException e) {
                    throw new RuntimeException(e);
                }
            } else {
                basedOn.load();
                image = basedFilter.getFilteredImage(basedOn.filterImages.get(null));
            }
            for (Sprite sprite : sprites) {
                sprite.loaded = true;
            }
            numSpritesLoaded = sprites.length;
            loadFilter(null, image);
            defaultImage = image;
            for (Map.Entry<Filter,Image> entry : filterImages.entrySet()) {
                Filter filter = entry.getKey();
                Image filteredImage = filter.getFilteredImage(image);
                loadFilter(filter, filteredImage);
                entry.setValue(filteredImage);
            }
            return true;
        }
        return false;
    }
    
    private void loadFilter(Filter filter, Image image) {
        org.cell2d.celick.SpriteSheet spriteSheet = new org.cell2d.celick.SpriteSheet(
                image, spriteWidth, spriteHeight, spacing, margin);
        int i = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                sprites[i].loadFilter(filter, spriteSheet.getSubImage(x, y));
                i++;
            }
        }
    }
    
    /**
     * Unloads this SpriteSheet, along with all of its Sprites, if it is
     * currently loaded.
     * @return Whether the unloading occurred
     */
    @Override
    public final boolean unload() {
        if (loaded) {
            loaded = false;
            destroyAndClear();
            for (Sprite sprite : sprites) {
                sprite.loaded = false;
                sprite.clear();
            }
            numSpritesLoaded = 0;
            return true;
        }
        return false;
    }
    
    final void unloadSprite() {
        numSpritesLoaded--;
        if (numSpritesLoaded == 0) {
            loaded = false;
            destroyAndClear();
            for (Sprite sprite : sprites) {
                sprite.clear();
            }
        }
    }
    
    private void destroyAndClear() {
        try {
            defaultImage.destroy();
            defaultImage = null;
            for (Map.Entry<Filter,Image> entry : filterImages.entrySet()) {
                entry.getValue().destroy();
                entry.setValue(null);
            }
        } catch (SlickException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Returns an unmodifiable Set view of the Filters that will have an effect
     * on this SpriteSheet's Sprites when applied to them with draw().
     * @return The Set of Filters that will have an effect on this SpriteSheet's
     * Sprites when applied to them with draw()
     */
    public final Set<Filter> getFilters() {
        return Collections.unmodifiableSet(filterImages.keySet());
    }
    
    /**
     * Returns the width in Sprites of this SpriteSheet.
     * @return The width in Sprites of this SpriteSheet
     */
    public final int getWidth() {
        return width;
    }
    
    /**
     * Returns the height in Sprites of this SpriteSheet.
     * @return The height in Sprites of this SpriteSheet
     */
    public final int getHeight() {
        return height;
    }
    
    /**
     * Returns this SpriteSheet's Sprite at the specified coordinates.
     * @param x The x-coordinate in Sprites of the Sprite
     * @param y The y-coordinate in Sprites of the Sprite
     * @return The Sprite at the specified coordinates
     * @throws IndexOutOfBoundsException if the location is out of this
     * SpriteSheet's bounds
     */
    public Sprite getSprite(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IndexOutOfBoundsException("Attempted to retrieve a Sprite from a SpriteSheet at"
                    + " invalid coordinates (" + x + ", " + y + ")");
        }
        return sprites[y*width + x];
    }
    
    /**
     * Returns the width in pixels of each of this SpriteSheet's sprites.
     * @return The width of each of this SpriteSheet's sprites
     */
    public final int getSpriteWidth() {
        return spriteWidth;
    }
    
    /**
     * Returns the height in pixels of each of this SpriteSheet's sprites.
     * @return The height of each of this SpriteSheet's sprites
     */
    public final int getSpriteHeight() {
        return spriteHeight;
    }
    
    /**
     * Returns the horizontal and vertical spacing in pixels between Sprites in
     * this SpriteSheet.
     * @return The spacing between Sprites in this SpriteSheet
     */
    public final int getSpacing() {
        return spacing;
    }
    
    /**
     * Returns the horizontal and vertical spacing in pixels between this
     * SpriteSheet's grid of Sprites and the edges of its image.
     * @return The spacing between this SpriteSheet's grid of Sprites and the
     * edges of its image
     */
    public final int getMargin() {
        return margin;
    }
    
    /**
     * Returns the x-coordinate in pixels on each of this SpriteSheet's Sprites
     * of that Sprite's origin.
     * @return The x-coordinate in pixels on each of this SpriteSheet's Sprites
     * of that Sprite's origin
     */
    public final int getOriginX() {
        return originX;
    }
    
    /**
     * Returns the y-coordinate in pixels on each of this SpriteSheet's Sprites
     * of that Sprite's origin.
     * @return The y-coordinate in pixels on each of this SpriteSheet's Sprites
     * of that Sprite's origin
     */
    public final int getOriginY() {
        return originY;
    }
    
}
