package bloombot;

import java.util.LinkedList;
import java.util.List;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class TechnologyInstituteHandler extends BaseBuildingHandler {

	protected TechnologyInstituteHandler(RobotController rc) {
		super(rc);
	}

	@Override
	public List<Action> chooseActions() throws GameActionException {
		// we don't need computers
		// computer are for chumps.
		return new LinkedList<Action>();
	}

}
