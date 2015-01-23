package dronerush;

import java.util.LinkedList;
import java.util.List;

import dronerush.BaseRobotHandler.Attack;
import dronerush.BaseRobotHandler.MoveTowardEnemyHq;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class MinerHandler extends BaseRobotHandler {

	protected MinerHandler(RobotController rc) {
		super(rc);
	}

	private final Action mine = new Mine(/* isBeaver= */false);
	private final Action scout = new ScoutOutward();
	private final Action attack = new Attack();
	private final Action advance = new MoveTowardEnemyHq(false);

	@Override
	public List<Action> chooseActions() throws GameActionException {
		LinkedList<Action> result = new LinkedList<Action>();
		if (shouldPullTheBoys()) {
			result.add(attack);
			result.add(advance);
		} else {
			result.add(attack);
			result.add(mine);
			result.add(scout);
		}
		return result;
	}

}
