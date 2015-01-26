package dronerush;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Strategy {
	// this provides suggestions for build order choices
	// i'm not sure where that information should live, which is why it lives as static members of this class

	// TODO: we really need to figure out optimal suggestions
	// intuitively, tanks and launchers are the best. mostly we want them.
	// if we always make both, we'll either be restricted by the build time (and we'll make both in an arbitrary ratio), or we'll be
	// capped by ore (and we'll only make the cheaper one). instead, let's just keep them in some ratio with each other.
	// bashers are second best. we want some of them, but more if the enemy has lots of launchers.

	private static final int MAX_NUMBER_OF_BASHERS = 10;
	private static final double TANKS_PER_LAUNCHER = 1.5;

	public static boolean shouldMakeTanks(RobotController rc) throws GameActionException {
		int tankCount = BroadcastInterface.getRobotCount(rc, RobotType.TANK, true);
		int launcherCount = BroadcastInterface.getRobotCount(rc, RobotType.LAUNCHER, true);
		// > vs >= doesn't really matter here. what matters is that AT LEAST one of shouldMakeTanks and shouldMakeLaunchers always
		// returns true. so don't let both of them be >.
		return tankCount <= TANKS_PER_LAUNCHER * launcherCount;
	}

	public static boolean shouldMakeLaunchers(RobotController rc) throws GameActionException {
		int tankCount = BroadcastInterface.getRobotCount(rc, RobotType.TANK, true);
		int launcherCount = BroadcastInterface.getRobotCount(rc, RobotType.LAUNCHER, true);
		return tankCount >= TANKS_PER_LAUNCHER * launcherCount;
	}

	public static boolean shouldMakeBashers(RobotController rc) throws GameActionException {
		return (BroadcastInterface.getRobotCount(rc, RobotType.BASHER, true) < MAX_NUMBER_OF_BASHERS);
	}

}
