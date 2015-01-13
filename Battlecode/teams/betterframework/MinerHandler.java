package betterframework;

import java.util.LinkedList;
import java.util.List;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class MinerHandler extends BaseRobotHandler {

	protected MinerHandler(RobotController rc) {
		super(rc);
	}

	private final Action mine = new Mine(/* isBeaver= */false);
	private final Action scout = new ScoutOutward();

	// private final Action attack = new Attack();
	// private final Action retreat = new Retreat();
	// private final Action scout = new Scout();

	@Override
	public List<Action> chooseActions() throws GameActionException {
		LinkedList<Action> result = new LinkedList<Action>();
		result.add(mine);
		result.add(scout);
		return result;
	}

}
