package betterframework;

import java.util.LinkedList;
import java.util.List;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import betterframework.BaseRobotHandler.Attack;

public class TowerHandler extends BaseRobotHandler {

	protected TowerHandler(RobotController rc) {
		super(rc);
	}

	@Override
	public List<Action> chooseActions() throws GameActionException {
		LinkedList<Action> result = new LinkedList<Action>();
		result.add(attack);
		return result;
	}

	private final Action attack = new Attack();
}
