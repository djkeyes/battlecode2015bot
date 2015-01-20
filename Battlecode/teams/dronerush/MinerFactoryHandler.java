package dronerush;

import java.util.LinkedList;
import java.util.List;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class MinerFactoryHandler extends BaseBuildingHandler {

	protected MinerFactoryHandler(RobotController rc) {
		super(rc);
	}

	// TODO
	// I just made the numbers up.
	// We should do some kind of map-based testing to figure out what are actually good values
	private final int MINIMUM_MINER_COUNT = 10;
	private final int MAXIMUM_MINER_COUNT = 30;
	private final double BUILD_MINER_MINIMUM_FEEDBACK_RATIO = 0.3;

	private boolean shouldBuildMoreMiners() throws GameActionException {
		int curMinerCount = BroadcastInterface.getRobotCount(rc, RobotType.MINER);
		if (curMinerCount < MINIMUM_MINER_COUNT) {
			return true;
		}
		if (curMinerCount >= MAXIMUM_MINER_COUNT) {
			return false;
		}

		int numMinersWithLotsOfOre = BroadcastInterface.readPreviousRoundAbundantOre(rc);
		double fractionOfMinersWithLotsOfOre = (double) numMinersWithLotsOfOre / curMinerCount;
		if (fractionOfMinersWithLotsOfOre > BUILD_MINER_MINIMUM_FEEDBACK_RATIO) {
			return true;
		}

		return false;
	}

	@Override
	public List<Action> chooseActions() throws GameActionException {
		LinkedList<Action> result = new LinkedList<Action>();
		if (shouldBuildMoreMiners()) {
			result.add(spawnMiner);
		}
		return result;
	}

	private final Action spawnMiner = new SpawnUnit(RobotType.MINER, false);

}
