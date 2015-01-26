package soldierrush;

import battlecode.common.RobotController;

public class RobotPlayer {


	public static void run(RobotController rc) {
		BaseRobotHandler myHandler = BaseRobotHandler.createHandler(rc);
		myHandler.run();
	}
}
