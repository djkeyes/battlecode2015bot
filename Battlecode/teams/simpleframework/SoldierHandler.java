package simpleframework;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class SoldierHandler extends RobotHandler {

	protected SoldierHandler(RobotController rc) {
		super(rc);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void doTheThing() throws GameActionException {
		if (rc.isCoreReady()) {
			if (!attackNearby()) {
				// move randomly
				Direction moveDir = findRandomMoveDirection();
				if (moveDir != null) {
					rc.move(moveDir);
				}
			}
		}
	}

}
