package cell2d.space;

import cell2d.CellGame;
import cell2d.Thinker;

/**
 * <p>A SpaceThinker is the type of Thinker that is used by SpaceStates and uses
 * SpaceThinkerStates. A SpaceThinker can take beforeMovementActions() and
 * afterMovementActions() every frame before and after its SpaceState moves its
 * assigned ThinkerObjects, respectively.</p>
 * @author Andrew Heyman
 * @param <T> The subclass of CellGame that this SpaceThinker's SpaceState is
 used by
 */
public abstract class SpaceThinker<T extends CellGame> extends Thinker<T,SpaceState<T>,SpaceThinker<T>,SpaceThinkerState<T>> {
    
    @Override
    public final SpaceThinker<T> getThis() {
        return this;
    }
    
    final void beforeMovement(T game, SpaceState<T> state) {
        SpaceThinkerState<T> thinkerState = getThinkerState();
        if (thinkerState != null) {
            thinkerState.beforeMovementActions(game, state);
        }
        beforeMovementActions(game, state);
    }
    
    /**
     * Actions for this SpaceThinker to take once every frame, after
     * SpaceThinkers take their frameActions() but before its SpaceState moves
     * its assigned ThinkerObjects.
     * @param game This SpaceThinker's CellGame
     * @param state This SpaceThinker's SpaceState
     */
    public void beforeMovementActions(T game, SpaceState<T> state) {}
    
    final void afterMovement(T game, SpaceState<T> state) {
        SpaceThinkerState<T> thinkerState = getThinkerState();
        if (thinkerState != null) {
            thinkerState.afterMovementActions(game, state);
        }
        afterMovementActions(game, state);
    }
    
    /**
     * Actions for this SpaceThinker to take once every frame, after its
     * SpaceState moves its assigned ThinkerObjects.
     * @param game This SpaceThinker's CellGame
     * @param state This SpaceThinker's SpaceState
     */
    public void afterMovementActions(T game, SpaceState<T> state) {}
    
}