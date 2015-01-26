package soldierrush;

import java.util.LinkedList;
import java.util.List;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class ComputerHandler extends BaseRobotHandler {

	protected ComputerHandler(RobotController rc) {
		super(rc);
	}

	@Override
	public List<Action> chooseActions() throws GameActionException {
		return new LinkedList<Action>();
	}

}
