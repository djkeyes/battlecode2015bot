package dronerush;

import battlecode.common.Clock;
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
		private Direction[] safeSpawningDirs;

		SpawnUnit(RobotType type, boolean spawnTowardEnemy) {
			this.type = type;
			this.spawnTowardEnemy = spawnTowardEnemy;

			safeSpawningDirs = findSafeSpawningDirs();
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
//			System.out.println("starting spawn loop...");
			for (Direction adjDir : safeSpawningDirs) {
//				System.out.println("top of loop, bytecodes used = " + Clock.getBytecodeNum());
				if (rc.canSpawn(adjDir, spawnType)) {
					MapLocation adjLoc = rc.getLocation().add(adjDir);
					// note: readDistance will return 0 if we haven't processed that location yet
					// but that's okay
					int distance = BroadcastInterface.readDistance(rc, adjLoc.x, adjLoc.y, getOurHqLocation());
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

			// check all the adjacent squares for one that's unblocked AND furthest from the opponent
			Direction ans = null;
			int minDist = Integer.MAX_VALUE;
//			System.out.println("starting spawn loop...");
			for (Direction adjDir : safeSpawningDirs) {
//				System.out.println("top of loop, bytecodes used = " + Clock.getBytecodeNum());
				if (rc.canSpawn(adjDir, spawnType)) {
					MapLocation adjLoc = rc.getLocation().add(adjDir);
					// note: readDistance will return 0 if we haven't processed that location yet
					int distance = BroadcastInterface.readDistance(rc, adjLoc.x, adjLoc.y, getOurHqLocation());
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

		private Direction[] findSafeSpawningDirs() {
			// precompute directions that are out of enemy tower range
			// even if we kill a tower, it's fine if we don't update this. we'll just spawn in a different direction
			Direction[] tmp = new Direction[Util.actualDirections.length];
			int size = 0;
			for (Direction d : Util.actualDirections) {
				if (!inEnemyHqOrTowerRange(rc.getLocation().add(d))) {
					tmp[size++] = d;
				}
			}

			Direction[] result = new Direction[size];
			System.arraycopy(tmp, 0, result, 0, size);

			return result;
		}

	}

	@Override
	public void onExcessBytecodes() {

	}

}
