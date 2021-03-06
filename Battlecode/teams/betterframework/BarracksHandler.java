package betterframework;

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
		// production buildings are expensive, and we need to save up money.
		// so don't produce units until we have enough production
		LinkedList<Action> result = new LinkedList<Action>();
		if (BroadcastInterface.getRobotCount(rc, RobotType.TANKFACTORY) >= 1) {
			result.add(makeSoldier);
		}
		return result;
	}

	private final Action makeSoldier = new SpawnUnit(RobotType.SOLDIER, true);
}
