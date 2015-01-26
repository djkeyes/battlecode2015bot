package soldierrush;

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
		LinkedList<Action> result = new LinkedList<Action>();
		if (Strategy.shouldMakeSoldiers(rc)) {
			result.add(makeSoldier);
		}
		return result;
	}

	private final Action makeSoldier = new SpawnUnit(RobotType.SOLDIER, true);

}
