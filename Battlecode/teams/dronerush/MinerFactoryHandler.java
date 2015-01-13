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

	@Override
	public List<Action> chooseActions() throws GameActionException {
		LinkedList<Action> result = new LinkedList<Action>();
		if (BroadcastInterface.getRobotCount(rc, RobotType.MINER) < 10) { // TODO: what's thhe optimum number?
			result.add(spawnMiner);
		}
		return result;
	}

	private final Action spawnMiner = new SpawnUnit(RobotType.MINER, false);

}
