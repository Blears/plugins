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
package net.runelite.client.plugins.plankmaker;

import com.google.inject.Provides;
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
import net.runelite.client.plugins.*;
import net.runelite.client.plugins.botutils.BotUtils;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static net.runelite.client.plugins.plankmaker.plankmakerState.*;


@Extension
@PluginDependency(BotUtils.class)
@PluginDescriptor(
	name = "Sandy Plankmaker",
	enabledByDefault = false,
	description = "Makes planks",
	tags = {"plank, maker, construction, sandy"},
	type = PluginType.SKILLING
)
@Slf4j
public class plankmakerPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private plankmakerConfiguration config;

	@Inject
	private BotUtils utils;

	@Inject
	private ConfigManager configManager;

	@Inject
	PluginManager pluginManager;

	@Inject
	OverlayManager overlayManager;

	@Inject
	private plankmakerOverlay overlay;


	plankmakerState state;
	GameObject targetObject;
	NPC targetNPC;
	MenuEntry targetMenu;
	WorldPoint skillLocation;
	Instant botTimer;
	LocalPoint beforeLoc;
	Player player;

	WorldArea BANK = new WorldArea(new WorldPoint(1585,3473,0),new WorldPoint(1597,3487,0));
	WorldArea GUILD_MID = new WorldArea(new WorldPoint(1595,3497,0),new WorldPoint(1610,3500,0));
	WorldArea OAK_TREES = new WorldArea(new WorldPoint(1609,3506,0),new WorldPoint(1624,3516,0));
	WorldPoint SAWMILL = new WorldPoint(1624,3500,0);


	int timeout = 0;
	long sleepLength;
	boolean startPlankMaker;
	private final Set<Integer> itemIds = new HashSet<>();
	private final Set<Integer> requiredIds = new HashSet<>();


	@Provides
	plankmakerConfiguration provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(plankmakerConfiguration.class);
	}

	private void resetVals()
	{
		overlayManager.remove(overlay);
		state = null;
		timeout = 0;
		botTimer = null;
		skillLocation = null;
		startPlankMaker = false;
		requiredIds.clear();
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked)
	{
		if (!configButtonClicked.getGroup().equalsIgnoreCase("plankmaker"))
		{
			return;
		}
		log.info("button {} pressed!", configButtonClicked.getKey());
		if (configButtonClicked.getKey().equals("startButton"))
		{
			if (!startPlankMaker)
			{
				startPlankMaker = true;
				state = null;
				targetMenu = null;
				botTimer = Instant.now();
				setLocation();
				overlayManager.add(overlay);
				requiredIds.add(995);
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
		if (!event.getGroup().equals("plankmaker"))
		{
			return;
		}
		startPlankMaker = false;
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
		sleepLength = utils.randomDelay(config.sleepWeightedDistribution(), config.sleepMin(), config.sleepMax(), config.sleepDeviation(), config.sleepTarget());
		return sleepLength;
	}

	private int tickDelay()
	{
		int tickLength = (int) utils.randomDelay(config.tickDelayWeightedDistribution(), config.tickDelayMin(), config.tickDelayMax(), config.tickDelayDeviation(), config.tickDelayTarget());
		log.debug("tick delay for {} ticks", tickLength);
		return tickLength;
	}

	private void interactOakTree()
	{
		targetObject = utils.findNearestGameObjectWithin(player.getWorldLocation(), 5, Collections.singleton(10820));
		if (targetObject != null)
		{
			targetMenu = new MenuEntry("Chop down", "<col=ffff>Oak", targetObject.getId(), MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId(), targetObject.getSceneMinLocation().getX(), targetObject.getSceneMinLocation().getY(), false);
			utils.setMenuEntry(targetMenu);
			utils.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());
		}
		else
		{
			log.info("Oak tree is null");
		}
	}

	private void useSawmill()
	{
		targetNPC = utils.findNearestNpcWithin(player.getWorldLocation(), 25, Collections.singleton(3101));
		if (targetNPC != null)
		{
			targetMenu = new MenuEntry("Buy-plank", "<col=ffff00>Sawmill operator", targetNPC.getIndex(), 11, 0, 0, false);
			utils.setMenuEntry(targetMenu);
			utils.delayMouseClick(targetNPC.getConvexHull().getBounds(), sleepDelay());
		}
		else
		{
			log.info("Banker is null");
		}
	}

	private void openBank()
	{
		targetObject = utils.findNearestGameObject(28861);
		if (targetObject != null)
		{
			targetMenu = new MenuEntry("Bank", "<col=ffff>Bank chest", targetObject.getId(), MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId(), targetObject.getSceneMinLocation().getX(), targetObject.getSceneMinLocation().getY(), false);
			utils.setMenuEntry(targetMenu);
			utils.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());
		}
		else
		{
			log.info("Bank is null");
		}
	}

	private plankmakerState getBankState()
	{
		if(utils.inventoryFull()){
			return DEPOSIT_ITEMS;
		}
		if(!utils.inventoryFull()){
			return WALK_TO_OAK;
		}
		return UNHANDLED_STATE;
	}

	public plankmakerState getState() {
		if (timeout > 0) {
			return TIMEOUT;
		}
		if (!utils.inventoryContains(requiredIds)) {
			return MISSING_ITEMS;
		}
		if (utils.isMoving(beforeLoc)) {
			timeout = 2 + tickDelay();
			return MOVING;
		}
		if (utils.isBankOpen()) {
			return getBankState();
		}
		if (client.getLocalPlayer().getAnimation() != -1) {
			return ANIMATING;
		}
		if (utils.inventoryFull()) {
			return getPlankMakerState();
		}
		if (player.getWorldArea().intersectsWith(OAK_TREES)) {
			return FIND_OAK;
		}
		if (player.getWorldArea().intersectsWith(GUILD_MID)) {
			if (!utils.inventoryFull()) {
				return WALK_TO_OAK;
			}
		}
		if (player.getWorldArea().intersectsWith(BANK)) {
			if (!utils.inventoryFull()) {
				return WALK_TO_OAK;
			}
		}
		return UNHANDLED_STATE;
	}

	@Subscribe
	private void onGameTick(GameTick tick)
	{
		if (!startPlankMaker)
		{
			return;
		}
		player = client.getLocalPlayer();
		if (client != null && player != null && skillLocation != null)
		{
			if (!client.isResized())
			{
				utils.sendGameMessage("Client must be set to resizable");
				startPlankMaker = false;
				return;
			}
			state = getState();
			beforeLoc = player.getLocalLocation();
			switch (state)
			{
				case TIMEOUT:
					utils.handleRun(30, 20);
					timeout--;
					break;
				case FIND_OAK:
					interactOakTree();
					timeout = tickDelay();
					break;
				case WALK_TO_SAWMILL:
					useSawmill();
					break;
				case MAKE_PLANK:
					makePlank();
					timeout = tickDelay();
					break;
				case MISSING_ITEMS:
					startPlankMaker = false;
					utils.sendGameMessage("Missing required items IDs: " + String.valueOf(requiredIds) + " from inventory. Stopping.");
					resetVals();
					break;
				case ANIMATING:
				case MOVING:
					utils.handleRun(30, 20);
					timeout = tickDelay();
					break;
				case WALK_TO_MIDDLE:
					utils.walk(new WorldPoint(1603+utils.getRandomIntBetweenRange(-3,3),3498+utils.getRandomIntBetweenRange(-1,1),0),0,sleepDelay());
					timeout = tickDelay();
					break;
				case WALK_TO_OAK:
					utils.walk(new WorldPoint(1619+utils.getRandomIntBetweenRange(0,1),3508+utils.getRandomIntBetweenRange(0,3),0),0,sleepDelay());
					timeout = tickDelay();
					break;
				case FIND_BANK:
					openBank();
					timeout = tickDelay();
					break;
				case DEPOSIT_ITEMS:
					utils.depositAllExcept(requiredIds);
					timeout = tickDelay();
					break;
			}
		}
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN && startPlankMaker)
		{
			state = TIMEOUT;
			timeout = 2;
		}
	}

	private void makePlank() {
		targetMenu = new MenuEntry("Make", "<col=ff9040>Oak - 250gp</col>", 1, 57, -1, 17694735, false);
		utils.delayMouseClick(client.getWidget(270,15).getBounds(), sleepDelay());
		}

	private plankmakerState getPlankMakerState()
	{
		log.info("getting plank maker state");
		if(utils.inventoryFull()){
			if(player.getWorldArea().intersectsWith(OAK_TREES)){
				return WALK_TO_SAWMILL;
			}
			if((player.getWorldLocation().equals(SAWMILL) && utils.inventoryContains(8778)) || (player.getWorldArea().intersectsWith(GUILD_MID)) || (player.getWorldArea().intersectsWith(BANK))){
				return FIND_BANK;
			}
			if (client.getWidget(270,15)!=null){
				return MAKE_PLANK;
			}
		}
		return TIMEOUT;
	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked event){
		log.info(event.toString());
	}
}