package simpleframework;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public abstract class BuildingHandler extends RobotHandler {

	protected BuildingHandler(RobotController rc) {
		super(rc);
	}

	public boolean tryToSpawn(RobotType type) throws GameActionException {
		// find a free direction to spawn
		Direction spawnDir = findRandomSpawningDirection(type);
		if (spawnDir != null) {
			rc.spawn(spawnDir, type);
			return true;
		}
		return false;
	}

}
