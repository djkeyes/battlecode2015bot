package dronerush;

import java.util.LinkedList;
import java.util.List;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class TankFactoryHandler extends BaseBuildingHandler {

	protected TankFactoryHandler(RobotController rc) {
		super(rc);
	}


	@Override
	public List<Action> chooseActions() throws GameActionException {
		LinkedList<Action> result = new LinkedList<Action>();
		return result;
	}

}
