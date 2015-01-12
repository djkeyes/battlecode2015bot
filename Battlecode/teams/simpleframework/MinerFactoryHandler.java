package simpleframework;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class MinerFactoryHandler extends BuildingHandler {

	protected MinerFactoryHandler(RobotController rc) {
		super(rc);
	}

	@Override
	protected void doTheThing() throws GameActionException {
		// default behavior: just make miners whenever possible
		if (rc.isCoreReady()) {
			// miners are great, but let's not make more than necessary
			int numBeavers = BroadcastInterface.getRobotCount(rc, RobotType.MINER);
			if (numBeavers < 40) {
				tryToSpawn(RobotType.MINER);
			}
		}
	}

}
