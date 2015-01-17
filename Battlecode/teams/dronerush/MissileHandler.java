package dronerush;

import java.util.List;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class MissileHandler extends BaseRobotHandler {

	private int spawnTurn;
	private boolean isFirstTurn;

	protected MissileHandler(RobotController rc) {
		super(rc);

		spawnTurn = Clock.getRoundNum();
		isFirstTurn = true;
	}

	// missiles are super strapped down in computation (they only get 500 bytecodes).
	// so they override the run() method so we can cut all the frills out of the framework
	@Override
	public void run() {
		while (true) {
			try {
				if (rc.isCoreReady()) {
					if (isFirstTurn) {
						// on the first turn, we don't really have time to look at ALL the nearby enemies
						// so just try to get away from allies
						RobotInfo[] nearbyAllies = rc.senseNearbyRobots(1, rc.getTeam());
						if (nearbyAllies.length > 0) {
							Direction directionAway = nearbyAllies[0].location.directionTo(rc.getLocation());
							if (rc.canMove(directionAway)) {
								rc.move(directionAway);
							}
						}
						isFirstTurn = false;
					} else {
						// it would be nice to always go towards the closest opponent
						// but we really don't have the bytecodes to do that.
						// so just go toward ANY opponent
						int turnsLeft = spawnTurn + GameConstants.MISSILE_LIFESPAN - Clock.getRoundNum();
						RobotInfo[] nearby = rc.senseNearbyRobots(turnsLeft * turnsLeft, rc.getTeam().opponent());
						if (nearby.length > 0) {
							if (nearby[0].location.isAdjacentTo(rc.getLocation())) {
								rc.explode();
							}

							Direction curDir = rc.getLocation().directionTo(nearby[0].location);
							if (rc.canMove(curDir)) {
								// if the path is blocked, no worries. we still have several more turns
								// alternatively, if the enemy is already adjacent to us, still no worries. we have several more turns,
								// and we don't want to explode the same turn we're launched.
								rc.move(curDir);
							}
						} else {
							// if we get here, it means our old target disappeared
							// just try to avoid teammates
							nearby = rc.senseNearbyRobots(1, rc.getTeam());
							if (nearby.length > 0) {
								Direction directionAway = nearby[0].location.directionTo(rc.getLocation());
								if (rc.canMove(directionAway)) {
									rc.move(directionAway);
								}
							}
						}
					}
				}
				rc.yield();
			} catch (GameActionException ex) {
				ex.printStackTrace();
			}
		}
	}

	@Override
	public List<Action> chooseActions() throws GameActionException {
		return null;
	}
}
