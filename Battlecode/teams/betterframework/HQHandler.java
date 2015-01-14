package betterframework;

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;

import sun.security.action.GetLongAction;

import com.sun.org.apache.bcel.internal.generic.Type;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import betterframework.BaseRobotHandler.Attack;

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

//		// for debugging, print out a portion of the pathfinding queue
//		MapLocation hq = rc.senseHQLocation();
//		if (Clock.getRoundNum() % 50 == 0) {
//			System.out.println("round: " + Clock.getRoundNum());
//			int minRow = -5;
//			int minCol = -5;
//			int maxRow = 5;
//			int maxCol = 5;
//			for (int row = minRow; row <= maxRow; row++) {
//				for (int col = minCol; col <= maxCol; col++) {
//					int curX = hq.x + col;
//					int curY = hq.y + row;
//					int dist = BroadcastInterface.readDistance(rc, curX, curY);
//					System.out.print(dist + ",\t");
//				}
//				System.out.println();
//			}
//		}
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

	private final Action attack = new Attack();
	private final Action makeBeavers = new SpawnUnit(RobotType.BEAVER, false);

}
