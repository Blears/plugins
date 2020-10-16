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
package net.runelite.client.plugins.tabmaker;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Point;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.*;
import net.runelite.client.plugins.botutils.BotUtils;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static net.runelite.client.plugins.tabmaker.tabmakerState.*;

@Extension
@PluginDependency(BotUtils.class)
@PluginDescriptor(
	name = "Sandy TabMaker",
	enabledByDefault = false,
	description = "Makes teletabs",
	tags = {"tab, maker, teletab, tele, sandy"},
	type = PluginType.SKILLING
)
@Slf4j
public class tabmakerPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private tabmakerConfiguration config;

	@Inject
	private BotUtils utils;

	@Inject
	private ConfigManager configManager;

	@Inject
	PluginManager pluginManager;

	@Inject
	OverlayManager overlayManager;

	@Inject
	private tabmakerOverlay overlay;


	tabmakerState state;
	GameObject targetObject;
	MenuEntry targetMenu;
	WorldPoint skillLocation;
	Instant botTimer;
	LocalPoint beforeLoc;
	Player player;
	private final Set<Integer> BANK = Set.of(11571,12342);

	//WorldArea BANK = new WorldArea(new WorldPoint(2928,3276,0),new WorldPoint(2942,3290,0));


	int timeout = 0;
	long sleepLength;
	boolean startTabMaker;
	private final Set<Integer> CRAFTING_CAPE = Set.of(ItemID.CRAFTING_CAPE);
	private final Set<Integer> requiredIds = new HashSet<>();


	@Provides
	tabmakerConfiguration provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(tabmakerConfiguration.class);
	}

	private void resetVals()
	{
		overlayManager.remove(overlay);
		state = null;
		timeout = 0;
		botTimer = null;
		skillLocation = null;
		startTabMaker = false;
		requiredIds.clear();
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked)
	{
		if (!configButtonClicked.getGroup().equalsIgnoreCase("tabmaker"))
		{
			return;
		}
		log.info("button {} pressed!", configButtonClicked.getKey());
		if (configButtonClicked.getKey().equals("startButton"))
		{
			if (!startTabMaker)
			{
				startTabMaker = true;
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

	@Override
	protected void shutDown()
	{
		// runs on plugin shutdown
		overlayManager.remove(overlay);
		log.info("Plugin stopped");
		startTabMaker=false;
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("plankmaker"))
		{
			return;
		}
		startTabMaker = false;
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

	private void interactLectern()
	{
		targetObject = utils.findNearestGameObjectWithin(player.getWorldLocation(), 10, Collections.singleton(13647));
		if (targetObject != null)
		{
			targetMenu = new MenuEntry("Study", "<col=ffff>Lectern", targetObject.getId(), MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId(), targetObject.getSceneMinLocation().getX(), targetObject.getSceneMinLocation().getY(), false);
			utils.setMenuEntry(targetMenu);
			utils.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());
		}
		else
		{
			log.info("Lectern is null");
		}
	}


	private void makeTab() {
		targetMenu = new MenuEntry("Create", "<col=ff9040>House Teleport</col>", 1, 57, -1, 5177359, false);
		utils.delayMouseClick(client.getWidget(79,15).getBounds(), sleepDelay());
	}

	private void houseTele() {
		targetMenu = new MenuEntry("Cast", "<col=00ff00>Teleport to House</col>", 1, 57, -1, 14286876, false);
		Widget spellWidget = client.getWidget(WidgetInfo.SPELL_TELEPORT_TO_HOUSE);
		if (spellWidget != null) {
					utils.setMenuEntry(targetMenu);
					utils.delayMouseClick(spellWidget.getBounds(), sleepDelay());
				}
		}

	private void bankTele(int menuIdentifier){
			targetMenu = new MenuEntry("", "", menuIdentifier, MenuOpcode.CC_OP.getId(), -1,
					25362447, false);
			Widget capeWidget = client.getWidget(WidgetInfo.EQUIPMENT_CAPE);
			if (capeWidget != null)
			{
				utils.setMenuEntry(targetMenu);
				utils.delayMouseClick(capeWidget.getBounds(), sleepDelay());
			}
			else
			{
				utils.setMenuEntry(targetMenu);
				utils.delayMouseClick(new Point(0,0), sleepDelay());
			}
		}

		private void gloryTele() {
			DecorativeObject decObstacle = utils.findNearestDecorObject(13523);
			if (decObstacle != null) {
				targetMenu = new MenuEntry("", "", decObstacle.getId(), MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId(), decObstacle.getLocalLocation().getSceneX(), decObstacle.getLocalLocation().getSceneY(), false);
				utils.setMenuEntry(targetMenu);
				utils.delayMouseClick(decObstacle.getConvexHull().getBounds(), sleepDelay());
				return;
			}
		}


	private void openBank()
	{
		targetObject = utils.findNearestGameObject(14886);
		NPC npc = utils.findNearestNpc(1618);
		if (targetObject != null)
		{
			targetMenu = new MenuEntry("Bank", "<col=ffff>Bank chest", targetObject.getId(), MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId(), targetObject.getSceneMinLocation().getX(), targetObject.getSceneMinLocation().getY(), false);
			utils.setMenuEntry(targetMenu);
			utils.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());
		}
		if (npc != null)
		{
			targetMenu = new MenuEntry("", "",
					npc.getIndex(), MenuOpcode.NPC_THIRD_OPTION.getId(), 0, 0, false);
			utils.setMenuEntry(targetMenu);
			utils.delayMouseClick(npc.getConvexHull().getBounds(), sleepDelay());
		}
	}


	private Point getRandomNullPoint()
	{
		if(client.getWidget(161,34)!=null){
			Rectangle nullArea = client.getWidget(161,34).getBounds();
			return new Point ((int)nullArea.getX()+utils.getRandomIntBetweenRange(0,nullArea.width), (int)nullArea.getY()+utils.getRandomIntBetweenRange(0,nullArea.height));
		}

		return new Point(client.getCanvasWidth()-utils.getRandomIntBetweenRange(0,2),client.getCanvasHeight()-utils.getRandomIntBetweenRange(0,2));
	}
	private tabmakerState getBankState()
	{
		if (!utils.inventoryContains(12791)) {
			return MISSING_ITEMS;
		}
		if(utils.inventoryFull() && utils.isBankOpen()){
			return CLOSING_BANK;
		}
		if(utils.inventoryContains(8013)) {
			return DEPOSIT_ITEMS;
		}
		if(!utils.inventoryContains(1761)) {
			return WITHDRAWING_CLAY;
		}
		else {
			utils.sendGameMessage("Ran out of clay");
			shutDown();
			}
		return IDLE;
	}

	public tabmakerState getState() {
		if (timeout > 0) {
			return TIMEOUT;
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
		if ((utils.getInventoryItemCount(1761,false) < 1 && utils.getInventoryItemCount(8013,true) > 25) && (!BANK.contains(client.getLocalPlayer().getWorldLocation().getRegionID()))) {
			return BANK_TELEPORT;
		}
		if (utils.inventoryFull() && BANK.contains(client.getLocalPlayer().getWorldLocation().getRegionID())){
			return HOUSE_TELEPORT;
		}
		if (utils.inventoryFull() && !BANK.contains(client.getLocalPlayer().getWorldLocation().getRegionID())){
			return getTabMakerState();
		}
		if (BANK.contains(client.getLocalPlayer().getWorldLocation().getRegionID()) && !utils.inventoryContains(ItemID.SOFT_CLAY)){
			openBank();
		}
		return IDLE;
	}

	@Subscribe
	private void onGameTick(GameTick tick)
	{
		if (!startTabMaker)
		{
			return;
		}
		player = client.getLocalPlayer();
		if (client != null && player != null && skillLocation != null)
		{
			if (!client.isResized())
			{
				utils.sendGameMessage("Client must be set to resizable");
				startTabMaker = false;
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
				case MAKE_TABS:
					makeTab();
					timeout = tickDelay();
					break;
				case MISSING_ITEMS:
					withdrawRequiredItems();
					break;
				case WITHDRAWING_CLAY:
					withdrawSoftClay();
					break;
				case HOUSE_TELEPORT:
					houseTele();
					break;
				case BANK_TELEPORT:
					if (utils.isItemEquipped(CRAFTING_CAPE)) {
						bankTele(3);
					}
					else {
						gloryTele();
					}
					break;
				case CLICK_LECTERN:
					interactLectern();
					break;
				case CLOSING_BANK:
					utils.closeBank();
					break;
				case ANIMATING:
				case MOVING:
					utils.handleRun(30, 20);
					timeout = tickDelay();
					break;
				case FIND_BANK:
					openBank();
					timeout = tickDelay();
					break;
				case DEPOSIT_ITEMS:
					utils.depositAllOfItem(8013);
					timeout = tickDelay();
					break;
				case IDLE:
						break;
					}
			}
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN && startTabMaker)
		{
			state = TIMEOUT;
			timeout = 2;
		}
	}

	private void withdrawSoftClay() {
		utils.withdrawAllItem(ItemID.SOFT_CLAY);
	}

	private void withdrawRequiredItems() {
		utils.withdrawItem(ItemID.RUNE_POUCH);
	}

	private tabmakerState getTabMakerState()
	{
		if (utils.inventoryFull() && !player.getWorldArea().equals(BANK) && (client.getWidget(79,15)== null)){
			return CLICK_LECTERN;
		}
		if (client.getWidget(79,15)!=null){
			return MAKE_TABS;
		}
			return TIMEOUT;
	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked event){
		log.info(event.toString());
	}
}