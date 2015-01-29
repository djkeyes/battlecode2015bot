package bloombot;

import java.util.LinkedList;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
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
	// 64639: "pull the boys" and all attack signal
	// 64640-64660: number of each enemy robot
	// 64661: a boolean, whether there is a tower in peril
	// 64662: coordinates of a tower in peril, if it exists
	// 64663: number of enemies near tower in peril
	// 64664: a number corrosponding to the current strategy
	// 64665-65264: a list of launchers and the enemies they are targeting
	// 65265: a map location of the next tower/hq to go to
	// 65266: a flag of whether to move into tower range
	// 65267: a count of the number of allies in tower range

	// is there a better/more efficient way to do this? we could use an enummap, but i think that's less efficient.
	// alternatively, I think type.ordinal() might be useful?
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

	private static final int enemyTeamCountOffset = 64640;

	public static int getRobotCount(RobotController rc, RobotType type, boolean isOurTeam) throws GameActionException {
		int index = getRobotIndex(type);
		if (!isOurTeam) {
			index += enemyTeamCountOffset;
		}
		return rc.readBroadcast(index);
	}

	public static void setRobotCount(RobotController rc, RobotType type, int count, boolean isOurTeam) throws GameActionException {
		int index = getRobotIndex(type);
		if (!isOurTeam) {
			index += enemyTeamCountOffset;
		}
		if (rc.readBroadcast(index) != count)
			rc.broadcast(index, count);
	}

	// a word on coordinates:
	// in the game, coordinates are offset by a (constant) random amount (for example, the coordinates you have might be [-12895,
	// 13174])
	// this is reasonable, since real world robots don't know their global position
	// to solve this, i'm converting all coordinates to be relative to the HQ. this means they can range from [-120, 120] in x and
	// [-30,30] in y. so be careful!

	public static void setDistance(RobotController rc, int x, int y, int d, MapLocation hqLoc) throws GameActionException {
		int channel = 20 + mapIndex(x - hqLoc.x, y - hqLoc.y);
		rc.broadcast(channel, d);
	}

	public static int readDistance(RobotController rc, int x, int y, MapLocation hqLoc) throws GameActionException {
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
		// TODO: when the queue is empty, that means we're done pathfinding. so we can just cache all the reads from here on out.
		// alternatively, that means the queue is corrupted. =/
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

	private static final int boysChannel = 64639;

	public static boolean readPullBoysMode(RobotController rc) throws GameActionException {
		return rc.readBroadcast(boysChannel) == 1;
	}

	public static void setPullBoysMode(RobotController rc, boolean shouldPull) throws GameActionException {
		if (shouldPull) {
			rc.broadcast(boysChannel, 1);
		} else {
			rc.broadcast(boysChannel, 0);
		}
	}

	// 64661: a boolean, whether there is a tower in peril
	// 64662: coordinates of a tower in peril, if it exists
	// 64663: number of enemies near tower in peril
	private static final int isTowerInPerilChannel = 64661;
	private static final int towerInPerilChannelChannel = 64662;
	private static final int numEnemiesNearTowerInPerilChannel = 64663;

	public static void resetTowerInPeril(RobotController rc) throws GameActionException {
		rc.broadcast(isTowerInPerilChannel, 0);
		rc.broadcast(numEnemiesNearTowerInPerilChannel, 0);
	}

	public static int getNumEnemiesNearTowerInPeril(RobotController rc) throws GameActionException {
		return rc.readBroadcast(numEnemiesNearTowerInPerilChannel);
	}

	public static MapLocation getTowerInPeril(RobotController rc) throws GameActionException {
		if (!isTowerInPeril(rc)) {
			return null;
		}
		int combined = rc.readBroadcast(towerInPerilChannelChannel);
		int x = (combined >> 16);
		int y = (short) (0xFFFF & combined);
		return new MapLocation(x, y);
	}

	public static void reportTowerInPeril(RobotController rc, int numEnemies, MapLocation location) throws GameActionException {
		if (!isTowerInPeril(rc)) {
			rc.broadcast(isTowerInPerilChannel, 1);
		}

		int combined = (location.x << 16) | (0xFFFF & location.y);
		rc.broadcast(towerInPerilChannelChannel, combined);
		rc.broadcast(numEnemiesNearTowerInPerilChannel, numEnemies);
	}

	private static boolean isTowerInPeril(RobotController rc) throws GameActionException {
		return rc.readBroadcast(isTowerInPerilChannel) == 1;
	}

	private static final int strategyChannel = 64664;

	public static void setStrategyValue(RobotController rc, int strategyValue) throws GameActionException {
		rc.broadcast(strategyChannel, strategyValue);
	}

	public static int getStrategyValue(RobotController rc) throws GameActionException {
		return rc.readBroadcast(strategyChannel);
	}

	// 64665-65264: a list of launchers and the enemies they are targeting
	private static final int launcherTargetListListBaseChannel = 64665;
	private static final int launcherListSize = 600;

	public static MapLocation findLauncherTarget(RobotController rc, int launcherIndex) throws GameActionException {
		if (rc.readBroadcast(launcherTargetListListBaseChannel + launcherIndex * 3 + 1) == 1) {
			int combined = rc.readBroadcast(launcherTargetListListBaseChannel + launcherIndex * 3 + 2);
			int x = (combined >> 16);
			int y = (short) (0xFFFF & combined);
			return new MapLocation(x, y);
		} else {
			return null;
		}
	}

	public static void clearDeadLaunchers(RobotController rc) throws GameActionException {
		for (int i = 0; i < launcherListSize / 3; i++) {
			int id = rc.readBroadcast(launcherTargetListListBaseChannel + i * 3);
			if (id > 0) {
				RobotInfo robot = rc.senseRobot(id);
				if (robot == null || robot.type != RobotType.LAUNCHER || robot.team != rc.getTeam()) {
					rc.broadcast(launcherTargetListListBaseChannel + i * 3, 0);
				}
			}
		}
	}

	public static int addLauncherAndGetLauncherIndex(RobotController rc, int launcherId) throws GameActionException {
		for (int i = 0; i < launcherListSize / 3; i++) {
			int id = rc.readBroadcast(launcherTargetListListBaseChannel + i * 3);
			if (id == 0) {
				rc.broadcast(launcherTargetListListBaseChannel + i * 3, launcherId);
				return i;
			}
		}
		return -1;
	}

	public static int getLauncherIndex(RobotController rc, int launcherId) throws GameActionException {
		for (int i = 0; i < launcherListSize / 3; i++) {
			int id = rc.readBroadcast(launcherTargetListListBaseChannel + i * 3);
			if (id == launcherId) {
				return i;
			}
		}
		return -1;
	}

	public static void setLauncherTarget(RobotController rc, int launcherIndex, MapLocation target) throws GameActionException {
		if (target == null) {
			rc.broadcast(launcherTargetListListBaseChannel + launcherIndex * 3 + 1, 0);
		} else {
			rc.broadcast(launcherTargetListListBaseChannel + launcherIndex * 3 + 1, 1);
			int combined = (target.x << 16) | (0xFFFF & target.y);
			rc.broadcast(launcherTargetListListBaseChannel + launcherIndex * 3 + 2, combined);
		}
	}

	// 65265: a map location of the next tower/hq to go to
	// 65266: a flag of whether to move into tower range
	// 65267: a count of the number of allies in tower range
	private static final int nextTargetChannel = 65265;
	private static final int advanceBitChannel = 65266;
	private static final int nearbyAllyCountsChannel = 65267;

	public static void setNextTarget(RobotController rc, MapLocation target) throws GameActionException {
		int combined = (target.x << 16) | (0xFFFF & target.y);
		rc.broadcast(nextTargetChannel, combined);
	}

	public static MapLocation getNextTarget(RobotController rc) throws GameActionException {
		int combined = rc.readBroadcast(nextTargetChannel);
		int x = (combined >> 16);
		int y = (short) (0xFFFF & combined);
		return new MapLocation(x, y);
	}

	public static boolean getAdvanceBit(RobotController rc) throws GameActionException {
		return rc.readBroadcast(advanceBitChannel) == 1;
	}

	public static void setAdvanceBit(RobotController rc, boolean value) throws GameActionException {
		rc.broadcast(advanceBitChannel, value ? 1 : 0);
	}

	public static int getAlliesInPosition(RobotController rc) throws GameActionException {
		return rc.readBroadcast(nearbyAllyCountsChannel);
	}

	public static void clearAlliesInPosition(RobotController rc) throws GameActionException {
		rc.broadcast(nearbyAllyCountsChannel, 0);
	}

	public static void incrementAlliesInPosition(RobotController rc) throws GameActionException {
		int value = getAlliesInPosition(rc);
		value++;
		rc.broadcast(nearbyAllyCountsChannel, value);
	}

}
