package net.runelite.client.plugins.template;

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
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.*;
import net.runelite.client.plugins.sUtils.*;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static net.runelite.client.plugins.template.templateState.*;

@Extension
@PluginDependency(sUtils.class)
@PluginDescriptor(
		name = "Sandy template",
		enabledByDefault = false,
		description = "template",
		tags = {"template, sandy"}
)
@Slf4j
public class templatePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private templateConfiguration config;

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
	private templateOverlay overlay;


	templateState state;
	GameObject targetObject;
	LegacyMenuEntry targetMenu;
	WorldPoint skillLocation;
	Instant botTimer;
	LocalPoint beforeLoc;
	Player player;

	WorldArea x = new WorldArea(new WorldPoint(0,0,0),new WorldPoint(0,0,0));
	WorldPoint y = new WorldPoint(0,0,0);



	int timeout = 0;
	long sleepLength;
	boolean startTemplate;
	private final Set<Integer> itemIds = new HashSet<>();
	private final Set<Integer> requiredIds = new HashSet<>();


	@Provides
	templateConfiguration provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(templateConfiguration.class);
	}

	private void resetVals()
	{
		overlayManager.remove(overlay);
		state = null;
		timeout = 0;
		botTimer = null;
		skillLocation = null;
		startTemplate = false;
		requiredIds.clear();
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked)
	{
		if (!configButtonClicked.getGroup().equalsIgnoreCase("template"))
		{
			return;
		}
		log.info("button {} pressed!", configButtonClicked.getKey());
		if (configButtonClicked.getKey().equals("startButton"))
		{
			if (!startTemplate)
			{
				startTemplate = true;
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
		startTemplate=false;
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("plankmaker"))
		{
			return;
		}
		startTemplate = false;
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


	private Point getRandomNullPoint()
	{
		if(client.getWidget(161,34)!=null){
			Rectangle nullArea = client.getWidget(161,34).getBounds();
			return new Point ((int)nullArea.getX()+calc.getRandomIntBetweenRange(0,nullArea.width), (int)nullArea.getY()+calc.getRandomIntBetweenRange(0,nullArea.height));
		}

		return new Point(client.getCanvasWidth()-calc.getRandomIntBetweenRange(0,2),client.getCanvasHeight()-calc.getRandomIntBetweenRange(0,2));
	}



	public templateState getState() {
		if (timeout > 0) {
			return TIMEOUT;
		}
		if (playerUtils.isMoving(beforeLoc)) {
			return MOVING;
		}
		if (client.getLocalPlayer().getAnimation() != -1) {
			return ANIMATING;
		}
		if (!inventory.containsItem(requiredIds)) {
			return MISSING_ITEMS;
		}
		if (config.templateBank() == true){
			return getBankState();
		}
		return IDLE;
	}

	private templateState getBankState()
	{
		return IDLE;
	}

	private void openBank() {
			NPC banker = npc.findNearestNpc(1618);
			if (banker != null) {
				targetMenu = new LegacyMenuEntry("", "", 9267, MenuAction.NPC_THIRD_OPTION.getId(), 0, 0, false);
				menu.setEntry(targetMenu);
				mouse.delayMouseClick(banker.getConvexHull().getBounds(), sleepDelay());
			}
		}

	private void targetObject() {
		targetObject = object.findNearestGameObject(0);
		if (targetObject != null)
		{
			targetMenu = new LegacyMenuEntry("", "", targetObject.getId(), MenuAction.GAME_OBJECT_FIRST_OPTION.getId(), targetObject.getSceneMinLocation().getX(), targetObject.getSceneMinLocation().getY(), false);
			menu.setEntry(targetMenu);
			mouse.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());
		}
	}

	private int distanceToObject(){
		targetObject = object.findNearestGameObject(0);
		if (targetObject != null){
			return client.getLocalPlayer().getWorldLocation().distanceTo2D(targetObject.getWorldLocation());
			}
		return 100;
	}

	@Subscribe
	private void onGameTick(GameTick tick)
	{
		if (!startTemplate)
		{
			return;
		}
		player = client.getLocalPlayer();
		if (client != null && player != null && skillLocation != null)
		{
			if (!client.isResized())
			{
				utils.sendGameMessage("Client must be set to resizable");
				startTemplate = false;
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
				case MISSING_ITEMS:
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
					bank.depositAll();
					timeout = tickDelay();
					break;
				case FIND_OBJECT:
					if(distanceToObject()>=1) {
						targetObject();
					}
				case IDLE:
					break;
			}
		}
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN && startTemplate)
		{
			state = TIMEOUT;
			timeout = 2;
		}
	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked event){
		log.info(event.toString());
	}
}