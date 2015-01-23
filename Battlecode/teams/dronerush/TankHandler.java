package dronerush;

import java.util.LinkedList;
import java.util.List;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class TankHandler extends BaseRobotHandler {

	protected TankHandler(RobotController rc) {
		super(rc);
	}

	@Override
	public List<Action> chooseActions() throws GameActionException {
		LinkedList<Action> result = new LinkedList<Action>();
		result.add(attack);
		
		// until the attack bit is set, just hang around at home
		if (BroadcastInterface.readAttackMode(rc)) {
			result.add(advance);
		} else {
			// TODO: gather at some kind of central point
			result.add(retreat);
		}
		return result;
	}
	
	private final Action attack = new Attack();
	private final Action advance = new MoveTowardEnemyHq(false);
	private final Action retreat = new Retreat();
}
