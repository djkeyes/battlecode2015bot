package betterframework;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
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
	// 21-57620: distance to opponent HQ of each map tile--maybe we should also have distance from our HQ?
	// 58625: attack/retreat signal
	// 58626: bitmask containing which configuration the map is in (reflection, rotation, etc)
	// 58627: x midpoint of the map
	// 58628: y midpoint of the map

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
		return rc.readBroadcast(index);
	}

	public static void setRobotCount(RobotController rc, RobotType type, int count) throws GameActionException {
		int index = getRobotIndex(type);
		if (rc.readBroadcast(index) != count)
			rc.broadcast(index, count);
	}

	// a word on coordinates:
	// in the game, coordinates are offset by a (constant) random amount (for example, the coordinates you have might be [-12895,
	// 13174])
	// this is reasonable, since real world robots don't know their global position
	// to solve this, i'm converting all coordinates to be relative to the HQ. this means they can range from [-120, 120] in x and
	// [-30,30] in y. so be careful!

	public static void setDistance(RobotController rc, int x, int y, int d) throws GameActionException {
		MapLocation hqLoc = rc.senseHQLocation();
		int channel = 20 + mapIndex(x - hqLoc.x, y - hqLoc.y);
		rc.broadcast(channel, d);
	}

	public static int readDistance(RobotController rc, int x, int y) throws GameActionException {
		MapLocation hqLoc = rc.senseHQLocation();
		int channel = 20 + mapIndex(x - hqLoc.x, y - hqLoc.y);
		// System.out.println("reading from channel " + channel + "(x=" + x + ", y=" + y + ")");
		return rc.readBroadcast(channel);
	}

	public static int mapIndex(int x, int y) {
		x += GameConstants.MAP_MAX_WIDTH;
		y += GameConstants.MAP_MAX_HEIGHT;
		return x * GameConstants.MAP_MAX_HEIGHT + y;
	}

	private static final int atkChannel = 58625;

	public static void setAttackMode(RobotController rc, boolean shouldAttack) throws GameActionException {
		if (shouldAttack) {
			rc.broadcast(atkChannel, 1);
		} else {
			rc.broadcast(atkChannel, 0);
		}
	}

	public static boolean readAttackMode(RobotController rc) throws GameActionException {
		return rc.readBroadcast(atkChannel) == 1;
	}

	private static final int configurationBitmaskChannel = 58626;
	private static final int xMidpointChannel = 58627;
	private static final int yMidpointChannel = 58628;
	
	private static final int VERTICAL_REFLECTION_OFFSET = 0;
	private static final int HORIZONTAL_REFLECTION_OFFSET = 1;
	private static final int DIAGONAL_REFLECTION_OFFSET = 2;
	private static final int REVERSE_DIAGONAL_REFLECTION_OFFSET = 3;
	private static final int ROTATION_OFFSET = 4;
	public static void setMapConfiguration(RobotController rc, float[] midpoint, boolean isVerticalReflection, boolean isHorizontalReflection,
			boolean isDiagonalReflection, boolean isReverseDiagonalReflection, boolean isRotation) throws GameActionException {
		int bitmask = 0;
		bitmask |= (isVerticalReflection?1:0) << VERTICAL_REFLECTION_OFFSET;
		bitmask |= (isHorizontalReflection?1:0) << HORIZONTAL_REFLECTION_OFFSET;
		bitmask |= (isDiagonalReflection?1:0) << DIAGONAL_REFLECTION_OFFSET;
		bitmask |= (isReverseDiagonalReflection?1:0) << REVERSE_DIAGONAL_REFLECTION_OFFSET;
		bitmask |= (isRotation?1:0) << ROTATION_OFFSET;

		rc.broadcast(configurationBitmaskChannel, bitmask);
		// we're storing the midpoint (a float which might end in 0.5) as an int here
		// so be sure to convert it back during lookup!
		rc.broadcast(xMidpointChannel,Float.floatToIntBits(midpoint[0]));
		rc.broadcast(yMidpointChannel, Float.floatToIntBits(midpoint[1]));
	}
}
