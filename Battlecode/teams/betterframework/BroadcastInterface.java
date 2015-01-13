package betterframework;

import java.util.LinkedList;

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
	// 57621: head of pathfinding queue
	// 57622: tail of pathfinding queue
	// 57623: size of pathfinding queue
	// 57624: mutex lock for the pathfinding queue (i'm not sure if this actually helps, since it isn't atomic?)
	// 57625-58624: pathfinding queue
	// 58625: attack/retreat signal

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

	// pathfinding

	private static final int pfqHeadAddr = 57621;
	private static final int pfqTailAddr = 57622;
	private static final int pfqSizeAddr = 57623;
	private static final int pfqLockAddr = 57624;
	private static final int pfqBaseAddr = 57625;
	private static final int PFQ_CAPACITY = 1000;

	public static void seedPathfindingQueue(RobotController rc) throws GameActionException {
		MapLocation hq = rc.senseHQLocation(); // rc.senseEnemyHQLocation();
		enqueuePathfindingQueue(rc, hq.x, hq.y);
	}

	public static boolean lockPfq(RobotController rc) throws GameActionException {
		if (rc.readBroadcast(pfqLockAddr) == 1) {
			return false;
		}
		rc.broadcast(pfqLockAddr, 1);
		return true;
	}

	public static void unLockPfq(RobotController rc) throws GameActionException {
		rc.broadcast(pfqLockAddr, 0);
	}

	// this isn't necessarily synchronous, so this might result in some race conditions =/
	public static int[] dequeuePathfindingQueue(RobotController rc) throws GameActionException {
		int size = rc.readBroadcast(pfqSizeAddr);
		if (size > 0) {
			// System.out.println("removing from pathfinding queue (size=" + size + ")");
			int head = rc.readBroadcast(pfqHeadAddr);
			rc.broadcast(pfqHeadAddr, (head + 2) % PFQ_CAPACITY);
			rc.broadcast(pfqSizeAddr, size - 2);
			// TODO: to be efficient, we should store x and y as shorts. then we can store 2 in 1 entry.
			int x = rc.readBroadcast(pfqBaseAddr + head);
			int y = rc.readBroadcast(pfqBaseAddr + ((head + 1) % PFQ_CAPACITY));
			// printPfq(rc);
			return new int[] { x, y };
		}
		return null;
	}

	public static void enqueuePathfindingQueue(RobotController rc, int x, int y) throws GameActionException {
		int size = rc.readBroadcast(pfqSizeAddr);
		// System.out.println("adding (" + x + "," + y + ") to pathfinding queue (size=" + size + ")");
		if (size < PFQ_CAPACITY) {
			int tail = rc.readBroadcast(pfqTailAddr);
			rc.broadcast(pfqTailAddr, (tail + 2) % PFQ_CAPACITY);
			rc.broadcast(pfqSizeAddr, size + 2);
			rc.broadcast(pfqBaseAddr + tail, x);
			rc.broadcast(pfqBaseAddr + ((tail + 1) % PFQ_CAPACITY), y);
			// printPfq(rc);
		}

	}

	// for debugging only
	public static void printPfq(RobotController rc) throws GameActionException {
		StringBuilder out = new StringBuilder();
		int head = rc.readBroadcast(pfqHeadAddr);
		int tail = rc.readBroadcast(pfqTailAddr);
		int size = rc.readBroadcast(pfqSizeAddr);
		LinkedList<int[]> pfq = getPfq(rc);
		for (int[] coord : pfq) {
			out.append("(" + coord[0] + ", " + coord[1] + "), ");
		}
		out.append("head=" + head + ", tail=" + tail + ", size=" + size);
		out.append("\n");
		System.out.println(out.toString());
	}

	// for debugging only
	public static LinkedList<int[]> getPfq(RobotController rc) throws GameActionException {
		LinkedList<int[]> result = new LinkedList<int[]>();
		int head = rc.readBroadcast(pfqHeadAddr);
		int tail = rc.readBroadcast(pfqTailAddr);
		// this doesn't handle the case when tail < head
		for (int i = head; i < tail; i += 2) {
			result.add(new int[] { rc.readBroadcast(pfqBaseAddr + i), rc.readBroadcast(pfqBaseAddr + i + 1) });
		}
		return result;
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
}
