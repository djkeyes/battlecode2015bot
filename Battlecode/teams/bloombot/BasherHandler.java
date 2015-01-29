package bloombot;

import java.util.LinkedList;
import java.util.List;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class BasherHandler extends BaseRobotHandler {

	public BasherHandler(RobotController rc) {
		super(rc);
	}

	@Override
	public List<Action> chooseActions() throws GameActionException {
		LinkedList<Action> result = new LinkedList<Action>();
		return result;
	}

	private final Action basherAttack = new BasherAttack();

	public class BasherAttack implements Action {
		private final int BASHER_ENEMY_SEARCH_RANGE_SQ = 9;

		@Override
		public boolean run() throws GameActionException {
			// just go toward the closest opponent
			if (rc.isCoreReady()) {
				boolean[] isTraversableDir = new boolean[Direction.values().length];
				for (Direction dir : Util.actualDirections) {
					if (rc.canMove(dir)) {
						isTraversableDir[dir.ordinal()] = true;
					}
				}
				RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(BASHER_ENEMY_SEARCH_RANGE_SQ, rc.getTeam().opponent());
				int minDist = Integer.MAX_VALUE;
				Direction bestDir = null;
				for (RobotInfo enemy : nearbyEnemies) {
					int dist = enemy.location.distanceSquaredTo(rc.getLocation());
					if (dist < minDist) {
						for (Direction dir : Util.getDirectionsStrictlyToward(rc.getLocation(), enemy.location)) {
							if (isTraversableDir[dir.ordinal()]) {
								dist = minDist;
								bestDir = dir;
							}
						}
					}
				}
				if (bestDir != null) {
					rc.move(bestDir);
					return true;
				}
			}
			return false;
		}

	}
}
