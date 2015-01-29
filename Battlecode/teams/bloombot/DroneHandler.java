package bloombot;

import java.util.LinkedList;
import java.util.List;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class DroneHandler extends BaseRobotHandler {

	protected DroneHandler(RobotController rc) {
		super(rc);
	}

	@Override
	public List<Action> chooseActions() throws GameActionException {
		LinkedList<Action> result = new LinkedList<Action>();
		return result;
	}

}
