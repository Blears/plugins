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
package net.runelite.client.plugins.cannonballer;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Point;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
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
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.iutils.*;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static net.runelite.client.plugins.cannonballer.cannonballerState.*;

@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(
	name = "Sandy Cannonballer",
	enabledByDefault = false,
	description = "Makes cannonballs",
	tags = {"cannon, ball, smelt, smith, sandy"}
)
@Slf4j
public class cannonballerPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private cannonballerConfiguration config;

	@Inject
	private iUtils utils;
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
	private cannonballerOverlay overlay;


	cannonballerState state;
	GameObject targetObject;
	MenuEntry targetMenu;
	WorldPoint skillLocation;
	Instant botTimer;
	LocalPoint beforeLoc;
	Player player;

	WorldArea EDGE = new WorldArea(new WorldPoint(3084,3486,0),new WorldPoint(3100,3501,0));
	WorldPoint FURNACE = new WorldPoint(3109,3499,0);



	int timeout = 0;
	long sleepLength;
	boolean startCannonBaller;
	private final Set<Integer> itemIds = new HashSet<>();
	private final Set<Integer> requiredIds = new HashSet<>();


	@Provides
	cannonballerConfiguration provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(cannonballerConfiguration.class);
	}

	private void resetVals()
	{
		overlayManager.remove(overlay);
		state = null;
		timeout = 0;
		botTimer = null;
		skillLocation = null;
		startCannonBaller = false;
		requiredIds.clear();
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked)
	{
		if (!configButtonClicked.getGroup().equalsIgnoreCase("cannonballer"))
		{
			return;
		}
		log.info("button {} pressed!", configButtonClicked.getKey());
		if (configButtonClicked.getKey().equals("startButton"))
		{
			if (!startCannonBaller)
			{
				startCannonBaller = true;
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
		startCannonBaller=false;
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("plankmaker"))
		{
			return;
		}
		startCannonBaller = false;
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

	private void openBank() {
		NPC Banker = npc.findNearestNpc(1618);
		if (Banker != null) {
			targetMenu = new MenuEntry("", "",
					Banker.getIndex(), MenuAction.NPC_THIRD_OPTION.getId(), 0, 0, false);
			menu.setEntry(targetMenu);
			mouse.delayMouseClick(Banker.getConvexHull().getBounds(), sleepDelay());
		}
	}

	private void useFurnace(){
		targetObject = object.findNearestGameObject(16469);
		if (targetObject != null)
		{
			targetMenu = new MenuEntry("Smelt", "<col=ffff>Furnace", targetObject.getId(), 4, targetObject.getSceneMinLocation().getX(),targetObject.getSceneMinLocation().getY(),false);
			menu.setEntry(targetMenu);
			mouse.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());
		}
	}

	private Point getRandomNullPoint()
	{
		if(client.getWidget(161,34)!=null){
			Rectangle nullArea = client.getWidget(161,34).getBounds();
			return new Point ((int)nullArea.getX()+calc.getRandomIntBetweenRange(0,nullArea.width), (int)nullArea.getY()+calc.getRandomIntBetweenRange(0,nullArea.height));
		}

		return new Point(client.getCanvasWidth()-calc.getRandomIntBetweenRange(0,2),client.getCanvasHeight()-calc.getRandomIntBetweenRange(0,2));
	}
	private cannonballerState getBankState()
	{
		if (!inventory.containsItem(4)) {
			return MISSING_ITEMS;
		}
		if(inventory.isFull()){
			return WALK_TO_FURNACE;
		}
		if(inventory.containsItem(2)) {
			return DEPOSIT_ITEMS;
		}
		if(!inventory.containsItem(2)) {
			return WITHDRAWING_STEEL;
		}
		else {
			utils.sendGameMessage("Ran out of steel bars");
			shutDown();
			}
		return IDLE;
	}

	public cannonballerState getState() {
		if (timeout > 0) {
			return TIMEOUT;
		}
		if (playerUtils.isMoving(beforeLoc)) {
			return MOVING;
		}
		if (bank.isOpen()) {
			return getBankState();
		}
		Widget lvlup = client.getWidget(WidgetInfo.LEVEL_UP_SKILL);
		if (lvlup != null && !lvlup.isHidden()) {
			if (inventory.containsItem(ItemID.STEEL_BAR)) {
				return WALK_TO_FURNACE;
			} else {
				return FIND_BANK;
			}
		}
		if (client.getLocalPlayer().getAnimation() != -1) {
			return ANIMATING;
		}
		if ((inventory.getItemCount(2353,false) < 1 && inventory.getItemCount(2,true) > 4) || (player.getWorldArea().intersectsWith(EDGE) && !inventory.isFull())){
			return FIND_BANK;
		}
		if (inventory.isFull()) {
			return getCannonBallerState();
		}
		if (player.getWorldArea().intersectsWith(EDGE) && !inventory.isFull()){
			openBank();
		}
		return IDLE;
	}

	@Subscribe
	private void onGameTick(GameTick tick)
	{
		if (!startCannonBaller)
		{
			return;
		}
		player = client.getLocalPlayer();
		if (client != null && player != null && skillLocation != null)
		{
			if (!client.isResized())
			{
				utils.sendGameMessage("Client must be set to resizable");
				startCannonBaller = false;
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
				case WALK_TO_FURNACE:
					useFurnace();
					break;
				case MAKE_BALLS:
					makeBalls();
					timeout = tickDelay();
					break;
				case MISSING_ITEMS:
					withdrawRequiredItems();
					break;
				case WITHDRAWING_STEEL:
					withdrawSteel();
					break;
				case ANIMATING:
				case MOVING:
					playerUtils.handleRun(30, 20);
					timeout = tickDelay();
					break;
				case FIND_BANK:
					openBank();
					timeout = tickDelay();
					break;
				case DEPOSIT_ITEMS:
					bank.depositAllOfItem(2);
					timeout = tickDelay();
					break;
				case IDLE:
					if (!inventory.containsItem(ItemID.STEEL_BAR)){
						openBank();
						break;
					}
			}
		}
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN && startCannonBaller)
		{
			state = TIMEOUT;
			timeout = 2;
		}
	}

	private void withdrawRequiredItems() {
		bank.withdrawItem(ItemID.AMMO_MOULD);
	}

	private void withdrawSteel() {
		bank.withdrawAllItem(ItemID.STEEL_BAR);
	}

	private void makeBalls() {
		targetMenu = new MenuEntry("Make sets:", "<col=ff9040>Cannonballs</col>", 1, MenuAction.CC_OP.getId(), -1, 17694734, false);
		menu.setEntry(targetMenu);
		mouse.delayMouseClick(client.getWidget(270,14).getBounds(), sleepDelay());
		}

	private cannonballerState getCannonBallerState()
	{
		if (client.getWidget(270,15)!=null){
				return MAKE_BALLS;
		}
				return TIMEOUT;
	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!startCannonBaller)
		{
			return;
		}
		if (targetMenu != null)
		{
			log.debug("MenuEntry string event: " + targetMenu.toString());
			timeout = tickDelay();
		}
	}
}