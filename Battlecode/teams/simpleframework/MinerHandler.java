package simpleframework;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class MinerHandler extends RobotHandler {

	protected MinerHandler(RobotController rc) {
		super(rc);
	}

	@Override
	protected void doTheThing() throws GameActionException {
		if (rc.isCoreReady()) {
			// mostly we want to mine
			// mine if there's ore, otherwise continue looping
			if (rc.canMine() && rc.senseOre(rc.getLocation()) > 0) {
				rc.mine();
			} else {
				// if we can't mine, move somewhere else
				// TODO: move towards more minerals
				Direction moveDir = findRandomMoveDirection();
				if (moveDir != null) {
					rc.move(moveDir);
				}
			}
		}
	}

}
