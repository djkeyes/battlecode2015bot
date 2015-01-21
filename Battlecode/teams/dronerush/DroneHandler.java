package dronerush;

import java.util.LinkedList;
import java.util.List;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class DroneHandler extends BaseRobotHandler {

	boolean isSupplyCourier = false;

	protected DroneHandler(RobotController rc) {
		super(rc);
	}

	@Override
	public List<Action> chooseActions() throws GameActionException {
		LinkedList<Action> result = new LinkedList<Action>();
		// even on the largest maps (ie eightgates.xml), we can reach the enemy by turn 400.
		// so if it's turn 500, we probably aren't doing much damage anymore (and we've already
		// done all the scouting we need), so we should relegate drones to supply-delivery
		if (Clock.getRoundNum() > 500) {
			isSupplyCourier = true;
			result.add(deliverSupplies); // TODO
			result.add(retreat);
		} else {
			if (BroadcastInterface.readAttackMode(rc)) {
				// if there are enough drones, charge the opponent!
				if (rc.isWeaponReady()) {
					result.add(attack);
					result.add(charge);
				} else {
					result.add(retreat);
				}
			} else {
				// else skirt the towers and hq so that we can kill other units
				result.add(attack);
				result.add(advanceAvoidingEnemies);
			}
		}
		return result;
	}

	@Override
	protected void distributeSupply() throws GameActionException {
		if (!isSupplyCourier) {
			super.distributeSupply();
		}
	}

	private final Action attack = new Attack();

	private final Action charge = new MoveTo(rc.senseEnemyHQLocation(), /* avoidEnemies */false);
	private final Action retreat = new MoveTo(rc.senseHQLocation(), /* avoidEnemies */true);

	private final Action advanceAvoidingEnemies = new MoveTo(rc.senseEnemyHQLocation(), /* avoidEnemies */true);

	private final Action deliverSupplies = new DeliverSupplies();

	// drones can fly over obstacles, so they don't need to rely on the BFS results
	// therefore, we'll just use a simple movement implementation
	private class MoveTo implements Action {
		private MapLocation target;
		private boolean avoidEnemies;

		private RobotInfo[] enemyRobots = null;
		private MapLocation[] enemyTowers = null;
		private MapLocation enemyHq = null;

		public MoveTo(MapLocation target, boolean avoidEnemies) {
			this.target = target;
			this.avoidEnemies = avoidEnemies;
		}

		@Override
		public boolean run() throws GameActionException {
			if (avoidEnemies) {
				// the robot with the longest range is the tower, with 24 range
				// (sqrt(24)+1)^2 ~= 35
				enemyRobots = rc.senseNearbyRobots(35, rc.getTeam().opponent());
				// TODO: the next 2 methods cost 100 and 50 bytecodes respectively. we should cache them in the broadcast array.
				enemyTowers = rc.senseEnemyTowerLocations();
				enemyHq = rc.senseEnemyHQLocation();
			}
			if (rc.isCoreReady()) {
				for (Direction d : Util.getDirectionsToward(rc.getLocation(), target)) {
					if (rc.canMove(d)) {
						if (avoidEnemies && isNearEnemy(rc.getLocation().add(d))) {
							continue;
						}
						rc.move(d);
						return true;
					}
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
			if (inHqOrTowerRange(nextLoc, enemyTowers, enemyHq)) {
				return true;
			}
			return false;
		}
	}

	private class DeliverSupplies implements Action {

		private RobotInfo[] enemyRobots = null;
		private MapLocation[] enemyTowers = null;
		private MapLocation enemyHq = null;

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
						enemyTowers = rc.senseEnemyTowerLocations();
						enemyHq = rc.senseEnemyHQLocation();
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
			if (inHqOrTowerRange(nextLoc, enemyTowers, enemyHq)) {
				return true;
			}
			return false;
		}

	}

}
