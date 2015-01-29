package bloombot;

import java.util.Arrays;
import java.util.Comparator;
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

		if (shouldPullTheBoys()) {
			result.add(attack);
			result.add(charge);
			result.add(scout);
			return result;
		}

		result.add(attack);
		result.add(rush);
		result.add(scout);
		return result;
	}

	private final Action attack = new Attack();
	private final Action retreat = new Retreat();
	private final Action retreatWithFlash = new RetreatWithFlash();
	private final Action scout = new ScoutOutward(true, false);
	private final Action rush = new MoveTo(getEnemyHqLocation(), true, false);
	private final Action charge = new MoveTowardEnemyHq(false, false);
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

		private int[][] orderedFlashOffsets;

		public RetreatWithFlash() {
			orderedFlashOffsets = generateOrderedFlashOffsets();
		}

		private int[][] generateOrderedFlashOffsets() {
			MapLocation[] outsourcedPositions = MapLocation.getAllMapLocationsWithinRadiusSq(new MapLocation(0, 0),
					GameConstants.FLASH_RANGE_SQUARED);
			int[][] result = new int[outsourcedPositions.length][3];
			for (int i = 0; i < outsourcedPositions.length; i++) {
				result[i][0] = outsourcedPositions[i].x;
				result[i][1] = outsourcedPositions[i].y;
				result[i][2] = result[i][0] * result[i][0] + result[i][1] * result[i][1];
			}
			Arrays.sort(result, new Comparator<int[]>() {
				@Override
				public int compare(int[] a, int[] b) {
					return b[2] - a[2];
				}
			});
			return result;
		}

		@Override
		public boolean run() throws GameActionException {
			if (!rc.isCoreReady() || rc.getFlashCooldown() > 0) {
				return false;
			}

			MapLocation ourHq = getOurHqLocation();
			// should we use the pathfinding dist or the actual dist here?
			// it sort of makes sense to use the actual dist, since we can flash over obstacles and we'd need to use this in a pinch
			int minDist = rc.getLocation().distanceSquaredTo(ourHq);
			MapLocation best = null;
			// System.out.println("bytecodes at start of loop: " + Clock.getBytecodeNum());
			for (int[] offset : orderedFlashOffsets) {
				MapLocation loc = rc.getLocation().add(offset[0], offset[1]);
				// System.out.println("bytecodes at current iteration: " + Clock.getBytecodeNum());
				int locDist = loc.distanceSquaredTo(ourHq);
				if (locDist < minDist) {

					if (rc.senseTerrainTile(loc).isTraversable() && rc.senseRobotAtLocation(loc) == null) {
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
