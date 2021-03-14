/*
 * Copyright (c) 2018, SomeoneWithAnInternetConnection
 * Copyright (c) 2018, oplosthee <https://github.com/oplosthee>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.agilitypyramid;

import net.runelite.client.config.*;

@ConfigGroup("AgilityPyramid")
public interface AgilityPyramidConfig extends Config
{

	@ConfigSection(
		keyName = "delayConfig",
		name = "Sleep Delay(ms) Configuration",
		description = "Configure how the bot handles sleep delays in milliseconds",
		position = 2
	)
	String delayConfig = "delayConfig";

	@Range(
		min = 0,
		max = 550
	)
	@ConfigItem(
		keyName = "sleepMin",
		name = "Sleep Min",
		description = "",
		position = 3,
		section = "delayConfig"
	)
	default int sleepMin()
	{
		return 60;
	}

	@Range(
		min = 0,
		max = 550
	)
	@ConfigItem(
		keyName = "sleepMax",
		name = "Sleep Max",
		description = "",
		position = 4,
		section = "delayConfig"
	)
	default int sleepMax()
	{
		return 350;
	}

	@Range(
		min = 0,
		max = 550
	)
	@ConfigItem(
		keyName = "sleepTarget",
		name = "Sleep Target",
		description = "",
		position = 5,
		section = "delayConfig"
	)
	default int sleepTarget()
	{
		return 100;
	}

	@Range(
		min = 0,
		max = 550
	)
	@ConfigItem(
		keyName = "sleepDeviation",
		name = "Sleep Deviation",
		description = "",
		position = 6,
		section = "delayConfig"
	)
	default int sleepDeviation()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "sleepWeightedDistribution",
		name = "Sleep Weighted Distribution",
		description = "Shifts the random distribution towards the lower end at the target, otherwise it will be an even distribution",
		position = 7,
		section = "delayConfig"
	)
	default boolean sleepWeightedDistribution()
	{
		return false;
	}

	@ConfigSection(
		keyName = "delayTickConfig",
		name = "Game Tick Configuration",
		description = "Configure how the bot handles game tick delays, 1 game tick equates to roughly 600ms",
		position = 8
	)
	String delayTickConfig = "delayTickConfig";

	@Range(
		min = 0,
		max = 10
	)
	@ConfigItem(
		keyName = "tickDelayMin",
		name = "Game Tick Min",
		description = "",
		position = 9,
		section = "delayTickConfig"
	)
	default int tickDelayMin()
	{
		return 1;
	}

	@Range(
		min = 0,
		max = 10
	)
	@ConfigItem(
		keyName = "tickDelayMax",
		name = "Game Tick Max",
		description = "",
		position = 10,
		section = "delayTickConfig"
	)
	default int tickDelayMax()
	{
		return 3;
	}

	@Range(
		min = 0,
		max = 10
	)
	@ConfigItem(
		keyName = "tickDelayTarget",
		name = "Game Tick Target",
		description = "",
		position = 11,
		section = "delayTickConfig"
	)
	default int tickDelayTarget()
	{
		return 2;
	}

	@Range(
		min = 0,
		max = 10
	)
	@ConfigItem(
		keyName = "tickDelayDeviation",
		name = "Game Tick Deviation",
		description = "",
		position = 12,
		section = "delayTickConfig"
	)
	default int tickDelayDeviation()
	{
		return 1;
	}

	@ConfigItem(
		keyName = "tickDelayWeightedDistribution",
		name = "Game Tick Weighted Distribution",
		description = "Shifts the random distribution towards the lower end at the target, otherwise it will be an even distribution",
		position = 13,
		section = "delayTickConfig"
	)
	default boolean tickDelayWeightedDistribution()
	{
		return false;
	}

	@ConfigTitle(
		keyName = "agilityTitle",
		name = "Plugin Configuration",
		description = "",
		position = 14
	)
	String agilityTitle = "agilityTitle";

	@ConfigItem(
			keyName = "instructions1",
			name = "",
			description = "Instructions. Don't enter anything into this field",
			position = 15,
			title = "agilityTitle"
	)
	default String instructions1()
	{
		return "Pyramid tops will be handed in to Simon Templeton every 3 laps";
	}

	@ConfigItem(
			keyName = "humidify",
			name = "Use Humidify",
			description = "Enable to use humidify when out of waterskins",
			position = 17,
			title = "agilityTitle"
	)
	default boolean humidify()
	{
		return true;
	}

	@ConfigItem(
			keyName = "useStam",
			name = "Use stamina pots",
			description = "Drinks stamina potions",
			position = 18,
			title = "agilityTitle"
	)
	default boolean useStam() { return false; }

	@ConfigItem(
			keyName = "minEnergy",
			name = "Minimum Energy",
			description = "Minimum energy before stam pot drank",
			position = 19,
			hidden = true,
			unhide = "useStam",
			title = "agilityTitle"
	)
	default int minEnergy() { return 35; }

	@ConfigItem(
			keyName = "foodToggle",
			name = "Eat Food",
			description = "Enable to eat food when damaged,",
			position = 21,
			title = "agilityTitle"
	)
	default boolean foodToggle() {return false;}

	@ConfigItem(
			keyName = "instructions2",
			name = "",
			description = "Instructions. Don't enter anything into this field",
			position = 22,
			hidden = true,
			unhide = "foodToggle",
			title = "agilityTitle"
	)
	default String instructions2()
	{
		return "Common food IDs: " +
				"Monkfish: 7946, Karambwan: 3144, Shark: 385 ";
	}

	@ConfigItem(
			keyName = "foodType",
			name = "Food ID",
			description = "ID of food to eat",
			position = 23,
			hidden = true,
			unhide = "foodToggle",
			title = "agilityTitle"
	)
	default int foodType() { return 7946; }


	@ConfigItem(
			keyName = "minHealth",
			name = "Minimum Health",
			description = "Minimum health before food eaten",
			position = 24,
			hidden = true,
			unhide = "foodToggle",
			title = "agilityTitle"
	)
	default int minHealth() { return 30; }

	@ConfigItem(
		keyName = "lowHP",
		name = "Stop bot at HP",
		description = "Stop if HP goes below given threshold",
		position = 28,
		title = "agilityTitle"
	)
	default int lowHP()
	{
		return 9;
	}


	@ConfigItem(
			keyName = "alchItemID",
			name = "Alch Item ID (un-noted)",
			description = "Item ID (un-noted) of item you wish to high alch.",
			position = 24,
			hidden = true,
			unhide = "highAlch"
	)
	default int alchItemID()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "highAlch",
		name = "High Alch",
		description = "Enable to High Alch while running",
		position = 25,
		hidden = true
	)
	default boolean highAlch()
	{
		return false;
	}

	@ConfigItem(
		keyName = "enableUI",
		name = "Enable UI",
		description = "Enable to turn on in game UI",
		position = 29,
		title = "agilityTitle"
	)
	default boolean enableUI()
	{
		return true;
	}

	@ConfigItem(
		keyName = "startButton",
		name = "Start/Stop",
		description = "Test button that changes variable value",
		position = 30
	)
	default Button startButton()
	{
		return new Button();
	}
}
