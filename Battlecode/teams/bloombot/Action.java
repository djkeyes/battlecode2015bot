package bloombot;

import battlecode.common.GameActionException;

/**
 * An encapsulation of an action to perform during a particular round. Actions are meant to be complex--while MoveAroundAction is a
 * valid kind of action, Scout, Retreat, and AttackMove are better actions.
 * 
 */
public interface Action {
	/**
	 * @return true if the action was performed and no further actions should be performed (ie because the core delay was incremented)
	 */
	public abstract boolean run() throws GameActionException;

}
