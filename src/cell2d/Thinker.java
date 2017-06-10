package cell2d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>A Thinker is a collection of methods that contributes to the mechanics of
 * the CellGameState to which it is assigned. A Thinker's assigned CellGameState
 * will keep track of time for it, thus allowing it to take its own
 * time-dependent actions, while the CellGameState is active. A Thinker's time
 * factor represents the average number of discrete time units the Thinker will
 * experience every frame while assigned to an active CellGameState. If its own
 * time factor is negative, a Thinker will use its assigned CellGameState's time
 * factor instead. If a Thinker is assigned to an inactive CellGameState or none
 * at all, time will not pass for it.</p>
 * 
 * <p>A Thinker's action priority determines when it will act relative to other
 * Thinkers. All of the Thinkers assigned to the active CellGameState will take
 * their timeUnitActions() and their frameActions() in order from highest to
 * lowest action priority.</p>
 * 
 * <p>A Thinker may occupy at most one ThinkerState at a time. ThinkerStates
 * take actions alongside their Thinker's own, as well as when entered and left
 * by a Thinker, and can help a Thinker keep track of its position in a
 * multi-frame procedure. A ThinkerState can have a limited duration in time
 * units, and at the beginning of the time unit when that duration is up, its
 * Thinker automatically transitions to its next ThinkerState.</p>
 * 
 * <p>A Thinker has timers that can activate TimedEvents after a certain number
 * of time units. Timers have integer values, with a positive value x indicating
 * that the TimedEvent will be activated in x time units, a negative value
 * indicating that the timer is not running, and a value of 0 indicating that
 * either the TimedEvent was activated or the value was deliberately set to 0
 * this time unit. Each time unit, after a Thinker automatically changes
 * ThinkerStates (if it did) but before it and its ThinkerState (if it has one)
 * take their timeUnitActions(), its non-negative timers' values are decreased
 * by 1 and the TimedEvents whose timers have reached 0 are activated.
 * </p>
 * 
 * <p>The Thinker class is intended to be directly extended by classes V that
 * extend Thinker&lt;T,U,V,W&gt; and interact with CellGameStates of class U and
 * ThinkerStates of class W. BasicThinker is an example of such a class. This
 * allows a Thinker's CellGameStates and ThinkerStates to interact with it in
 * ways unique to its subclass of Thinker.</p>
 * @author Andrew Heyman
 * @param <T> The subclass of CellGame that this Thinker's CellGameState is used
 * by
 * @param <U> The subclass of CellGameState that this Thinker is used by
 * @param <V> The subclass of Thinker that this Thinker is
 */
public abstract class Thinker<T extends CellGame, U extends CellGameState<T,U,V>,
        V extends Thinker<T,U,V>> extends ThinkerGroup<T,U,V> {
    
    private static final AtomicLong idCounter = new AtomicLong(0);
    
    final long id;
    private final V thisThinker;
    ThinkerGroup<T,U,V> group = null;
    ThinkerGroup<T,U,V> newGroup = null;
    private T game = null;
    private U state = null;
    private V superThinker = null;
    private long timeFactor = -1;
    private long timeToRun = 0;
    int actionPriority = 0;
    int newActionPriority = 0;
    private final Map<TimedEvent,Integer> timers = new HashMap<>();
    
    /**
     * Creates a new Thinker.
     */
    public Thinker() {
        id = idCounter.getAndIncrement();
        thisThinker = getThis();
    }
    
    /**
     * A method which returns this Thinker as a V, rather than as a
     * Thinker&lt;T,U,V,W&gt;. This must be implemented somewhere in the lineage
     * of every subclass of Thinker in order to get around Java's limitations
     * with regard to generic types.
     * @return This Thinker as a V
     */
    public abstract V getThis();
    
    public final ThinkerGroup<T,U,V> getThinkerGroup() {
        return group;
    }
    
    public final ThinkerGroup<T,U,V> getNewThinkerGroup() {
        return newGroup;
    }
    
    /**
     * Sets the CellGameState to which this Thinker is currently assigned. If it
     * is set to a null CellGameState, this Thinker will be removed from its
     * current CellGameState if it has one.
     * @param group The CellGameState to which this Thinker should be assigned
     */
    public final void setThinkerGroup(ThinkerGroup<T,U,V> group) {
        if (newGroup != null) {
            newGroup.removeThinker(thisThinker);
        }
        if (group != null) {
            group.addThinker(thisThinker);
        }
    }
    
    public final T getGame() {
        return game;
    }
    
    /**
     * Returns the CellGameState to which this Thinker is currently assigned, or
     * null if it is assigned to none.
     * @return The CellGameState to which this Thinker is currently assigned
     */
    public final U getGameState() {
        return state;
    }
    
    final void setGameAndState(T game, U state) {
        this.game = game;
        this.state = state;
        if (getNumThinkers() > 0) {
            Iterator<V> iterator = thinkerIterator();
            while (iterator.hasNext()) {
                iterator.next().setGameAndState(game, state);
            }
        }
    }
    
    public final V getSuperThinker() {
        return superThinker;
    }
    
    /**
     * Returns this Thinker's time factor.
     * @return This Thinker's time factor
     */
    public final long getTimeFactor() {
        return timeFactor;
    }
    
    /**
     * Returns this Thinker's effective time factor; that is, the average number
     * of time units it experiences every frame. If it is not assigned to a
     * CellGameState, this will be 0.
     * @return This Thinker's effective time factor
     */
    public final long getEffectiveTimeFactor() {
        if (state == null) {
            return 0;
        }
        Thinker<T,U,V> ancestor = this;
        while (ancestor.superThinker != null) {
            ancestor = ancestor.superThinker;
        }
        return (ancestor.timeFactor < 0 ? state.getTimeFactor() : ancestor.timeFactor);
    }
    
    /**
     * Sets this Thinker's time factor to the specified value.
     * @param timeFactor The new time factor
     */
    public final void setTimeFactor(long timeFactor) {
        this.timeFactor = timeFactor;
    }
    
    /**
     * Returns this Thinker's action priority.
     * @return This Thinker's action priority
     */
    public final int getActionPriority() {
        return actionPriority;
    }
    
    /**
     * Returns the action priority that this Thinker is about to have, but does
     * not yet have due to its CellGameState's Thinker list being iterated over.
     * If this Thinker is not about to change its action priority, this method
     * will simply return its current action priority.
     * @return The action priority that this Thinker is about to have
     */
    public final int getNewActionPriority() {
        return newActionPriority;
    }
    
    /**
     * Sets this Thinker's action priority to the specified value.
     * @param actionPriority The new action priority
     */
    public final void setActionPriority(int actionPriority) {
        if (group == null) {
            newActionPriority = actionPriority;
            this.actionPriority = actionPriority;
        } else if (newActionPriority != actionPriority) {
            newActionPriority = actionPriority;
            group.changeThinkerActionPriority(thisThinker, actionPriority);
        }
    }
    
    /**
     * Returns the current value of this Thinker's timer for the specified
     * TimedEvent.
     * @param timedEvent The TimedEvent whose timer value should be returned
     * @return The current value of the timer for the specified TimedEvent
     */
    public final int getTimerValue(TimedEvent timedEvent) {
        Integer value = timers.get(timedEvent);
        return (value == null ? -1 : value);
    }
    
    /**
     * Sets the value of this Thinker's timer for the specified TimedEvent to
     * the specified value.
     * @param timedEvent The TimedEvent whose timer value should be set
     * @param value The new value of the specified TimedEvent's timer
     */
    public final void setTimerValue(TimedEvent timedEvent, int value) {
        if (timedEvent == null) {
            throw new RuntimeException("Attempted to set the value of a timer for a null TimedEvent");
        }
        if (value < 0) {
            timers.remove(timedEvent);
        } else {
            timers.put(timedEvent, value);
        }
    }
    
    private void timeUnit() {
        if (!timers.isEmpty()) {
            List<TimedEvent> timedEventsToDo = new ArrayList<>();
            Iterator<Map.Entry<TimedEvent,Integer>> iterator = timers.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<TimedEvent,Integer> entry = iterator.next();
                int value = entry.getValue();
                if (value == 0) {
                    iterator.remove();
                } else {
                    if (value == 1) {
                        timedEventsToDo.add(entry.getKey());
                    }
                    entry.setValue(value - 1);
                }
            }
            for (TimedEvent timedEvent : timedEventsToDo) {
                timedEvent.eventActions();
            }
        }
        timeUnitActions(game, state);
        if (getNumThinkers() > 0) {
            Iterator<V> iterator = thinkerIterator();
            while (iterator.hasNext()) {
                Thinker<T,U,V> thinker = iterator.next();
                thinker.timeUnit();
            }
        }
    }
    
    /**
     * Actions for this Thinker to take once every time unit, after
     * AnimationInstances update their indices but before Thinkers take their
     * frameActions().
     * @param game This Thinker's CellGame
     * @param state This Thinker's CellGameState
     */
    public void timeUnitActions(T game, U state) {}
    
    final void frame() {
        frameActions(game, state);
        if (getNumThinkers() > 0) {
            Iterator<V> iterator = thinkerIterator();
            while (iterator.hasNext()) {
                iterator.next().frame();
            }
        }
    }
    
    /**
     * Actions for this Thinker to take once every frame, after Thinkers take
     * their timeUnitActions() but before its CellGameState takes its own
     * frameActions().
     * @param game This Thinker's CellGame
     * @param state This Thinker's CellGameState
     */
    public void frameActions(T game, U state) {}
    
    final void added() {
        addedActions(game, state);
    }
    
    /**
     * Actions for this Thinker to take immediately after being added to a new
     * CellGameState.
     * @param game This Thinker's CellGame
     * @param state This Thinker's CellGameState
     */
    public void addedActions(T game, U state) {}
    
    final void removed() {
        removedActions(game, state);
    }
    
    /**
     * Actions for this Thinker to take immediately before being removed from
     * its CellGameState.
     * @param game This Thinker's CellGame
     * @param state This Thinker's CellGameState
     */
    public void removedActions(T game, U state) {}
    
    @Override
    public final void addThinkerActions(V thinker) {
        thinker.setGameAndState(game, state);
        ((Thinker<T,U,V>)thinker).superThinker = thisThinker;
        addThinkerActions(game, state, thinker);
    }
    
    public final void addThinkerActions(T game, U state, V thinker) {}
    
    @Override
    public final void removeThinkerActions(V thinker) {
        removeThinkerActions(game, state, thinker);
        thinker.setGameAndState(null, null);
        ((Thinker<T,U,V>)thinker).superThinker = null;
    }
    
    public final void removeThinkerActions(T game, U state, V thinker) {}
    
    final void update() {
        timeToRun += (timeFactor < 0 ? state.getTimeFactor() : timeFactor);
        while (timeToRun >= Frac.UNIT) {
            timeUnit();
            timeToRun -= Frac.UNIT;
        }
    }
    
}
