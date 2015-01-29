package dronerush;

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

		// until the attack bit is set, just hang around at home
		if (BroadcastInterface.readAttackMode(rc)) {
			result.add(attackMaxDps);
			result.add(advance);
		} else {
			result.add(attackMaxDps);
			result.add(defend);
		}
		return result;
	}

	private final Action attackMaxDps = new AttackCautiously(false);
	private final Action advance = new AttackInAWave();
	private final Action defend = new Defend();
}
