package dronerush;

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
		// seed the distances
		BroadcastInterface.setDistance(rc, rc.getLocation().x, rc.getLocation().y, 1);
	}

	@Override
	public int maxBytecodesToUse() {
		return 9001;
	}

	@Override
	public void onExcessBytecodes() throws GameActionException {
		doPathfinding();
	}

	@Override
	public List<Action> chooseActions() throws GameActionException {
		atBeginningOfTurn();

		LinkedList<Action> result = new LinkedList<Action>();
		result.add(attack);
		if (BroadcastInterface.getRobotCount(rc, RobotType.BEAVER) < 3) {
			result.add(makeBeavers);
		}
		return result;
	}

	private void atBeginningOfTurn() throws GameActionException {
		// the HQ is guaranteed to run first
		// so if you want to run code exactly once with a high priority, run it here

		countUnits();

		determineAttackSignal();

		// for debugging, print out a portion of the pathfinding queue
		// MapLocation hq = rc.senseHQLocation();
		// if (Clock.getRoundNum() % 50 == 3) {
		// System.out.println("round: " + Clock.getRoundNum());
		// BroadcastInterface.printPfq(rc);
		// int minRow = -13;
		// int minCol = -7;
		// int maxRow = 7;
		// int maxCol = 13;
		// for (int row = minRow; row <= maxRow; row++) {
		// for (int col = minCol; col <= maxCol; col++) {
		// int curX = hq.x + col;
		// int curY = hq.y + row;
		// int dist = BroadcastInterface.readDistance(rc, curX, curY);
		// // boolean inQueue = false;
		// // LinkedList<int[]> pfq = BroadcastInterface.getPfq(rc);
		// // for (int[] coord : pfq) {
		// // if (coord[0] == curX && coord[1] == curY) {
		// // inQueue = true;
		// // break;
		// // }
		// // }
		//
		// System.out.print(dist + /* (inQueue ? "*" : "") + */",\t");
		// }
		// System.out.println();
		// }
		// }
	}

	private static final int DRONES_NEEDED_TO_CHARGE = 50;
	private static final int DRONES_NEEDED_TO_RETREAT = 45;

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
