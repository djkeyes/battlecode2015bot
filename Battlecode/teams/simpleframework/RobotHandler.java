package simpleframework;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public abstract class RobotHandler {

	public static RobotHandler createHandler(RobotController rc) {
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

	protected RobotHandler(RobotController rc) {
		this.rc = rc;
		gen = new Random(rc.getID());

	}

	public final void run() {
		try {
			// do we want to make an init() method here?
			// TODO: there are 2 bugs here
			// 1. we record robot births, but not robot deaths, making these counts innaccurate when a robot dies. track deaths
			// somehow.
			// 2. we don't record when we've STARTED building a robot, only when it gets completely, track in-progress robots and
			// buildings somehow.
			int count = BroadcastInterface.getRobotCount(rc, rc.getType());
			BroadcastInterface.setRobotCount(rc, rc.getType(), ++count);

		} catch (GameActionException ex) {
			// we should probably decide on an exception behavior. like "never get into a situation where you might throw one."
			ex.printStackTrace();
		}
		while (true) {
			try {
				doTheThing();
				distributeSupply();
				rc.yield();
			} catch (GameActionException ex) {
				// we should probably decide on an exception behavior. like "never get into a situation where you might throw one."
				ex.printStackTrace();
			}
		}
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
			rc.transferSupplies((int) transferAmount, suppliesToThisLocation);
		}

	}

	protected abstract void doTheThing() throws GameActionException;

	private static class NullRobotHandler extends RobotHandler {
		protected NullRobotHandler(RobotController rc) {
			super(rc);
		}

		protected void doTheThing() {
			// do nothing
		}
	}

	// utility methods
	// TODO: add a unit direction-finding method that paths toward or away from opponents
	// TODO: add a building placement-finding method that avoids blocking paths
	// TODO: add a building placement-finding method that intentionally blocks opponents to fuck up their pathfinding (supply depots
	// are the best option for this)
	// TODO: add some space to the broadcast system that records planned movement, so that robots don't crash into each other.
	// TODO: perform pathfinding (probably done when robots have extra bytecodes to spare)
	// TODO: add a "spend excess time computing" function for robots who are done computing and have extra bytecodes
	public Direction findRandomSpawningDirection(RobotType type) {
		Direction[] randomDirections = Util.getRandomDirectionOrdering(gen);

		for (int i = 0; i < randomDirections.length; i++) {
			if (rc.canSpawn(randomDirections[i], type)) {
				return randomDirections[i];
			}
		}
		return null;
	}

	public Direction findRandomBuildDirection(RobotType type) {
		Direction[] randomDirections = Util.getRandomDirectionOrdering(gen);

		for (int i = 0; i < randomDirections.length; i++) {
			if (rc.canBuild(randomDirections[i], type)) {
				return randomDirections[i];
			}
		}
		return null;
	}

	public Direction findRandomMoveDirection() {
		Direction[] randomDirections = Util.getRandomDirectionOrdering(gen);

		for (int i = 0; i < randomDirections.length; i++) {
			if (rc.canMove(randomDirections[i])) {
				return randomDirections[i];
			}
		}
		return null;
	}

	// returns true if we found a target to attack
	public boolean attackNearby() throws GameActionException {
		RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(rc.getLocation(), rc.getType().attackRadiusSquared, rc.getTeam()
				.opponent());
		// TODO pick a good target, like the closest one or the weakest one
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
