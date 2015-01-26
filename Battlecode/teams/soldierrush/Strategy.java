package soldierrush;

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

	private static final double TANKS_PER_LAUNCHER = 1.5;
	private static final double SOLDIERS_PER_TANK = 2.5;

	public static boolean shouldMakeTanks(RobotController rc) throws GameActionException {
		int tankCount = BroadcastInterface.getRobotCount(rc, RobotType.TANK, true);
		int launcherCount = BroadcastInterface.getRobotCount(rc, RobotType.LAUNCHER, true);
		int soldierCount = BroadcastInterface.getRobotCount(rc, RobotType.SOLDIER, true);
		// > vs >= doesn't really matter here. what matters is that AT LEAST one of shouldMakeTanks and shouldMakeLaunchers always
		// returns true. so don't let both of them be >.
		return tankCount <= TANKS_PER_LAUNCHER * launcherCount && soldierCount >= SOLDIERS_PER_TANK * tankCount;
	}

	public static boolean shouldMakeLaunchers(RobotController rc) throws GameActionException {
		int tankCount = BroadcastInterface.getRobotCount(rc, RobotType.TANK, true);
		int launcherCount = BroadcastInterface.getRobotCount(rc, RobotType.LAUNCHER, true);
		return tankCount >= TANKS_PER_LAUNCHER * launcherCount;
	}

	public static boolean shouldMakeSoldiers(RobotController rc) throws GameActionException {
		int tankCount = BroadcastInterface.getRobotCount(rc, RobotType.TANK, true);
		int soldierCount = BroadcastInterface.getRobotCount(rc, RobotType.SOLDIER, true);
		
		return soldierCount <= SOLDIERS_PER_TANK * tankCount;
	}

}
