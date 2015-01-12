package betterframework;

import java.util.List;
import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.TerrainTile;

public abstract class BaseRobotHandler {

	public static BaseRobotHandler createHandler(RobotController rc) {
		switch (rc.getType()) {
		case AEROSPACELAB:
			return new AerospaceLabHandler(rc);
		case BARRACKS:
			return new BarracksHandler(rc);
		case BASHER:
			return new BasherHandler(rc);
		case BEAVER:
			return new BeaverHandler(rc);
		case COMMANDER:
			return new CommanderHandler(rc);
		case COMPUTER:
			return new ComputerHandler(rc);
		case DRONE:
			return new DroneHandler(rc);
		case HANDWASHSTATION:
			return new HandwashStationHandler(rc);
		case HELIPAD:
			return new HelipadHandler(rc);
		case HQ:
			return new HQHandler(rc);
		case LAUNCHER:
			return new LauncherHandler(rc);
		case MINER:
			return new MinerHandler(rc);
		case MINERFACTORY:
			return new MinerFactoryHandler(rc);
		case MISSILE:
			return new MissileHandler(rc);
		case SOLDIER:
			return new SoldierHandler(rc);
		case SUPPLYDEPOT:
			return new SupplyDepotHandler(rc);
		case TANK:
			return new TankHandler(rc);
		case TANKFACTORY:
			return new TankFactoryHandler(rc);
		case TECHNOLOGYINSTITUTE:
			return new TechnologyInstituteHandler(rc);
		case TOWER:
			return new TowerHandler(rc);
		case TRAININGFIELD:
			return new TrainingFieldHandler(rc);
		}
		return null;
	}

	protected RobotController rc;
	protected Random gen;

	protected BaseRobotHandler(RobotController rc) {
		this.rc = rc;
		gen = new Random(rc.getID());

	}

	public int maxBytecodesToUse() {
		return 1500;
	}

	public final void run() {
		// here's the breakdown for this robot:
		// 1. init(). makes sense.
		// then we start looping.
		// 2. chooseActions() picks a ranked set of actions and returns them as a queue. this is where decision-making happens.
		// 3. performActions() performs each action in the action queue. (by calling Action.run())
		// 4. distributeSupply(), which obviously distributes supply.
		// 5. onExcessBytecodes() is called if this robot has used less than 2000 bytecodes.
		//
		// when adding functionality, I would recommend overriding one of the above methods. Actions are represented as
		// the enum Action, so subclasses with a broader field of actions should create new subclasses of Action.
		// all actions in the action queue may be performed, so they should all check to make sure they are executable.

		try {
			rc.setIndicatorString(0, "init()");
			init();
		} catch (GameActionException ex) {
			rc.setIndicatorString(0, "onException()");
			onException(ex);
		}
		while (true) {
			try {
				rc.setIndicatorString(0, "chooseActions()");
				List<Action> actions = chooseActions();
				rc.setIndicatorString(0, "performActions()");
				performActions(actions);
				rc.setIndicatorString(0, "distributeSupply()");
				distributeSupply();
				while (Clock.getBytecodeNum() < maxBytecodesToUse()) {
					rc.setIndicatorString(0, "onExcessBytecodes()");
					onExcessBytecodes();
				}
				rc.yield();
			} catch (GameActionException ex) {
				rc.setIndicatorString(0, "onException()");
				onException(ex);
			}
		}
	}

	public void performActions(List<Action> actionQueue) throws GameActionException {
		for (Action actionToAttempt : actionQueue) {
			if (actionToAttempt.run()) {
				break;
			}
		}
	}

	public abstract List<Action> chooseActions() throws GameActionException;

	public void onExcessBytecodes() throws GameActionException {
		doPathfinding();
	}

	protected void doPathfinding() throws GameActionException {
		// first lock the pathfinding queue while we modifiy it
		while (!BroadcastInterface.lockPfq(rc)) {
			if (Clock.getBytecodeNum() > maxBytecodesToUse()) {
				return;
			}
		}
		// pick a point from the front of the pathfinding queue
		int[] coords = BroadcastInterface.dequeuePathfindingQueue(rc);
		BroadcastInterface.unLockPfq(rc);
		if (coords == null) {
			return;
		}
		// System.out.println(Arrays.toString(coords));

		int curDist = BroadcastInterface.readDistance(rc, coords[0], coords[1]);
		MapLocation curLoc = new MapLocation(coords[0], coords[1]);
		boolean hasUnknownAdjacent = false;

		// first do a quick check on neighboring locations. sometimes due to race conditions, the current distance may be wrong, but
		// neighboring ones may be right. this isn't a cure-all for those race conditions, but it helps some.
		// TODO: maybe turn this into a randomized algorithm? add random elements of the current explored set to the queue?
		int minAdjDist = Integer.MAX_VALUE;
		for (Direction d : Util.actualDirections) {
			MapLocation adjLoc = curLoc.add(d);
			TerrainTile adjTile = rc.senseTerrainTile(adjLoc);
			if (adjTile == TerrainTile.NORMAL) {
				int adjDist = BroadcastInterface.readDistance(rc, adjLoc.x, adjLoc.y);
				if (adjDist != 0 && adjDist + 1 < curDist) {
					minAdjDist = adjDist;
				}
			}
		}
		if (minAdjDist < curDist - 1) {
			curDist = minAdjDist + 1;
			BroadcastInterface.setDistance(rc, curLoc.x, curLoc.y, curDist);
		}

		// explore all the neighboring points
		for (Direction d : Util.actualDirections) {
			MapLocation adjLoc = curLoc.add(d);
			// TODO: if there's enough processing power and space in the queue, also check if this tile is occupied
			TerrainTile adjTile = rc.senseTerrainTile(adjLoc);
			if (adjTile == TerrainTile.NORMAL) {
				// if this hasn't been explored, OR if it has been explored and we've found a shorter path, explore it!
				int adjDist = BroadcastInterface.readDistance(rc, adjLoc.x, adjLoc.y);
				if (adjDist == 0 || adjDist > curDist + 1) {
					BroadcastInterface.setDistance(rc, adjLoc.x, adjLoc.y, curDist + 1);

					while (!BroadcastInterface.lockPfq(rc)) {
						if (Clock.getBytecodeNum() > maxBytecodesToUse()) {
							return;
						}
					}
					BroadcastInterface.enqueuePathfindingQueue(rc, adjLoc.x, adjLoc.y);
					BroadcastInterface.unLockPfq(rc);
				}
			} else if (adjTile == TerrainTile.UNKNOWN) {
				hasUnknownAdjacent = true;
			}
		}
		if (hasUnknownAdjacent) {
			// if there are unknown tiles nearby, re-add this to the queue so we can process again later

			while (!BroadcastInterface.lockPfq(rc)) {
				if (Clock.getBytecodeNum() > maxBytecodesToUse()) {
					return;
				}
			}
			BroadcastInterface.enqueuePathfindingQueue(rc, curLoc.x, curLoc.y);
			BroadcastInterface.unLockPfq(rc);
		}
	}

	public void onException(GameActionException ex) {
		// TODO: only do this in debug mode. we don't want to crash and burn in an actual contest.
		ex.printStackTrace();
	}

	public void init() throws GameActionException {
	}

	protected void distributeSupply() throws GameActionException {
		RobotInfo[] nearbyAllies = rc.senseNearbyRobots(rc.getLocation(), GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, rc.getTeam());
		double lowestSupply = rc.getSupplyLevel();
		double transferAmount = 0;
		MapLocation suppliesToThisLocation = null;
		for (RobotInfo ri : nearbyAllies) {
			if (ri.supplyLevel < lowestSupply) {
				lowestSupply = ri.supplyLevel;
				transferAmount = (rc.getSupplyLevel() - ri.supplyLevel) / 2;
				suppliesToThisLocation = ri.location;
			}
		}
		if (suppliesToThisLocation != null) {
			rc.transferSupplies((int) transferAmount, suppliesToThisLocation);
		}

	}

	// some default action implementations

	// do nothing
	public class Idle implements Action {
		@Override
		public boolean run() throws GameActionException {
			return true; // intentionally idling indicates that we want to avoid doing anything else
		}
	}

	// almost every unit can attack, but be sure to double check before calling this. production buildings generally cannot.
	public class Attack implements Action {

		@Override
		public boolean run() throws GameActionException {
			// TODO pick a good target, like the closest one, or the weakest one, or one that all our allies have agreed to attack
			// this just picks any one.
			RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(rc.getLocation(), rc.getType().attackRadiusSquared, rc.getTeam()
					.opponent());
			if (nearbyEnemies.length > 0) {
				MapLocation enemyLoc = nearbyEnemies[0].location;
				if (rc.isWeaponReady() && rc.canAttackLocation(enemyLoc)) {
					rc.attackLocation(enemyLoc);
					return true;
				}
			}
			return false;
		}
	}

	// move this unit to an unexplored square
	// this is implemented by simple moving further away from the HQ--however, this means it can get stuck in corners
	public class ScoutOutward implements Action {

		@Override
		public boolean run() throws GameActionException {
			if (rc.isCoreReady()) {
				int maxDist = 0;
				Direction nextDir = null;
				for (Direction adjDir : Util.actualDirections) {
					MapLocation adjLoc = rc.getLocation().add(adjDir);
					if (rc.canMove(adjDir)) {
						int adjDist = BroadcastInterface.readDistance(rc, adjLoc.x, adjLoc.y);
						if (adjDist > maxDist) {
							maxDist = adjDist;
							nextDir = adjDir;
						}
					}
				}
				if (nextDir != null) {
					rc.move(nextDir);
					return true;
				}
			}
			return false;
		}
	}

	// opposite of scouting outward. try to move back toward hq.
	public class Retreat implements Action {

		@Override
		public boolean run() throws GameActionException {
			if (rc.isCoreReady()) {
				int minDist = Integer.MAX_VALUE;
				Direction nextDir = null;
				for (Direction adjDir : Util.actualDirections) {
					MapLocation adjLoc = rc.getLocation().add(adjDir);
					if (rc.canMove(adjDir)) {
						int adjDist = BroadcastInterface.readDistance(rc, adjLoc.x, adjLoc.y);
						if (adjDist != 0 && adjDist < minDist) {
							minDist = adjDist;
							nextDir = adjDir;
						}
					}
				}
				if (nextDir != null) {
					rc.move(nextDir);
					return true;
				}
			}
			return false;
		}
	}

	// only beavers and miners can mine
	// this also includes some exploratory behavior, which is triggered if there's better mineral patches nearby
	public static final double MIN_ORE_PER_TURN_THRESHOLD = 0.2;

	public class Mine implements Action {
		private boolean isBeaver;

		public Mine(boolean isBeaver) {
			this.isBeaver = isBeaver;
		}

		@Override
		public boolean run() throws GameActionException {
			if (rc.isCoreReady()) {
				// we have 2 options here:
				// 1. mine in current square
				// 2. move and mine in adjacent square
				// if we can get more ore by moving to an adjacent square, we should do it. however, staying in the current square
				// gets us more ore NOW, so there's a tradeoff which diminishes as we look more turns into the future.
				// this code checks two turns into the future to do the comparison, but it might be worth playing around with that.
				// TODO
				double curOre = rc.senseOre(rc.getLocation());
				double firstTurnOre = miningRate(curOre, isBeaver);
				double secondTurnOre = miningRate(curOre - firstTurnOre, isBeaver);
				double maxOre = firstTurnOre + secondTurnOre;
				Direction moveDir = null;

				for (Direction d : Util.actualDirections) {
					MapLocation adjLoc = rc.getLocation().add(d);
					if (rc.canMove(d)) {
						double adjOre = miningRate(rc.senseOre(adjLoc), isBeaver);
						if (adjOre > maxOre) {
							moveDir = d;
							maxOre = adjOre;
						}
					}
				}
				if (maxOre > MIN_ORE_PER_TURN_THRESHOLD) {
					if (moveDir == null) {
						// rc.canMine() is only false if this is called by a unit other than a beaver or miner. SO DON'T DO THAT!
						// actually checking costs us 10 bytecodes! (from rc.canMine())
						rc.mine();
					} else {
						rc.move(moveDir);
					}
					return true;
				}
			}
			return false;
		}

		private double miningRate(double orePresent, boolean isBeaver) {
			if (isBeaver) {
				return Math.min(orePresent, Math.max(Math.min(2., orePresent / 20.), 0.2));
			} else {
				return Math.min(orePresent, Math.max(Math.min(3., orePresent / 4.), 0.2));
			}
		}
	}

	// utility methods
	// TODO: add a building placement-finding method that avoids blocking paths
	// TODO: add a building placement-finding method that intentionally blocks opponents to fuck up their pathfinding (supply depots
	// are the best option for this)
	// TODO: add some space to the broadcast system that records planned movement, so that robots don't crash into each other.
}
