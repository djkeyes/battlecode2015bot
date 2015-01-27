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
			return new SoldierMass(rc); // TODO
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

	public abstract boolean shouldMakeDrones() throws GameActionException;

	public abstract boolean shouldAggroWithDrones() throws GameActionException;

	public abstract RobotType getBeaverBuildOrder() throws GameActionException;

	public abstract boolean shouldAttack() throws GameActionException;

	public abstract boolean shouldWithdraw() throws GameActionException;

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
			// bashers suck for early rush defense. their real purpose in life is dealing with clumps of launchers.
			// so don't bother making them until we have some bulkier units out first.
			int numTankFactories = BroadcastInterface.getRobotCount(rc, RobotType.TANKFACTORY, true);
			int numTanks = BroadcastInterface.getRobotCount(rc, RobotType.TANK, true);
			return numTankFactories >= 1 && numTanks >= 4
					&& BroadcastInterface.getRobotCount(rc, RobotType.BASHER, true) < MAX_NUMBER_OF_BASHERS;
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

		@Override
		public boolean shouldMakeDrones() throws GameActionException {
			return BroadcastInterface.getRobotCount(rc, RobotType.DRONE, true) < 9;
		}

		@Override
		public boolean shouldAggroWithDrones() throws GameActionException {
			return true;
		}

		private final int TANKS_NEEDED_TO_ATTACK = 10;
		private final int BASHERS_NEEDED_TO_ATTACK = 10;
		private final int LAUNCHERS_NEEDED_TO_ATTACK = 5;
		private final int TANKS_NEEDED_TO_RETREAT = 3;
		private final int BASHERS_NEEDED_TO_RETREAT = 2;
		private final int LAUNCHERS_NEEDED_TO_RETREAT = 2;

		@Override
		public boolean shouldAttack() throws GameActionException {
			int tankCount = BroadcastInterface.getRobotCount(rc, RobotType.TANK, true);
			int basherCount = BroadcastInterface.getRobotCount(rc, RobotType.BASHER, true);
			int launcherCount = BroadcastInterface.getRobotCount(rc, RobotType.LAUNCHER, true);

			return ((tankCount >= TANKS_NEEDED_TO_ATTACK) && (basherCount >= BASHERS_NEEDED_TO_ATTACK) && launcherCount >= (LAUNCHERS_NEEDED_TO_ATTACK));
		}

		@Override
		public boolean shouldWithdraw() throws GameActionException {
			int tankCount = BroadcastInterface.getRobotCount(rc, RobotType.TANK, true);
			int basherCount = BroadcastInterface.getRobotCount(rc, RobotType.BASHER, true);
			int launcherCount = BroadcastInterface.getRobotCount(rc, RobotType.LAUNCHER, true);
			return (tankCount <= TANKS_NEEDED_TO_RETREAT) && (basherCount <= BASHERS_NEEDED_TO_RETREAT)
					&& launcherCount <= (LAUNCHERS_NEEDED_TO_RETREAT);
		}
	}

	public static class SoldierMass extends Strategy {
		private static final double TANKS_PER_LAUNCHER = 1.5;
		private static final double SOLDIERS_PER_TANK = 2.5;

		public SoldierMass(RobotController rc) {
			super(rc);
		}

		@Override
		public boolean shouldMakeTanks() throws GameActionException {
			int tankCount = BroadcastInterface.getRobotCount(rc, RobotType.TANK, true);
			int launcherCount = BroadcastInterface.getRobotCount(rc, RobotType.LAUNCHER, true);
			int soldierCount = BroadcastInterface.getRobotCount(rc, RobotType.SOLDIER, true);
			// > vs >= doesn't really matter here. what matters is that AT LEAST one of shouldMakeTanks and shouldMakeLaunchers always
			// returns true. so don't let both of them be >.
			return tankCount <= TANKS_PER_LAUNCHER * launcherCount && soldierCount >= SOLDIERS_PER_TANK * tankCount;
		}

		@Override
		public boolean shouldMakeLaunchers() throws GameActionException {
			int tankCount = BroadcastInterface.getRobotCount(rc, RobotType.TANK, true);
			int launcherCount = BroadcastInterface.getRobotCount(rc, RobotType.LAUNCHER, true);
			return tankCount >= TANKS_PER_LAUNCHER * launcherCount;
		}

		@Override
		public boolean shouldMakeSoldiers() throws GameActionException {
			int tankCount = BroadcastInterface.getRobotCount(rc, RobotType.TANK, true);
			int soldierCount = BroadcastInterface.getRobotCount(rc, RobotType.SOLDIER, true);

			return soldierCount <= SOLDIERS_PER_TANK * tankCount;
		}

		@Override
		public boolean shouldMakeBashers() throws GameActionException {
			return false;
		}

		@Override
		public RobotType getBeaverBuildOrder() throws GameActionException {
			int numMinerFactories = BroadcastInterface.getRobotCount(rc, RobotType.MINERFACTORY, true);
			if (numMinerFactories < 1) {
				return RobotType.MINERFACTORY;
			}
			int numBarracks = BroadcastInterface.getRobotCount(rc, RobotType.BARRACKS, true);
			if (numBarracks < 1) {
				return RobotType.BARRACKS;
			}
			int numTankFactories = BroadcastInterface.getRobotCount(rc, RobotType.TANKFACTORY, true);
			if (numTankFactories < 1) {
				return RobotType.TANKFACTORY;
			}
			int numHelipads = BroadcastInterface.getRobotCount(rc, RobotType.HELIPAD, true);
			if (numHelipads < 1) {
				return RobotType.HELIPAD;
			}
			int numAerospacelabs = BroadcastInterface.getRobotCount(rc, RobotType.AEROSPACELAB, true);
			if (numAerospacelabs < 1) {
				return RobotType.AEROSPACELAB;
			}
			int numTechnologyInstitutes = BroadcastInterface.getRobotCount(rc, RobotType.TECHNOLOGYINSTITUTE, true);
			if (numTechnologyInstitutes < 1) {
				return RobotType.TECHNOLOGYINSTITUTE;
			}
			int numTrainingFields = BroadcastInterface.getRobotCount(rc, RobotType.TRAININGFIELD, true);
			if (numTrainingFields < 1) {
				return RobotType.TRAININGFIELD;
			}

			if (shouldMakeTanks()) {
				return RobotType.TANKFACTORY;
			} else if (shouldMakeSoldiers()) {
				return RobotType.BARRACKS;
			} else {
				return RobotType.AEROSPACELAB;
			}
		}

		@Override
		public boolean shouldMakeDrones() throws GameActionException {
			return BroadcastInterface.getRobotCount(rc, RobotType.DRONE, true) < 6;
		}

		@Override
		public boolean shouldAggroWithDrones() throws GameActionException {
			return false;
		}

		private final int TANKS_NEEDED_TO_ATTACK = 10;
		private final int SOLDIERS_NEEDED_TO_ATTACK = 20;
		private final int LAUNCHERS_NEEDED_TO_ATTACK = 5;
		private final int TANKS_NEEDED_TO_RETREAT = 5;
		private final int SOLDIERS_NEEDED_TO_RETREAT = 3;
		private final int LAUNCHERS_NEEDED_TO_RETREAT = 2;

		@Override
		public boolean shouldAttack() throws GameActionException {
			int tankCount = BroadcastInterface.getRobotCount(rc, RobotType.TANK, true);
			int soldierCount = BroadcastInterface.getRobotCount(rc, RobotType.SOLDIER, true);
			int launcherCount = BroadcastInterface.getRobotCount(rc, RobotType.LAUNCHER, true);
			return ((tankCount >= TANKS_NEEDED_TO_ATTACK) && (soldierCount >= SOLDIERS_NEEDED_TO_ATTACK) && launcherCount >= (LAUNCHERS_NEEDED_TO_ATTACK));
		}

		@Override
		public boolean shouldWithdraw() throws GameActionException {
			int tankCount = BroadcastInterface.getRobotCount(rc, RobotType.TANK, true);
			int soldierCount = BroadcastInterface.getRobotCount(rc, RobotType.SOLDIER, true);
			int launcherCount = BroadcastInterface.getRobotCount(rc, RobotType.LAUNCHER, true);
			return (tankCount <= TANKS_NEEDED_TO_RETREAT) && (soldierCount <= SOLDIERS_NEEDED_TO_RETREAT)
					&& launcherCount <= (LAUNCHERS_NEEDED_TO_RETREAT);
		}

	}
}
