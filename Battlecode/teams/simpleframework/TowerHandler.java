package simpleframework;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class TowerHandler extends RobotHandler {

	protected TowerHandler(RobotController rc) {
		super(rc);
	}

	@Override
	protected void doTheThing() throws GameActionException {
		attackNearby();
	}

}
