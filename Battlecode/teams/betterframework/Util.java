package betterframework;

import java.util.LinkedList;
import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class Util {

	// these are all the directions excluding OMNI and NONE
	public static final Direction[] actualDirections = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST,
			Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST, };
	public static final double F_EPSILON = 1e-6;
	public static final double EPSILON = 1e-6;

	// precondition: 0 <= i <= 12
	public static int factorial(int i) {
		return f[i];
	}

	private static int[] f = { 1, 1, 2, 6, 24, 120, 720, 5040, 40320, 362880, 3628800, 39916800, 479001600 };

	public static Direction[] getRandomDirectionOrdering(Random gen) {
		// create a random permutation
		LinkedList<Direction> dirsLeft = new LinkedList<Direction>();

		for (int i = 0; i < actualDirections.length; i++) {
			dirsLeft.add(actualDirections[i]);
		}

		Direction[] ans = new Direction[dirsLeft.size()];
		int index = gen.nextInt(Util.factorial(8));
		for (int i = dirsLeft.size(); i >= 1; i--) {
			int selected = index % i;
			index /= i;
			ans[i - 1] = dirsLeft.remove(selected);
		}
		return ans;
	}

	public static Direction getRandomDirection(Random gen) {
		return actualDirections[gen.nextInt(actualDirections.length)];
	}
	

    public static Direction[] getDirectionsToward(MapLocation cur, MapLocation dest) {
        Direction toDest = cur.directionTo(dest);
        Direction[] dirs = {toDest,
	    		toDest.rotateLeft(), toDest.rotateRight(),
			toDest.rotateLeft().rotateLeft(), toDest.rotateRight().rotateRight()};

        return dirs;
    }
}
