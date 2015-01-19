package betterframework;

import java.util.LinkedList;
import java.util.List;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class SoldierHandler extends BaseRobotHandler {

	protected SoldierHandler(RobotController rc) {
		super(rc);
	}

	@Override
	public List<Action> chooseActions() throws GameActionException {
		LinkedList<Action> result = new LinkedList<Action>();
		result.add(attack);
		// defending is weird. it doesn't make sense to stay in one place, but it doesn't make sense to move far away either. as a
		// happy medium, do on a coin flip.
		if (BroadcastInterface.readAttackMode(rc) || gen.nextDouble() < 0.5) {
			result.add(advance);
			result.add(scout);
		} else {
			result.add(retreat);
		}
		return result;
	}

	private final Action attack = new Attack();
	private final Action scout = new ScoutOutward();
	private final Action advance = new MoveTowardEnemyHq();
	private final Action retreat = new Retreat();
}
