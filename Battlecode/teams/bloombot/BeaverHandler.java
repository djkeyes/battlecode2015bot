package bloombot;

import java.util.LinkedList;
import java.util.List;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

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

		switch (curStrategy.getBeaverBuildOrder()) {
		case AEROSPACELAB:
			result.add(buildAerospaceLab);
			break;
		case MINERFACTORY:
			result.add(buildMinerFactory);
			break;
		case BARRACKS:
			result.add(buildBarracks);
			break;
		case HELIPAD:
			result.add(buildHelipad);
			break;
		case TRAININGFIELD:
			result.add(buildTrainingField);
			break;
		case TECHNOLOGYINSTITUTE:
			result.add(buildTechInstitute);
			break;
		case TANKFACTORY:
			result.add(buildTankFactory);
			break;
		default:
			break;
		}
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
	private final Action scout = new ScoutOutward(true, true);
	private final Action attack = new Attack();

	private class BuildBuilding implements Action {
		private RobotType type;

		BuildBuilding(RobotType type) {
			this.type = type;
		}

		@Override
		public boolean run() throws GameActionException {
			if (rc.getTeamOre() >= type.oreCost) {

				Direction buildDir = findBuildDirection();
				if (buildDir != null) {
					rc.build(buildDir, type);
					return true;
				}
			}
			return false;
		}

		private Direction findBuildDirection() throws GameActionException {
			if (!rc.isCoreReady()) {
				return null;
			}

			// TODO: calling senseRobotAtLocation is actually kind of costly (25 bytecodes), so maybe we should initialize this lazily
			// right now, this method can cost as much as 3000 bytecodes
			// or maybe the payoff wouldn't be worth it.
			boolean[][] isTileOccupied = new boolean[5][5];
			boolean[][] isAdjToBuilding = new boolean[5][5];
			for (int i = 0; i < 5; i++) {
				for (int j = 0; j < 5; j++) {
					MapLocation loc = rc.getLocation().add(i - 2, j - 2);
					TerrainTile tile = rc.senseTerrainTile(loc);
					if (!tile.isTraversable()) {
						isTileOccupied[i][j] = true;
					} else {
						// don't build next to buildings. otherwise, we might wall our own buildings in.
						RobotInfo occupant = rc.senseRobotAtLocation(loc);
						if (occupant != null && occupant.type.isBuilding) {
							isTileOccupied[i][j] = true;
							for (int k = -1; k <= 1; k++) {
								for (int l = -1; l <= 1; l++) {
									if (i + k >= 0 && i + k < 5 && j + l >= 0 && j + l < 5) {
										isAdjToBuilding[i + k][j + l] = true;
									}
								}
							}
						}
					}
				}
			}

			for (Direction d : Util.actualDirections) {
				if (!isTileOccupied[2 + d.dx][2 + d.dy]) {
					if (!isAdjToBuilding[2 + d.dx][2 + d.dy]) {
						boolean tileNW = isTileOccupied[1 + d.dx][1 + d.dy];
						boolean tileN = isTileOccupied[1 + d.dx][2 + d.dy];
						boolean tileNE = isTileOccupied[1 + d.dx][3 + d.dy];
						boolean tileE = isTileOccupied[2 + d.dx][3 + d.dy];
						boolean tileSE = isTileOccupied[3 + d.dx][3 + d.dy];
						boolean tileS = isTileOccupied[3 + d.dx][2 + d.dy];
						boolean tileSW = isTileOccupied[3 + d.dx][1 + d.dy];
						boolean tileW = isTileOccupied[2 + d.dx][1 + d.dy];
						if (isOkayToBuild(tileN, tileNE, tileE, tileSE, tileS, tileSW, tileW, tileNW)) {
							if (rc.canBuild(d, type)) {
								return d;
							}
						}
					}
				}
			}
			return null;
		}

		private boolean isOkayToBuild(boolean tileN, boolean tileNE, boolean tileE, boolean tileSE, boolean tileS, boolean tileSW,
				boolean tileW, boolean tileNW) {
			// this was output from weka
			// this is the model for an alternating decision tree which learned the set of building placements which block paths
			double sum = 0.039;
			if (!tileN) {
				sum += 0.28;
				if (!tileE) {
					sum += 1.425;
				} else {
					sum += -0.771;
				}
				if (!tileW) {
					sum += 0.799;
				} else {
					sum += -0.572;
				}
			} else {
				sum += -0.273;
				if (!tileNE) {
					sum += -0.694;
					if (!tileE) {
						sum += 0.42;
					} else {
						sum += -1.129;
					}
				} else {
					sum += 0.298;
				}
				if (!tileNW) {
					sum += -0.645;
					if (!tileW) {
						sum += 0.559;
					} else {
						sum += -1.101;
					}
				} else {
					sum += 0.477;
					if (!tileW) {
						sum += -0.363;
					} else {
						sum += 0.83;
					}
				}
			}
			if (!tileS) {
				sum += 0.392;
				if (!tileE) {
					sum += 0.545;
				} else {
					sum += -0.39;
				}
				if (!tileW) {
					sum += 0.672;
				} else {
					sum += -0.443;
				}
			} else {
				sum += -0.343;
				if (!tileSE) {
					sum += -0.534;
				} else {
					sum += 0.394;
					if (!tileE) {
						sum += -0.538;
					} else {
						sum += 0.927;
					}
				}
				if (!tileSW) {
					sum += -0.639;
					if (!tileW) {
						sum += 0.075;
					} else {
						sum += -1.11;
					}
				} else {
					sum += 0.425;
					if (!tileW) {
						sum += -0.569;
					} else {
						sum += 0.672;
					}
				}
			}
			return sum > 0;
		}
	}
}
