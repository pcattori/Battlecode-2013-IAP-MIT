package StrategyPlayer2;

import java.util.Random;
//import java.util.*;

import java.util.Arrays;

import battlecode.common.*;

/**
 * 
 * @author brando
 * Caution: Do not create NavigationController instances inside run method because instance variables won't be remembered
 * as expected and unexpected behavior might occur. Instead, NavigationController should be a instance of SoldierPlayer.
 * 
 */
public class NavigationController {
	private RobotController rc;
	private int previousMove = -1;
	//private int lastIndex = 0; 
	//private int cycleBreakerCounter = 0;
	private MapLocation[] previousFour = new MapLocation[4];
	private Team myTeam;
	private int[][] mapIntDistribution;
	private Random dice;

	NavigationController(RobotController rc){
		this.rc = rc;
		this.myTeam = rc.getTeam();
		this.mapIntDistribution = new int[rc.getMapWidth()][rc.getMapHeight()];
		this.dice = new Random(Clock.getRoundNum());
	}

	public void progressMove(MapLocation whereToGo) throws GameActionException{
		MapLocation currentLocation = rc.getLocation();
		int dist = currentLocation.distanceSquaredTo(whereToGo);

		if (dist>0 && rc.isActive()){
			Direction toTarget = currentLocation.directionTo(whereToGo);
			int[] directionOffsets = {-2,2,-1,1,0};
			Direction potentialDirectionMovement; //direction of where we are going to move
			MapLocation newLocation;

			int offset;
			for (int i = 5; --i>= 0;){
				offset = directionOffsets[i];
				potentialDirectionMovement = Direction.values()[(toTarget.ordinal()+offset+8)%8];
				newLocation = rc.getLocation().add(potentialDirectionMovement);
				Team mineAtPotentialNewLocation = rc.senseMine(newLocation);
				if (rc.canMove(potentialDirectionMovement) &&  this.shouldMakeMoveRand(newLocation, currentLocation)
						&& (mineAtPotentialNewLocation == myTeam || mineAtPotentialNewLocation == null)){
					rc.move(potentialDirectionMovement);
					//this.previousFour[lastIndex] = currentLocation;
					//lastIndex = (lastIndex + 1)%4;
					try{
						try{
							this.mapIntDistribution[newLocation.x][newLocation.y]++;
						} catch (Exception e){
							this.mapIntDistribution[newLocation.x][newLocation.y] = 1;
						}
					} catch (Exception e){

					}
					//rc.setIndicatorString(0,Arrays.toString(this.previousFour));
					//rc.setIndicatorString(1,"locationBeforeMove: "+currentLocation);
					//rc.setIndicatorString(2,"to Target Direction: "+toTarget.toString());
					return;
				}
			}
			for (int i = 5; --i>= 0;){
				offset = directionOffsets[i];
				potentialDirectionMovement = Direction.values()[(toTarget.ordinal()+offset+8)%8];
				if(rc.canMove(potentialDirectionMovement) ){
					newLocation = currentLocation.add(toTarget);
					this.defuseFirstorThenMove(potentialDirectionMovement);
					//this.previousFour[lastIndex] = currentLocation;
					//lastIndex = (lastIndex + 1)%4;
					try{
						try{
							this.mapIntDistribution[newLocation.x][newLocation.y]++;
						} catch (Exception e){
							this.mapIntDistribution[newLocation.x][newLocation.y] = 1;
						}
					} catch (Exception e){

					}
					//rc.setIndicatorString(0,Arrays.toString(this.previousFour));
					//rc.setIndicatorString(1,"locationBeforeMove: "+currentLocation);
					//rc.setIndicatorString(2,"to Target Direction: "+toTarget.toString());
					return;
				} 
			}
		}
	}

	public void evalMove(MapLocation currentLocation, MapLocation whereToGo) throws GameActionException{//New pathing algorithm
		//SHOULD WE CONSIDER NOT MOVING??
		if (rc.isActive()){
			try{
				this.mapIntDistribution[currentLocation.x][currentLocation.y]+=1;
			} catch (Exception e){
				this.mapIntDistribution[currentLocation.x][currentLocation.y]=1;
			}
			int[] dirOffsets = {0,1,2,3,4,5,6,7};
			int bestScore = -1;
			Direction bestDir = null;
			for (int offset : dirOffsets){
				Direction dir = Direction.values()[offset];
				if (rc.canMove(dir)){
					int score = this.evalDirection(currentLocation,dir,whereToGo, currentLocation.directionTo(whereToGo), this.movesAway(currentLocation, whereToGo));
					if (bestDir == null){
						bestScore = score;
						bestDir = dir;
					} else if (score < bestScore){
						bestScore = score;
						bestDir = dir;
					}
				}
			}
			if (bestDir != null){
				rc.setIndicatorString(0,"Best score: "+bestScore+", Direction to target: "+currentLocation.directionTo(whereToGo));
				MapLocation moveSquare = currentLocation.add(bestDir);
				if (this.mineHazard(moveSquare)){
					rc.defuseMine(moveSquare);
				} else{
					rc.move(bestDir);
				}
			}
		}
	}

	public int movesAway(MapLocation start, MapLocation target){
		return Math.max(Math.abs(target.x-start.x), Math.abs(target.y-start.y));
	}

	public int evalDirection(MapLocation currentLocation, Direction dir, MapLocation goal, Direction dirToGoal,int currentMovesToGoal){
		boolean hasDefusion = rc.hasUpgrade(Upgrade.DEFUSION);
		MapLocation square = currentLocation.add(dir);

		int directionPenalty;
		int dirDiff = Math.abs(dir.ordinal() - dirToGoal.ordinal());
		if (dirDiff <= 2){
			directionPenalty = this.movesAway(square,goal) - currentMovesToGoal;
		} else{
			directionPenalty = hasDefusion?5:12;
		}
		int minePenalty;
		if (hasDefusion){
			minePenalty = this.mineHazard(square)?5:0;
		} else{
			minePenalty = this.mineHazard(square)?12:0;
		}
		int visitPenalty = 2*this.mapIntDistribution[square.x][square.y]*(hasDefusion?5:12);
		return directionPenalty+minePenalty+visitPenalty;
	}

	protected boolean mineHazard(MapLocation location) {
		Team mine = rc.senseMine(location);
		return (mine == Team.NEUTRAL || mine == myTeam.opponent());
	}

	@SuppressWarnings("unused")
	private boolean beenThereAlready(MapLocation new_location) {
		try{
			for (MapLocation prevLoc: this.previousFour){
				if (prevLoc.equals(new_location)){
					return true;
				}
			}
		}catch(Exception e){
			return false;	
		}
		return false;
	}

	@SuppressWarnings("unused")
	private boolean isPreviousLocation(MapLocation new_location){
		try{
			if(new_location.equals(previousFour[0])){
				return true;
			}
		} catch (Exception e){
			return false;
		}
		return false;
	}

	private void defuseFirstorThenMove(Direction dir) throws GameActionException{
		MapLocation ahead = rc.getLocation().add(dir);
		if(rc.canMove(dir) && rc.senseMine(ahead)!= null && rc.senseMine(ahead) != rc.getTeam()){
			rc.defuseMine(ahead);
		}else{
			if(rc.canMove(dir)){
				rc.move(dir);     
			}
		}
	}

	private void moveOrDefuse(Direction dir) throws GameActionException{
		MapLocation ahead = rc.getLocation().add(dir);
		if(rc.canMove(dir) && rc.senseMine(ahead)!= null && rc.senseMine(ahead) != rc.getTeam()){
			rc.defuseMine(ahead);
		}else{
			if(rc.canMove(dir)){
				rc.move(dir);     
			}
		}
	}


	/**
	 * 
	 * @param whereToGo
	 * @throws GameActionException
	 * The original/old version of progressMove.
	 */
	@SuppressWarnings("unused")
	private void progressMoveOld(MapLocation whereToGo) throws GameActionException{
		rc.setIndicatorString(1, "Swarm target: "+whereToGo.toString());
		int dist = rc.getLocation().distanceSquaredTo(whereToGo);
		if (dist>0 && rc.isActive()){
			Direction toTarget = rc.getLocation().directionTo(whereToGo);
			int[] directionOffsets = {0,1,-1,2,-2};
			Direction move;
			MapLocation ahead;
			for (int offset: directionOffsets){
				move = Direction.values()[(toTarget.ordinal()+offset+8)%8];
				ahead = rc.getLocation().add(move);
				Team mine = rc.senseMine(ahead);
				int move_num = move.ordinal();
				if (rc.canMove(move) && (mine == rc.getTeam() || mine == null) && move_num != (previousMove+4)%8){
					previousMove = move_num;
					rc.move(move);
					return;
				}
			}
			previousMove = -1;
			if(rc.canMove(toTarget) || rc.senseMine(rc.getLocation())==rc.getTeam()){
				moveOrDefuse(toTarget);
			} 
		}
	}

	@SuppressWarnings("unused")
	private boolean makeMoveDeterministic(MapLocation potentialNewLocation){
		try{
			int numberOfTimesBeenOnLocation = this.mapIntDistribution[potentialNewLocation.x][potentialNewLocation.y];
			if (numberOfTimesBeenOnLocation < 2){
				return true;
			}
			return false;
		} catch (Exception e){
			return true;
		}
	}

	/**
	 * 
	 * @param potentialNewLocation
	 * @param currentLocation
	 * @return true if the robot should move to potentialNewLocation (with high probability). 
	 * If the new location has NOT been considered before, then move to that location with probability 1.
	 */
	private boolean shouldMakeMoveRand(MapLocation potentialNewLocation, MapLocation currentLocation) {
		try{
			int newLocNum = this.mapIntDistribution[potentialNewLocation.x][potentialNewLocation.y];
			//			if (newLocNum <= 0){
			//				return true;
			//			}
			int currLocNum = this.mapIntDistribution[currentLocation.x][currentLocation.y];
			int sum = newLocNum + currLocNum;
			//Double randomNumber = this.dice.nextDouble() * sum;
			int randomNumber = this.dice.nextInt(sum+1);
			if (newLocNum >= currLocNum ){
				//newLocNum is max
				if (randomNumber <= newLocNum){
					return true;
				}else{
					return false;
				}
			}else{
				//currLocNum is max
				if (randomNumber <= currLocNum){
					return true;
				}else{
					return false;
				}
			}
		} catch (Exception e){
			return true;
		}
	}

}
