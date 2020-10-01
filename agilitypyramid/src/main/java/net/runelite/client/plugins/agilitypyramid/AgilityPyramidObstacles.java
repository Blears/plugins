package net.runelite.client.plugins.agilitypyramid;

import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import static net.runelite.api.ObjectID.*;

public enum AgilityPyramidObstacles
{
	//GROUND FLOOR
	STAIRS0(new WorldPoint(3352, 2827, 0), new WorldPoint(3380, 2857, 0), STAIRS_10857),
	//FLOOR 1, af
	LOW_WALL1(new WorldPoint(3354, 2833, 1), new WorldPoint(3356, 2849, 1), LOW_WALL_10865),
	LEDGE1(new WorldPoint(3354, 2850, 1), new WorldPoint(3365, 2852, 1), LEDGE_10860),
	PLANK1(new WorldPoint(3368, 2845, 1), new WorldPoint(3375, 2852, 1), PLANK_10868, AgilityPyramidObstacleType.GROUND_OBJECT),
	GAP1(new WorldPoint(3370, 2831, 1), new WorldPoint(3380, 2842, 1), GAP_10882),
	LEDGE11(new WorldPoint(3362, 2831, 1), new WorldPoint(3368, 2834, 1), LEDGE_10886),
	STAIRS1(new WorldPoint(3356, 2831, 1), new WorldPoint(3361, 2834, 1), STAIRS_10857),
	//FLOOR 2
	GAP2(new WorldPoint(3356, 2835, 2), new WorldPoint(3360, 2838, 2), GAP_10884),
	GAP21(new WorldPoint(3355, 2840, 2), new WorldPoint(3358, 2848, 2), GAP_10859),
	GAP22(new WorldPoint(3356, 2849, 2), new WorldPoint(3361, 2850, 2), GAP_10861),
	LEDGE2(new WorldPoint(3364, 2837, 2), new WorldPoint(3373, 2850, 2), LEDGE_10860),
	LOW_WALL2(new WorldPoint(3370, 2830, 2), new WorldPoint(3375, 2837, 2), LOW_WALL_10865),
	GAP23(new WorldPoint(3364, 2833, 2), new WorldPoint(3370, 2836, 2), GAP_10859), //deze ff kijken zo
	STAIRS2(new WorldPoint(3358, 2833, 2), new WorldPoint(3364, 2836, 2), STAIRS_10857),
	//FLOOR 3
	LOW_WALL3(new WorldPoint(3358, 2836, 3), new WorldPoint(3363, 2839, 3), LOW_WALL_10865),
	LEDGE3(new WorldPoint(3358, 2838, 3), new WorldPoint(3360, 2846, 3), LEDGE_10888),
	GAP3(new WorldPoint(3358, 2841, 3), new WorldPoint(3373, 2849, 3), GAP_10859),
	PLANK3(new WorldPoint(3369, 2835, 3), new WorldPoint(3372, 2842, 3), PLANK_10868, AgilityPyramidObstacleType.GROUND_OBJECT), //check deze ff nog, was 2840
	STAIRS3(new WorldPoint(3360, 2835, 3), new WorldPoint(3369, 2838, 3), STAIRS_10857),
	//FLOOR 4
	GAP4(new WorldPoint(3040, 4695, 2), new WorldPoint(3042, 4698, 2), GAP_10859),
	LOW_WALL4(new WorldPoint(3040, 4699, 2), new WorldPoint(3042, 4702, 2), LOW_WALL_10865),
	GAP41(new WorldPoint(3046, 4695, 2), new WorldPoint(3050, 4705, 2), GAP_10859), //heel belangrijk
	LOW_WALL41(new WorldPoint(3047, 4693, 2), new WorldPoint(3052, 4695, 2), LOW_WALL_10865),
	STAIRS4(new WorldPoint(3042, 4693, 2), new WorldPoint(3047, 4695, 2), STAIRS_10857),
	//FLOOR 5
	CLIMBING_ROCKS(new WorldPoint(3042, 4697, 3), new WorldPoint(3044, 4698, 3), CLIMBING_ROCKS_10851, AgilityPyramidObstacleType.DECORATION),
	GAP5(new WorldPoint(3042, 4697, 3), new WorldPoint(3050, 4705, 3), GAP_10859),
	DOORWAY5(new WorldPoint(3046, 4693, 3), new WorldPoint(3048, 4697, 3), DOORWAY_10855, AgilityPyramidObstacleType.DECORATION);


	@Getter(AccessLevel.PACKAGE)
	private final WorldArea location;

	@Getter(AccessLevel.PACKAGE)
	private final int obstacleId;

	@Getter(AccessLevel.PACKAGE)
	private AgilityPyramidObstacleType type = AgilityPyramidObstacleType.NORMAL;

	@Getter(AccessLevel.PACKAGE)
	private int bankID = 0;

	AgilityPyramidObstacles(final WorldPoint min, final WorldPoint max, final int obstacleId)
	{
		this.location = new WorldArea(min, max);
		this.obstacleId = obstacleId;
	}

	AgilityPyramidObstacles(final WorldPoint min, final WorldPoint max, final int obstacleId, final int bankID)
	{
		this.location = new WorldArea(min, max);
		this.obstacleId = obstacleId;
		this.bankID = bankID;
	}

	AgilityPyramidObstacles(final WorldPoint min, final WorldPoint max, final int obstacleId, final AgilityPyramidObstacleType type)
	{
		this.location = new WorldArea(min, max);
		this.obstacleId = obstacleId;
		this.type = type;
	}

	AgilityPyramidObstacles(final WorldPoint min, final WorldPoint max, final int obstacleId, final AgilityPyramidObstacleType type, final int bankID)
	{
		this.location = new WorldArea(min, max);
		this.obstacleId = obstacleId;
		this.type = type;
		this.bankID = bankID;
	}

	public AgilityPyramidObstacleType getObstacleType()
	{
		return type;
	}

	public static AgilityPyramidObstacles getObstacle(WorldPoint worldPoint)
	{
		for (AgilityPyramidObstacles obstacle : values())
		{
			if (obstacle.getLocation().distanceTo(worldPoint) == 0)
			{
				return obstacle;
			}
		}
		return null;
	}

}
