package dronerush;

import java.util.LinkedList;
import java.util.List;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
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
		if (BroadcastInterface.readAttackMode(rc)) {
			// if there are enough drones, charge the opponent!
			// TODO: attack towers first
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
		return result;
	}

	private final Action attack = new Attack();

	private final Action charge = new MoveTo(rc.senseEnemyHQLocation(), /* avoidEnemies */false);
	private final Action retreat = new MoveTo(rc.senseHQLocation(), /* avoidEnemies */true);

	private final Action advanceAvoidingEnemies = new MoveTo(rc.senseEnemyHQLocation(), /* avoidEnemies */true);

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
			if(inRobotRange(nextLoc, enemyRobots)){
				return true;
			}
			if (inHqOrTowerRange(nextLoc, enemyTowers, enemyHq)) {
				return true;
			}
			return false;
		}
	}

}
