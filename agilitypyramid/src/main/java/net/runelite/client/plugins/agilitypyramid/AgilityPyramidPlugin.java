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

import com.google.inject.Provides;
import com.owain.chinbreakhandler.ChinBreakHandler;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.*;
import net.runelite.client.plugins.botutils.BotUtils;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static net.runelite.client.plugins.agilitypyramid.AgilityPyramidState.*;


@Extension
@PluginDependency(BotUtils.class)
@PluginDescriptor(
        name = "Sandy Agility Pyramid",
        enabledByDefault = false,
        description = "Sandy's Auto Agility Pyramid plugin",
        tags = {"sandy", "agility", "pyramid"},
        type = PluginType.SKILLING
)
@Slf4j
public class AgilityPyramidPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private BotUtils utils;

    @Inject
    private AgilityPyramidConfig config;

    @Inject
    PluginManager pluginManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    AgilityPyramidOverlay overlay;

    @Inject
    ItemManager itemManager;

    @Inject
    private ChinBreakHandler chinBreakHandler;

    Player player;
    AgilityPyramidState state;
    Instant botTimer;
    MenuEntry targetMenu;
    LocalPoint beforeLoc = new LocalPoint(0, 0);
    Set<Integer> inventoryItems = new HashSet<>();

    private final Set<Integer> REGION_IDS = Set.of(13356, 12105);
    WorldArea PYRAMID0 = new WorldArea(new WorldPoint(3354,2828,0),new WorldPoint(3366,2832,0));
    WorldArea SIMON_TEMPLETON = new WorldArea (new WorldPoint (3334,2820,0), new WorldPoint(3350,2831,0));

    int timeout;
    int waterskinsLeft;
    long sleepLength;
    boolean startAgility;

    @Override
    protected void startUp() {
        chinBreakHandler.registerPlugin(this);
    }

    @Override
    protected void shutDown() {
        resetVals();
        chinBreakHandler.unregisterPlugin(this);
    }

    @Provides
    AgilityPyramidConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AgilityPyramidConfig.class);
    }

    private void resetVals() {
        overlayManager.remove(overlay);
        chinBreakHandler.stopPlugin(this);
        startAgility = false;
        botTimer = null;
        inventoryItems.clear();
    }

    @Subscribe
    private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked) {
        if (!configButtonClicked.getGroup().equalsIgnoreCase("AgilityPyramid")) {
            return;
        }
        log.info("button {} pressed!", configButtonClicked.getKey());
        switch (configButtonClicked.getKey()) {
            case "startButton":
                if (!startAgility) {
                    startAgility = true;
                    chinBreakHandler.startPlugin(this);
                    state = null;
                    targetMenu = null;
                    botTimer = Instant.now();
                    overlayManager.add(overlay);
                } else {
                    resetVals();
                }
                break;
        }
    }

    private long sleepDelay() {
        sleepLength = utils.randomDelay(config.sleepWeightedDistribution(), config.sleepMin(), config.sleepMax(), config.sleepDeviation(), config.sleepTarget());
        return sleepLength;
    }

    private int tickDelay() {
        int tickLength = (int) utils.randomDelay(config.tickDelayWeightedDistribution(), config.tickDelayMin(), config.tickDelayMax(), config.tickDelayDeviation(), config.tickDelayTarget());
        log.debug("tick delay for {} ticks", tickLength);
        return tickLength;
    }

    private AgilityPyramidObstacles getCurrentObstacle() {
        return AgilityPyramidObstacles.getObstacle(client.getLocalPlayer().getWorldLocation());
    }

    private void findObstacle() {
        AgilityPyramidObstacles obstacle = getCurrentObstacle();
        if (obstacle != null) {
            log.debug(String.valueOf(obstacle.getObstacleId()));

            if (obstacle.getObstacleType() == AgilityPyramidObstacleType.DECORATION) {
                DecorativeObject decObstacle = utils.findNearestDecorObject(obstacle.getObstacleId());
                if (decObstacle != null) {
                    targetMenu = new MenuEntry("", "", decObstacle.getId(), MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId(), decObstacle.getLocalLocation().getSceneX(), decObstacle.getLocalLocation().getSceneY(), false);
                    utils.setMenuEntry(targetMenu);
                    utils.delayMouseClick(decObstacle.getConvexHull().getBounds(), sleepDelay());
                    return;
                }
            }
            if (obstacle.getObstacleType() == AgilityPyramidObstacleType.GROUND_OBJECT) {
                GroundObject groundObstacle = utils.findNearestGroundObject(obstacle.getObstacleId());
                if (groundObstacle != null) {
                    targetMenu = new MenuEntry("", "", groundObstacle.getId(), MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId(), groundObstacle.getLocalLocation().getSceneX(), groundObstacle.getLocalLocation().getSceneY(), false);
                    utils.setMenuEntry(targetMenu);
                    utils.delayMouseClick(groundObstacle.getConvexHull().getBounds(), sleepDelay());
                    return;
                }
            }
            GameObject objObstacle = utils.findNearestGameObject(obstacle.getObstacleId());
            if (objObstacle != null) {
                targetMenu = new MenuEntry("", "", objObstacle.getId(), MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId(), objObstacle.getSceneMinLocation().getX(), objObstacle.getSceneMinLocation().getY(), false);
                utils.setMenuEntry(targetMenu);
                utils.delayMouseClick(objObstacle.getConvexHull().getBounds(), sleepDelay());
                return;
            }
        } else {
            log.debug("Not in obstacle area");
        }
    }

    public AgilityPyramidState getState() {
        if (config.humidify() && REGION_IDS.contains(client.getLocalPlayer().getWorldLocation().getRegionID())) {
            updateWaterskinsLeft();
            if (waterskinsLeft == 0) {
                return CASTING_HUMIDIFY;
            }
        }
        if (utils.isMoving(beforeLoc)) {
            if (config.foodToggle()){
                if (client.getBoostedSkillLevel(Skill.HITPOINTS) < config.minHealth()){
                    return EATING_FOOD;
                }
            }
            if(config.useStam()) {
                if (client.getEnergy() < config.minEnergy()) {
                    return DRINKING_STAMINA;
                }
            }
            timeout = tickDelay();
            return MOVING;
        }
        AgilityPyramidObstacles currentObstacle = AgilityPyramidObstacles.getObstacle(client.getLocalPlayer().getWorldLocation());
        if (currentObstacle == null) {
            timeout = tickDelay();
            if (SIMON_TEMPLETON.intersectsWith(player.getWorldArea())) {
                return HAND_IN_TOPS;
            }
            else {
                walkToSimon();
            }
            return MOVING;

        }
        if (chinBreakHandler.shouldBreak(this)) {
            return HANDLE_BREAK;
        }
        if (!utils.isMoving(beforeLoc)) {
            return FIND_OBSTACLE;
        }
        return ANIMATING;
    }

    @Subscribe
    private void onGameTick(GameTick tick) {
        if (!startAgility || chinBreakHandler.isBreakActive(this)) {
            return;
        }
        player = client.getLocalPlayer();
        if (client != null && player != null && client.getGameState() == GameState.LOGGED_IN && client.getBoostedSkillLevel(Skill.HITPOINTS) > config.lowHP()) {
            if (!client.isResized()) {
                utils.sendGameMessage("Sandy Pyramid - client must be set to resizable");
                startAgility = false;
                return;
            }
            if (!REGION_IDS.contains(client.getLocalPlayer().getWorldLocation().getRegionID())) {
                log.debug("not in agility pyramid region");
                return;
            }
            utils.handleRun(30, 20);
            state = getState();
            beforeLoc = client.getLocalPlayer().getLocalLocation();
            switch (state) {
                case TIMEOUT:
                    timeout--;
                    break;
                case FIND_OBSTACLE:
                    if (player.getAnimation() == 3063) {
                        utils.walk((new WorldPoint(3045 + utils.getRandomIntBetweenRange(0, 2), 4699 + utils.getRandomIntBetweenRange(0, 1), 3)), 1, sleepDelay());
                    }
                    else {
                        if (utils.inventoryItemContainsAmount(ItemID.PYRAMID_TOP,3,false,true)) {
                            if (PYRAMID0.intersectsWith(player.getWorldArea())) {
                                walkToSimon();
                            }
                            else {
                                findObstacle();
                            }
                        }
                        else {
                            findObstacle();
                            }
                        }
                    break;
                case EATING_FOOD:
                    eatFood();
                    break;
                case DRINKING_STAMINA:
                    drinkStam();
                    break;
                case CASTING_HUMIDIFY:
                    castHumidify();
                    break;
                case MOVING:
                    if (player.getWorldLocation().equals(new WorldPoint(3043, 4701, 2))) {
                        utils.walk((new WorldPoint(3048 + utils.getRandomIntBetweenRange(0, 1), 4697 + utils.getRandomIntBetweenRange(0, 2), 2)), 1, sleepDelay());
                    }
                    break;
                case HAND_IN_TOPS:
                    if (utils.inventoryContains(ItemID.PYRAMID_TOP)) {
                        handInTops();
                        break;
                    }
                    else {
                        walkToSimon();
                        break;
                        }
                case HANDLE_BREAK:
                    chinBreakHandler.startBreak(this);
                    timeout = 10;
                    break;
            }
        } else {
            log.debug("client/ player is null or bot isn't started");
            return;
        }
    }


    private void walkToSimon() {
        GroundObject climbRock = utils.findNearestGroundObject(11949);
            if (climbRock != null) {
            //Climb Climbing rocks, id = 11949 (ground object)
                targetMenu = new MenuEntry("", "", climbRock.getId(), MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId(), climbRock.getLocalLocation().getSceneX(), climbRock.getLocalLocation().getSceneY(), false);
                utils.setMenuEntry(targetMenu);
                utils.delayMouseClick(climbRock.getConvexHull().getBounds(), sleepDelay());
            }
        }
    private void handInTops() {
        NPC Simon = utils.findNearestNpc(5786);
            if (Simon != null) {
            targetMenu = new MenuEntry("Use", "<col=ff9040>Pyramid top<col=ffffff> -> <col=ffff00>Simon Templeton", 1648, 7,
                    0, 0, false);
                utils.setModifiedMenuEntry(targetMenu,ItemID.PYRAMID_TOP,utils.getInventoryWidgetItem(ItemID.PYRAMID_TOP).getIndex(),7);
                utils.delayMouseClick(Simon.getConvexHull().getBounds(), sleepDelay());
            }
        }
    private void updateWaterskinsLeft(){
        waterskinsLeft=0;
        waterskinsLeft+=utils.getInventoryItemCount(1823,false)*4; //4 dose waterskin
        waterskinsLeft+=utils.getInventoryItemCount(1825,false)*3; //3 dose waterskin
        waterskinsLeft+=utils.getInventoryItemCount(1827,false)*2; //2 dose waterskin
        waterskinsLeft+=utils.getInventoryItemCount(1829,false); //1 dose waterskin

        if(waterskinsLeft==0){
            if(!utils.inventoryContains(1831)){
                waterskinsLeft=-1; //no waterskins detected
            }
        }
    }
    private void castHumidify() {
        if ( !utils.inventoryContains(9075) && !utils.runePouchContains(9075)) {
            utils.sendGameMessage("Out of astrals runes");
            startAgility = false;
        }
        targetMenu = new MenuEntry("Cast", "<col=00ff00>Humidify</col>", 1, 57, -1, 14286954, false);
        Widget spellWidget = utils.getSpellWidget("Humidify");
        if (spellWidget == null) {
            utils.sendGameMessage("Unable to find humidify widget");
            startAgility = false;
        }
        utils.oneClickCastSpell(utils.getSpellWidgetInfo("Humidify"), targetMenu, sleepDelay());
    }
    private void eatFood() {
        if (utils.inventoryContains(config.foodType())) {
            targetMenu = new MenuEntry("", "", utils.getInventoryWidgetItem(config.foodType()).getId(), MenuOpcode.ITEM_FIRST_OPTION.getId(), utils.getInventoryWidgetItem(config.foodType()).getIndex(), 9764864, false);
            utils.delayMouseClick(utils.getInventoryWidgetItem(config.foodType()).getCanvasBounds(), sleepDelay());
        }
    }
    private void drinkStam() {
        utils.drinkStamPot(config.minEnergy());
                }
}