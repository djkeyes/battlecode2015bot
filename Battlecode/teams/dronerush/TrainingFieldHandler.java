package dronerush;

import java.util.LinkedList;
import java.util.List;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class TrainingFieldHandler extends BaseBuildingHandler {

	protected TrainingFieldHandler(RobotController rc) {
		super(rc);
	}

	@Override
	public List<Action> chooseActions() throws GameActionException {
		LinkedList<Action> result = new LinkedList<>();
		if (BroadcastInterface.getRobotCount(rc, RobotType.COMMANDER, true) < 1) {
			result.add(makeCommander);
		}
		return result;
	}

	private final Action makeCommander = new SpawnUnit(RobotType.COMMANDER, true);
}
