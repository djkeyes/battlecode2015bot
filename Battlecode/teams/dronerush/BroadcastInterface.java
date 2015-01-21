package dronerush;

import java.util.LinkedList;

import battlecode.common.Clock;
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
	// 58629: pathfinding queue head address
	// 58630: pathfinding queue tail address
	// 58631: pathfinding queue current size
	// 58632-61631: pathfinding queue
	// 61632: number of miners with abundant ore, for economy feedback system, odd turns
	// 61633: number of miners with abundant ore, for economy feedback system, even turns
	// 61634: supply queue head address
	// 61635: supply queue tail address
	// 61636: supply queue current size
	// 61637-64637: supply queue
	// 64638: build more supply depots signal

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

	public static void setMapConfiguration(RobotController rc, float[] midpoint, int configurationBitmask) throws GameActionException {
		rc.broadcast(configurationBitmaskChannel, configurationBitmask);
		// we're storing the midpoint (a float which might end in 0.5) as an int here
		// so be sure to convert it back during lookup!
		rc.broadcast(xMidpointChannel, Float.floatToIntBits(midpoint[0]));
		rc.broadcast(yMidpointChannel, Float.floatToIntBits(midpoint[1]));
	}

	public static int getConfigurationBitmask(RobotController rc) throws GameActionException {
		return rc.readBroadcast(configurationBitmaskChannel);
	}

	public static float[] getConfigurationMidpoint(RobotController rc) throws GameActionException {
		float x = Float.intBitsToFloat(rc.readBroadcast(xMidpointChannel));
		float y = Float.intBitsToFloat(rc.readBroadcast(yMidpointChannel));
		return new float[] { x, y };
	}

	// pathfinding

	private static final int pfqHeadAddr = 58629;
	private static final int pfqTailAddr = 58630;
	private static final int pfqSizeAddr = 58631;
	private static final int pfqBaseAddr = 58632;
	private static final int PFQ_CAPACITY = 3000;

	// TODO: if we could buffer and dequeue things several at a time, it would probably save us some bytecodes
	// note: daniel tried doing this for enqueuing, but the cost of the linked list hardly made it worth it
	// by reducing the number of broadcasts needed for head updates
	public static int[] dequeuePathfindingQueue(RobotController rc) throws GameActionException {
		int size = rc.readBroadcast(pfqSizeAddr);
		if (size > 0) {
			int head = rc.readBroadcast(pfqHeadAddr);
			rc.broadcast(pfqHeadAddr, (head + 1) % PFQ_CAPACITY);
			rc.broadcast(pfqSizeAddr, size - 1);

			// to be compact, we store the coordinates as two concatenated shorts
			int combined = rc.readBroadcast(pfqBaseAddr + head);
			int x = (combined >> 16);
			int y = (short) (0xFFFF & combined);
			return new int[] { x, y };
		}
		return null;
	}

	public static boolean enqueuePathfindingQueue(RobotController rc, int x, int y) throws GameActionException {
		int size = rc.readBroadcast(pfqSizeAddr);
		if (size < PFQ_CAPACITY) {
			int tail = rc.readBroadcast(pfqTailAddr);
			rc.broadcast(pfqTailAddr, (tail + 1) % PFQ_CAPACITY);
			rc.broadcast(pfqSizeAddr, size + 1);

			int combined = (x << 16) | (0xFFFF & y);
			rc.broadcast(pfqBaseAddr + tail, combined);
			return true;
		}
		// TODO: handle the case when the queue is full
		System.out.println("The pathfinding queue is full. Maybe you should increase the size, or investigate why it filled up.");
		return false;
	}

	public static boolean enqueueAllInPathfindingQueue(RobotController rc, LinkedList<MapLocation> toEnqueue)
			throws GameActionException {
		int size = rc.readBroadcast(pfqSizeAddr);
		int bufferSize = toEnqueue.size();

		if (size + bufferSize <= PFQ_CAPACITY) {
			int tail = rc.readBroadcast(pfqTailAddr);
			rc.broadcast(pfqTailAddr, (tail + bufferSize) % PFQ_CAPACITY);
			rc.broadcast(pfqSizeAddr, size + bufferSize);

			for (MapLocation cur : toEnqueue) {
				int combined = (cur.x << 16) | (0xFFFF & cur.y);
				rc.broadcast(pfqBaseAddr + tail, combined);
				tail++;
				if (tail > PFQ_CAPACITY) {
					tail -= PFQ_CAPACITY;
				}
			}

			return true;
		}
		// TODO: handle the case when the queue is full
		return false;
	}

	private static void printPfq(RobotController rc) throws GameActionException {
		int head = rc.readBroadcast(pfqHeadAddr);
		int tail = rc.readBroadcast(pfqTailAddr);
		int size = rc.readBroadcast(pfqSizeAddr);
		// this doesn't handle the case when tail < head
		StringBuilder out = new StringBuilder();
		for (int i = head; i < tail; i++) {
			out.append(rc.readBroadcast(pfqBaseAddr + i) + ", ");
		}
		out.append("head=" + head + ", tail=" + tail + ", size=" + size);
		out.append("\n");
		System.out.println(out.toString());
	}

	// 61632: number of miners with abundant ore, for economy feedback system
	private static final int abundantOreChannel1 = 61632;
	private static final int abundantOreChannel2 = 61633;

	public static void resetAbundantOre(RobotController rc) throws GameActionException {
		if ((Clock.getRoundNum() & 0x1) == 0) {
			rc.broadcast(abundantOreChannel1, 0);
		} else {
			rc.broadcast(abundantOreChannel2, 0);
		}
	}

	public static void incrementAbundantOre(RobotController rc) throws GameActionException {
		if ((Clock.getRoundNum() & 0x1) == 0) {
			rc.broadcast(abundantOreChannel1, rc.readBroadcast(abundantOreChannel1) + 1);
		} else {
			rc.broadcast(abundantOreChannel2, rc.readBroadcast(abundantOreChannel2) + 1);
		}
	}

	public static int readPreviousRoundAbundantOre(RobotController rc) throws GameActionException {
		if ((Clock.getRoundNum() & 0x1) != 0) {
			return rc.readBroadcast(abundantOreChannel1);
		} else {
			return rc.readBroadcast(abundantOreChannel2);
		}
	}

	// 61634: supply queue head address
	// 61635: supply queue tail address
	// 61636: supply queue current size
	// 61637-64637: supply queue
	private static final int sqHeadAddr = 61634;
	private static final int sqTailAddr = 61635;
	private static final int sqSizeAddr = 61636;
	private static final int sqBaseAddr = 61637;
	private static final int SQ_CAPACITY = 3000;

	public static int dequeueSupplyQueue(RobotController rc) throws GameActionException {
		int size = rc.readBroadcast(sqSizeAddr);
		if (size > 0) {
			int head = rc.readBroadcast(sqHeadAddr);
			rc.broadcast(sqHeadAddr, (head + 1) % SQ_CAPACITY);
			rc.broadcast(sqSizeAddr, size - 1);
			return rc.readBroadcast(sqBaseAddr + head);
		}
		return -1;
	}

	public static boolean enqueueSupplyQueue(RobotController rc, int robotID) throws GameActionException {
		int size = rc.readBroadcast(sqSizeAddr);
		if (size < SQ_CAPACITY) {
			int tail = rc.readBroadcast(sqTailAddr);
			rc.broadcast(sqTailAddr, (tail + 1) % SQ_CAPACITY);
			rc.broadcast(sqSizeAddr, size + 1);

			rc.broadcast(sqBaseAddr + tail, robotID);
			return true;
		}
		// TODO: handle the case when the queue is full
		System.out.println("The supply queue is full. Maybe you should increase the size, or investigate why it filled up.");
		return false;
	}

	private static int moreSupplyDepotChannel = 64638;

	public static boolean shouldBuildMoreSupplyDepots(RobotController rc) throws GameActionException {
		return rc.readBroadcast(moreSupplyDepotChannel) == 1;
	}

	public static void setBuildMoreSupplyDepots(RobotController rc, boolean shouldBuildMore) throws GameActionException {
		rc.broadcast(moreSupplyDepotChannel, shouldBuildMore ? 1 : 0);
	}

}
