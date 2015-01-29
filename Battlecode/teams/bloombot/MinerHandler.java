package bloombot;

import java.util.LinkedList;
import java.util.List;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class MinerHandler extends BaseRobotHandler {

	protected MinerHandler(RobotController rc) {
		super(rc);
	}

	private final Action mine = new Mine(/* isBeaver= */false);
	private final Action attack = new Attack();
	private final Action scoutTowardOpponent = new MoveToFrontier();

	@Override
	public List<Action> chooseActions() throws GameActionException {
		LinkedList<Action> result = new LinkedList<Action>();
		result.add(attack);
		result.add(mine);
		result.add(scoutTowardOpponent);
		return result;
	}

}
