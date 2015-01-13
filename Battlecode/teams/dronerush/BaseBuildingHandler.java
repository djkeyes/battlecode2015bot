package dronerush;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public abstract class BaseBuildingHandler extends BaseRobotHandler {

	BaseBuildingHandler(RobotController rc) {
		super(rc);
	}

	// TODO: maybe this should accept a predicate function? like: new SpawnUnitUntilPredicate(new Predicate(){});
	// that way we could spawn beavers until we have 30 beavers. or we could spawn soldiers and bashers until there's a 3:1 ratio
	class SpawnUnit implements Action {
		private RobotType type;
		private boolean spawnTowardEnemy;

		SpawnUnit(RobotType type, boolean spawnTowardEnemy) {
			this.type = type;
			this.spawnTowardEnemy = spawnTowardEnemy;
		}

		@Override
		public boolean run() throws GameActionException {
			if (rc.isCoreReady()) {
				// find a free direction to spawn
				Direction spawnDir = null;
				if (spawnTowardEnemy) {
					spawnDir = findSpawnTowardEnemy(type);
				} else {
					spawnDir = findSpawnAwayFromEnemy(type);
				}
				if (spawnDir != null) {
					rc.spawn(spawnDir, type);
					return true;
				}
			}
			return false;
		}

		private Direction findSpawnAwayFromEnemy(RobotType spawnType) throws GameActionException {
			// find a spawn location that's far from the enemy

			// check all the adjacent squares for one that's unblocked AND furthest from the opponent
			Direction ans = null;
			int maxDist = -1;
			for (Direction adjDir : Util.actualDirections) {
				if (rc.canSpawn(adjDir, spawnType)) {
					MapLocation adjLoc = rc.getLocation().add(adjDir);
					// note: readDistance will return 0 if we haven't processed that location yet
					// but that's okay
					int distance = BroadcastInterface.readDistance(rc, adjLoc.x, adjLoc.y);
					if (distance > maxDist) {
						maxDist = distance;
						ans = adjDir;
					}
				}
			}
			return ans;
		}

		private Direction findSpawnTowardEnemy(RobotType spawnType) throws GameActionException {
			// find a spawn location that's closest to the enemy
			// TODO: regardless of which spawn method is called, though, we probably shouldn't spawn thing in the line of fire. meh.

			// check all the adjacent squares for one that's unblocked AND furthest from the opponent
			Direction ans = null;
			int minDist = Integer.MAX_VALUE;
			for (Direction adjDir : Util.actualDirections) {
				if (rc.canSpawn(adjDir, spawnType)) {
					MapLocation adjLoc = rc.getLocation().add(adjDir);
					// note: readDistance will return 0 if we haven't processed that location yet
					int distance = BroadcastInterface.readDistance(rc, adjLoc.x, adjLoc.y);
					if (distance == 0 && ans == null) {
						ans = adjDir;
					} else if (distance != 0 && distance < minDist) {
						minDist = distance;
						ans = adjDir;
					}
				}
			}
			return ans;
		}
	}

}
