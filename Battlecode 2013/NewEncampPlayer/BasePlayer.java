package NewEncampPlayer;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;

public abstract class BasePlayer {
	RobotController rc;
	int width;
	
	protected int base_channel = 20500;
	protected int totalEncamps = 0;

	
	
	public BasePlayer(RobotController rc) {
		this.rc = rc;
		width = rc.getMapWidth();
	}
		
 
	
	public abstract void run() throws GameActionException;
	
	public void loop() {
		while(true) {
			try {
				
				// Execute turn
				run();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			// End turn
			rc.yield();
		}
	}
	
	
}
