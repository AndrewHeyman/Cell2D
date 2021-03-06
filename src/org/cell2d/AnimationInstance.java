package org.cell2d;

import org.cell2d.celick.Graphics;

/**
 * <p>An AnimationInstance is an instance of an Animation that represents a
 * point in the Animation and rates at which that point progresses through the
 * Animation. An AnimationInstance has one <i>level</i> for each dimension of
 * its Animation, numbered 0 through the number of dimensions minus 1 inclusive.
 * For example, an instance of a level 2 Animation (an Animation of Animations)
 * has two levels: level 0 for the component Animations, and level 1 for the
 * Animation of Animations.</p>
 * 
 * <p>At each of its levels, an AnimationInstance has an <i>index</i> of its
 * current position in that level of the Animation, as well as a <i>speed</i>
 * that represents how fast, in fracunits per GameState time unit, it advances
 * through that level. Each level's index and speed are both initially 0. If a
 * level's speed is negative, the AnimationInstance will cycle through the
 * Animation at that level backward. If an AnimationInstance moves forward past
 * a level's end or backward past its beginning, it will loop back to the
 * beginning or end, respectively.</p>
 * 
 * <p>When an AnimationInstance is drawn, it will appear as the Sprite in its
 * Animation that its current indices specify. The Filters that can be
 * successfully applied to that Sprite when it is drawn through the
 * AnimationInstance are the same ones as when the Sprite is drawn by itself.
 * </p>
 * 
 * <p>AnimationInstances keep track of time by being assigned to one GameState
 * each. An AnimationInstance's <i>time factor</i> represents the average number
 * of discrete time units the AnimationInstance will experience every frame
 * while assigned to an active GameState. If its own time factor is negative, as
 * it is by default, an AnimationInstance will use its assigned GameState's time
 * factor instead. If an AnimationInstance is assigned to an inactive GameState
 * or none at all, time will not pass for it. AnimationInstances assigned to the
 * active GameState update their indices at the beginning of each frame, before
 * Thinkers' timer values are updated.</p>
 * 
 * <p>All operations on an AnimationInstance return the AnimationInstance itself
 * to allow operations to be easily strung together.</p>
 * @see GameState
 * @author Alex Heyman
 */
public class AnimationInstance implements Drawable {
    
    /**
     * A blank AnimationInstance of Animation.BLANK.
     */
    public static final AnimationInstance BLANK = new AnimationInstance();
    
    private final boolean blank;
    GameState state = null;
    private long timeFactor = -1;
    private final Animation animation;
    private final int level;
    private final int[] indices;
    private final long[] indexChanges;
    private final long[] speeds;
    private Sprite currentSprite;
    
    private AnimationInstance() {
        blank = true;
        this.animation = Animation.BLANK;
        level = 1;
        indices = new int[1];
        indexChanges = new long[1];
        speeds = new long[1];
        currentSprite = Sprite.BLANK;
    }
    
    /**
     * Constructs an AnimationInstance of the specified Animation. The
     * instance's indices and speeds at each level will start at 0.
     * @param animation The animation to make the AnimationInstance of
     */
    public AnimationInstance(Animation animation) {
        blank = false;
        this.animation = animation;
        level = animation.getLevel();
        indices = new int[level];
        indexChanges = new long[level];
        speeds = new long[level];
        updateCurrentSprite();
    }
    
    private void updateCurrentSprite() {
        Animatable frame = animation;
        for (int i = indices.length - 1; i >= 0; i--) {
            frame = frame.getFrame(indices[i]);
            if (frame instanceof Sprite) {
                currentSprite = (Sprite)frame;
                return;
            }
        }
        currentSprite = Sprite.BLANK;
    }
    
    /**
     * Returns the Sprite that is specified by this AnimationInstance's current
     * indices, and that the draw() methods will draw if called now.
     * @return This AnimationInstance's current Sprite
     */
    public final Sprite getCurrentSprite() {
        return currentSprite;
    }
    
    /**
     * Returns the GameState to which this AnimationInstance is assigned, or
     * null if it is not assigned to one.
     * @return The GameState to which this AnimationInstance is assigned
     */
    public final GameState getGameState() {
        return state;
    }
    
    /**
     * Sets the GameState to which this AnimationInstance is assigned. If it is
     * set to a null GameState, this AnimationInstance will be removed from its
     * current GameState if it has one.
     * @param state The GameState to which this AnimationInstance should be
     * assigned
     * @return This AnimationInstance
     */
    public final AnimationInstance setGameState(GameState state) {
        if (this.state != null) {
            this.state.removeAnimInstance(this);
        }
        if (state != null) {
            state.addAnimInstance(this);
        }
        return this;
    }
    
    /**
     * Returns this AnimationInstance's time factor.
     * @return This AnimationInstance's time factor
     */
    public final long getTimeFactor() {
        return timeFactor;
    }
    
    /**
     * Returns this AnimationInstance's effective time factor; that is, the
     * average number of time units it experiences every frame. If it is not
     * assigned to a GameState, this will be 0.
     * @return This AnimationInstance's effective time factor
     */
    public final long getEffectiveTimeFactor() {
        return (state == null ? 0 : (timeFactor < 0 ? state.getEffectiveTimeFactor() : timeFactor));
    }
    
    /**
     * Sets this AnimationInstance's time factor to the specified value.
     * @param timeFactor The new time factor
     * @return This AnimationInstance
     */
    public final AnimationInstance setTimeFactor(long timeFactor) {
        if (!blank) {
            this.timeFactor = timeFactor;
        }
        return this;
    }
    
    /**
     * Returns the Animation that this AnimationInstance is an instance of.
     * @return The Animation that this AnimationInstance is an instance of
     */
    public final Animation getAnimation() {
        return animation;
    }
    
    /**
     * Returns this AnimationInstance's current index at the specified level.
     * @param level The level of the index to be returned
     * @return The current index at the specified level
     * @throws IndexOutOfBoundsException if the specified level is not valid for
     * this AnimationInstance
     */
    public final int getIndex(int level) {
        if (level < 0 || level >= indices.length) {
            throw new IndexOutOfBoundsException("Attempted to get an AnimationInstance's index at invalid"
                    + " level " + level);
        }
        return indices[level];
    }
    
    /**
     * Returns this AnimationInstance's current index at its highest level. If
     * this AnimationInstance has only one level, this will be its only index.
     * @return This AnimationInstance's current index at its highest level
     */
    public final int getIndex() {
        return indices[indices.length - 1];
    }
    
    private long setIndex(int level, Animatable frame, int index, boolean resetLowerIndices) {
        int length = frame.getNumFrames();
        index %= length;
        if (index < 0) {
            index += length;
        }
        if (level > 0 && (resetLowerIndices || !frame.framesAreCompatible(indices[level], index))) {
            for (int i = level - 1; i >= 0; i--) {
                indices[i] = 0;
                indexChanges[i] = 0;
            }
        }
        indices[level] = index;
        return frame.getFrameDuration(index);
    }
    
    /**
     * Sets this AnimationInstance's index at the specified level to the
     * specified value.
     * @param level The level of the index to be set
     * @param index The value to which the index will be set
     * @param resetLowerIndices If this is false and the frame at the new index
     * is compatible with the frame at the current one, the indices at lower
     * levels will not be reset to 0. Otherwise, they will be.
     * @return This AnimationInstance
     * @throws IndexOutOfBoundsException if this AnimationInstance does not have
     * the specified level, or the specified index is out of range at the
     * specified level
     */
    public final AnimationInstance setIndex(int level, int index, boolean resetLowerIndices) {
        if (level < 0 || level >= indices.length) {
            throw new IndexOutOfBoundsException("Attempted to set an AnimationInstance's index at invalid"
                    + " level " + level);
        }
        if (!blank) {
            Animatable frame = animation;
            for (int i = indices.length - 1; i > level; i--) {
                try {
                    frame = frame.getFrame(indices[i]);
                } catch (IndexOutOfBoundsException e) {
                    throw new IndexOutOfBoundsException("Attempted to set an AnimationInstance's index to a"
                            + " value (" + index + ") that is invalid at the relevant level (" + level + ")");
                }
            }
            setIndex(level, frame, index, resetLowerIndices);
            indexChanges[level] = 0;
            updateCurrentSprite();
        }
        return this;
    }
    
    /**
     * Sets this AnimationInstance's index at the specified level to the
     * specified value. All indices at lower levels will be reset to 0.
     * @param level The level of the index to be set
     * @param index The value to which the index will be set
     * @return This AnimationInstance
     * @throws IndexOutOfBoundsException if this AnimationInstance does not have
     * the specified level, or the specified index is out of range at the
     * specified level
     */
    public final AnimationInstance setIndex(int level, int index) {
        return setIndex(level, index, true);
    }
    
    /**
     * Sets this AnimationInstance's index at its highest level to the specified
     * value. If this AnimationInstance has only one level, this will be its
     * only index. Otherwise, all indices at lower levels will be reset to 0.
     * @param index The value to which the index will be set
     * @return This AnimationInstance
     * @throws IndexOutOfBoundsException if the specified index is out of range
     */
    public final AnimationInstance setIndex(int index) {
        return setIndex(indices.length - 1, index, true);
    }
    
    /**
     * Returns this AnimationInstance's speed at the specified level. If the
     * specified level is not valid, this method will return 0.
     * @param level The level of the speed to be returned
     * @return The speed at the specified level
     * @throws IndexOutOfBoundsException if the specified level is not valid for
     * this AnimationInstance
     */
    public final long getSpeed(int level) {
        if (level < 0 || level >= speeds.length) {
            throw new IndexOutOfBoundsException("Attempted to get an AnimationInstance's speed at invalid"
                    + " level " + level);
        }
        return speeds[level];
    }
    
    /**
     * Returns this AnimationInstance's speed at its highest level. If this
     * AnimationInstance has only one level, this will be its only speed.
     * @return This AnimationInstance's speed at its highest level
     */
    public final long getSpeed() {
        return speeds[speeds.length - 1];
    }
    
    /**
     * Sets this AnimationInstance's speed at the specified level to the
     * specified value.
     * @param level The level of the speed to be set
     * @param speed The value to which the speed will be set
     * @return This AnimationInstance
     * @throws IndexOutOfBoundsException if the specified level is not valid for
     * this AnimationInstance
     */
    public final AnimationInstance setSpeed(int level, long speed) {
        if (level < 0 || level >= speeds.length) {
            throw new IndexOutOfBoundsException("Attempted to set an AnimationInstance's speed at invalid"
                    + " level " + level);
        }
        if (!blank) {
            speeds[level] = speed;
        }
        return this;
    }
    
    /**
     * Sets this AnimationInstance's speed at its highest level to the specified
     * value. If this AnimationInstance has only one level, this will be its
     * only speed.
     * @param speed The value to which the speed will be set
     * @return This AnimationInstance
     */
    public final AnimationInstance setSpeed(long speed) {
        if (!blank) {
            speeds[indices.length - 1] = speed;
        }
        return this;
    }
    
    final void update() {
        if (blank) {
            return;
        }
        long timeToRun = getEffectiveTimeFactor();
        if (timeToRun == 0) {
            return;
        }
        boolean spriteChanged = false;
        Animatable frame = animation;
        for (int i = indices.length - 1; i >= 0; i--) {
            if (speeds[i] != 0) {
                long duration = frame.getFrameDuration(indices[i]);
                if (duration > 0) {
                    indexChanges[i] += Frac.mul(timeToRun, speeds[i]);
                    if (speeds[i] > 0) {
                        while (indexChanges[i] >= duration) {
                            spriteChanged = true;
                            indexChanges[i] -= duration;
                            duration = setIndex(i, frame, indices[i] + 1, false);
                            if (duration <= 0) {
                                indexChanges[i] = 0;
                                break;
                            }
                        }
                    } else {
                        while (indexChanges[i] < 0) {
                            spriteChanged = true;
                            duration = setIndex(i, frame, indices[i] - 1, false);
                            if (duration <= 0) {
                                indexChanges[i] = 0;
                                break;
                            }
                            indexChanges[i] -= duration;
                        }
                    }
                }
            }
            frame = frame.getFrame(indices[i]);
        }
        if (spriteChanged) {
            updateCurrentSprite();
        }
    }
    
    @Override
    public final void draw(Graphics g, int x, int y) {
        currentSprite.draw(g, x, y);
    }
    
    @Override
    public final void draw(Graphics g, int x, int y,
            boolean xFlip, boolean yFlip, double angle, double alpha, Filter filter) {
        currentSprite.draw(g, x, y, xFlip, yFlip, angle, alpha, filter);
    }
    
    @Override
    public final void draw(Graphics g, int x, int y, double scale,
            boolean xFlip, boolean yFlip, double alpha, Filter filter) {
        currentSprite.draw(g, x, y, scale, xFlip, yFlip, alpha, filter);
    }
    
    @Override
    public final void draw(Graphics g, int x, int y, int left, int right, int top, int bottom) {
        currentSprite.draw(g, x, y, left, right, top, bottom);
    }
    
    @Override
    public final void draw(Graphics g, int x, int y, int left, int right, int top, int bottom,
            boolean xFlip, boolean yFlip, double angle, double alpha, Filter filter) {
        currentSprite.draw(g, x, y, left, right, top, bottom, xFlip, yFlip, angle, alpha, filter);
    }
    
    @Override
    public final void draw(Graphics g, int x, int y, int left, int right, int top, int bottom,
            double scale, boolean xFlip, boolean yFlip, double alpha, Filter filter) {
        currentSprite.draw(g, x, y, left, right, top, bottom, scale, xFlip, yFlip, alpha, filter);
    }
    
}
