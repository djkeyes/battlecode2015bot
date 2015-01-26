package dronerush;

import java.util.LinkedList;
import java.util.List;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class TowerHandler extends BaseBuildingHandler {

	protected TowerHandler(RobotController rc) {
		super(rc);
	}

	@Override
	public List<Action> chooseActions() throws GameActionException {
		LinkedList<Action> result = new LinkedList<Action>();
		result.add(attack);
		return result;
	}

	private final Action attack = new TowerAttack();

	// same as normal Attack, but also has a hook to detect if towers are in peril
	private class TowerAttack extends Attack {
		@Override
		public RobotInfo[] senseNearbyEnemies() throws GameActionException {
			RobotInfo[] result =super.senseNearbyEnemies();
			int numEnemies = result.length;
			if(numEnemies > 0){
				int prevTowerInPerilCount = BroadcastInterface.getNumEnemiesNearTowerInPeril(rc);
				if(numEnemies > prevTowerInPerilCount){
					BroadcastInterface.reportTowerInPeril(rc, numEnemies, rc.getLocation());
				}
			}
			return result;
		}
	}
}
