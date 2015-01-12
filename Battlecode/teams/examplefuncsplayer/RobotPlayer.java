package examplefuncsplayer;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class RobotPlayer {
	static final int HIGH_LEVEL_PLAY_CHANNEL_OFFSET = 0;

	static enum HIGH_LEVEL_PLAY_CHANNELS {
		ORE_SCAVENGE, BUILD
	};

	static final int COUNTING_CHANNELS_OFFSET = 1;

	static final int COUNTED_CHANNELS_OFFSET = COUNTING_CHANNELS_OFFSET + RobotType.values().length;

	static final boolean DEBUG = true;
	static RobotController rc;
	static Team myTeam;
	static Team enemyTeam;
	static int myRange;
	static Random rand;
	static Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
			Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };

	public static void run(RobotController tomatojuice) {
		int i = 0;
		int fate = 0;
		int channel = 0;
		boolean firstturn = true;
		rc = tomatojuice;
		rand = new Random(rc.getID());

		myRange = rc.getType().attackRadiusSquared;
		MapLocation enemyLoc = rc.senseEnemyHQLocation();
		Direction lastDirection = null;
		myTeam = rc.getTeam();
		enemyTeam = myTeam.opponent();
		RobotInfo[] myRobots;

		while (true) {

			if (DEBUG) {
				try {
					rc.setIndicatorString(0, "This is an indicator string.");
					rc.setIndicatorString(1, "I am a " + rc.getType());
				} catch (Exception e) {
					System.out.println("Unexpected exception");
					e.printStackTrace();
				}
			}

			channel = COUNTING_CHANNELS_OFFSET + rc.getType().ordinal();
			try {
				rc.broadcast(channel, rc.readBroadcast(channel) + 1);
			} catch (GameActionException e1) {
				e1.printStackTrace();
			}

			if (rc.getType() == RobotType.HQ) {
				try {
					if (firstturn) {
						for (i = 0; i < RobotType.values().length; i++) {
							rc.broadcast(COUNTED_CHANNELS_OFFSET + RobotType.values()[i].ordinal(), 0);
						}
					} else {
						for (i = 0; i < RobotType.values().length; i++) {
							rc.broadcast(COUNTED_CHANNELS_OFFSET + i, rc.readBroadcast(COUNTING_CHANNELS_OFFSET + i));
							rc.broadcast(COUNTING_CHANNELS_OFFSET + i, 0);
						}

					}
					fate = rand.nextInt(10000);
					myRobots = rc.senseNearbyRobots(999999, myTeam);

					if (rc.isWeaponReady()) {
						attackSomething();
					}
					int numBeavers = rc.readBroadcast(COUNTED_CHANNELS_OFFSET + RobotType.BEAVER.ordinal());
					if (rc.isCoreReady() && rc.getTeamOre() >= 100 && fate < Math.pow(1.2, 12 - numBeavers) * 10000) {
						trySpawn(directions[rand.nextInt(8)], RobotType.BEAVER);
					}
				} catch (Exception e) {
					System.out.println("HQ Exception");
					e.printStackTrace();
				}
			}

			if (rc.getType() == RobotType.TOWER) {
				try {
					if (rc.isWeaponReady()) {
						attackSomething();
					}
				} catch (Exception e) {
					System.out.println("Tower Exception");
					e.printStackTrace();
				}
			}

			if (rc.getType() == RobotType.BASHER) {
				try {
					RobotInfo[] adjacentEnemies = rc.senseNearbyRobots(2, enemyTeam);

					// BASHERs attack automatically, so let's just move around mostly randomly
					if (rc.isCoreReady()) {
						fate = rand.nextInt(1000);
						if (fate < 800) {
							tryMove(directions[rand.nextInt(8)]);
						} else {
							tryMove(rc.getLocation().directionTo(rc.senseEnemyHQLocation()));
						}
					}
				} catch (Exception e) {
					System.out.println("Basher Exception");
					e.printStackTrace();
				}
			}

			if (rc.getType() == RobotType.SOLDIER) {
				try {
					if (rc.isWeaponReady()) {
						attackSomething();
					}
					if (rc.isCoreReady()) {
						fate = rand.nextInt(1000);
						if (fate < 800) {
							tryMove(directions[rand.nextInt(8)]);
						} else {
							tryMove(rc.getLocation().directionTo(rc.senseEnemyHQLocation()));
						}
					}
				} catch (Exception e) {
					System.out.println("Soldier Exception");
					e.printStackTrace();
				}
			}

			if (rc.getType() == RobotType.BEAVER) {
				try {
					if (rc.isWeaponReady()) {
						attackSomething();
					}
					if (rc.isCoreReady()) {
						fate = rand.nextInt(1000);
						if (fate < 8 && rc.getTeamOre() >= 300) {
							tryBuild(directions[rand.nextInt(8)], RobotType.BARRACKS);
						} else if (fate < 600) {
							rc.mine();
						} else if (fate < 900) {
							tryMove(directions[rand.nextInt(8)]);
						} else {
							tryMove(rc.senseHQLocation().directionTo(rc.getLocation()));
						}
					}
				} catch (Exception e) {
					System.out.println("Beaver Exception");
					e.printStackTrace();
				}
			}

			if (rc.getType() == RobotType.BARRACKS) {
				try {
					fate = rand.nextInt(10000);
					int numBeavers = rc.readBroadcast(COUNTED_CHANNELS_OFFSET + RobotType.BEAVER.ordinal());
					int numSoldiers = rc.readBroadcast(COUNTED_CHANNELS_OFFSET + RobotType.SOLDIER.ordinal());
					int numBashers = rc.readBroadcast(COUNTED_CHANNELS_OFFSET + RobotType.BASHER.ordinal());

					// get information broadcasted by the HQ
					if (rc.isCoreReady() && rc.getTeamOre() >= 60
							&& fate < Math.pow(1.2, 15 - numSoldiers - numBashers + numBeavers) * 10000) {
						if (rc.getTeamOre() > 80 && fate % 2 == 0) {
							trySpawn(directions[rand.nextInt(8)], RobotType.BASHER);
						} else {
							trySpawn(directions[rand.nextInt(8)], RobotType.SOLDIER);
						}
					}
				} catch (Exception e) {
					System.out.println("Barracks Exception");
					e.printStackTrace();
				}
			}

			rc.yield();
		}
	}

	// This method will attack an enemy in sight, if there is one
	static void attackSomething() throws GameActionException {
		RobotInfo[] enemies = rc.senseNearbyRobots(myRange, enemyTeam);
		if (enemies.length > 0) {
			rc.attackLocation(enemies[0].location);
		}
	}

	// This method will attempt to move in Direction d (or as close to it as possible)
	static void tryMove(Direction d) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = { 0, 1, -1, 2, -2 };
		int dirint = directionToInt(d);
		boolean blocked = false;
		while (offsetIndex < 5 && !rc.canMove(directions[(dirint + offsets[offsetIndex] + 8) % 8])) {
			offsetIndex++;
		}
		if (offsetIndex < 5) {
			rc.move(directions[(dirint + offsets[offsetIndex] + 8) % 8]);
		}
	}

	// This method will attempt to spawn in the given direction (or as close to it as possible)
	static void trySpawn(Direction d, RobotType type) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = { 0, 1, -1, 2, -2, 3, -3, 4 };
		int dirint = directionToInt(d);
		boolean blocked = false;
		while (offsetIndex < 8 && !rc.canSpawn(directions[(dirint + offsets[offsetIndex] + 8) % 8], type)) {
			offsetIndex++;
		}
		if (offsetIndex < 8) {
			rc.spawn(directions[(dirint + offsets[offsetIndex] + 8) % 8], type);
		}
	}

	// This method will attempt to build in the given direction (or as close to it as possible)
	static void tryBuild(Direction d, RobotType type) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = { 0, 1, -1, 2, -2, 3, -3, 4 };
		int dirint = directionToInt(d);
		boolean blocked = false;
		while (offsetIndex < 8 && !rc.canMove(directions[(dirint + offsets[offsetIndex] + 8) % 8])) {
			offsetIndex++;
		}
		if (offsetIndex < 8) {
			rc.build(directions[(dirint + offsets[offsetIndex] + 8) % 8], type);
		}
	}

	static int directionToInt(Direction d) {
		switch (d) {
		case NORTH:
			return 0;
		case NORTH_EAST:
			return 1;
		case EAST:
			return 2;
		case SOUTH_EAST:
			return 3;
		case SOUTH:
			return 4;
		case SOUTH_WEST:
			return 5;
		case WEST:
			return 6;
		case NORTH_WEST:
			return 7;
		default:
			return -1;
		}
	}
}
