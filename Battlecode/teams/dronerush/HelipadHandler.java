package dronerush;

import java.util.LinkedList;
import java.util.List;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class HelipadHandler extends BaseBuildingHandler {

	protected HelipadHandler(RobotController rc) {
		super(rc);
	}

	@Override
	public List<Action> chooseActions() throws GameActionException {
		LinkedList<Action> result = new LinkedList<Action>();
		if (curStrategy.shouldMakeDrones()) {
			result.add(makeDrone);
		}
		return result;
	}

	private final Action makeDrone = new SpawnUnit(RobotType.DRONE, true);

}
