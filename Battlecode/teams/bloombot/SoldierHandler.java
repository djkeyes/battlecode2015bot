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

		result.add(attackMaxDps);

		// until we have bfs results, scout toward the opponent
		if (BroadcastInterface.findDirectionToHq(rc) != null) {
			// once the results are in through, head over
			// if there's a tower in the way, bunch up before advancing
			// TODO: this calls BroadcastInterface.findDirectionToHq(rc) twice, which is like 50 unneeded bytecodes
			result.add(attackInAWave);
		} else {
			result.add(attackMaxDps);
			result.add(scout);
		}
		return result;
	}

	private final Action attackMaxDps = new AttackCautiously(false);
	private final Action attackInAWave = new AttackInAWave();
	private final Action scout = new MoveToFrontier();
}
