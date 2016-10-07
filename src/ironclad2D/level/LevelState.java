package ironclad2D.level;

import ironclad2D.IroncladGame;
import ironclad2D.IroncladGameState;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.newdawn.slick.Graphics;

public class LevelState extends IroncladGameState {
    
    private static LevelState delayState = null;
    private static final List<ObjectChangeData> objectsToChange = new LinkedList<>();
    
    private final Set<LevelThinker> levelThinkers = new HashSet<>();
    private final Set<LevelThinker> newThinkers = new HashSet<>();
    private boolean thinkersHaveReactedToLevel = true;
    private boolean thinkersHaveReactedToInput = false;
    private final Set<LevelObject> levelObjects = new HashSet<>();
    private double chunkWidth, chunkHeight;
    private final Map<Point,Chunk> chunks = new HashMap<>();
    private final SortedMap<Integer,LevelLayer> levelLayers = new TreeMap<>();
    private HUD hud = null;
    private final Map<Integer,Viewport> viewports = new HashMap<>();
    
    public LevelState(IroncladGame game, int id, double chunkWidth, double chunkHeight) {
        super(game, id);
        setChunkDimensions(chunkWidth, chunkHeight);
    }
    
    public LevelState(IroncladGame game, int id) {
        this(game, id, 256, 256);
    }
    
    private class Chunk {
        
        private final SortedMap<Integer,Set<Hitbox>> locatorHitboxes = new TreeMap<>();
        private final Set<Hitbox> overlapHitboxes = new HashSet<>();
        private final Set<Hitbox> solidHitboxes = new HashSet<>();
        
        private Chunk() {}
        
        private Set<Hitbox> getLocatorHitboxes(int drawLayer) {
            Set<Hitbox> hitboxes = locatorHitboxes.get(drawLayer);
            if (hitboxes == null) {
                hitboxes = new HashSet<>();
                locatorHitboxes.put(drawLayer, hitboxes);
            }
            return hitboxes;
        }
        
    }
    
    private Chunk getChunk(Point point) {
        Chunk chunk = chunks.get(point);
        if (chunk == null) {
            chunk = new Chunk();
            chunks.put(point, chunk);
        }
        return chunk;
    }
    
    final int[] getChunkRange(double x1, double y1, double x2, double y2) {
        int[] chunkRange = {(int)Math.ceil(x1/chunkWidth) - 1, (int)Math.ceil(y1/chunkHeight) - 1, (int)Math.floor(x2/chunkWidth), (int)Math.floor(y2/chunkHeight)};
        return chunkRange;
    }
    
    private class ChunkRangeIterator implements Iterator<Chunk> {
        
        private final int[] chunkRange;
        private int xPos, yPos;
        
        private ChunkRangeIterator(int[] chunkRange) {
            this.chunkRange = chunkRange;
            xPos = chunkRange[0];
            yPos = chunkRange[1];
        }
        
        @Override
        public boolean hasNext() {
            return yPos > chunkRange[3];
        }
        
        @Override
        public Chunk next() {
            Chunk next = getChunk(new Point(xPos, yPos));
            if (xPos == chunkRange[2]) {
                xPos = chunkRange[0];
                yPos++;
            } else {
                xPos++;
            }
            return next;
        }
        
    }
    
    private void updateChunkRange(Hitbox hitbox) {
        hitbox.chunkRange = getChunkRange(hitbox.getLeftEdge(), hitbox.getTopEdge(), hitbox.getRightEdge(), hitbox.getBottomEdge());
    }
    
    final void updateChunks(Hitbox hitbox) {
        int[] oldRange = hitbox.chunkRange;
        updateChunkRange(hitbox);
        int[] newRange = hitbox.chunkRange;
        if (oldRange == null || oldRange[0] != newRange[0] || oldRange[1] != newRange[1]
                || oldRange[2] != newRange[2] || oldRange[3] != newRange[3]) {
            int[] addRange;
            if (oldRange == null) {
                addRange = newRange;
            } else {
                int[] removeRange = oldRange;
                Iterator<Chunk> iterator = new ChunkRangeIterator(removeRange);
                while (iterator.hasNext()) {
                    Chunk chunk = iterator.next();
                    if (hitbox.roles[0]) {
                        chunk.getLocatorHitboxes(hitbox.drawLayer).remove(hitbox);
                    }
                    if (hitbox.roles[1]) {
                        chunk.overlapHitboxes.remove(hitbox);
                    }
                    if (hitbox.roles[2]) {
                        chunk.solidHitboxes.remove(hitbox);
                    }
                }
                addRange = newRange;
            }
            Iterator<Chunk> iterator = new ChunkRangeIterator(addRange);
            while (iterator.hasNext()) {
                Chunk chunk = iterator.next();
                if (hitbox.roles[0]) {
                    chunk.getLocatorHitboxes(hitbox.drawLayer).add(hitbox);
                }
                if (hitbox.roles[1]) {
                    chunk.overlapHitboxes.add(hitbox);
                }
                if (hitbox.roles[2]) {
                    chunk.solidHitboxes.add(hitbox);
                }
            }
        }
    }
    
    final void addLocatorHitbox(Hitbox hitbox) {
        if (hitbox.numChunkRoles == 0) {
            updateChunkRange(hitbox);
        }
        hitbox.numChunkRoles++;
        Iterator<Chunk> iterator = new ChunkRangeIterator(hitbox.chunkRange);
        while (iterator.hasNext()) {
            iterator.next().getLocatorHitboxes(hitbox.drawLayer).add(hitbox);
        }
    }
    
    final void removeLocatorHitbox(Hitbox hitbox) {
        Iterator<Chunk> iterator = new ChunkRangeIterator(hitbox.chunkRange);
        while (iterator.hasNext()) {
            iterator.next().getLocatorHitboxes(hitbox.drawLayer).remove(hitbox);
        }
        hitbox.numChunkRoles--;
        if (hitbox.numChunkRoles == 0) {
            hitbox.chunkRange = null;
        }
    }
    
    final void changeLocatorHitboxDrawLayer(Hitbox hitbox, int drawLayer) {
        Iterator<Chunk> iterator = new ChunkRangeIterator(hitbox.chunkRange);
        while (iterator.hasNext()) {
            Chunk chunk = iterator.next();
            chunk.getLocatorHitboxes(hitbox.drawLayer).remove(hitbox);
            chunk.getLocatorHitboxes(drawLayer).add(hitbox);
        }
    }
    
    final void addOverlapHitbox(Hitbox hitbox) {
        if (hitbox.numChunkRoles == 0) {
            updateChunkRange(hitbox);
        }
        hitbox.numChunkRoles++;
        Iterator<Chunk> iterator = new ChunkRangeIterator(hitbox.chunkRange);
        while (iterator.hasNext()) {
            iterator.next().overlapHitboxes.add(hitbox);
        }
    }
    
    final void removeOverlapHitbox(Hitbox hitbox) {
        Iterator<Chunk> iterator = new ChunkRangeIterator(hitbox.chunkRange);
        while (iterator.hasNext()) {
            iterator.next().overlapHitboxes.remove(hitbox);
        }
        hitbox.numChunkRoles--;
        if (hitbox.numChunkRoles == 0) {
            hitbox.chunkRange = null;
        }
    }
    
    final void addSolidHitbox(Hitbox hitbox) {
        if (hitbox.numChunkRoles == 0) {
            updateChunkRange(hitbox);
        }
        hitbox.numChunkRoles++;
        Iterator<Chunk> iterator = new ChunkRangeIterator(hitbox.chunkRange);
        while (iterator.hasNext()) {
            iterator.next().solidHitboxes.add(hitbox);
        }
    }
    
    final void removeSolidHitbox(Hitbox hitbox) {
        Iterator<Chunk> iterator = new ChunkRangeIterator(hitbox.chunkRange);
        while (iterator.hasNext()) {
            iterator.next().solidHitboxes.remove(hitbox);
        }
        hitbox.numChunkRoles--;
        if (hitbox.numChunkRoles == 0) {
            hitbox.chunkRange = null;
        }
    }
    
    public final double getChunkWidth() {
        return chunkWidth;
    }
    
    public final double getChunkHeight() {
        return chunkHeight;
    }
    
    public final void setChunkDimensions(double chunkWidth, double chunkHeight) {
        if (chunkWidth <= 0) {
            throw new RuntimeException("Attempted to give a level state a non-positive chunk width");
        }
        if (chunkHeight <= 0) {
            throw new RuntimeException("Attempted to give a level state a non-positive chunk height");
        }
        this.chunkWidth = chunkWidth;
        this.chunkHeight = chunkHeight;
        chunks.clear();
        if (!levelObjects.isEmpty()) {
            
        }
    }
    
    public final void loadArea(Area area) {
        
    }
    
    private class ObjectChangeData {
        
        private final LevelThinker thinker;
        private final LevelObject object;
        private final LevelState newLevelState;
        
        private ObjectChangeData(LevelThinker thinker, LevelObject object, LevelState newLevelState) {
            this.thinker = thinker;
            this.object = object;
            this.newLevelState = newLevelState;
        }
        
    }
    
    public final boolean addThinker(LevelThinker thinker) {
        if (thinker.newLevelState == null) {
            thinker.newLevelState = this;
            if (delayState != null && (delayState == this || delayState == thinker.levelState)) {
                objectsToChange.add(new ObjectChangeData(thinker, null, this));
            } else {
                addActions(thinker);
            }
            return true;
        }
        return false;
    }
    
    private void addActions(LevelThinker thinker) {
        levelThinkers.add(thinker);
        thinker.levelState = this;
        newThinkers.add(thinker);
        thinker.addTo(this);
    }
    
    public final boolean removeThinker(LevelThinker thinker) {
        if (thinker.newLevelState == this) {
            thinker.newLevelState = null;
            if (delayState != null && (delayState == this || delayState == thinker.levelState)) {
                objectsToChange.add(new ObjectChangeData(thinker, null, null));
            } else {
                removeActions(thinker);
            }
            return true;
        }
        return false;
    }
    
    private void removeActions(LevelThinker thinker) {
        thinker.remove();
        newThinkers.remove(thinker);
        levelThinkers.remove(thinker);
        thinker.levelState = null;
    }
    
    public final boolean addObject(LevelObject object) {
        if (object.newLevelState == null) {
            object.newLevelState = this;
            if (delayState != null && (delayState == this || delayState == object.levelState)) {
                objectsToChange.add(new ObjectChangeData(null, object, this));
            } else {
                addActions(object);
            }
            return true;
        }
        return false;
    }
    
    private void addActions(LevelObject object) {
        levelObjects.add(object);
        object.levelState = this;
        object.addActions();
    }
    
    public final boolean removeObject(LevelObject object) {
        if (object.newLevelState == this) {
            object.newLevelState = null;
            if (delayState != null && (delayState == this || delayState == object.levelState)) {
                objectsToChange.add(new ObjectChangeData(null, object, null));
            } else {
                removeActions(object);
            }
            return true;
        }
        return false;
    }
    
    private void removeActions(LevelObject object) {
        object.removeActions();
        levelObjects.remove(object);
        object.levelState = null;
    }
    
    private void catchNewThinkersUp(IroncladGame game) {
        if (!newThinkers.isEmpty()) {
            List<LevelThinker> thinkers = new ArrayList<>(newThinkers);
            newThinkers.clear();
            if (thinkersHaveReactedToLevel) {
                for (LevelThinker thinker : thinkers) {
                    thinker.reactToLevel(game, this);
                }
                changeObjects(game);
            }
            if (thinkersHaveReactedToInput) {
                for (LevelThinker thinker : thinkers) {
                    thinker.reactToInput(game, this);
                }
                changeObjects(game);
            }
        }
    }
    
    private void changeObjects(IroncladGame game) {
        while (!objectsToChange.isEmpty()) {
            List<ObjectChangeData> newChanges = new ArrayList<>(objectsToChange);
            objectsToChange.clear();
            for (ObjectChangeData data : newChanges) {
                if (data.thinker != null) {
                    if (data.thinker.levelState != null) {
                        data.thinker.levelState.removeActions(data.thinker);
                    }
                    if (data.newLevelState != null) {
                        data.newLevelState.addActions(data.thinker);
                    }
                } else if (data.object != null) {
                    if (data.object.levelState != null) {
                        data.object.levelState.removeActions(data.object);
                    }
                    if (data.newLevelState != null) {
                        data.newLevelState.addActions(data.object);
                    }
                }
            }
        }
        catchNewThinkersUp(game);
    }
    
    @Override
    public final void stepActions(IroncladGame game) {
        delayState = this;
        double timeFactor = getTimeFactor();
        catchNewThinkersUp(game);
        for (LevelThinker thinker : levelThinkers) {
            thinker.reactToInput(game, this);
        }
        thinkersHaveReactedToInput = true;
        changeObjects(game);
        
        thinkersHaveReactedToLevel = false;
        thinkersHaveReactedToInput = false;
        for (LevelThinker thinker : levelThinkers) {
            thinker.reactToLevel(game, this);
        }
        thinkersHaveReactedToLevel = true;
        changeObjects(game);
        delayState = null;
    }
    
    public final LevelLayer getLayer(int id) {
        return levelLayers.get(id);
    }
    
    public final boolean setLayer(int id, LevelLayer layer) {
        if (id == 0) {
            throw new RuntimeException("Attempted to set a level layer with an ID of 0");
        }
        if (layer == null) {
            return removeLayer(id);
        }
        if (addThinker(layer)) {
            LevelLayer oldLayer = levelLayers.get(id);
            if (oldLayer != null) {
                removeThinker(oldLayer);
            }
            levelLayers.put(id, layer);
            return true;
        }
        return false;
    }
    
    public final boolean removeLayer(int id) {
        LevelLayer oldLayer = levelLayers.get(id);
        if (oldLayer != null) {
            removeThinker(oldLayer);
            levelLayers.remove(id);
            return true;
        }
        return false;
    }
    
    public final void clearLayers() {
        for (LevelLayer layer : levelLayers.values()) {
            removeThinker(layer);
        }
        levelLayers.clear();
    }
    
    public final HUD getHUD() {
        return hud;
    }
    
    public final boolean setHUD(HUD hud) {
        if (hud == null || addThinker(hud)) {
            if (this.hud != null) {
                removeThinker(this.hud);
            }
            this.hud = hud;
            return true;
        }
        return false;
    }
    
    public final Viewport getViewport(int id) {
        return viewports.get(id);
    }
    
    public final boolean setViewport(int id, Viewport viewport) {
        if (viewport == null) {
            return removeViewport(id);
        }
        if (addThinker(viewport)) {
            Viewport oldViewport = viewports.get(id);
            if (oldViewport != null) {
                removeThinker(oldViewport);
            }
            viewports.put(id, viewport);
            return true;
        }
        return false;
    }
    
    public final boolean removeViewport(int id) {
        Viewport oldViewport = viewports.get(id);
        if (oldViewport != null) {
            removeThinker(oldViewport);
            viewports.remove(id);
            return true;
        }
        return false;
    }
    
    public final void clearViewports() {
        for (Viewport viewport : viewports.values()) {
            removeThinker(viewport);
        }
        viewports.clear();
    }
    
    @Override
    public final void renderActions(IroncladGame game, Graphics g, int x1, int y1, int x2, int y2) {
        g.clearWorldClip();
        for (Viewport viewport : viewports.values()) {
            if (viewport.roundX1 != viewport.roundX2 && viewport.roundY1 != viewport.roundY2) {
                int vx1 = x1 + viewport.roundX1;
                int vy1 = y1 + viewport.roundY1;
                int vx2 = x1 + viewport.roundX2;
                int vy2 = y1 + viewport.roundY2;
                g.setWorldClip(vx1, vy1, vx2 - vx1, vy2 - vy1);
                if (viewport.camera != null && viewport.camera.levelState == this) {
                    double cx = viewport.camera.getCenterX();
                    double cy = viewport.camera.getCenterY();
                    for (LevelLayer layer : levelLayers.headMap(0).values()) {
                        layer.renderActions(game, delayState, g, cx, cy, vx1, vy1, vx2, vy2);
                    }
                    
                    for (LevelLayer layer : levelLayers.tailMap(1).values()) {
                        layer.renderActions(game, delayState, g, cx, cy, vx1, vy1, vx2, vy2);
                    }
                }
                if (viewport.hud != null) {
                    viewport.hud.renderActions(game, this, g, vx1, vy1, vx2, vy2);
                }
                g.clearWorldClip();
            }
        }
        g.setWorldClip(x1, y1, x2 - x1, y2 - y1);
        if (hud != null) {
            hud.renderActions(game, this, g, x1, y1, x2, y2);
        }
    }
    
}
