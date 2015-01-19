package betterframework;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
		BroadcastInterface.setDistance(rc, rc.getLocation().x, rc.getLocation().y, 1);
		BroadcastInterface.enqueuePathfindingQueue(rc, rc.getLocation().x, rc.getLocation().y);

		checkIfRotatedOrReflected();
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
		if (BroadcastInterface.getRobotCount(rc, RobotType.BEAVER) < 10) {
			result.add(makeBeavers);
		}
		return result;
	}

	private void atBeginningOfTurn() throws GameActionException {
		// the HQ is guaranteed to run first
		// so if you want to run code exactly once with a high priority, run it here

		countUnits();

		determineAttackSignal();

		// // for debugging, print out a portion of the pathfinding queue
		// MapLocation hq = rc.senseHQLocation();
		// if (Clock.getRoundNum() % 50 == 0) {
		// System.out.println("round: " + Clock.getRoundNum());
		// int minRow = -5;
		// int minCol = -5;
		// int maxRow = 5;
		// int maxCol = 5;
		// for (int row = minRow; row <= maxRow; row++) {
		// for (int col = minCol; col <= maxCol; col++) {
		// int curX = hq.x + col;
		// int curY = hq.y + row;
		// int dist = BroadcastInterface.readDistance(rc, curX, curY);
		// System.out.print(dist + ",\t");
		// }
		// System.out.println();
		// }
		// }
	}

	private void checkIfRotatedOrReflected() throws GameActionException {
		// infers whether the map is a rotation or a reflection
		// this is relevant for pathfinding and movement

		// we can figure this out at the beginning of the game by comparing the arrangement of our towers to our opponent's
		// however, it IS possible to be wrong. some maps may have identical tower placements whether they are rotated or reflected,
		// and we would need to check the terrain and ore distributions to get a better idea

		MapLocation ourHq = rc.senseHQLocation();
		MapLocation theirHq = rc.senseEnemyHQLocation();
		MapLocation[] ourTowers = rc.senseTowerLocations();
		MapLocation[] theirTowers = rc.senseEnemyTowerLocations();

		// this is represented as a float because we eventually store it in 32-bits in the broadcast array
		float[] midpoint = findMidpoint(ourHq, theirHq);
		boolean isVerticalReflection = checkIsVerticalReflection(midpoint, ourHq, theirHq, ourTowers, theirTowers);
		boolean isHorizontalReflection = checkIsHorizontalReflection(midpoint, ourHq, theirHq, ourTowers, theirTowers);
		// BOTH diagonals
		boolean isDiagonalReflection = checkIsDiagonalReflection(midpoint, ourHq, theirHq, ourTowers, theirTowers);
		boolean isReverseDiagonalReflection = checkIsReverseDiagonalReflection(midpoint, ourHq, theirHq, ourTowers, theirTowers);
		boolean isRotation = checkIsRotation(midpoint, ourHq, theirHq, ourTowers, theirTowers);
		BroadcastInterface.setMapConfiguration(rc, midpoint, Util.encodeMapConfigurationAsBitmask(isVerticalReflection,
				isHorizontalReflection, isDiagonalReflection, isReverseDiagonalReflection, isRotation));
	}

	private float[] findMidpoint(MapLocation ourHq, MapLocation theirHq) {
		float x = midpoint(ourHq.x, theirHq.x);
		float y = midpoint(ourHq.y, theirHq.y);
		return new float[] { x, y };
	}

	private float midpoint(float x1, float x2) {
		return (x1 + x2) / 2f;
	}

	private boolean checkIsVerticalReflection(float[] midpoint, MapLocation ourHq, MapLocation theirHq, MapLocation[] ourTowers,
			MapLocation[] theirTowers) {
		// to be a vertical reflection, we should be able to flip every tower and hq around the y-midpoint and get an enemy tower
		float y = midpoint[1];
		// our hq already matches midpoint, we just need to check the x-coordinate
		if (ourHq.x != theirHq.x) {
			return false;
		}

		for (MapLocation ourTower : ourTowers) {
			boolean reflectionFound = false;
			for (MapLocation enemyTower : theirTowers) {
				if (enemyTower.x == ourTower.x && Math.abs(midpoint(ourTower.y, enemyTower.y) - y) < Util.F_EPSILON) {
					reflectionFound = true;
					break;
				}
			}
			if (!reflectionFound) {
				return false;
			}
		}

		return true;
	}

	private boolean checkIsHorizontalReflection(float[] midpoint, MapLocation ourHq, MapLocation theirHq,
			MapLocation[] ourTowers, MapLocation[] theirTowers) {
		// this is basically the same as the vertical reflection
		float x = midpoint[0];
		// our hq already matches midpoint, we just need to check the y-coordinate
		if (ourHq.y != theirHq.y) {
			return false;
		}

		for (MapLocation ourTower : ourTowers) {
			boolean reflectionFound = false;
			for (MapLocation enemyTower : theirTowers) {
				if (enemyTower.y == ourTower.y && Math.abs(midpoint(ourTower.x, enemyTower.x) - x) < Util.F_EPSILON) {
					reflectionFound = true;
					break;
				}
			}
			if (!reflectionFound) {
				return false;
			}
		}

		return true;
	}

	private boolean checkIsDiagonalReflection(float[] midpoint, MapLocation ourHq, MapLocation theirHq, MapLocation[] ourTowers,
			MapLocation[] theirTowers) {
		// this is a little trickier than horizontal and vertical reflections
		// (x1, y1) and (x2, y2) are diagonally flipped around (mx, my) if
		// x1 - mx == y2 - my
		// AND
		// x2 - mx == y1 - my
		float mx = midpoint[0];
		float my = midpoint[1];

		boolean cond1 = Math.abs((ourHq.x - mx) - (theirHq.y - my)) < Util.F_EPSILON;
		boolean cond2 = Math.abs((theirHq.x - mx) - (ourHq.y - my)) < Util.F_EPSILON;
		if (!(cond1 && cond2)) {
			return false;
		}

		for (MapLocation ourTower : ourTowers) {
			boolean reflectionFound = false;
			for (MapLocation enemyTower : theirTowers) {
				// written on separate lines for clarity
				cond1 = Math.abs((ourTower.x - mx) - (enemyTower.y - my)) < Util.F_EPSILON;
				cond2 = Math.abs((enemyTower.x - mx) - (ourTower.y - my)) < Util.F_EPSILON;
				if (cond1 && cond2) {
					reflectionFound = true;
					break;
				}
			}
			if (!reflectionFound) {
				return false;
			}
		}

		return true;
	}

	private boolean checkIsReverseDiagonalReflection(float[] midpoint, MapLocation ourHq, MapLocation theirHq,
			MapLocation[] ourTowers, MapLocation[] theirTowers) {
		// slightly different from the original diagonal reflection
		// previous one was flipped around 45 degrees, this one is flipped around 135 degrees
		// this is a little trickier than horizontal and vertical reflections
		// (x1, y1) and (x2, y2) are diagonally flipped around (mx, my) if
		// mx - x1 == y2 - my
		// AND
		// x2 - mx == my - y1
		float mx = midpoint[0];
		float my = midpoint[1];

		boolean cond1 = Math.abs((mx - ourHq.x) - (theirHq.y - my)) < Util.F_EPSILON;
		boolean cond2 = Math.abs((theirHq.x - mx) - (my - ourHq.y)) < Util.F_EPSILON;
		if (!(cond1 && cond2)) {
			return false;
		}

		for (MapLocation ourTower : ourTowers) {
			boolean reflectionFound = false;
			for (MapLocation enemyTower : theirTowers) {
				// written on separate lines for clarity
				cond1 = Math.abs((mx - ourTower.x) - (enemyTower.y - my)) < Util.F_EPSILON;
				cond2 = Math.abs((enemyTower.x - mx) - (my - ourTower.y)) < Util.F_EPSILON;
				if (cond1 && cond2) {
					reflectionFound = true;
					break;
				}
			}
			if (!reflectionFound) {
				return false;
			}
		}

		return true;
	}

	private boolean checkIsRotation(float[] midpoint, MapLocation ourHq, MapLocation theirHq, MapLocation[] ourTowers,
			MapLocation[] theirTowers) {
		// we only need to check 180 degree rotations. 90- and 270-degrees are verboten.
		// this is fairly straightforward to derive, but here are the equations we'll be checking:
		// x1 +x2 == 2*mx
		// y1 +y2 == 2*my
		double mx = midpoint[0];
		double my = midpoint[1];

		boolean cond1 = (ourHq.x + theirHq.x - 2 * mx) < Util.F_EPSILON;
		boolean cond2 = (ourHq.y + theirHq.y - 2 * my) < Util.F_EPSILON;
		if (!(cond1 && cond2)) {
			return false;
		}

		for (MapLocation ourTower : ourTowers) {
			boolean rotationFound = false;
			for (MapLocation enemyTower : theirTowers) {
				// written on separate lines for clarity
				cond1 = (ourTower.x + enemyTower.x - 2 * mx) < Util.F_EPSILON;
				cond2 = (ourTower.y + enemyTower.y - 2 * my) < Util.F_EPSILON;
				if (cond1 && cond2) {
					rotationFound = true;
					break;
				}
			}
			if (!rotationFound) {
				return false;
			}
		}

		return true;
	}

	private static final int DRONES_NEEDED_TO_CHARGE = 30;
	private static final int DRONES_NEEDED_TO_RETREAT = 20;

	private void determineAttackSignal() throws GameActionException {
		boolean isSet = BroadcastInterface.readAttackMode(rc);
		if (isSet) {
			if (BroadcastInterface.getRobotCount(rc, RobotType.DRONE) <= DRONES_NEEDED_TO_RETREAT) {
				BroadcastInterface.setAttackMode(rc, false);
			}
		} else {
			if (BroadcastInterface.getRobotCount(rc, RobotType.DRONE) >= DRONES_NEEDED_TO_CHARGE) {
				BroadcastInterface.setAttackMode(rc, true);
			}
		}
	}

	// it turns out EnumMaps really suck. they cost like 5x more bytecodes.
	private final int[] counts = new int[RobotType.values().length];

	private final RobotType[] releventTypes = { RobotType.BEAVER, RobotType.MINER, RobotType.SOLDIER, RobotType.DRONE,
			RobotType.TANK, RobotType.MINERFACTORY, RobotType.BARRACKS, RobotType.HELIPAD, RobotType.TANKFACTORY, };

	public void countUnits() throws GameActionException {
		// the actual max map radius is like 120*120 + 100*100 or something. idk. but this is bigger, so it's okay.
		int MAX_MAP_RADIUS = 100000000;
		RobotInfo[] ourRobots = rc.senseNearbyRobots(MAX_MAP_RADIUS, rc.getTeam());

		for (RobotType type : releventTypes) {
			counts[type.ordinal()] = 0;
		}
		for (RobotInfo robot : ourRobots) {
			counts[robot.type.ordinal()]++;
		}
		for (RobotType type : releventTypes) {
			BroadcastInterface.setRobotCount(rc, type, counts[type.ordinal()]);
		}
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
			int numTowers = rc.senseTowerLocations().length;
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
