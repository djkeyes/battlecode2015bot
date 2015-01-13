package dronerush;

import java.util.LinkedList;
import java.util.List;

import dronerush.BaseBuildingHandler.SpawnUnit;
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
		if (BroadcastInterface.getRobotCount(rc, RobotType.DRONE) > 20) {
			// imho, soldiers are better than bashers
			result.add(makeLauncher);
		}
		return result;
	}

	private final Action makeLauncher = new SpawnUnit(RobotType.LAUNCHER, true);

}
