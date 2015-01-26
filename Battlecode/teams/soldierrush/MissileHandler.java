package soldierrush;

import java.util.List;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

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
						RobotInfo[] nearbyAllies = rc.senseNearbyRobots(2, rc.getTeam());
						for (int i = 0; i < nearbyAllies.length; i++) {
							if (nearbyAllies[i].type == RobotType.LAUNCHER) {
								Direction directionAway = nearbyAllies[i].location.directionTo(rc.getLocation());
								if (rc.canMove(directionAway)) {
									rc.move(directionAway);
								}
								break;
							}
						}
						isFirstTurn = false;
					} else {
						// it would be nice to always go towards the closest opponent
						// but we really don't have the bytecodes to do that.
						// so just go toward ANY opponent
						int turnsLeft = spawnTurn + GameConstants.MISSILE_LIFESPAN - Clock.getRoundNum() + 1;
						RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(2 * turnsLeft * turnsLeft, rc.getTeam().opponent());
						RobotInfo[] nearbyAllies;
						if (nearbyEnemies.length > 0) {
							nearbyAllies = rc.senseNearbyRobots(2, rc.getTeam());
							if (nearbyEnemies[0].location.isAdjacentTo(rc.getLocation()) && nearbyAllies.length == 0) {
								rc.explode();
							}

							Direction curDir = rc.getLocation().directionTo(nearbyEnemies[0].location);
							if (rc.canMove(curDir)) {
								// if the path is blocked, no worries. we still have several more turns
								rc.move(curDir);
							}
						} else {
							// if we get here, it means our old target disappeared
							// just try to avoid teammates
							nearbyAllies = rc.senseNearbyRobots(2, rc.getTeam());
							if (nearbyAllies.length > 0) {
								Direction directionAway = nearbyAllies[0].location.directionTo(rc.getLocation());
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
