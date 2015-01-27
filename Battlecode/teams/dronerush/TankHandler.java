package dronerush;

import java.util.LinkedList;
import java.util.List;

import dronerush.BaseRobotHandler.MoveTowardEnemyHq;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class TankHandler extends BaseRobotHandler {

	protected TankHandler(RobotController rc) {
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
			result.add(attack);
			result.add(defend);
		}
		return result;
	}

	// TODO: tanks have a large loading delay, so retreating would make them attack even more slowly. maybe they shouldn't retreat
	// during weapon cooldown
	// that being said, they outrange most things, so staying out of enemy range is kind of important.
	private final Action attack = new AttackCautiously(/* retreatOnWeaponCooldown */true);
	private final Action attackMaxDps = new AttackCautiously(false);
	private final Action advance = new AttackInAWave();
	private final Action defend = new Defend();

}
