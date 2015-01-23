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
	private final Action advance = new MoveTowardEnemyHq(/* avoidEnemies= */true);

	private final class MissileAttack implements Action {
		private Action retreat;
		private int originalMissileCount;

		public MissileAttack() {
			retreat = new Retreat();
		}

		@Override
		public boolean run() throws GameActionException {
			// this does a cool micro i observed from another team
			// launchMissle doesn't increment any delays, so we can launch multiple missiles in one round
			// that means it would be smart to launch 3+ missiles at once, then retreat and make more missiles

			originalMissileCount = rc.getMissileCount();

			if (originalMissileCount >= 3) {
				Direction enemy = findEnemyDirection();
				if (enemy == null) {
					return false;
				}

				if (launchMissiles(enemy)) {
					return true;
				}
			}
			return retreatAndStockUp();
		}

		private boolean retreatAndStockUp() throws GameActionException {
			// neat, I can nest actions. baller.
			return retreat.run();
		}

		private Direction findEnemyDirection() {
			// find a desireable missile direction
			int effectiveRange = GameConstants.MISSILE_LIFESPAN + 1; // or is this +2 due to splash?
			RobotInfo[] nearby = rc.senseNearbyRobots(effectiveRange * effectiveRange, rc.getTeam().opponent());
			int minDistSq = (effectiveRange + 1) * (effectiveRange + 1);
			Direction bestDir = null;
			for (RobotInfo enemy : nearby) {
				int distSq = enemy.location.distanceSquaredTo(rc.getLocation());
				if (distSq <= minDistSq) {
					for (Direction curDir : Util.getDirectionsStrictlyToward(rc.getLocation(), enemy.location)) {
						if (rc.canLaunch(curDir)) {
							minDistSq = distSq;
							bestDir = curDir;
							break;
						}
					}
				}
			}
			// also check the towers and hq
			// (sorry this code is messy. maybe we should add a function or array in Util
			if (bestDir == null) {
				for (MapLocation enemyTower : getEnemyTowerLocations()) {
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
				MapLocation enemyHq = getEnemyHqLocation();
				int distSq = enemyHq.distanceSquaredTo(rc.getLocation());
				Direction curDir = rc.getLocation().directionTo(enemyHq);
				if (rc.canLaunch(curDir)) {
					if (distSq <= minDistSq) {
						minDistSq = distSq;
						bestDir = curDir;
					}
				}
			}
			return bestDir;
		}

		private boolean launchMissiles(Direction enemyDir) throws GameActionException {
			if (rc.isCoreReady()) {
				// int missilesLaunched = 0;
				for (Direction curDir : Util.getDirectionsStrictlyToward(enemyDir)) {
					if (rc.canLaunch(curDir)) {
						rc.launchMissile(curDir);
						// missilesLaunched++;
					}
				}
				// launching missiles doesn't increment any delays, so we could retreat as soon as we launch
				// that being said, missiles use nearby launcher positions to boostrap their targeting
				// so stay in place
				return true;
				// if (originalMissileCount - missilesLaunched > 0) {
				// return true; // if we still have missiles, we might as well stay here and launch more.
				// } else {
				// return false; // launching missiles doesn't increment any delays. baller.
				// }
			}
			return false;
		}
	}

}
