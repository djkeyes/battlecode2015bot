package dronerush;

import java.util.LinkedList;
import java.util.List;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class HQHandler extends BaseBuildingHandler {

	protected HQHandler(RobotController rc) {
		super(rc);
	}

	@Override
	public void init() throws GameActionException {
		// seed the distances for pathfinding
		BroadcastInterface.setDistance(rc, rc.getLocation().x, rc.getLocation().y, 1, rc.getLocation());
		BroadcastInterface.enqueuePathfindingQueue(rc, rc.getLocation().x, rc.getLocation().y);

		checkIfRotatedOrReflected();
	}

	private void atBeginningOfTurn() throws GameActionException {
		// the HQ is guaranteed to run first
		// so if you want to run code exactly once with a high priority, run it here

		countUnitsAndCheckSupply();

		determineAttackSignal();
		determineShouldPullTheBoys();

		BroadcastInterface.resetAbundantOre(rc);
	}

	private void determineShouldPullTheBoys() throws GameActionException {
		boolean isSet = BroadcastInterface.readPullBoysMode(rc);
		if (!isSet) {
			// also factor in time to reach opponent
			int bfsDist = getDistanceFromEnemyHq(getOurHqLocation());
			int euclidianDist = (int) Math.sqrt(getOurHqLocation().distanceSquaredTo(getEnemyHqLocation()));
			// the 3 here is to factor in movement delay. most units cost 2 when supplied.
			int maxDist = 2 * Math.max(bfsDist, euclidianDist);

			if (rc.getRoundLimit() - Clock.getRoundNum() < 200 + maxDist) {
				BroadcastInterface.setPullBoysMode(rc, true);
			}
		}
	}

	@Override
	public int maxBytecodesToUse() {
		return 9001;
	}

	@Override
	public List<Action> chooseActions() throws GameActionException {
		atBeginningOfTurn();

		LinkedList<Action> result = new LinkedList<Action>();
		result.add(attack);
		if (BroadcastInterface.getRobotCount(rc, RobotType.BEAVER, true) < 2) {
			result.add(makeBeavers);
		}
		return result;
	}

	@Override
	public boolean distributeSupply() throws GameActionException {
		// drones are our primary supply distribution mechanism, so if there are some nearby drones, give them lots of supply
		RobotInfo[] nearby = rc.senseNearbyRobots(8, rc.getTeam());
		for (RobotInfo robot : nearby) {
			if (robot.type == RobotType.DRONE) {
				rc.transferSupplies((int) rc.getSupplyLevel(), robot.location);
				return true;
			}
		}

		return super.distributeSupply();
	}

	private void checkIfRotatedOrReflected() throws GameActionException {
		// infers whether the map is a rotation or a reflection
		// this is relevant for pathfinding and movement

		// we can figure this out at the beginning of the game by comparing the arrangement of our towers to our opponent's
		// however, it IS possible to be wrong. some maps may have identical tower placements whether they are rotated or reflected,
		// and we would need to check the terrain and ore distributions to get a better idea

		MapLocation ourHq = rc.getLocation();
		MapLocation theirHq = getEnemyHqLocation();
		MapLocation[] ourTowers = getOurTowerLocations();
		MapLocation[] theirTowers = getEnemyTowerLocations();

		// this is represented as a float because we eventually store it in 32-bits in the broadcast array
		float[] midpoint = Util.findMidpoint(ourHq, theirHq);
		boolean isVerticalReflection = Util.checkIsVerticalReflection(midpoint, ourHq, theirHq, ourTowers, theirTowers);
		boolean isHorizontalReflection = Util.checkIsHorizontalReflection(midpoint, ourHq, theirHq, ourTowers, theirTowers);
		// BOTH diagonals
		boolean isDiagonalReflection = Util.checkIsDiagonalReflection(midpoint, ourHq, theirHq, ourTowers, theirTowers);
		boolean isReverseDiagonalReflection = Util.checkIsReverseDiagonalReflection(midpoint, ourHq, theirHq, ourTowers,
				theirTowers);
		boolean isRotation = Util.checkIsRotation(midpoint, ourHq, theirHq, ourTowers, theirTowers);
		BroadcastInterface.setMapConfiguration(rc, midpoint, Util.encodeMapConfigurationAsBitmask(isVerticalReflection,
				isHorizontalReflection, isDiagonalReflection, isReverseDiagonalReflection, isRotation));
	}

	private final int TANKS_NEEDED_TO_ATTACK = 15;
	private final int BASHERS_NEEDED_TO_ATTACK = 10;
	private final int LAUNCHERS_NEEDED_TO_ATTACK = 5;
	private final int TANKS_NEEDED_TO_RETREAT = 5;
	private final int BASHERS_NEEDED_TO_RETREAT = 2;
	private final int LAUNCHERS_NEEDED_TO_RETREAT = 2;

	private void determineAttackSignal() throws GameActionException {
		boolean isSet = BroadcastInterface.readAttackMode(rc);
		int tankCount = BroadcastInterface.getRobotCount(rc, RobotType.TANK, true);
		int basherCount = BroadcastInterface.getRobotCount(rc, RobotType.BASHER, true);
		int launcherCount = BroadcastInterface.getRobotCount(rc, RobotType.LAUNCHER, true);
		boolean isTimeToPullTheBoys = shouldPullTheBoys();
		if (isSet) {
			boolean shouldRetreat = (tankCount <= TANKS_NEEDED_TO_RETREAT) && (basherCount <= BASHERS_NEEDED_TO_RETREAT)
					&& launcherCount <= (LAUNCHERS_NEEDED_TO_RETREAT) && !isTimeToPullTheBoys;
			if (shouldRetreat) {
				BroadcastInterface.setAttackMode(rc, false);
			}

		} else {
			boolean shouldAttack = ((tankCount >= TANKS_NEEDED_TO_ATTACK) && (basherCount >= BASHERS_NEEDED_TO_ATTACK) && launcherCount >= (LAUNCHERS_NEEDED_TO_ATTACK))
					|| isTimeToPullTheBoys;
			if (shouldAttack) {
				BroadcastInterface.setAttackMode(rc, true);
			}
		}
	}

	// it turns out EnumMaps really suck. they cost like 5x more bytecodes.
	private final int[] allyCounts = new int[RobotType.values().length];
	private final int[] enemyCounts = new int[RobotType.values().length];

	// we need to factor in that robots will always use some extra bytecodes
	// that being said, we won't supply *everyone*, just a fraction of our army
	private final double excessSupplyFactor = 1.1;
	private final double fractionToKeepSupplied = 0.6;

	public void countUnitsAndCheckSupply() throws GameActionException {
		// the actual max map radius is like 120*120 + 100*100 or something. idk. but this is bigger, so it's okay.
		int MAX_MAP_RADIUS = 100000000;
		RobotInfo[] allRobots = rc.senseNearbyRobots(MAX_MAP_RADIUS);

		for (int i = 0; i < allyCounts.length; i++) {
			allyCounts[i] = 0;
			enemyCounts[i] = 0;
		}
		for (RobotInfo robot : allRobots) {
			if (robot.team == rc.getTeam()) {
				allyCounts[robot.type.ordinal()]++;
			} else {
				enemyCounts[robot.type.ordinal()]++;
			}
		}
		int supplyUpkeepNeeded = 0;
		for (RobotType type : RobotType.values()) {
			BroadcastInterface.setRobotCount(rc, type, allyCounts[type.ordinal()], true);
			supplyUpkeepNeeded += allyCounts[type.ordinal()] * type.supplyUpkeep;

			BroadcastInterface.setRobotCount(rc, type, enemyCounts[type.ordinal()], false);
		}
		double currentSupplyOutput = GameConstants.SUPPLY_GEN_BASE
				* (GameConstants.SUPPLY_GEN_MULTIPLIER + Math.pow(allyCounts[RobotType.SUPPLYDEPOT.ordinal()],
						GameConstants.SUPPLY_GEN_EXPONENT));
		BroadcastInterface.setBuildMoreSupplyDepots(rc, currentSupplyOutput < supplyUpkeepNeeded * excessSupplyFactor
				* fractionToKeepSupplied);
	}

	private final Action attack = new HqAttack();
	private final Action makeBeavers = new SpawnUnit(RobotType.BEAVER, false);

	private final class HqAttack implements Action {
		@Override
		public boolean run() throws GameActionException {
			// the HQ differs from normal attacks in that its range is sometimes a little longer, even more so with AOE
			// unfortunately due to integer rounding, we can't just add together the two ranges like (sqrt(35)+sqrt(2))^2
			// for example, the HQ can't hit an object at sq range 49 on the horizontal, but it CAN hit an object at sq range 50 on the
			// diagonal
			// so we have a little more logic to handle that
			int numTowers = getOurTowerLocations().length;
			boolean hasRangeBuff = numTowers >= 2;
			boolean hasAoeBuff = numTowers >= 5;
			int actualRangeSq;
			int sensingRangeSq;
			if (hasAoeBuff) {
				// draw out the ranges yourself if you want to verify this number
				// this needs to be updated if hq ranges change
				sensingRangeSq = 53;
				actualRangeSq = GameConstants.HQ_BUFFED_ATTACK_RADIUS_SQUARED;
			} else if (hasRangeBuff) {
				sensingRangeSq = actualRangeSq = GameConstants.HQ_BUFFED_ATTACK_RADIUS_SQUARED;
			} else {
				sensingRangeSq = actualRangeSq = RobotType.HQ.attackRadiusSquared;
			}

			RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(rc.getLocation(), sensingRangeSq, rc.getTeam().opponent());
			if (nearbyEnemies.length > 0 && rc.isWeaponReady()) {
				MapLocation enemyLoc = null;
				MapLocation targetLoc = null;

				double minHealth = Integer.MAX_VALUE;
				for (RobotInfo info : nearbyEnemies) {
					if (info.health < minHealth) {
						enemyLoc = info.location;
						minHealth = info.health;

						int distSq = rc.getLocation().distanceSquaredTo(enemyLoc);
						if (distSq > actualRangeSq) { // this may happen if we need to use AOE
							targetLoc = enemyLoc.add(enemyLoc.directionTo(rc.getLocation()));
						} else {
							targetLoc = enemyLoc;
						}
					}
				}
				if (rc.canAttackLocation(targetLoc)) {
					rc.attackLocation(targetLoc);
					return true;
				}
			}
			return false;
		}
	}
}
