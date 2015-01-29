package bloombot;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public abstract class Strategy {
	// this provides suggestions for build order choices
	// i'm not sure where that information should live, which is why it lives as static members of this class

	protected RobotController rc;

	public Strategy(RobotController rc) {
		this.rc = rc;
	}

	public static Strategy getStrategy(RobotController rc) throws GameActionException {
		return new LaunchAndScout(rc);
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

	// the premise of this strategy is:
	// launcher can shoot missiles, which can scout void terrain. awesome! but they're really slow
	// on the flip side, soldies are really cheap, so we can scout with them. (TODO: or should we just scout with miners?)
	private static class LaunchAndScout extends Strategy {

		public LaunchAndScout(RobotController rc) {
			super(rc);
		}

		@Override
		public boolean shouldMakeTanks() throws GameActionException {
			return false;
		}

		private final double SOLDIERS_PER_LAUNCHER = 3;

		@Override
		public boolean shouldMakeLaunchers() throws GameActionException {
			int launcherCount = BroadcastInterface.getRobotCount(rc, RobotType.LAUNCHER, true);
			int soldiercount = BroadcastInterface.getRobotCount(rc, RobotType.SOLDIER, true);
			return launcherCount * SOLDIERS_PER_LAUNCHER <= soldiercount;
		}

		@Override
		public boolean shouldMakeSoldiers() throws GameActionException {
			int launcherCount = BroadcastInterface.getRobotCount(rc, RobotType.LAUNCHER, true);
			int soldiercount = BroadcastInterface.getRobotCount(rc, RobotType.SOLDIER, true);
			return launcherCount * SOLDIERS_PER_LAUNCHER >= soldiercount;
		}

		@Override
		public boolean shouldMakeBashers() throws GameActionException {
			return false;
		}

		@Override
		public boolean shouldMakeDrones() throws GameActionException {
			return false;
		}

		@Override
		public boolean shouldAggroWithDrones() throws GameActionException {
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
			int numAerospacelabs = BroadcastInterface.getRobotCount(rc, RobotType.AEROSPACELAB, true);
			if (numAerospacelabs < 1) {
				return RobotType.AEROSPACELAB;
			}
			int numBarracks = BroadcastInterface.getRobotCount(rc, RobotType.BARRACKS, true);
			if (numBarracks < 1) {
				return RobotType.BARRACKS;
			}
			int numTechnologyInstitutes = BroadcastInterface.getRobotCount(rc, RobotType.TECHNOLOGYINSTITUTE, true);
			if (numTechnologyInstitutes < 1) {
				return RobotType.TECHNOLOGYINSTITUTE;
			}
			int numTrainingFields = BroadcastInterface.getRobotCount(rc, RobotType.TRAININGFIELD, true);
			if (numTrainingFields < 1) {
				return RobotType.TRAININGFIELD;
			}

			if (shouldMakeLaunchers()) {
				return RobotType.AEROSPACELAB;
			} else {
				return RobotType.BARRACKS;
			}
		}

		@Override
		public boolean shouldAttack() throws GameActionException {
			return true;
		}

		@Override
		public boolean shouldWithdraw() throws GameActionException {
			return false;
		}

	}
}
