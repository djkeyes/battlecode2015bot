package soldierrush;

import java.util.LinkedList;
import java.util.List;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class DroneHandler extends BaseRobotHandler {

	protected DroneHandler(RobotController rc) {
		super(rc);
	}

	@Override
	public List<Action> chooseActions() throws GameActionException {
		LinkedList<Action> result = new LinkedList<Action>();
		result.add(deliverSupplies);
		result.add(retreat);
		return result;
	}

	@Override
	protected boolean distributeSupply() throws GameActionException {
		return false;
	}

	private final Action retreat = new MoveTo(getOurHqLocation(), /* avoidEnemies */true, /* avoidEnemiesAndTowers */true);
	private final Action deliverSupplies = new DeliverSupplies();

	private class DeliverSupplies implements Action {

		private RobotInfo[] enemyRobots = null;

		private int curTargetID = -1;

		@Override
		public boolean run() throws GameActionException {
			if (rc.isCoreReady()) {
				if (rc.getSupplyLevel() > 1000) {
					if (curTargetID == -1) {
						curTargetID = BroadcastInterface.dequeueSupplyQueue(rc);
					}
					if (curTargetID == -1) {
						// the queue is empty
						return false;
					}

					if (!rc.canSenseRobot(curTargetID)) {
						// the target may have died after enqueueing itself
						// TODO loop until we get a valid id, or run out of bytecodes
						curTargetID = -1;
						return false;
					}
					RobotInfo targetRobot = rc.senseRobot(curTargetID);
					if (rc.getLocation().distanceSquaredTo(targetRobot.location) <= GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED) {
						rc.transferSupplies(10000, targetRobot.location);
						curTargetID = -1;
					} else {
						Direction nextDir = null;
						// TODO: we call these several times in this file. maybe we should make them a static memberof DroneHandler,
						// and lazily initialize them each turn
						enemyRobots = rc.senseNearbyRobots(35, rc.getTeam().opponent());
						for (Direction d : Util.getDirectionsToward(rc.getLocation(), targetRobot.location)) {
							// TODO: we should also avoid enemies
							if (rc.canMove(d) && !isNearEnemy(rc.getLocation().add(d))) {
								nextDir = d;
								break;
							}
						}

						if (nextDir != null) {
							rc.move(nextDir);
							return true;
						}
					}
				} else {
					return retreat.run();
				}
			}
			return false;
		}

		private boolean isNearEnemy(MapLocation nextLoc) {
			// note: this method is temporally coupled to the enemyRobots, enemyTowers, and enemyHq variables.
			// if they haven't been updated to reflect the current round, this method WILL return inaccurate results!
			if (inRobotRange(nextLoc, enemyRobots)) {
				return true;
			}
			if (inEnemyHqOrTowerRange(nextLoc)) {
				return true;
			}
			return false;
		}

	}

}
