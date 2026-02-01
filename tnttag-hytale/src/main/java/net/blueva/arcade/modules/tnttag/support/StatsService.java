package net.blueva.arcade.modules.tnttag.support;

import net.blueva.arcade.api.module.ModuleInfo;
import net.blueva.arcade.api.stats.StatDefinition;
import net.blueva.arcade.api.stats.StatScope;
import net.blueva.arcade.api.stats.StatsAPI;
import com.hypixel.hytale.server.core.entity.entities.Player;

import java.util.Collection;

public class StatsService {

    private final StatsAPI<Player> statsAPI;
    private final ModuleInfo moduleInfo;

    public StatsService(StatsAPI<Player> statsAPI, ModuleInfo moduleInfo) {
        this.statsAPI = statsAPI;
        this.moduleInfo = moduleInfo;
    }

    public void registerStats() {
        if (statsAPI == null) {
            return;
        }

        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("wins", "Wins", "TNT Tag wins", StatScope.MODULE));
        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("games_played", "Games Played", "TNT Tag games played", StatScope.MODULE));
        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("tags_given", "Tags passed", "Times you passed the TNT to others", StatScope.MODULE));
        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("rounds_survived", "Rounds survived", "Rounds outlived in TNT Tag", StatScope.MODULE));
        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("tnt_escaped", "TNT escapes", "Times you passed TNT before it exploded", StatScope.MODULE));
    }

    public void recordWin(Player player) {
        if (statsAPI == null) {
            return;
        }
        statsAPI.addModuleStat(player, moduleInfo.getId(), "wins", 1);
        statsAPI.addGlobalStat(player, "wins", 1);
    }

    public void recordGamesPlayed(Collection<Player> players) {
        if (statsAPI == null) {
            return;
        }
        for (Player player : players) {
            statsAPI.addModuleStat(player, moduleInfo.getId(), "games_played", 1);
        }
    }

    public void recordTag(Player player) {
        if (statsAPI == null || player == null) {
            return;
        }
        statsAPI.addModuleStat(player, moduleInfo.getId(), "tags_given", 1);
    }

    public void recordSurvival(Player player) {
        if (statsAPI == null || player == null) {
            return;
        }
        statsAPI.addModuleStat(player, moduleInfo.getId(), "rounds_survived", 1);
    }

    public void recordEscape(Player player) {
        if (statsAPI == null || player == null) {
            return;
        }
        statsAPI.addModuleStat(player, moduleInfo.getId(), "tnt_escaped", 1);
    }
}
