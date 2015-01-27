package dronerush;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public abstract class Strategy {
	// this provides suggestions for build order choices
	// i'm not sure where that information should live, which is why it lives as static members of this class

	// TODO: we really need to figure out optimal suggestions
	// intuitively, tanks and launchers are the best. mostly we want them.
	// if we always make both, we'll either be restricted by the build time (and we'll make both in an arbitrary ratio), or we'll be
	// capped by ore (and we'll only make the cheaper one). instead, let's just keep them in some ratio with each other.
	// bashers are second best. we want some of them, but more if the enemy has lots of launchers.

	protected RobotController rc;

	public Strategy(RobotController rc) {
		this.rc = rc;
	}

	private static final int DRONE_RUSH_VALUE = 0;
	private static final int SOLDIER_MASS_VALUE = 1;

	public static Strategy getStrategy(RobotController rc) throws GameActionException {
		switch (BroadcastInterface.getStrategyValue(rc)) {
		case DRONE_RUSH_VALUE:
			return new DroneRush(rc);
		case SOLDIER_MASS_VALUE:
			// return new SoldierMass(rc); // TODO
		default:
			return new DroneRush(rc);
		}
	}

	public static void setStrategy(RobotController rc, boolean shouldDroneRush) throws GameActionException {
		// TODO: a tank-basher-launcher strat that doesn't start off by drone rushing
		if (shouldDroneRush) {
			BroadcastInterface.setStrategyValue(rc, DRONE_RUSH_VALUE);
		} else {
			BroadcastInterface.setStrategyValue(rc, SOLDIER_MASS_VALUE);
		}
	}

	public abstract boolean shouldMakeTanks() throws GameActionException;

	public abstract boolean shouldMakeLaunchers() throws GameActionException;

	public abstract boolean shouldMakeSoldiers() throws GameActionException;

	public abstract boolean shouldMakeBashers() throws GameActionException;

	public abstract RobotType getBeaverBuildOrder() throws GameActionException;

	private static class DroneRush extends Strategy {
		public DroneRush(RobotController rc) {
			super(rc);
		}

		private static final int MAX_NUMBER_OF_BASHERS = 10;
		private static final double TANKS_PER_LAUNCHER = 1.5;

		public boolean shouldMakeTanks() throws GameActionException {
			int tankCount = BroadcastInterface.getRobotCount(rc, RobotType.TANK, true);
			int launcherCount = BroadcastInterface.getRobotCount(rc, RobotType.LAUNCHER, true);
			// > vs >= doesn't really matter here. what matters is that AT LEAST one of shouldMakeTanks and shouldMakeLaunchers always
			// returns true. so don't let both of them be >.
			return tankCount <= TANKS_PER_LAUNCHER * launcherCount;
		}

		public boolean shouldMakeLaunchers() throws GameActionException {
			int tankCount = BroadcastInterface.getRobotCount(rc, RobotType.TANK, true);
			int launcherCount = BroadcastInterface.getRobotCount(rc, RobotType.LAUNCHER, true);
			return tankCount >= TANKS_PER_LAUNCHER * launcherCount;
		}

		public boolean shouldMakeBashers() throws GameActionException {
			return (BroadcastInterface.getRobotCount(rc, RobotType.BASHER, true) < MAX_NUMBER_OF_BASHERS);
		}

		@Override
		public boolean shouldMakeSoldiers() throws GameActionException {
			return false;
		}

		@Override
		public RobotType getBeaverBuildOrder() throws GameActionException {
			int numMinerFactories = BroadcastInterface.getRobotCount(rc, RobotType.MINERFACTORY, true);
			if (numMinerFactories < 1) {
				return RobotType.MINERFACTORY;
			}
			int numHelipads = BroadcastInterface.getRobotCount(rc, RobotType.HELIPAD, true);
			if (numHelipads < 1) {
				return RobotType.HELIPAD;
			}
			int numTechnologyInstitutes = BroadcastInterface.getRobotCount(rc, RobotType.TECHNOLOGYINSTITUTE, true);
			if (numTechnologyInstitutes < 1) {
				return RobotType.TECHNOLOGYINSTITUTE;
			}
			int numTrainingFields = BroadcastInterface.getRobotCount(rc, RobotType.TRAININGFIELD, true);
			if (numTrainingFields < 1) {
				return RobotType.TRAININGFIELD;
			}
			int numBarracks = BroadcastInterface.getRobotCount(rc, RobotType.BARRACKS, true);
			if (numBarracks < 1) {
				return RobotType.BARRACKS;
			}
			int numTankFactories = BroadcastInterface.getRobotCount(rc, RobotType.TANKFACTORY, true);
			if (numTankFactories < 1) {
				return RobotType.TANKFACTORY;
			}
			int numAerospacelabs = BroadcastInterface.getRobotCount(rc, RobotType.AEROSPACELAB, true);
			if (numAerospacelabs < 1) {
				return RobotType.AEROSPACELAB;
			}

			if (shouldMakeTanks()) {
				return RobotType.TANKFACTORY;
			} else {
				return RobotType.AEROSPACELAB;
			}
		}
	}
}
