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

	private int launcherIndex;

	protected LauncherHandler(RobotController rc) {
		super(rc);

		try {
			launcherIndex = BroadcastInterface.addLauncherAndGetLauncherIndex(rc, rc.getID());
		} catch (GameActionException e) {
			e.printStackTrace();
		}
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
	private final Action scout = new ScoutOutward(false, false);
	private final Action advance = new MoveTowardEnemyHq(/* avoidEnemies= */true, true);

	private final class MissileAttack implements Action {
		private Action retreat;
		private int originalMissileCount;

		// these variables have some weird temporal coupling. please fix them in the future. :(
		private MapLocation curTarget = null;
		private boolean[] isDirLaunchable;

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

				if (launchMissiles(enemy, curTarget)) {
					return true;
				}
			}
			return retreatAndStockUp();
		}

		private boolean retreatAndStockUp() throws GameActionException {
			// don't retreat if we're advancing
			if (!BroadcastInterface.readAttackMode(rc)) {
				// neat, I can nest actions. baller.
				return retreat.run();
			}
			return false;
		}

		private Direction findEnemyDirection() {
			isDirLaunchable = new boolean[Direction.values().length];
			for (Direction d : Util.actualDirections) {
				if (rc.canLaunch(d)) {
					isDirLaunchable[d.ordinal()] = true;
				}
			}
			// find a desireable missile direction
			int effectiveRange = GameConstants.MISSILE_LIFESPAN + 1; // or is this +2 due to splash?
			RobotInfo[] nearby = rc.senseNearbyRobots(effectiveRange * effectiveRange, rc.getTeam().opponent());
			int minDistSq = (effectiveRange + 1) * (effectiveRange + 1);
			Direction bestDir = null;
			for (RobotInfo enemy : nearby) {
				int distSq = enemy.location.distanceSquaredTo(rc.getLocation());
				if (distSq <= minDistSq) {
					// we don't actually check all directions, just the straight direction
					// if that's clogged, launching explosives might not be a smart idea
					Direction dirToward = rc.getLocation().directionTo(enemy.location);
					if (isDirLaunchable[dirToward.ordinal()]) {
						minDistSq = distSq;
						bestDir = dirToward;
						curTarget = enemy.location;
						break;
					}
				}
			}
			// also check the towers and hq
			// (sorry this code is messy. maybe we should add a function or array in Util
			if (bestDir == null) {
				for (MapLocation enemyTower : getEnemyTowerLocations()) {
					int distSq = enemyTower.distanceSquaredTo(rc.getLocation());
					if (distSq <= minDistSq) {
						Direction curDir = rc.getLocation().directionTo(enemyTower);
						if (isDirLaunchable[curDir.ordinal()]) {
							minDistSq = distSq;
							bestDir = curDir;
							curTarget = enemyTower;
						}
					}
				}
			}
			if (bestDir == null) {
				MapLocation enemyHq = getEnemyHqLocation();
				int distSq = enemyHq.distanceSquaredTo(rc.getLocation());
				if (distSq <= minDistSq) {
					Direction curDir = rc.getLocation().directionTo(enemyHq);
					if (isDirLaunchable[curDir.ordinal()]) {
						minDistSq = distSq;
						bestDir = curDir;
						curTarget = enemyHq;
					}
				}
			}
			return bestDir;
		}

		private boolean launchMissiles(Direction enemyDir, MapLocation enemyLoc) throws GameActionException {
			if (rc.isCoreReady()) {
				BroadcastInterface.setLauncherTarget(rc, launcherIndex, enemyLoc);
				for (Direction curDir : Util.getDirectionsStrictlyToward(enemyDir)) {
					if (isDirLaunchable[curDir.ordinal()]) {
						rc.launchMissile(curDir);
					}
				}
				// launching missiles doesn't increment any delays, so we could retreat as soon as we launch
				// that being said, missiles use nearby launcher positions to boostrap their targeting
				// so stay in place
				return true;
			}
			BroadcastInterface.setLauncherTarget(rc, launcherIndex, null);
			return false;
		}
	}

}
