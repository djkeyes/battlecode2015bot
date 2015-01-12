package betterframework;

import java.util.LinkedList;
import java.util.List;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class MissileHandler extends BaseRobotHandler {

	protected MissileHandler(RobotController rc) {
		super(rc);
	}

	@Override
	public List<Action> chooseActions() throws GameActionException {
		// TODO
		return new LinkedList<Action>();
	}

}
