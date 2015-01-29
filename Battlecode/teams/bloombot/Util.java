package bloombot;

import java.util.LinkedList;
import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class Util {

	// these are all the directions excluding OMNI and NONE
	public static final Direction[] actualDirections = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
			Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST, };

	public static final Direction[] cardinalDirections = { Direction.NORTH, Direction.EAST,
			Direction.SOUTH, Direction.WEST, };
	public static final Direction[] unCardinalDirections = { Direction.NORTH_EAST, Direction.SOUTH_EAST,
		Direction.SOUTH_WEST, Direction.NORTH_WEST, };

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

	public static Direction[] getDirectionsStrictlyToward(MapLocation cur, MapLocation dest) {
		Direction toDest = cur.directionTo(dest);
		return getDirectionsStrictlyToward(toDest);
	}

	public static Direction[] getDirectionsStrictlyToward(Direction toDest) {
		Direction[] dirs = { toDest, toDest.rotateLeft(), toDest.rotateRight() };

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

	public static float[] findMidpoint(MapLocation ourHq, MapLocation theirHq) {
		float x = midpoint(ourHq.x, theirHq.x);
		float y = midpoint(ourHq.y, theirHq.y);
		return new float[] { x, y };
	}

	public static float midpoint(float x1, float x2) {
		return (x1 + x2) / 2f;
	}

	public static boolean checkIsVerticalReflection(float[] midpoint, MapLocation ourHq, MapLocation theirHq, MapLocation[] ourTowers,
			MapLocation[] theirTowers) {
		// to be a vertical reflection, we should be able to flip every tower and hq around the y-midpoint and get an enemy tower
		float y = midpoint[1];
		// our hq already matches midpoint, we just need to check the x-coordinate
		if (ourHq.x != theirHq.x) {
			return false;
		}

		for (MapLocation ourTower : ourTowers) {
			boolean reflectionFound = false;
			for (MapLocation enemyTower : theirTowers) {
				if (enemyTower.x == ourTower.x && Math.abs(midpoint(ourTower.y, enemyTower.y) - y) < Util.F_EPSILON) {
					reflectionFound = true;
					break;
				}
			}
			if (!reflectionFound) {
				return false;
			}
		}

		return true;
	}

	public static boolean checkIsHorizontalReflection(float[] midpoint, MapLocation ourHq, MapLocation theirHq,
			MapLocation[] ourTowers, MapLocation[] theirTowers) {
		// this is basically the same as the vertical reflection
		float x = midpoint[0];
		// our hq already matches midpoint, we just need to check the y-coordinate
		if (ourHq.y != theirHq.y) {
			return false;
		}

		for (MapLocation ourTower : ourTowers) {
			boolean reflectionFound = false;
			for (MapLocation enemyTower : theirTowers) {
				if (enemyTower.y == ourTower.y && Math.abs(midpoint(ourTower.x, enemyTower.x) - x) < Util.F_EPSILON) {
					reflectionFound = true;
					break;
				}
			}
			if (!reflectionFound) {
				return false;
			}
		}

		return true;
	}

	public static boolean checkIsDiagonalReflection(float[] midpoint, MapLocation ourHq, MapLocation theirHq, MapLocation[] ourTowers,
			MapLocation[] theirTowers) {
		// this is a little trickier than horizontal and vertical reflections
		// (x1, y1) and (x2, y2) are diagonally flipped around (mx, my) if
		// x1 - mx == y2 - my
		// AND
		// x2 - mx == y1 - my
		float mx = midpoint[0];
		float my = midpoint[1];

		boolean cond1 = Math.abs((ourHq.x - mx) - (theirHq.y - my)) < Util.F_EPSILON;
		boolean cond2 = Math.abs((theirHq.x - mx) - (ourHq.y - my)) < Util.F_EPSILON;
		if (!(cond1 && cond2)) {
			return false;
		}

		for (MapLocation ourTower : ourTowers) {
			boolean reflectionFound = false;
			for (MapLocation enemyTower : theirTowers) {
				// written on separate lines for clarity
				cond1 = Math.abs((ourTower.x - mx) - (enemyTower.y - my)) < Util.F_EPSILON;
				cond2 = Math.abs((enemyTower.x - mx) - (ourTower.y - my)) < Util.F_EPSILON;
				if (cond1 && cond2) {
					reflectionFound = true;
					break;
				}
			}
			if (!reflectionFound) {
				return false;
			}
		}

		return true;
	}

	public static boolean checkIsReverseDiagonalReflection(float[] midpoint, MapLocation ourHq, MapLocation theirHq,
			MapLocation[] ourTowers, MapLocation[] theirTowers) {
		// slightly different from the original diagonal reflection
		// previous one was flipped around 45 degrees, this one is flipped around 135 degrees
		// this is a little trickier than horizontal and vertical reflections
		// (x1, y1) and (x2, y2) are diagonally flipped around (mx, my) if
		// mx - x1 == y2 - my
		// AND
		// x2 - mx == my - y1
		float mx = midpoint[0];
		float my = midpoint[1];

		boolean cond1 = Math.abs((mx - ourHq.x) - (theirHq.y - my)) < Util.F_EPSILON;
		boolean cond2 = Math.abs((theirHq.x - mx) - (my - ourHq.y)) < Util.F_EPSILON;
		if (!(cond1 && cond2)) {
			return false;
		}

		for (MapLocation ourTower : ourTowers) {
			boolean reflectionFound = false;
			for (MapLocation enemyTower : theirTowers) {
				// written on separate lines for clarity
				cond1 = Math.abs((mx - ourTower.x) - (enemyTower.y - my)) < Util.F_EPSILON;
				cond2 = Math.abs((enemyTower.x - mx) - (my - ourTower.y)) < Util.F_EPSILON;
				if (cond1 && cond2) {
					reflectionFound = true;
					break;
				}
			}
			if (!reflectionFound) {
				return false;
			}
		}

		return true;
	}

	public static boolean checkIsRotation(float[] midpoint, MapLocation ourHq, MapLocation theirHq, MapLocation[] ourTowers,
			MapLocation[] theirTowers) {
		// we only need to check 180 degree rotations. 90- and 270-degrees are verboten.
		// this is fairly straightforward to derive, but here are the equations we'll be checking:
		// x1 +x2 == 2*mx
		// y1 +y2 == 2*my
		double mx = midpoint[0];
		double my = midpoint[1];

		boolean cond1 = (ourHq.x + theirHq.x - 2 * mx) < Util.F_EPSILON;
		boolean cond2 = (ourHq.y + theirHq.y - 2 * my) < Util.F_EPSILON;
		if (!(cond1 && cond2)) {
			return false;
		}

		for (MapLocation ourTower : ourTowers) {
			boolean rotationFound = false;
			for (MapLocation enemyTower : theirTowers) {
				// written on separate lines for clarity
				cond1 = (ourTower.x + enemyTower.x - 2 * mx) < Util.F_EPSILON;
				cond2 = (ourTower.y + enemyTower.y - 2 * my) < Util.F_EPSILON;
				if (cond1 && cond2) {
					rotationFound = true;
					break;
				}
			}
			if (!rotationFound) {
				return false;
			}
		}

		return true;
	}
}
