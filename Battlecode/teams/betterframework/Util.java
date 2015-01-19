package betterframework;

import java.util.LinkedList;
import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class Util {

	// these are all the directions excluding OMNI and NONE
	public static final Direction[] actualDirections = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST,
			Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST, };

	// for use when comparing floating point numbers
	public static final double F_EPSILON = 1e-6;
	public static final double EPSILON = 1e-6;

	public static enum MapConfiguration {
		VERTICAL_REFLECTION, HORIZONTAL_REFLECTION, DIAGONAL_REFLECTION, INVERSE_DIAGONAL_REFLECTION, ROTATION
	};

	// precondition: 0 <= i <= 12
	public static int factorial(int i) {
		return f[i];
	}

	private static int[] f = { 1, 1, 2, 6, 24, 120, 720, 5040, 40320, 362880, 3628800, 39916800, 479001600 };

	private static Direction[] randomDirections = null;

	public static Direction[] getRandomDirectionOrdering(Random gen) {
		if (randomDirections == null) {
			randomDirections = resetRandomDirectionOrdering(gen);
		}
		return randomDirections;
	}

	public static Direction[] resetRandomDirectionOrdering(Random gen) {
		// create a random permutation
		LinkedList<Direction> dirsLeft = new LinkedList<Direction>();

		for (int i = 0; i < actualDirections.length; i++) {
			dirsLeft.add(actualDirections[i]);
		}

		Direction[] ans = new Direction[dirsLeft.size()];
		int index = gen.nextInt(Util.factorial(actualDirections.length));
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
		Direction[] dirs = { toDest, toDest.rotateLeft(), toDest.rotateRight(), toDest.rotateLeft().rotateLeft(),
				toDest.rotateRight().rotateRight() };

		return dirs;
	}

	public static int encodeMapConfigurationAsBitmask(boolean isVerticalReflection, boolean isHorizontalReflection,
			boolean isDiagonalReflection, boolean isReverseDiagonalReflection, boolean isRotation) {
		int bitmask = 0;
		bitmask |= (isVerticalReflection ? 1 : 0) << MapConfiguration.VERTICAL_REFLECTION.ordinal();
		bitmask |= (isHorizontalReflection ? 1 : 0) << MapConfiguration.HORIZONTAL_REFLECTION.ordinal();
		bitmask |= (isDiagonalReflection ? 1 : 0) << MapConfiguration.DIAGONAL_REFLECTION.ordinal();
		bitmask |= (isReverseDiagonalReflection ? 1 : 0) << MapConfiguration.INVERSE_DIAGONAL_REFLECTION.ordinal();
		bitmask |= (isRotation ? 1 : 0) << MapConfiguration.ROTATION.ordinal();
		return bitmask;
	}

	public static boolean decodeVerticalReflection(int configurationBitmask) {
		return (configurationBitmask & (1 << MapConfiguration.VERTICAL_REFLECTION.ordinal())) > 0;
	}

	public static boolean decodeHorizontalReflection(int configurationBitmask) {
		return (configurationBitmask & (1 << MapConfiguration.HORIZONTAL_REFLECTION.ordinal())) > 0;
	}

	public static boolean decodeDiagonalReflection(int configurationBitmask) {
		return (configurationBitmask & (1 << MapConfiguration.DIAGONAL_REFLECTION.ordinal())) > 0;
	}

	public static boolean decodeReverseDiagonalReflection(int configurationBitmask) {
		return (configurationBitmask & (1 << MapConfiguration.INVERSE_DIAGONAL_REFLECTION.ordinal())) > 0;
	}

	public static boolean decodeRotation(int configurationBitmask) {
		return (configurationBitmask & (1 << MapConfiguration.ROTATION.ordinal())) > 0;
	}

	public static MapLocation rotateAround(float[] midpoint, MapLocation original) {
		int dx = Math.round(2 * (midpoint[0] - (float) original.x));
		int dy = Math.round(2 * (midpoint[1] - (float) original.y));
		return original.add(dx, dy);
	}

	public static MapLocation reflectHorizontallyAccross(float[] midpoint, MapLocation original) {
		int dx = Math.round(2 * (midpoint[0] - (float) original.x));
		return original.add(dx, 0);
	}

	public static MapLocation reflectVerticallyAccross(float[] midpoint, MapLocation original) {
		int dy = Math.round(2 * (midpoint[1] - (float) original.y));
		return original.add(0, dy);
	}

	public static MapLocation reflectDiagonallyAccross(float[] midpoint, MapLocation original) {
		int dx = Math.round((midpoint[0] - (float) original.x) + ((float) original.y - midpoint[1]));
		int dy = -dx;
		return original.add(dx, dy);
	}

	public static MapLocation reflectInvDiagonallyAccross(float[] midpoint, MapLocation original) {
		int dx = Math.round((midpoint[0] - (float) original.x) + (midpoint[1] - (float) original.y));
		int dy = dx;
		return original.add(dx, dy);
	}
}
