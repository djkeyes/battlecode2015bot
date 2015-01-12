package simpleframework;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class BeaverHandler extends RobotHandler {

	protected BeaverHandler(RobotController rc) {
		super(rc);
	}

	@Override
	protected void doTheThing() throws GameActionException {
		if (rc.isCoreReady()) {
			// beavers can do a lot of things
			// they can move, attack, mine, build, or just stay still
			// we need to figure out some kind of way to value these options

			updatePriorities();

			BeaverAction result = chooseAndRunAction();

		}
	}

	private BeaverAction chooseAndRunAction() throws GameActionException {
		// for now, the options are ranked from best to worst by getPrioritizedActions()
		List<BeaverAction> rankedActions = getPrioritizedActions();
		// System.out.println(rankedActions);

		for (BeaverAction action : rankedActions) {
			switch (action) {
			case IDLE:
				return action;
			case MINE:
				// mine if there's ore, otherwise continue looping
				if (rc.canMine() && rc.senseOre(rc.getLocation()) > 0) {
					rc.mine();
					return action;
				} else {
					continue;
				}
			case MOVE:
				Direction moveDir = findRandomMoveDirection();
				if (moveDir != null) {
					rc.move(moveDir);
					return action;
				}
			case BUILD_AEROSPACE_LAB:
				if (tryToBuild(RobotType.AEROSPACELAB)) {
					return action;
				}
				continue;
			case BUILD_BARRACKS:
				if (tryToBuild(RobotType.BARRACKS)) {
					return action;
				}
				continue;
			case BUILD_HANDWASH_STATION:
				if (tryToBuild(RobotType.HANDWASHSTATION)) {
					return action;
				}
				continue;
			case BUILD_HELIPAD:
				if (tryToBuild(RobotType.HELIPAD)) {
					return action;
				}
				continue;
			case BUILD_MINER_FACTORY:
				if (tryToBuild(RobotType.MINERFACTORY)) {
					return action;
				}
				continue;
			case BUILD_SUPPLY_DEPOT:
				if (tryToBuild(RobotType.SUPPLYDEPOT)) {
					return action;
				}
				continue;
			case BUILD_TANK_FACTORY:
				if (tryToBuild(RobotType.TANK)) {
					return action;
				}
				continue;
			case BUILD_TECHNOLOGY_INSTITUTE:
				if (tryToBuild(RobotType.TECHNOLOGYINSTITUTE)) {
					return action;
				}
				continue;
			case BUILD_TRAINING_FIELD:
				if (tryToBuild(RobotType.TRAININGFIELD)) {
					return action;
				}
				continue;
			case ATTACK:
				if(attackNearby()){
					return action;
				}
				continue;
			}
		}
		return null;
	}

	/**
	 * updates the priorities for future actions TODO: share this information with other robots
	 * 
	 * @throws GameActionException
	 */
	private void updatePriorities() throws GameActionException {
		// TODO: this method takes up way too much time
		// we should determine priorities collaboratively through broadcasts
		
		int mapSizeSq = GameConstants.MAP_MAX_HEIGHT * GameConstants.MAP_MAX_HEIGHT + GameConstants.MAP_MAX_WIDTH
				* GameConstants.MAP_MAX_WIDTH;
		RobotInfo[] teamRobots = rc.senseNearbyRobots(mapSizeSq, rc.getTeam());

		int numAero = BroadcastInterface.getRobotCount(rc, RobotType.AEROSPACELAB); // TODO: these aren't updated until the robots are completed. fix that?
		int numBarracks = BroadcastInterface.getRobotCount(rc, RobotType.BARRACKS);
		int numHandWash = BroadcastInterface.getRobotCount(rc, RobotType.HANDWASHSTATION);
		int numHeli = BroadcastInterface.getRobotCount(rc, RobotType.HELIPAD);
		int numMinerFact = BroadcastInterface.getRobotCount(rc, RobotType.MINERFACTORY);
		int numSupply = BroadcastInterface.getRobotCount(rc, RobotType.SUPPLYDEPOT);
		int numTank = BroadcastInterface.getRobotCount(rc, RobotType.TANK);
		int numTech = BroadcastInterface.getRobotCount(rc, RobotType.TECHNOLOGYINSTITUTE);
		int numTrain = BroadcastInterface.getRobotCount(rc, RobotType.TRAININGFIELD);

		// here's the gameplan:
		// if we have nothing, make some miner factories
		// else if we don't have a supply depot, build one,
		// else prioritize tech, rax, and heli
		// if those are all done, make a train, tank, and aero
		// if those are all done, make more helis and handwashes
		if (numMinerFact < 3) {
			weights.put(BeaverAction.BUILD_MINER_FACTORY, 1.0);
		} else if (numSupply == 0) {
			weights.put(BeaverAction.BUILD_SUPPLY_DEPOT, 1.0);
			weights.put(BeaverAction.BUILD_MINER_FACTORY, 0.0);
		} else if (numTech == 0 || numBarracks == 0 || numHeli == 0) {
			if (numTech == 0)
				weights.put(BeaverAction.BUILD_TECHNOLOGY_INSTITUTE, 1.0);
			else
				weights.put(BeaverAction.BUILD_TECHNOLOGY_INSTITUTE, 0.0);
			if (numBarracks == 0)
				weights.put(BeaverAction.BUILD_BARRACKS, 1.0);
			else
				weights.put(BeaverAction.BUILD_BARRACKS, 0.0);
			if (numHeli == 0)
				weights.put(BeaverAction.BUILD_HELIPAD, 1.0);
			else
				weights.put(BeaverAction.BUILD_HELIPAD, 0.0);
			weights.put(BeaverAction.BUILD_SUPPLY_DEPOT, 0.0);
		} else if (numTrain == 0 || numTank == 0 || numAero == 0) {
			if (numTrain == 0)
				weights.put(BeaverAction.BUILD_TRAINING_FIELD, 1.0);
			else
				weights.put(BeaverAction.BUILD_TRAINING_FIELD, 0.0);
			if (numTank == 0)
				weights.put(BeaverAction.BUILD_TANK_FACTORY, 1.0);
			else
				weights.put(BeaverAction.BUILD_TANK_FACTORY, 0.0);
			if (numAero == 0)
				weights.put(BeaverAction.BUILD_AEROSPACE_LAB, 1.0);
			else
				weights.put(BeaverAction.BUILD_AEROSPACE_LAB, 0.0);
			weights.put(BeaverAction.BUILD_HELIPAD, 0.0);
			weights.put(BeaverAction.BUILD_BARRACKS, 0.0);
			weights.put(BeaverAction.BUILD_TECHNOLOGY_INSTITUTE, 0.0);
		} else {
			weights.put(BeaverAction.BUILD_HELIPAD, 1.0);
			weights.put(BeaverAction.BUILD_HANDWASH_STATION, 1.0);
			weights.put(BeaverAction.BUILD_TRAINING_FIELD, 0.0);
			weights.put(BeaverAction.BUILD_TANK_FACTORY, 0.0);
			weights.put(BeaverAction.BUILD_AEROSPACE_LAB, 0.0);
		}
	}

	private static enum BeaverAction {
		IDLE, MINE, MOVE, ATTACK, BUILD_SUPPLY_DEPOT, BUILD_TECHNOLOGY_INSTITUTE, BUILD_TRAINING_FIELD, BUILD_BARRACKS, BUILD_TANK_FACTORY, BUILD_HELIPAD, BUILD_AEROSPACE_LAB, BUILD_HANDWASH_STATION, BUILD_MINER_FACTORY,
	};

	/**
	 * Returns the best action to do, as a sorted list. Not all returned actions are always possible, so check to see if they're
	 * possible before executing them. If an action isn't possible, users should try the next one on the list. Also, not all possible
	 * actions are listed. Sometimes taking a particular action would be *undesireable*, in which case it is excluded.
	 */
	public List<BeaverAction> getPrioritizedActions() {
		LinkedList<BeaverAction> result = new LinkedList<BeaverAction>();

		// In order to randomize this a little, we're going to give each action a weight, and then put it into a hat with the other
		// actions.
		// The order we draw from the hat is the priority.
		double sum = 0;
		Collection<BeaverAction> actionsLeft = new LinkedList<BeaverAction>();
		for (BeaverAction action : weights.keySet()) {
			double weight = weights.get(action);
			if (weight > 0) {
				sum += weight;
				actionsLeft.add(action);
			}
		}
		double epsilon = 0.0001;
		while (sum > epsilon) {
			double rand = gen.nextDouble() * sum;
			boolean drawSuccess = false;
			for (BeaverAction action : actionsLeft) {
				double curWeight = weights.get(action);
				rand -= curWeight;
				if (rand <= 0) {
					result.add(action);
					actionsLeft.remove(action);
					sum -= curWeight;
					drawSuccess = true;
					break;
				}
			}
			// if for some reason we didn't draw anything from the hat, then there were probably some kind of double-precision errors.
			// terminate early.
			if (!drawSuccess) {
				break;
			}
		}
		if (actionsLeft.size() > 0) {
			result.addAll(actionsLeft);
		}

		return result;
	}

	// this is the weighted priority of all actions (used in getPrioritizedActions). consider running tests to estimate actual values,
	// or modifying it on the fly for better results.
	// note: these DON'T have to sum to 1. The MAY be zero or negative, but if they are zero or negative, the action will never be
	// performed.
	private static Map<BeaverAction, Double> weights = null;

	static {
		weights = new HashMap<BeaverAction, Double>();
		weights.put(BeaverAction.IDLE, 0.0);
		weights.put(BeaverAction.MINE, 2.0);
		weights.put(BeaverAction.MOVE, 0.2); // this might want to be increased for scouts
		weights.put(BeaverAction.ATTACK, 0.1);
		weights.put(BeaverAction.BUILD_AEROSPACE_LAB, 0.0);
		weights.put(BeaverAction.BUILD_BARRACKS, 0.0);
		weights.put(BeaverAction.BUILD_HANDWASH_STATION, 0.0); // this might want to be increased during the late-game
		weights.put(BeaverAction.BUILD_HELIPAD, 0.0);
		weights.put(BeaverAction.BUILD_MINER_FACTORY, 0.0);
		weights.put(BeaverAction.BUILD_SUPPLY_DEPOT, 0.0);
		weights.put(BeaverAction.BUILD_TANK_FACTORY, 0.0);
		weights.put(BeaverAction.BUILD_TECHNOLOGY_INSTITUTE, 0.0);
		weights.put(BeaverAction.BUILD_TRAINING_FIELD, 0.0);
	}

	public boolean tryToBuild(RobotType type) throws GameActionException {
		if (rc.getTeamOre() >= type.oreCost) {
			Direction buildDir = findRandomBuildDirection(type);
			if (buildDir != null) {
				rc.build(buildDir, type);
				return true;
			}
		}
		return false;
	}
}
