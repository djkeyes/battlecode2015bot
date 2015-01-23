package dronerush;

import java.util.LinkedList;
import java.util.List;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class BeaverHandler extends BaseRobotHandler {
	protected BeaverHandler(RobotController rc) {
		super(rc);
	}

	@Override
	public List<Action> chooseActions() throws GameActionException {
		// beavers are basically the worst unit, except in that they can build buildings.
		// so in general, that should be their priority
		LinkedList<Action> result = new LinkedList<Action>();
		result.add(attack);

		// some turn-based logic
		int roundNum = Clock.getRoundNum();
		if (roundNum >= rc.getRoundLimit() - (RobotType.HANDWASHSTATION.buildTurns + 25)) {
			// SUPERIOR SANITATION
			result.add(buildHandwashStation);
			result.add(mine);
			result.add(scout);
			return result;
		}

		if (BroadcastInterface.shouldBuildMoreSupplyDepots(rc)) {
			result.add(buildSupplyDepot);
			result.add(mine);
			result.add(scout);
			return result;
		}

		int numMinerFactories = BroadcastInterface.getRobotCount(rc, RobotType.MINERFACTORY);
		if (numMinerFactories < 1) {
			result.add(buildMinerFactory);
			result.add(mine);
			result.add(scout);
			return result;
		}
		int numHelipads = BroadcastInterface.getRobotCount(rc, RobotType.HELIPAD);
		if (numHelipads < 1) {
			result.add(buildHelipad);
			result.add(mine);
			result.add(scout);
			return result;
		}
		int numTechnologyInstitutes = BroadcastInterface.getRobotCount(rc, RobotType.TECHNOLOGYINSTITUTE);
		if (numTechnologyInstitutes < 1) {
			result.add(buildTechInstitute);
			result.add(mine);
			result.add(scout);
			return result;
		}
		int numTrainingFields = BroadcastInterface.getRobotCount(rc, RobotType.TRAININGFIELD);
		if (numTrainingFields < 1) {
			result.add(buildTrainingField);
			result.add(mine);
			result.add(scout);
			return result;
		}
		int numAerospacelabs = BroadcastInterface.getRobotCount(rc, RobotType.AEROSPACELAB);
		if (numAerospacelabs < 1) {
			result.add(buildAerospaceLab);
			result.add(mine);
			result.add(scout);
			return result;
		}

		// if (gen.nextDouble() < 0.2) {
		// result.add(buildHelipad);
		// } else {
		result.add(buildAerospaceLab);
		// }
		result.add(mine);
		result.add(scout);
		return result;

	}

	// beavers have a lot of actions - they can idle, mine, scout, attack, and build
	private final Action buildSupplyDepot = new BuildBuilding(RobotType.SUPPLYDEPOT);
	private final Action buildTechInstitute = new BuildBuilding(RobotType.TECHNOLOGYINSTITUTE);
	private final Action buildTrainingField = new BuildBuilding(RobotType.TRAININGFIELD);
	private final Action buildBarracks = new BuildBuilding(RobotType.BARRACKS);
	private final Action buildTankFactory = new BuildBuilding(RobotType.TANKFACTORY);
	private final Action buildHelipad = new BuildBuilding(RobotType.HELIPAD);
	private final Action buildAerospaceLab = new BuildBuilding(RobotType.AEROSPACELAB);
	private final Action buildHandwashStation = new BuildBuilding(RobotType.HANDWASHSTATION);
	private final Action buildMinerFactory = new BuildBuilding(RobotType.MINERFACTORY);
	private final Action mine = new Mine(/* isBeaver= */true);
	private final Action scout = new ScoutOutward();
	private final Action attack = new Attack();

	private class BuildBuilding implements Action {
		private RobotType type;

		BuildBuilding(RobotType type) {
			this.type = type;
		}

		@Override
		public boolean run() throws GameActionException {
			if (rc.getTeamOre() >= type.oreCost) {
				Direction buildDir = findBuildDirectionTowardOpponent();
				if (buildDir != null) {
					rc.build(buildDir, type);
					return true;
				}
			}
			return false;
		}

		private Direction findBuildDirectionTowardOpponent() throws GameActionException {
			if (!rc.isCoreReady()) {
				return null;
			}
			// TODO: this actually does the opposite of what we want: it spawns AWAY from our HQ, instead of TOWARD the opponent's.
			// given the info we have, we could figure out the other way, but we'd need to figure out whether the map is a rotation or
			// a reflection

			Direction bestDir = null;
			int maxDist = 0;
			// protip: only buildings on checker squares, so that way your base is (almost) always pathable
			// to do this, we check to see if both tiles of the square are even XOR odd
			Direction[] potentialDirs;
			if ((rc.getLocation().x & 0x1) == (rc.getLocation().y & 0x1)) {
				potentialDirs = Util.cardinalDirections;
			} else {
				potentialDirs = Util.unCardinalDirections;
			}
			for (Direction d : potentialDirs) {
				MapLocation adjLoc = rc.getLocation();
				if (rc.canBuild(d, type)) {
					int adjDist = BroadcastInterface.readDistance(rc, adjLoc.x, adjLoc.y, getOurHqLocation());
					if (bestDir == null || adjDist > maxDist) {
						bestDir = d;
					}
				}
			}
			return bestDir;
		}
	}
}
