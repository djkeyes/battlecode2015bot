package dronerush;

import java.util.LinkedList;
import java.util.List;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class LauncherHandler extends BaseBuildingHandler {

	protected LauncherHandler(RobotController rc) {
		super(rc);
	}

	@Override
	public List<Action> chooseActions() throws GameActionException {

		// once we can estimate the distance to the opponent's hq, travel toward it
		// we outrange everything, so we probably don't need to wait for support
		// TODO: wait for a launcher attack signal
		// but pending that, path from our hq to protect drones

		LinkedList<Action> result = new LinkedList<Action>();
		result.add(missileAttack);
		result.add(advance);
		result.add(scout);
		return result;
	}

	private final Action missileAttack = new MissileAttack();
	private final Action scout = new ScoutOutward();
	private final Action advance = new MoveTowardEnemyHq();

	private final class MissileAttack implements Action {

		@Override
		public boolean run() throws GameActionException {
			if (rc.isCoreReady()) {
				// find a desireable missile direction
				int effectiveRange = GameConstants.MISSILE_LIFESPAN + 1; // or is this +2 due to splash?
				RobotInfo[] nearby = rc.senseNearbyRobots(effectiveRange * effectiveRange, rc.getTeam().opponent());
				int minDistSq = (effectiveRange + 1) * (effectiveRange + 1);
				Direction bestDir = null;
				for (RobotInfo enemy : nearby) {
					int distSq = enemy.location.distanceSquaredTo(rc.getLocation());
					Direction curDir = rc.getLocation().directionTo(enemy.location);
					if (rc.canLaunch(curDir)) {
						if (distSq <= minDistSq) {
							minDistSq = distSq;
							bestDir = curDir;
						}
					}
				}
				// also check the towers and hq
				// (sorry this code is messy. maybe we should add a function or array in Util
				if (bestDir == null) {
					for (MapLocation enemyTower : rc.senseEnemyTowerLocations()) {
						int distSq = enemyTower.distanceSquaredTo(rc.getLocation());
						Direction curDir = rc.getLocation().directionTo(enemyTower);
						if (rc.canLaunch(curDir)) {
							if (distSq <= minDistSq) {
								minDistSq = distSq;
								bestDir = curDir;
							}
						}
					}
				}
				if (bestDir == null) {
					MapLocation enemyHq = rc.senseEnemyHQLocation();
					int distSq = enemyHq.distanceSquaredTo(rc.getLocation());
					Direction curDir = rc.getLocation().directionTo(enemyHq);
					if (rc.canLaunch(curDir)) {
						if (distSq <= minDistSq) {
							minDistSq = distSq;
							bestDir = curDir;
						}
					}
				}
				if (bestDir != null) {
					rc.launchMissile(bestDir);
					return true;
				}
			}
			return false;
		}

	}

}
