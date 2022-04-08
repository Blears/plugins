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
package net.runelite.client.plugins.teakchopper;

import com.google.inject.Provides;
import com.sandyplugins.plugin.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static net.runelite.client.plugins.teakchopper.teakchopperState.*;


@Extension
@PluginDependency(sUtils.class)
@PluginDescriptor(
	name = "Sandy Chopper",
	enabledByDefault = false,
	description = "Chops and banks at Fossil Island",
	tags = {"teak, mahogany, chopper, woodcutting, sandy"}
)
@Slf4j
public class teakchopperPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private teakchopperConfiguration config;

	@Inject
	private sUtils utils;
	@Inject
	private MouseUtils mouse;
	@Inject
	private KeyboardUtils key;
	@Inject
	private PlayerUtils playerUtils;
	@Inject
	private InventoryUtils inventory;
	@Inject
	private InterfaceUtils interfaceUtils;
	@Inject
	private CalculationUtils calc;
	@Inject
	private MenuUtils menu;
	@Inject
	private ObjectUtils object;
	@Inject
	private BankUtils bank;
	@Inject
	private NPCUtils npc;
	@Inject
	private WalkUtils walk;

	@Inject
	private ConfigManager configManager;

	@Inject
	PluginManager pluginManager;

	@Inject
	OverlayManager overlayManager;

	@Inject
	private teakchopperOverlay overlay;


	teakchopperState state;
	GameObject targetObject;
	LegacyMenuEntry targetMenu;
	WorldPoint skillLocation;
	Instant botTimer;
	LocalPoint beforeLoc;
	Player player;

	WorldArea TEAKS = new WorldArea(new WorldPoint(3700,3830,0),new WorldPoint(3720,3840,0));
	WorldArea BANK = new WorldArea(new WorldPoint(3711,3800,0),new WorldPoint(3745,3816,0));


	int timeout = 0;
	long sleepLength;
	boolean startTeakChopper;
	private final Set<Integer> itemIds = new HashSet<>();
	private final Set<Integer> requiredIds = new HashSet<>();
	private final Set<Integer> TREES = Set.of(30481, 30482);


	@Provides
	teakchopperConfiguration provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(teakchopperConfiguration.class);
	}

	private void resetVals()
	{
		overlayManager.remove(overlay);
		state = null;
		timeout = 0;
		botTimer = null;
		skillLocation = null;
		startTeakChopper = false;
		requiredIds.clear();
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked)
	{
		if (!configButtonClicked.getGroup().equalsIgnoreCase("teakchopper"))
		{
			return;
		}
		log.info("button {} pressed!", configButtonClicked.getKey());
		if (configButtonClicked.getKey().equals("startButton"))
		{
			if (!startTeakChopper)
			{
				startTeakChopper = true;
				state = null;
				targetMenu = null;
				botTimer = Instant.now();
				setLocation();
				overlayManager.add(overlay);
			}
			else
			{
				resetVals();
			}
		}
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("teakchopper"))
		{
			return;
		}
		startTeakChopper = false;
	}

	public void setLocation()
	{
		if (client != null && client.getLocalPlayer() != null && client.getGameState().equals(GameState.LOGGED_IN))
		{
			skillLocation = client.getLocalPlayer().getWorldLocation();
			beforeLoc = client.getLocalPlayer().getLocalLocation();
		}
		else
		{
			log.debug("Tried to start bot before being logged in");
			skillLocation = null;
			resetVals();
		}
	}

	private long sleepDelay()
	{
		sleepLength = calc.randomDelay(config.sleepWeightedDistribution(), config.sleepMin(), config.sleepMax(), config.sleepDeviation(), config.sleepTarget());
		return sleepLength;
	}

	private int tickDelay()
	{
		int tickLength = (int) calc.randomDelay(config.tickDelayWeightedDistribution(), config.tickDelayMin(), config.tickDelayMax(), config.tickDelayDeviation(), config.tickDelayTarget());
		log.debug("tick delay for {} ticks", tickLength);
		return tickLength;
	}

	private void interactTeakTree1()
	{
		targetObject = object.findNearestGameObject(30482);
		if (targetObject != null) {
				targetMenu = new LegacyMenuEntry("Chop down", "<col=ffff>Mahogany", targetObject.getId(), MenuAction.GAME_OBJECT_FIRST_OPTION, targetObject.getSceneMinLocation().getX(), targetObject.getSceneMinLocation().getY(), false);
				menu.setEntry(targetMenu);
				mouse.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());
			}
	}

	private void interactTeakTree2()
	{
		targetObject = object.findNearestGameObject(30480);
		if (targetObject != null) {
			targetMenu = new LegacyMenuEntry("Chop down", "<col=ffff>Mahogany", targetObject.getId(), MenuAction.GAME_OBJECT_FIRST_OPTION, targetObject.getSceneMinLocation().getX(), targetObject.getSceneMinLocation().getY(), false);
			menu.setEntry(targetMenu);
			mouse.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());
		}
	}

	private void interactTeakTree3()
	{
		targetObject = object.findNearestGameObject(30481);
		if (targetObject != null) {
			targetMenu = new LegacyMenuEntry("Chop down", "<col=ffff>Mahogany", targetObject.getId(), MenuAction.GAME_OBJECT_FIRST_OPTION, targetObject.getSceneMinLocation().getX(), targetObject.getSceneMinLocation().getY(), false);
			menu.setEntry(targetMenu);
			mouse.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());
		}
	}

	private void openBank()
	{
		targetObject = object.findNearestGameObject(31427);
		if (targetObject != null)
		{
			targetMenu = new LegacyMenuEntry("Bank", "<col=ffff>Bank chest", targetObject.getId(), MenuAction.GAME_OBJECT_FIRST_OPTION, targetObject.getSceneMinLocation().getX(), targetObject.getSceneMinLocation().getY(), false);
			menu.setEntry(targetMenu);
			mouse.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());
		}
		else
		{
			log.info("Bank is null");
		}
	}

	private teakchopperState getBankState()
	{
		if(inventory.isFull()){
			return DEPOSIT_ITEMS;
		}
		if(!inventory.isFull()){
			return WALK_TO_TEAK;
		}
		return UNHANDLED_STATE;
	}

	public teakchopperState getState() {
		if (timeout > 0) {
			return TIMEOUT;
		}
		if (playerUtils.isMoving(beforeLoc)) {
			return MOVING;
		}
		if (bank.isOpen()) {
			return getBankState();
		}
		if (client.getLocalPlayer().getAnimation() != -1) {
			return ANIMATING;
		}
		if (inventory.isFull()) {
			return getTeakChopperState();
		}
		if (!player.getWorldArea().intersectsWith(BANK)) {
			return FIND_TEAK;
		}
		if (player.getWorldArea().intersectsWith(BANK)) {
			if (!inventory.isFull()) {
				return WALK_TO_TEAK;
			}
		}
		return UNHANDLED_STATE;
	}

	@Subscribe
	private void onGameTick(GameTick tick)
	{
		if (!startTeakChopper)
		{
			return;
		}
		player = client.getLocalPlayer();
		if (client != null && player != null && skillLocation != null)
		{
			if (!client.isResized())
			{
				utils.sendGameMessage("Client must be set to resizable");
				startTeakChopper = false;
				return;
			}
			state = getState();
			beforeLoc = player.getLocalLocation();
			switch (state)
			{
				case TIMEOUT:
					playerUtils.handleRun(30, 20);
					timeout--;
					break;
				case FIND_TEAK: //tree 1 = east, tree 2 is middle, tree 3 is west
					if (client.getVarbitValue(4771) == 39 && client.getVarbitValue(4772) == 39){
						interactTeakTree1();
						timeout = tickDelay();
						}
					if (client.getVarbitValue(4771) == 40 && client.getVarbitValue(4772) == 39) { //if right tree = cut
						interactTeakTree2();
						timeout = tickDelay();
					}
					if (client.getVarbitValue(4772) == 40 && client.getVarbitValue(4771) == 39) {// if left tree = cut{
						interactTeakTree1();
						timeout = tickDelay();
					}
					if (client.getVarbitValue(4771) == 40 && client.getVarbitValue(4772) == 40 && client.getVarbitValue(4773) == 39){ //if both = cut
						interactTeakTree3();
						timeout = tickDelay();
					}
					if (client.getVarbitValue(4773) == 40 && client.getVarbitValue(4771) == 39){
						interactTeakTree1();
						timeout = tickDelay();
					}
					if (client.getVarbitValue(4773) == 40 && client.getVarbitValue(4772) == 40 && client.getVarbitValue(4771) == 39){
						interactTeakTree1();
						timeout = tickDelay();
					}
					if (client.getVarbitValue(4773) == 40 && client.getVarbitValue(4772) == 39 && client.getVarbitValue(4771) == 40){
						interactTeakTree2();
						timeout = tickDelay();
					}
					if (client.getVarbitValue(4771) == 40 && client.getVarbitValue(4772) == 40 && client.getVarbitValue(4773) == 40){
						timeout = 5 + tickDelay();
					}
					timeout = tickDelay();
					break;
				case MISSING_ITEMS:
					startTeakChopper = false;
					utils.sendGameMessage("Missing required items IDs: " + String.valueOf(requiredIds) + " from inventory. Stopping.");
					resetVals();
					break;
				case ANIMATING:
				case MOVING:
					playerUtils.handleRun(30, 20);
					break;
				case WALK_TO_BANK:
					targetObject = object.findNearestGameObject(31482);
					if (targetObject != null)
					{
						targetMenu = new LegacyMenuEntry("Climb through", "<col=ffff>Hole", targetObject.getId(), MenuAction.GAME_OBJECT_FIRST_OPTION, targetObject.getSceneMinLocation().getX(), targetObject.getSceneMinLocation().getY(), false);
						menu.setEntry(targetMenu);
						mouse.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());
					}
					else
					{
						log.info("cave is null");
					}
					break;
				case WALK_TO_TEAK:
					targetObject = object.findNearestGameObject(31481 );
					if (targetObject != null)
					{
						targetMenu = new LegacyMenuEntry("Climb through", "<col=ffff>Hole", targetObject.getId(), MenuAction.GAME_OBJECT_FIRST_OPTION, targetObject.getSceneMinLocation().getX(), targetObject.getSceneMinLocation().getY(), false);
						menu.setEntry(targetMenu);
						mouse.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());
					}
					else
					{
						log.info("cave is null");
					}
					break;
				case FIND_BANK:
					openBank();
					break;
				case DEPOSIT_ITEMS:
					bank.depositAllExcept(requiredIds);
					break;
			}
		}
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN && startTeakChopper)
		{
			state = TIMEOUT;
			timeout = 2;
		}
	}


	private teakchopperState getTeakChopperState()
	{
		log.info("getting teak chopper state");
		if(inventory.isFull()){
			if(player.getWorldArea().intersectsWith(TEAKS)){
				return WALK_TO_BANK;
			}
			if((player.getWorldArea().intersectsWith(BANK)) && inventory.containsItem(6332)){
				return FIND_BANK;
			}
		}
		return TIMEOUT;
	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!startTeakChopper)
		{
			return;
		}
		if (targetMenu != null)
		{
			log.debug("LegacyMenuEntry string event: " + targetMenu.toString());
			timeout = tickDelay();
		}
	}
}
