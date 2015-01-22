package dronerush;

import java.util.LinkedList;
import java.util.List;

import battlecode.common.CommanderSkillType;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class CommanderHandler extends BaseRobotHandler {

	private static int HEALTH_TO_RETREAT = 50;

	protected CommanderHandler(RobotController rc) {
		super(rc);
	}

	@Override
	public List<Action> chooseActions() throws GameActionException {
		LinkedList<Action> result = new LinkedList<Action>();
		// Leadership and Regenerate are passive skills, which is awesome
		// Flash and Heavy Hands are active though
		if (rc.getHealth() <= HEALTH_TO_RETREAT) {
			if (rc.hasLearnedSkill(CommanderSkillType.FLASH)) {
				result.add(retreatWithFlash);
			}
			result.add(retreat);
			return result;
		}
		if (rc.hasLearnedSkill(CommanderSkillType.HEAVY_HANDS)) {
			result.add(useHeavyHands);
		}
		result.add(attack);
		result.add(advance);
		result.add(scout);
		return result;
	}

	private final Action attack = new Attack();
	private final Action retreat = new Retreat();
	private final Action retreatWithFlash = new RetreatWithFlash();
	private final Action scout = new ScoutOutward();
	private final Action advance = new MoveTowardEnemyHq(/* avoidEnemies= */true);
	private final Action useHeavyHands = new HeavyHands();

	private class HeavyHands implements Action {
		@Override
		public boolean run() throws GameActionException {
			// TODO Auto-generated method stub
			// Heavy Hands is automatically cast on anyone we attack, so we should make sure to attack several different targets
			return false;
		}
	}

	public class RetreatWithFlash implements Action {

		@Override
		public boolean run() throws GameActionException {
			if (!rc.isCoreReady() || rc.getFlashCooldown() > 0) {
				return false;
			}

			MapLocation ourHq = getOurHqLocation();
			MapLocation[] potentialLocs = MapLocation.getAllMapLocationsWithinRadiusSq(rc.getLocation(),
					GameConstants.FLASH_RANGE_SQUARED);
			// should we use the pathfinding dist or the actual dist here?
			// it almost makes sense to use the actual dist, since we can flash over obstacles and we'd need to use this in a pinch
			int minDist = rc.getLocation().distanceSquaredTo(ourHq);
			MapLocation best = null;
			for (MapLocation loc : potentialLocs) {
				int locDist = loc.distanceSquaredTo(ourHq);
				if (locDist < minDist) {
					if (rc.senseRobotAtLocation(loc) == null) {
						if (!inEnemyHqOrTowerRange(loc)) {
							minDist = locDist;
							best = loc;
						}
					}
				}
			}
			if (best != null) {
				rc.castFlash(best);
				return true;
			}
			return false;
		}
	}

}
