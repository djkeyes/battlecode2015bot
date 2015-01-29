package bloombot;

import java.util.List;
import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
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
	protected Strategy curStrategy;

	protected BaseRobotHandler(RobotController rc) {
		this.rc = rc;
		gen = new Random(rc.getID());

		try {
			// notably, if this is the HQ on round 0, the strategy won't be set yet.
			curStrategy = Strategy.getStrategy(rc);
		} catch (GameActionException e) {
		}
	}

	protected BaseRobotHandler(RobotController rc, boolean lowPower) {
		this.rc = rc;
	}

	public int maxBytecodesToUse() {
		if (rc.getSupplyLevel() <= 1.0) {
			// if we're unsupplied, we get a bunch of free bytecodes
			return 4000;
		} else {
			// however if we have supply, those bytecodes cost supply
			// weird, eh?
			return 1500;
		}
	}

	public void run() {
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
		// this method tends to use between 1000 and 3000 bytecodes per iteration =/
		// pick a location, then do pathfinding
		int[] coords = BroadcastInterface.dequeuePathfindingQueue(rc);
		if (coords != null) {
			MapLocation curLoc = new MapLocation(coords[0], coords[1]);
			updateDistances(curLoc);
		}
	}

	private void updateDistances(MapLocation curLoc) throws GameActionException {
		// find the smallest non-zero distance (zero indicates unknown distance)
		// int startBytecodes = Clock.getBytecodeNum();
		int curDist = getDistanceFromOurHq(curLoc);
		// 287
		boolean hasUnknownTiles = false;
		for (Direction d : Util.actualDirections) {
			MapLocation nextLoc = curLoc.add(d);
			int dist = getDistanceFromOurHq(nextLoc);
			TerrainTile tileType = rc.senseTerrainTile(nextLoc);

			if (tileType == TerrainTile.NORMAL) {
				if (dist == 0 || dist > curDist + 1) {
					BroadcastInterface.setDistance(rc, nextLoc.x, nextLoc.y, curDist + 1, getOurHqLocation());
					BroadcastInterface.enqueuePathfindingQueue(rc, nextLoc.x, nextLoc.y);
				}
			} else if (tileType == TerrainTile.UNKNOWN) {
				hasUnknownTiles = true;
			}

			if (Clock.getBytecodeNum() > maxBytecodesToUse()) {
				// if we run out of bytecodes, end early and let someone else do the rest
				// TODO: in this case, the coordinates really should be added to the front, not the back, of the queue, so that someone
				// else can pick up where we left off.
				hasUnknownTiles = true;
				break;
			}
		}
		if (hasUnknownTiles) {
			BroadcastInterface.enqueuePathfindingQueue(rc, curLoc.x, curLoc.y);
		}
	}

	public void onException(GameActionException ex) {
		// TODO: only do this in debug mode. we don't want to crash and burn in an actual contest.
		ex.printStackTrace();
	}

	public void init() throws GameActionException {
	}

	private boolean isSupplyLow = false;

	// return true if there was a transfer
	protected boolean distributeSupply() throws GameActionException {
		// ask couriers for supply, if they're available
		if (!rc.getType().isBuilding) {
			double supply = rc.getSupplyLevel();
			if (!isSupplyLow && supply <= 500) {
				BroadcastInterface.enqueueSupplyQueue(rc, rc.getID());
				isSupplyLow = true;
			} else if (supply > 500) {
				isSupplyLow = false;
			}
		}

		// if there are a lot of nearby allies, we might run out of bytecodes.
		// invoking transferSupplies() costs us 500 bytecodes (it's really expensive!)
		if (!hasTimeToTransferSupply()) {
			return false;
		}
		RobotInfo[] nearbyAllies = rc.senseNearbyRobots(rc.getLocation(), GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, rc.getTeam());
		double lowestSupply;
		if (rc.getType().isBuilding) {
			lowestSupply = Double.MAX_VALUE;
		} else {
			lowestSupply = rc.getSupplyLevel();
		}
		MapLocation suppliesToThisLocation = null;

		for (RobotInfo ri : nearbyAllies) {
			if (!hasTimeToTransferSupply()) {
				break;
			}

			if (ri.type == RobotType.MISSILE) { // missiles don't need supply
				continue;
			}

			if (ri.type.isBuilding) {
				// buildings don't need supply
				// sometimes buildings will transfer supply to each other if there aren't units nearby, but that's handled by a
				// subclass
				continue;
			}
			if (ri.supplyLevel < lowestSupply) {
				lowestSupply = ri.supplyLevel;
				suppliesToThisLocation = ri.location;
			}
		}
		if (suppliesToThisLocation != null) {
			double transferAmount = 0;
			if (rc.getType().isBuilding) {
				transferAmount = rc.getSupplyLevel();
			} else {
				transferAmount = (rc.getSupplyLevel() - lowestSupply) / 2;
			}

			try {
				rc.transferSupplies((int) transferAmount, suppliesToThisLocation);
				return true;
			} catch (Exception e) {
				// this should be fixed. if it hasn't happened for several commits, just delete these lines.
				// I think this happens when a unit misses the turn change (too many bytecodes)
				System.out.println("exception while sending supplies to " + suppliesToThisLocation
						+ ". Did this robot run out of bytecodes?");
				// e.printStackTrace();
			}
		}

		return false;
	}

	// some default action implementations

	protected boolean hasTimeToTransferSupply() {
		int maxBytecodesForTransfer = maxBytecodesToUse() - 500;
		return (Clock.getBytecodeNum() < maxBytecodesForTransfer);
	}

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
			RobotInfo[] nearbyEnemies = senseNearbyEnemies();
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

		public RobotInfo[] senseNearbyEnemies() throws GameActionException {
			return rc.senseNearbyRobots(rc.getLocation(), rc.getType().attackRadiusSquared, rc.getTeam().opponent());
		}
	}

	// attacks, but retreats during cooldowns
	public class AttackCautiously extends Attack {
		private Action attack = new Attack();

		private boolean retreatOnWeaponCooldown;
		private Action retreat;

		public AttackCautiously(boolean retreatOnWeaponCooldown) {
			this.retreatOnWeaponCooldown = retreatOnWeaponCooldown;
			if (retreatOnWeaponCooldown) {
				retreat = new Retreat();
			}
		}

		@Override
		public boolean run() throws GameActionException {
			if (retreatOnWeaponCooldown) {
				if (rc.getWeaponDelay() > rc.getType().cooldownDelay / 2) {
					return retreat.run();
				}
			} else {
				if (rc.getWeaponDelay() > 0) {
					return true;
				}
			}
			return attack.run();
		}

	}

	// move this unit to an unexplored square
	// this is implemented by simple moving further away from the HQ--however, this means we can get stuck.
	// to counteract this, we move randomly for a number of turns, but that's obvious sub-optimal
	public class ScoutOutward implements Action {
		private boolean avoidEnemyTowers;
		private boolean avoidEnemyUnits;

		public ScoutOutward(boolean avoidEnemyTowers, boolean avoidEnemyUnits) {
			this.avoidEnemyTowers = avoidEnemyTowers;
			this.avoidEnemyUnits = avoidEnemyUnits;
		}

		private int turnsToRandomize = 0;
		private final int turnsToRandomizeIncrement = 10;

		@Override
		public boolean run() throws GameActionException {
			if (rc.isCoreReady()) {
				if (turnsToRandomize > 0) {
					Util.resetRandomDirectionOrdering(gen);
					for (Direction adjDir : Util.getRandomDirectionOrdering(gen)) {
						if (rc.canMove(adjDir)) {
							rc.move(adjDir);
							turnsToRandomize--;
							return true;
						}
					}
					turnsToRandomize--;
					return false;
				} else {
					RobotInfo[] nearbyEnemies = null;
					if (avoidEnemyUnits) {
						nearbyEnemies = getNearbyEnemies();
					}
					int maxDist = getDistanceFromOurHq(rc.getLocation());
					Direction nextDir = null;
					for (Direction adjDir : Util.getRandomDirectionOrdering(gen)) {
						MapLocation adjLoc = rc.getLocation().add(adjDir);
						if (rc.canMove(adjDir)) {
							if (!avoidEnemyTowers || inEnemyHqOrTowerRange(adjLoc)) {
								if (!avoidEnemyUnits || inEnemyRange(adjLoc, nearbyEnemies)) {
									int adjDist = getDistanceFromOurHq(adjLoc);
									if (adjDist > maxDist) {
										maxDist = adjDist;
										nextDir = adjDir;
									}
								}
							}
						}
					}
					if (nextDir != null) {
						rc.move(nextDir);
						return true;
					} else {
						turnsToRandomize += turnsToRandomizeIncrement;
						return false;
					}
				}
			}
			return false;
		}
	}

	public class MoveToFrontier implements Action {

		@Override
		public boolean run() throws GameActionException {
			// get the movement direction recommended for the current tile
			Direction recommendedDir = BroadcastInterface.getScoutingDirection(rc);
			if(recommendedDir != null){
				for(Direction d : Util.getDirectionsStrictlyToward(recommendedDir)){
					if(rc.canMove(d)){
						rc.move(d);
						break;
					}
				}
			}
			// TODO: if we haven't calculated that yet, just move away from the hq or something smart
			return false;
		}
	}
	
	public class MoveTo implements Action {
		private boolean avoidTowers;
		private boolean avoidEnemiesAndTowers;
		private MapLocation target;

		public MoveTo(MapLocation target, boolean avoidTowers, boolean avoidEnemiesAndTowers) {
			this.target = target;
			this.avoidTowers = avoidTowers;
			this.avoidEnemiesAndTowers = avoidEnemiesAndTowers;
		}

		// bug mode state
		private int distSqToHqAtBugModeStart;
		private boolean inBugMode = false;
		private int lastRoundInBugMode = -2;
		private MapLocation lastWall;
		private MapLocation bugModeStartLocation;
		private MapLocation[] lastNBugModeLocations;
		private int bugModeLocationsToStore = 10;
		private int bugModeLocationIterator = 0;
		private boolean isGoingLeft;

		@Override
		public boolean run() throws GameActionException {

			if (inBugMode) {
				// even if we have a delay cooldown, check to see if we're still in bug mode
				// if another Action pre-empted us (and so we skipped a round) then we should stop bugging
				if (lastRoundInBugMode == Clock.getRoundNum() - 1) {
					lastRoundInBugMode++;
				} else {
					inBugMode = false;
				}
			}

			if (rc.isCoreReady()) {
				// bug navigation
				return bugNavigate();
			}

			return false;
		}

		public boolean bugNavigate() throws GameActionException {
			// how does bug navigation work? first, you need a metric, like euclidian distance to hq.
			// first, you follow the metric. however, if you get stuck, you enter bug mode. You also pick a direction ordering to
			// follow, clockwise or counterclockwise.
			// BUG MODE:
			// 1. save startDist = euclidianDist(curLoc, hq)
			// 2. pick the wall orthogonal to you, relative to your previous direction
			// 3. go in your favorite direction ordering, relative to the wall, and save the direction you went
			// 4. if you are now closer to the hq than you were when you started (euclidianDist(curLoc, hq) < startDist),
			// leave bug mode
			// 4b. (alternatively, if you have a BFS distance, leave bug mode. but this is handled trivially, you'll see.)
			// 5. otherwise, go to step 2.

			Direction[] traversableDirections = getTraversableDirections(avoidTowers, avoidEnemiesAndTowers);

			if (inBugMode) {
				if (target.distanceSquaredTo(rc.getLocation()) < distSqToHqAtBugModeStart) {
					inBugMode = false;
				}
				// cycle detection
				// this is helpful if we were blocked by another bot when we entered bug mode, but now we're okay
				// or if we're going the wrong bug mode direction
				// or something
				// actually sometimes this doesn't help at all. =/
				if (bugModeStartLocation.equals(rc.getLocation())) {
					inBugMode = false;
				}
			}

			if (!inBugMode) {
				int curDist = target.distanceSquaredTo(rc.getLocation());
				int minDist = curDist;
				Direction nextDir = null;
				for (int i = 0; i < traversableDirections.length && traversableDirections[i] != null; i++) {
					Direction adjDir = traversableDirections[i];
					MapLocation adjLoc = rc.getLocation().add(adjDir);
					int adjDist = target.distanceSquaredTo(adjLoc);
					if (adjDist < minDist) {
						minDist = adjDist;
						nextDir = adjDir;
					}
				}
				if (nextDir != null) {
					rc.move(nextDir);
					return true;
				} else {
					inBugMode = true;
					distSqToHqAtBugModeStart = curDist;
					isGoingLeft = gen.nextBoolean();
					Direction hqDir = rc.getLocation().directionTo(target);
					lastWall = rc.getLocation().add(hqDir);
					bugModeStartLocation = rc.getLocation();

					bugModeLocationIterator = 0;
					lastNBugModeLocations = new MapLocation[bugModeLocationsToStore];
				}

			}
			if (inBugMode) {
				// BUGMODE

				// more cycle detection
				for (int i = 0; i < lastNBugModeLocations.length; i++) {
					if (lastNBugModeLocations[i] == null) {
						break;
					}
					if (rc.getLocation().equals(lastNBugModeLocations[i])) {
						inBugMode = false;
						return false;
					}
				}
				lastNBugModeLocations[bugModeLocationIterator++] = rc.getLocation();
				bugModeLocationIterator %= bugModeLocationsToStore;

				// if we're near a tower or enemy, it sometimes makes sense to just skirt back and forth around them,
				// rather than backtracking along other obstacles
				if (avoidEnemiesAndTowers || avoidTowers) {
					Direction[] nearEnemyTraversableDirections;
					boolean nearEnemy = false;
					boolean[] isDirNearEnemy = getIsDirNearTower(avoidEnemiesAndTowers);
					for (boolean b : isDirNearEnemy) {
						if (b) {
							nearEnemy = true;
							break;
						}
					}
					if (nearEnemy) {
						nearEnemyTraversableDirections = new Direction[8];
						int size = 0;
						for (int i = 0; i < traversableDirections.length && traversableDirections[i] != null; i++) {
							Direction adjDir = traversableDirections[i];
							if ((isDirNearEnemy[adjDir.rotateLeft().ordinal()] || isDirNearEnemy[adjDir.rotateRight().ordinal()])
									&& !isDirNearEnemy[adjDir.ordinal()]) {
								nearEnemyTraversableDirections[size++] = adjDir;
							}
						}
						traversableDirections = nearEnemyTraversableDirections;
					}
				}

				lastRoundInBugMode = Clock.getRoundNum();

				Direction facingDir = rc.getLocation().directionTo(lastWall);
				// find a traversable tile
				for (int i = 0; i < 8; i++) {
					if (isGoingLeft) {
						facingDir = facingDir.rotateLeft();
					} else {
						facingDir = facingDir.rotateRight();
					}
					// TODO: initialize traversableDirections to be in an order conducive to bug pathfinding, so we can just iterate
					// through it
					boolean isTraversable = false;
					for (Direction d : traversableDirections) {
						if (facingDir == d) {
							isTraversable = true;
							break;
						}
					}
					if (isTraversable) {
						if (isGoingLeft) {
							lastWall = rc.getLocation().add(facingDir.rotateRight());
						} else {
							lastWall = rc.getLocation().add(facingDir.rotateLeft());
						}
						rc.move(facingDir);
						return true;
					}
				}
			}

			return false;
		}

	}

	public class MoveTowardEnemyHq implements Action {
		private boolean avoidTowers;
		private boolean avoidEnemiesAndTowers;

		private Action bugNavigateToEnemyHq;

		public MoveTowardEnemyHq(boolean avoidTowers, boolean avoidEnemiesAndTowers) {
			this.avoidTowers = avoidTowers;
			this.avoidEnemiesAndTowers = avoidEnemiesAndTowers;
			this.bugNavigateToEnemyHq = new MoveTo(getEnemyHqLocation(), avoidTowers, avoidEnemiesAndTowers);
		}

		@Override
		public boolean run() throws GameActionException {
			// okay, here's the plan:
			// our BFS is (almost) optimal. so if it has results, just use them
			// if not though, we need a backup plan
			// scouting outward is a shitty backup plan. Bug Navigation is where the money is.

			if (rc.isCoreReady()) {
				if (bfsToHq()) {
					return true;
				}
			}

			return bugNavigateToEnemyHq.run();
		}

		public boolean bfsToHq() throws GameActionException {
			// quick check: if we don't know the BFS to our current location, we *probably* don't know the adjacent ones
			// this is sub-optimal, but saves us bytecodes
			if (getDistanceFromEnemyHq(rc.getLocation()) == 0) {
				return false;
			}

			Direction[] traversableDirections = getTraversableDirections(avoidTowers, avoidEnemiesAndTowers);

			int minDist = Integer.MAX_VALUE;
			Direction nextDir = null;
			for (int i = 0; i < traversableDirections.length && traversableDirections[i] != null; i++) {
				Direction adjDir = traversableDirections[i];
				MapLocation adjLoc = rc.getLocation().add(adjDir);
				int adjDist = getDistanceFromEnemyHq(adjLoc);
				// 0 indicates unexplored tiles
				if (adjDist != 0 && adjDist < minDist) {
					minDist = adjDist;
					nextDir = adjDir;
				}
			}
			if (nextDir != null) {
				rc.move(nextDir);
				return true;
			}
			return false;
		}
	}

	private boolean[] getIsDirNearTower(boolean alsoAvoidEnemies) {
		int roundNum = Clock.getRoundNum();
		if (alsoAvoidEnemies) {
			if (cacheTimeIsDirNearEnemyOrTower == roundNum) {
				return cachedIsDirNearEnemyOrTower;
			}

			RobotInfo[] nearbyEnemies = getNearbyEnemies();

			cachedIsDirNearEnemyOrTower = new boolean[Direction.values().length];
			for (Direction adjDir : Util.actualDirections) {
				MapLocation adjLoc = rc.getLocation().add(adjDir);
				if (rc.canMove(adjDir)) {
					if (inEnemyHqOrTowerRange(adjLoc) || inEnemyRange(adjLoc, nearbyEnemies)) {
						cachedIsDirNearEnemyOrTower[adjDir.ordinal()] = true;
					}
				}
			}

			cacheTimeIsDirNearEnemyOrTower = roundNum;
			return cachedIsDirNearEnemyOrTower;
		} else {
			if (cacheTimeIsDirNearTower == roundNum) {
				return cachedIsDirNearTower;
			}

			RobotInfo[] nearbyEnemies = getNearbyEnemies();

			cachedIsDirNearTower = new boolean[Direction.values().length];
			for (Direction adjDir : Util.actualDirections) {
				MapLocation adjLoc = rc.getLocation().add(adjDir);
				if (rc.canMove(adjDir)) {
					if (inEnemyHqOrTowerRange(adjLoc) || inEnemyRange(adjLoc, nearbyEnemies)) {
						cachedIsDirNearTower[adjDir.ordinal()] = true;
					}
				}
			}

			cacheTimeIsDirNearTower = roundNum;
			return cachedIsDirNearTower;
		}
	}

	private int cacheTimeIsDirNearEnemyOrTower;
	private boolean[] cachedIsDirNearEnemyOrTower;

	private int cacheTimeIsDirNearTower;
	private boolean[] cachedIsDirNearTower;

	// a round-based cache of some things relevant to pathing
	// this is a null-terminated list of traversable directions (sort of like a cstring)
	public Direction[] getTraversableDirections(boolean avoidTowers, boolean avoidEnemiesAndTowers) {
		int roundNum = Clock.getRoundNum();
		if (avoidEnemiesAndTowers) {
			if (cacheTimeTraversableDirectionsEAndT == roundNum) {
				return cachedTraversableDirectionsEAndT;
			}
		} else if (avoidTowers) {
			if (cacheTimeTraversableDirectionsT == roundNum) {
				return cachedTraversableDirectionsT;
			}
		} else {
			if (cacheTimeTraversableDirections == roundNum) {
				return cachedTraversableDirections;
			}
		}

		Direction[] result = new Direction[8];
		int size = 0;

		if (avoidTowers) {
			boolean[] isNearEnemy = getIsDirNearTower(avoidEnemiesAndTowers);
			for (Direction adjDir : Util.actualDirections) {
				if (rc.canMove(adjDir) && !isNearEnemy[adjDir.ordinal()]) {
					result[size++] = adjDir;
				}
			}
		} else {
			for (Direction adjDir : Util.actualDirections) {
				if (rc.canMove(adjDir)) {
					result[size++] = adjDir;
				}
			}
		}

		if (avoidEnemiesAndTowers) {
			cacheTimeTraversableDirectionsEAndT = roundNum;
			return cachedTraversableDirectionsEAndT = result;
		} else if (avoidTowers) {
			cacheTimeTraversableDirectionsT = roundNum;
			return cachedTraversableDirectionsT = result;
		} else {
			cacheTimeTraversableDirections = roundNum;
			return cachedTraversableDirections = result;
		}
	}

	private Direction[] cachedTraversableDirections = null;
	private int cacheTimeTraversableDirections = -1;
	private Direction[] cachedTraversableDirectionsT = null;
	private int cacheTimeTraversableDirectionsT = -1;
	private Direction[] cachedTraversableDirectionsEAndT = null;
	private int cacheTimeTraversableDirectionsEAndT = -1;

	// opposite of scouting outward. try to move back toward hq.
	public class Retreat implements Action {
		private boolean retreatOrthogonally;

		public Retreat() {
			this(false);
		}

		public Retreat(boolean retreatOrthogonally) {
			this.retreatOrthogonally = retreatOrthogonally;
		}

		@Override
		public boolean run() throws GameActionException {
			if (rc.isCoreReady()) {
				// quick check: if we don't know the BFS to our current location, we *probably* don't know the adjacent ones
				// this is sub-optimal, but saves us bytecodes
				int curDist = getDistanceFromOurHq(rc.getLocation());
				if (curDist == 0) {
					return false;
				}
				int minDist = curDist;
				Direction nextDir = null;
				for (Direction adjDir : Util.getRandomDirectionOrdering(gen)) {
					MapLocation adjLoc = rc.getLocation().add(adjDir);
					if (rc.canMove(adjDir) && !inEnemyHqOrTowerRange(adjLoc)) {
						int adjDist = getDistanceFromOurHq(adjLoc);
						if (adjDist != 0 && adjDist < minDist) {
							// for some units, it's smarter to expand out, rather than taking the shortest path
							if (retreatOrthogonally || adjDist >= curDist - 1) {
								minDist = adjDist;
								nextDir = adjDir;
							}
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

	public class AttackInAWave implements Action {
		// TODO: this needs to be totally reworked for bloombot.
		private MapLocation target = null;
		private Action curAction;
		private boolean avoidingTowers = false;
		private boolean goingToHq = false;

		@Override
		public boolean run() throws GameActionException {
			if (target != null && rc.canSenseLocation(target)) {
				RobotInfo curOccupant = rc.senseRobotAtLocation(target);
				if (curOccupant == null || curOccupant.type != RobotType.TOWER) {
					// WE GOT IT!
					target = null;
				}
			}
			if (target == null) {
				target = BroadcastInterface.getNextTarget(rc);

				if (target.equals(getEnemyHqLocation())) {
					curAction = new MoveTowardEnemyHq(true, false);
					goingToHq = true;
				} else {
					curAction = new MoveTo(target, true, false);
					goingToHq = false;
				}
				avoidingTowers = true;
			}

			boolean avoidTowers = !BroadcastInterface.getAdvanceBit(rc);
			// if we were avoiding towers but now we don't have to, change the actions
			if (avoidingTowers && !avoidTowers) {
				if (target.equals(getEnemyHqLocation())) {
					curAction = new MoveTowardEnemyHq(false, false);
				} else {
					curAction = new MoveTo(target, false, false);
				}
			}

			if (nearTarget()) {
				BroadcastInterface.incrementAlliesInPosition(rc);
			}
			return curAction.run();
		}

		private boolean nearTarget() {
			int distSqThresh;
			if (goingToHq) {
				distSqThresh = 81;
			} else {
				distSqThresh = 64;
			}
			return rc.getLocation().distanceSquaredTo(target) <= distSqThresh;
		}
	}

	// only beavers and miners can mine
	// this also includes some exploratory behavior, which is triggered if there's better mineral patches nearby

	// TODO: I just made these numbers up.
	// can we do some testing to figure out optimial behavior?
	public static final double MIN_ORE_PER_TURN_THRESHOLD = 0.2;
	public static final double MIN_ADJ_ORE_PER_TURN_TO_REPORT_ABUNDANT_THRESHOLD = 0.5;

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

				int numAbundant = 0;
				if (maxOre > MIN_ADJ_ORE_PER_TURN_TO_REPORT_ABUNDANT_THRESHOLD) {
					numAbundant++;
				}
				// use a random direction ordering, so we don't deplete all the ore in one place
				for (Direction d : Util.getRandomDirectionOrdering(gen)) {
					MapLocation adjLoc = rc.getLocation().add(d);
					double adjOre = miningRate(rc.senseOre(adjLoc), isBeaver);
					if (adjOre > MIN_ADJ_ORE_PER_TURN_TO_REPORT_ABUNDANT_THRESHOLD) {
						numAbundant++;
					}
					if (rc.canMove(d)) {
						if (!inEnemyHqOrTowerRange(adjLoc)) {
							if (adjOre > maxOre) {
								moveDir = d;
								maxOre = adjOre;
							}
						}
					}
				}
				if (numAbundant >= 2) {
					BroadcastInterface.incrementAbundantOre(rc);
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

	public boolean inRobotRange(MapLocation loc, RobotInfo[] robots) {
		for (RobotInfo enemy : robots) {
			if (loc.distanceSquaredTo(enemy.location) <= enemy.type.attackRadiusSquared) {
				return true;
			}
		}
		return false;
	}

	public boolean inEnemyHqOrTowerRange(MapLocation loc) {
		MapLocation enemyHq = getEnemyHqLocation();
		MapLocation[] enemyTowers = getEnemyTowerLocations();
		for (MapLocation enemyTower : enemyTowers) {
			if (loc.distanceSquaredTo(enemyTower) <= RobotType.TOWER.attackRadiusSquared) {
				return true;
			}
		}
		int hqAttackRadius;
		if (enemyTowers.length >= 5) {
			hqAttackRadius = 52;
		} else if (enemyTowers.length >= 2) {
			hqAttackRadius = GameConstants.HQ_BUFFED_ATTACK_RADIUS_SQUARED;
		} else {
			hqAttackRadius = RobotType.HQ.attackRadiusSquared;
		}
		if (loc.distanceSquaredTo(enemyHq) <= hqAttackRadius) {
			return true;
		}
		return false;
	}

	public boolean inEnemyRange(MapLocation loc, RobotInfo[] nearbyEnemies) {
		for (RobotInfo enemy : nearbyEnemies) {
			if (enemy.type == RobotType.BEAVER || enemy.type == RobotType.MINER) {
				continue;
			}
			if (loc.distanceSquaredTo(enemy.location) <= enemy.type.attackRadiusSquared) {
				return true;
			}
		}
		return false;
	}

	public RobotInfo[] getNearbyEnemies() {
		int roundNum = Clock.getRoundNum();
		if (cacheTimeNearbyEnemies == roundNum) {
			return cachedNearbyEnemies;
		}

		// the longest ranged unit (excluding the launcher) is the tank, with square range 15
		// most people who call this method are worried about pathfinding, so if we take 1 step from the current position,
		// we will be concerned with tanks within range 25, at most
		// (tanks can hit an offset (2, 3) away, which means if we move one tile, they can hit an offset (3, 4) away).
		int searchRangeSq = 25;
		cacheTimeNearbyEnemies = roundNum;
		return cachedNearbyEnemies = rc.senseNearbyRobots(searchRangeSq, rc.getTeam().opponent());

	}

	private RobotInfo[] cachedNearbyEnemies = null;
	private int cacheTimeNearbyEnemies = -1;

	public int getDistanceFromOurHq(MapLocation target) throws GameActionException {
		return BroadcastInterface.readDistance(rc, target.x, target.y, getOurHqLocation());
	}

	public int getDistanceFromEnemyHq(MapLocation target) throws GameActionException {
		// MapLocation transformed = getSymmetricLocation(target);
		//
		// return BroadcastInterface.readDistance(rc, transformed.x, transformed.y, getOurHqLocation());
		return 0;
	}

	// some cachable things
	// if we had more bytecodes, it might be cool to implement this as some kind of generic class
	public MapLocation[] getOurTowerLocations() {
		int roundNum = Clock.getRoundNum(); // TODO: does this cost (a nontrivial amount of) bytecodes? or is it just an accessor?
		if (cacheTimeOurTowerLocations == roundNum) {
			return cachedOurTowerLocations;
		}
		cacheTimeOurTowerLocations = roundNum;
		return cachedOurTowerLocations = rc.senseTowerLocations();
	}

	private MapLocation[] cachedOurTowerLocations = null;
	private int cacheTimeOurTowerLocations = -1;

	public MapLocation getOurHqLocation() {
		if (cachedOurHqLocation != null) {
			return cachedOurHqLocation;
		}
		return cachedOurHqLocation = rc.senseHQLocation();
	}

	private MapLocation cachedOurHqLocation = null;

	public MapLocation[] getEnemyTowerLocations() {
		int roundNum = Clock.getRoundNum();
		if (cacheTimeEnemyTowerLocations == roundNum) {
			return cachedEnemyTowerLocations;
		}
		cacheTimeEnemyTowerLocations = roundNum;
		return cachedEnemyTowerLocations = rc.senseEnemyTowerLocations();
	}

	private MapLocation[] cachedEnemyTowerLocations = null;
	private int cacheTimeEnemyTowerLocations = -1;

	public MapLocation getEnemyHqLocation() {
		if (cachedEnemyHqLocation != null) {
			return cachedEnemyHqLocation;
		}
		return cachedEnemyHqLocation = rc.senseEnemyHQLocation();
	}

	private MapLocation cachedEnemyHqLocation = null;
}
