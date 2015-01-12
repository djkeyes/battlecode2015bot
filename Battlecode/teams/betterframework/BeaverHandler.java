package betterframework;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class BeaverHandler extends BaseRobotHandler {
	protected BeaverHandler(RobotController rc) {
		super(rc);
	}

	@Override
	public List<Action> chooseActions() {
		// beavers are basically the worst unit, except in that they can build buildings.
		// so in general, that should be their priority
		// TODO: update this to follow some semblance of a build order
		LinkedList<Action> result = new LinkedList<Action>();
		result.add(buildMinerFactory);
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

	private class BuildBuilding implements Action {
		private RobotType type;

		BuildBuilding(RobotType type) {
			this.type = type;
		}

		@Override
		public boolean run() throws GameActionException {
			// if (rc.getTeamOre() >= type.oreCost) {
			// Direction buildDir = findRandomBuildDirection(type);
			// if (buildDir != null) {
			// rc.build(buildDir, type);
			// return true;
			// }
			// }
			return false;
		}
	}
}
