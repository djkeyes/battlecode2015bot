package simpleframework;

import battlecode.common.RobotController;

public class RobotPlayer {


	public static void run(RobotController rc) {
		RobotHandler myHandler = RobotHandler.createHandler(rc);
		myHandler.run();
	}
}
