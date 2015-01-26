package dronerush;

import java.util.LinkedList;
import java.util.List;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class BarracksHandler extends BaseBuildingHandler {

	protected BarracksHandler(RobotController rc) {
		super(rc);
	}

	@Override
	public List<Action> chooseActions() throws GameActionException {
		int numTankFactories = BroadcastInterface.getRobotCount(rc, RobotType.TANKFACTORY);
		int numTanks = BroadcastInterface.getRobotCount(rc, RobotType.TANK);

		LinkedList<Action> result = new LinkedList<Action>();
		// bashers suck for early rush defense. their real purpose in life is dealing with clumps of launchers.
		// so don't bother making them until we have some bulkier units out first.
		if (numTankFactories >= 1 && numTanks >= 4) {
			if (Strategy.shouldMakeBashers(rc)) {
				result.add(makeBasher);
			}
		}
		return result;
	}

	private final Action makeBasher = new SpawnUnit(RobotType.BASHER, true);

}
