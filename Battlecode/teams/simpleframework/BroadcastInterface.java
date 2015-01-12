package simpleframework;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class BroadcastInterface {

	// broadcasting is great. it lets us save on computation time and outsource it to other robots.
	// but managing it sucks. this class manages it for us. all the channels are handled in one central place.

	// it would be nice to define 65536 variables for claiming addresses, but that's obviously prohibitive. alternatively,
	// it would be nice if we could keep a text file somewhere of who has claimed what address, then generate this file
	// based off the text file. todo.
	// so just keep track of who's using what when you use this file.
	// addresses claimed so far:
	// 0-20: number of each robot

	// here are the things we keep track of:
	// - number of each robot
	// - action priorities for beavers (TODO: other robots)
	// - map contents?

	// is there a better/more efficient way to do this? we could use an enummap, but i think that's less efficient.
	private static int getRobotIndex(RobotType type) {
		switch (type) {
		case AEROSPACELAB:
			return 0;
		case BARRACKS:
			return 1;
		case BASHER:
			return 2;
		case BEAVER:
			return 3;
		case COMMANDER:
			return 4;
		case COMPUTER:
			return 5;
		case DRONE:
			return 6;
		case HANDWASHSTATION:
			return 7;
		case HELIPAD:
			return 8;
		case HQ:
			return 9;
		case LAUNCHER:
			return 10;
		case MINER:
			return 11;
		case MINERFACTORY:
			return 12;
		case MISSILE:
			return 13;
		case SOLDIER:
			return 14;
		case SUPPLYDEPOT:
			return 15;
		case TANK:
			return 16;
		case TANKFACTORY:
			return 17;
		case TECHNOLOGYINSTITUTE:
			return 18;
		case TOWER:
			return 19;
		case TRAININGFIELD:
			return 20;
		}
		return -1;
	}

	public static int getRobotCount(RobotController rc, RobotType type) throws GameActionException {
		int index = getRobotIndex(type);
		if (index >= 0) {
			return rc.readBroadcast(index);
		}
		return -1;
	}

	public static void setRobotCount(RobotController rc, RobotType type, int count) throws GameActionException {
		int index = getRobotIndex(type);
		if (index >= 0) {
			rc.broadcast(index, count);
		}
	}

}
