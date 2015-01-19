package betterframework;

import java.util.LinkedList;
import java.util.List;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class DroneHandler extends BaseRobotHandler {

	protected DroneHandler(RobotController rc) {
		super(rc);
	}

	@Override
	public List<Action> chooseActions() throws GameActionException {
		LinkedList<Action> result = new LinkedList<Action>();
		if (BroadcastInterface.readAttackMode(rc)) {
			// if there are enough drones, charge the opponent!
			// TODO: attack towers first
			if (rc.isWeaponReady()) {
				result.add(attack);
				result.add(charge);
			} else {
				result.add(retreat);
			}
		} else {
			// else skirt the towers and hq so that we can kill other units
			result.add(attack);
			result.add(retreat);
		}
		return result;
	}

	private final Action attack = new Attack();

	private final Action charge = new MoveTo(rc.senseEnemyHQLocation());
	private final Action retreat = new MoveTo(rc.senseHQLocation());

	// drones can fly over obstacles, so they don't need to rely on the BFS results
	// therefore, we'll just use a simple movement implementation
	private class MoveTo implements Action {
		private MapLocation target;

		public MoveTo(MapLocation target) {
			this.target = target;
		}

		@Override
		public boolean run() throws GameActionException {
			if (rc.isCoreReady()) {
				for (Direction d : Util.getDirectionsToward(rc.getLocation(), target)) {
					if (rc.canMove(d)) {
						rc.move(d);
						return true;
					}
				}
			}
			return false;
		}

	}
}
