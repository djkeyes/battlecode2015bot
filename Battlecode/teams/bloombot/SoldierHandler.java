package bloombot;

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

		// until the attack bit is set, scout toward the opponent
		if (BroadcastInterface.readAttackMode(rc)) {
			result.add(attackInAWave);
		} else {
			result.add(attackMaxDps);
			result.add(advance);
		}
		return result;
	}

	private final Action attackMaxDps = new AttackCautiously(false);
	private final Action attackInAWave = new AttackInAWave();
	private final Action advance = new MoveTo(getEnemyHqLocation(), true, true);
}
