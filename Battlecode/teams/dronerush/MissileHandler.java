package dronerush;

import java.util.List;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class MissileHandler extends BaseRobotHandler {

	private int motherId = -1;
	private int motherLauncherIndex = -1;
	private MapLocation target = null;

	protected MissileHandler(RobotController rc) {
		super(rc, true);
	}

	// missiles are super strapped down in computation (they only get 500 bytecodes).
	// so they override the run() method so we can cut all the frills out of the framework
	@Override
	public void run() {
		while (true) {
			try {
				if (rc.isCoreReady()) {
					// travel toward the designated target
					if (motherId == -1) {
						// try to find whoever launched us
						RobotInfo[] nearbyAllies = rc.senseNearbyRobots(2, rc.getTeam());
						for (int i = 0; i < nearbyAllies.length; i++) {
							if (nearbyAllies[i].type == RobotType.LAUNCHER) {
								motherId = nearbyAllies[i].ID;

								// during the first turn, we don't have enough bytecodes to look up the actual target
								// so just try to get away from mom
								Direction awayDir = nearbyAllies[i].location.directionTo(rc.getLocation());
								if (rc.canMove(awayDir)) {
									rc.move(awayDir);
								} else if (rc.canMove(awayDir.rotateLeft())) {
									rc.move(awayDir.rotateLeft());
								} else if (rc.canMove(awayDir.rotateRight())) {
									rc.move(awayDir.rotateRight());
								}
								break;
							}
						}
					} else {
						if (motherLauncherIndex == -1) {
							motherLauncherIndex = BroadcastInterface.getLauncherIndex(rc, motherId);
							target = BroadcastInterface.findLauncherTarget(rc, motherLauncherIndex);
						}

						// we found our mother, but she doesn't want us to kill anything :(
						if (target != null) {
							// TODO: should we disregard adjacent allied missiles in these counts?
							if (rc.getLocation().isAdjacentTo(target)
									|| (rc.senseNearbyRobots(2, rc.getTeam().opponent()).length > 0 && rc.senseNearbyRobots(2, rc
											.getTeam().opponent()).length == 0)) {
								rc.explode();
							} else {
								Direction awayDir = rc.getLocation().directionTo(target);
								if (rc.canMove(awayDir)) {
									rc.move(awayDir);
								} else if (rc.canMove(awayDir.rotateLeft())) {
									rc.move(awayDir.rotateLeft());
								} else if (rc.canMove(awayDir.rotateRight())) {
									rc.move(awayDir.rotateRight());
								}
							}
						} else {
							// otherwise just path away from teammates
							RobotInfo[] nearbyAllies = rc.senseNearbyRobots(2, rc.getTeam());
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
