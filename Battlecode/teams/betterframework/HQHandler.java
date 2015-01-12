package betterframework;

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
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
		BroadcastInterface.seedPathfindingQueue(rc);
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
		atBegginningOfTurn();

		LinkedList<Action> result = new LinkedList<Action>();
		result.add(countUnits);
		if (BroadcastInterface.getRobotCount(rc, RobotType.BEAVER) < 10) {
			result.add(makeBeavers);
		}
		return result;
	}

	private void atBegginningOfTurn() throws GameActionException {
		// the HQ is guaranteed to run first
		// so if you want to run code exactly once with a high priority, run it here

		// robots could have died while appending to the queue
		// so first unlock this if anyone is still holding on
		BroadcastInterface.unLockPfq(rc);

		// for debugging, print out a portion of the pathfinding queue
		MapLocation hq = rc.senseHQLocation();
		if (Clock.getRoundNum() % 50 == 0) {
			System.out.println("round: " + Clock.getRoundNum());
			int rad = 10;
			for (int row = -rad; row <= rad; row++) {
				for (int col = -rad; col <= rad; col++) {
					int dist = BroadcastInterface.readDistance(rc, hq.x + col, hq.y + row);
					System.out.print(dist + ", ");
				}
				System.out.println();
			}
		}
	}

	private final Action makeBeavers = new SpawnUnit(RobotType.BEAVER, false);
	private final EnumMap<RobotType, Integer> counts = new EnumMap<RobotType, Integer>(RobotType.class);
	private final Action countUnits = new Action() {
		@Override
		public boolean run() throws GameActionException {
			// the actual max map radius is like 120*120 + 100*100 or something. idk. but this is bigger, so it's okay.
			int MAX_MAP_RADIUS = 100000000;
			RobotInfo[] ourRobots = rc.senseNearbyRobots(MAX_MAP_RADIUS, rc.getTeam());

			// TODO: the following 9 lines consume upwards of 3000 bytecodes, even with only a few robots. what's up with that?
			for (RobotType type : RobotType.values()) {
				counts.put(type, 0);
			}
			for (RobotInfo robot : ourRobots) {
				counts.put(robot.type, counts.get(robot.type) + 1);
			}
			for (RobotType type : RobotType.values()) {
				BroadcastInterface.setRobotCount(rc, type, counts.get(type));
			}

			return false; // this action doesn't increment coredelay
		}
	};
}
