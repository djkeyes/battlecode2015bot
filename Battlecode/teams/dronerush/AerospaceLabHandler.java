package dronerush;

import java.util.LinkedList;
import java.util.List;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class AerospaceLabHandler extends BaseBuildingHandler {

	protected AerospaceLabHandler(RobotController rc) {
		super(rc);
	}

	@Override
	public List<Action> chooseActions() throws GameActionException {
		LinkedList<Action> result = new LinkedList<Action>();
		if (Strategy.shouldMakeLaunchers(rc)) {
			result.add(makeLauncher);
		}
		return result;
	}

	private final Action makeLauncher = new SpawnUnit(RobotType.LAUNCHER, true);

}
