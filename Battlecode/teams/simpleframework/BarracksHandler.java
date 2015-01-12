package simpleframework;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class BarracksHandler extends BuildingHandler {

	protected BarracksHandler(RobotController rc) {
		super(rc);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void doTheThing() throws GameActionException {
		if (rc.isCoreReady()) {
			// which is better, soldiers or bashers?
			tryToSpawn(RobotType.SOLDIER);
		}
	}

}
