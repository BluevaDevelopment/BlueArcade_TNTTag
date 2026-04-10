package net.blueva.arcade.modules.tnttag;

import net.blueva.arcade.api.ModuleAPI;
import net.blueva.arcade.api.achievements.AchievementsAPI;
import net.blueva.arcade.api.config.CoreConfigAPI;
import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.events.CustomEventRegistry;
import net.blueva.arcade.api.events.EventSubscription;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.game.GameModule;
import net.blueva.arcade.api.game.GameResult;
import net.blueva.arcade.api.module.ModuleInfo;
import net.blueva.arcade.api.stats.StatsAPI;
import net.blueva.arcade.api.ui.VoteMenuAPI;
import net.blueva.arcade.modules.tnttag.game.TNTTagGameManager;
import net.blueva.arcade.modules.tnttag.listener.TNTTagDamageListener;
import net.blueva.arcade.modules.tnttag.listener.TNTTagItemDropListener;
import net.blueva.arcade.modules.tnttag.setup.TNTTagSetup;
import net.blueva.arcade.modules.tnttag.support.StatsService;
import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Holder;

import java.util.Map;

public class TNTTagModule implements GameModule<Player, Location, World, String, ItemStack, String, Holder, Entity, EventSubscription<?>, Short> {

    private ModuleConfigAPI moduleConfig;
    public CoreConfigAPI coreConfig;
    private ModuleInfo moduleInfo;
    private StatsService statsService;
    private TNTTagGameManager gameManager;
    private boolean damageListenerRegistered;
    private boolean itemDropListenerRegistered;

    @Override
    public void onLoad() {
        moduleInfo = ModuleAPI.getModuleInfo("tnt_tag");
        if (moduleInfo == null) {
            throw new IllegalStateException("ModuleInfo not available for TNT Tag module");
        }

        moduleConfig = ModuleAPI.getModuleConfig(moduleInfo.getId());
        coreConfig = ModuleAPI.getCoreConfig();
        StatsAPI<Player> statsAPI = ModuleAPI.getStatsAPI();
        VoteMenuAPI<String> voteMenu = ModuleAPI.getVoteMenuAPI();
        AchievementsAPI achievementsAPI = ModuleAPI.getAchievementsAPI();

        statsService = new StatsService(statsAPI, moduleInfo);
        statsService.registerStats();

        moduleConfig.register("language.yml", 1);
        moduleConfig.register("settings.yml", 1);
        moduleConfig.register("achievements.yml", 1);

        if (achievementsAPI != null) {
            achievementsAPI.registerModuleAchievements(moduleInfo.getId(), "achievements.yml");
        }

        gameManager = new TNTTagGameManager(moduleConfig, coreConfig, moduleInfo, statsService);
        ModuleAPI.getSetupAPI().registerHandler(moduleInfo.getId(), new TNTTagSetup(moduleConfig));

        if (moduleConfig != null && voteMenu != null) {
            String voteItem = moduleConfig.getString("menus.vote.item");
            voteMenu.registerGame(
                    moduleInfo.getId(),
                    voteItem,
                    moduleConfig.getStringFrom("language.yml", "vote_menu.name"),
                    moduleConfig.getStringListFrom("language.yml", "vote_menu.lore")
            );
        }
    }

    @Override
    public void onStart(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context) {
        gameManager.handleStart(context);
    }

    @Override
    public void onCountdownTick(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context,
                                int secondsLeft) {
        gameManager.handleCountdownTick(context, secondsLeft);
    }

    @Override
    public void onCountdownFinish(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context) {
        gameManager.handleCountdownFinish(context);
    }

    @Override
    public boolean freezePlayersOnCountdown() {
        return gameManager.freezePlayersOnCountdown();
    }

    @Override
    public void onGameStart(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context) {
        gameManager.handleGameStart(context);
    }

    @Override
    public void onEnd(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context,
                      GameResult<Player> result) {
        gameManager.handleEnd(context, result);
    }

    @Override
    public void onDisable() {
        gameManager.onDisable();
    }

    @Override
    public void registerEvents(CustomEventRegistry<EventSubscription<?>, Short> registry) {
        if (!damageListenerRegistered || !itemDropListenerRegistered) {
            if (registry instanceof net.blueva.arcade.api.events.hytale.HytaleEventRegistry hytaleRegistry) {
                if (!damageListenerRegistered) {
                    hytaleRegistry.registerSystem(new TNTTagDamageListener(gameManager));
                    damageListenerRegistered = true;
                }
                if (!itemDropListenerRegistered) {
                    hytaleRegistry.registerSystem(new TNTTagItemDropListener(gameManager));
                    itemDropListenerRegistered = true;
                }
            }
        }
    }

    @Override
    public Map<String, String> getCustomPlaceholders(Player player) {
        return gameManager.getCustomPlaceholders(player);
    }

    public ModuleConfigAPI getModuleConfig() {
        return moduleConfig;
    }

    public CoreConfigAPI getCoreConfig() {
        return coreConfig;
    }

    public TNTTagGameManager getGameManager() {
        return gameManager;
    }
}
