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
		LinkedList<Action> result = new LinkedList<Action>();
		if (curStrategy.shouldMakeSoldiers() || rc.getTeamOre() > 1000) {
			result.add(makeSoldier);
		} else if (curStrategy.shouldMakeBashers()) {
			result.add(makeBasher);
		}
		return result;
	}

	private final Action makeBasher = new SpawnUnit(RobotType.BASHER, true);
	private final Action makeSoldier = new SpawnUnit(RobotType.SOLDIER, true);

}
