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
import betterframework.Util.MapConfiguration;

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
	private MapLocation ourHq = null;

	protected BaseRobotHandler(RobotController rc) {
		this.rc = rc;
		gen = new Random(rc.getID());

		ourHq = rc.senseHQLocation();
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
				rc.setIndicatorString(0, "onExcessBytecodes()");
				while (Clock.getBytecodeNum() < maxBytecodesToUse()) {
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
		// this is a randomized pathfinding algorithm--it's much less efficient than a distributed BFS, but it's more resilient to
		// robot failure/death/bytecode limits

		// pick a location, then do pathfinding
		MapLocation randomLoc;
		// generate random tiles until we get one that's actually visible
		while (rc.senseTerrainTile(randomLoc = getRandomPathfindingTile()) != TerrainTile.NORMAL)
			;
		updateDistances(randomLoc);
	}

	private MapLocation getRandomPathfindingTile() {
		// what is a good place to pathfind?
		// well, locations near us seem good
		// also locations near the hq seem good (since we start there)
		// also eventually we'll want to get every location
		// so pick one of those 3 options
		double result = gen.nextDouble();
		if (result < 1. / 3.) {
			// pick one of the tiles near us
			return getLocationNear(rc.getLocation());
		} else if (result < 2. / 3.) {
			// pick a tile near hq
			return getLocationNear(ourHq);
		} else {
			// pick a tile anywhere
			int x = gen.nextInt(2 * GameConstants.MAP_MAX_WIDTH + 1) - GameConstants.MAP_MAX_WIDTH;
			int y = gen.nextInt(2 * GameConstants.MAP_MAX_HEIGHT + 1) - GameConstants.MAP_MAX_HEIGHT;
			return ourHq.add(x, y);
		}
	}

	private MapLocation getLocationNear(MapLocation orig) {
		int x = gen.nextInt(7) - 5;
		int y = gen.nextInt(7) - 5;
		return orig.add(x, y);
	}

	private void updateDistances(MapLocation randomLoc) throws GameActionException {
		// find the smallest non-zero distance (zero indicates unknown distance)
		int curDist = getDistanceFromOurHq(randomLoc);
		int minDist = Integer.MAX_VALUE;
		for (Direction d : Util.actualDirections) {
			MapLocation nextLoc = randomLoc.add(d);
			int dist = getDistanceFromOurHq(nextLoc);
			if (dist != 0) {
				minDist = Math.min(minDist, dist);
			}
		}
		if (minDist < Integer.MAX_VALUE) {
			if (curDist == 0 || minDist < curDist) {
				int dist = minDist + 1;
				BroadcastInterface.setDistance(rc, randomLoc.x, randomLoc.y, dist);
			}
		}
	}

	public void onException(GameActionException ex) {
		// TODO: only do this in debug mode. we don't want to crash and burn in an actual contest.
		ex.printStackTrace();
	}

	public void init() throws GameActionException {
	}

	protected void distributeSupply() throws GameActionException {
		RobotInfo[] nearbyAllies = rc.senseNearbyRobots(rc.getLocation(), GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED,
				rc.getTeam());
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
			try {
				rc.transferSupplies((int) transferAmount, suppliesToThisLocation);
			} catch (Exception e) {
				// this should be fixed. if it hasn't happened for several commits, just delete these lines.
				// I think this happens when a unit misses the turn change (too many bytecodes)
				System.out.println("exception while sending supplies to " + suppliesToThisLocation);
				// e.printStackTrace();
			}
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
			// TODO figure out a good way to determine targets
			// this picks the weakest one, but we might also want to kill specific types (commanders? beavers?) or use other criteria
			// (can my allies and I 1-shot it?)
			RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(rc.getLocation(), rc.getType().attackRadiusSquared, rc.getTeam()
					.opponent());
			if (nearbyEnemies.length > 0) {
				MapLocation enemyLoc = nearbyEnemies[0].location;
				double minHealth = nearbyEnemies[0].health;
				for (RobotInfo info : nearbyEnemies) {
					if (info.health < minHealth) {
						enemyLoc = info.location;
						minHealth = info.health;
					}
				}
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
				for (Direction adjDir : Util.getRandomDirectionOrdering(gen)) {
					MapLocation adjLoc = rc.getLocation().add(adjDir);
					if (rc.canMove(adjDir)) {
						int adjDist = getDistanceFromOurHq(adjLoc);
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

	public class MoveTowardEnemyHq implements Action {
		@Override
		public boolean run() throws GameActionException {
			if (rc.isCoreReady()) {
				int minDist = Integer.MAX_VALUE;
				Direction nextDir = null;
				for (Direction adjDir : Util.actualDirections) {
					MapLocation adjLoc = rc.getLocation().add(adjDir);
					if (rc.canMove(adjDir)) {
						int adjDist = getDistanceFromEnemyHq(adjLoc);
						// 0 indicates unexplored tiles
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
						int adjDist = getDistanceFromOurHq(adjLoc);
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

				// use a random direction ordering, so we don't deplete all the ore in one place
				for (Direction d : Util.getRandomDirectionOrdering(gen)) {
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
				return Math.min(orePresent, Math.max(
						Math.min(GameConstants.BEAVER_MINE_MAX, orePresent / (double) GameConstants.BEAVER_MINE_RATE),
						GameConstants.MINIMUM_MINE_AMOUNT));
			} else {
				return Math.min(orePresent, Math.max(
						Math.min(GameConstants.MINER_MINE_MAX, orePresent / (double) GameConstants.MINER_MINE_RATE),
						GameConstants.MINIMUM_MINE_AMOUNT));
			}
		}
	}

	// utility methods
	// TODO: add a building placement-finding method that avoids blocking paths
	// TODO: add a building placement-finding method that intentionally blocks opponents to fuck up their pathfinding (supply depots
	// are the best option for this)
	// TODO: add some space to the broadcast system that records planned movement, so that robots don't crash into each other.

	public int getDistanceFromOurHq(MapLocation target) throws GameActionException {
		return BroadcastInterface.readDistance(rc, target.x, target.y);
	}

	public int getDistanceFromEnemyHq(MapLocation target) throws GameActionException {
		MapConfiguration configuration = getMapConfiguration();
		float[] midpoint = getCachedMidpoint();

		MapLocation transformed = null;
		switch (configuration) {
		case ROTATION:
			transformed = Util.rotateAround(midpoint, target);
			break;
		case HORIZONTAL_REFLECTION:
			transformed = Util.reflectHorizontallyAccross(midpoint, target);
			break;
		case VERTICAL_REFLECTION:
			transformed = Util.reflectVerticallyAccross(midpoint, target);
			break;
		case DIAGONAL_REFLECTION:
			transformed = Util.reflectDiagonallyAccross(midpoint, target);
			break;
		case INVERSE_DIAGONAL_REFLECTION:
			transformed = Util.reflectInvDiagonallyAccross(midpoint, target);
			break;
		default:
			// this should never happen
			transformed = Util.rotateAround(midpoint, target);
			break;
		}

		return BroadcastInterface.readDistance(rc, transformed.x, transformed.y);
	}

	// caches the map configuration, since it (probably) won't change.
	private MapConfiguration getMapConfiguration() throws GameActionException {
		if (cachedConfiguration != null) {
			return cachedConfiguration;
		}
		// we can only pick one map configuration, so these are given in trump order
		int bitmask = BroadcastInterface.getConfigurationBitmask(rc);
		if (Util.decodeRotation(bitmask)) {
			cachedConfiguration = Util.MapConfiguration.ROTATION;
		} else if (Util.decodeHorizontalReflection(bitmask)) {
			cachedConfiguration = Util.MapConfiguration.HORIZONTAL_REFLECTION;
		} else if (Util.decodeVerticalReflection(bitmask)) {
			cachedConfiguration = Util.MapConfiguration.VERTICAL_REFLECTION;
		} else if (Util.decodeDiagonalReflection(bitmask)) {
			cachedConfiguration = Util.MapConfiguration.DIAGONAL_REFLECTION;
		} else if (Util.decodeReverseDiagonalReflection(bitmask)) {
			cachedConfiguration = Util.MapConfiguration.INVERSE_DIAGONAL_REFLECTION;
		}

		return cachedConfiguration;
	}

	private float[] getCachedMidpoint() throws GameActionException {
		if (cachedMidpoint != null) {
			return cachedMidpoint;
		}

		return cachedMidpoint = BroadcastInterface.getConfigurationMidpoint(rc);
	}

	private MapConfiguration cachedConfiguration = null;
	private float[] cachedMidpoint = null;
}
