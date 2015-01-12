package simpleframework;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class HQHandler extends BuildingHandler {

	private Direction[] randomDirections;

	protected HQHandler(RobotController rc) {
		super(rc);
	}

	@Override
	protected void doTheThing() throws GameActionException {
		if (rc.isCoreReady()) {
			// try attacking, but if there's no one around, make beavers
			if (!attackNearby()) {
				// only make 10 beavers. they kind of suck.
				int numBeavers = BroadcastInterface.getRobotCount(rc, RobotType.BEAVER);
				if (numBeavers < 10) {
					tryToSpawn(RobotType.BEAVER);
				}
			}
		}
	}

}
