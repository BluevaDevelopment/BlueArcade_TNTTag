package net.blueva.arcade.modules.tnttag.support;

import net.blueva.arcade.api.config.CoreConfigAPI;
import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.module.ModuleInfo;
import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Holder;

import java.util.List;

public class MessagingService {

    private final ModuleConfigAPI moduleConfig;
    private final CoreConfigAPI coreConfig;
    private final ModuleInfo moduleInfo;

    public MessagingService(ModuleConfigAPI moduleConfig, CoreConfigAPI coreConfig, ModuleInfo moduleInfo) {
        this.moduleConfig = moduleConfig;
        this.coreConfig = coreConfig;
        this.moduleInfo = moduleInfo;
    }

    public void sendDescription(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context) {
        List<String> description = moduleConfig.getStringListFrom("language.yml", "description");

        for (Player player : context.getPlayers()) {
            for (String line : description) {
                context.getMessagesAPI().sendRaw(player, line);
            }
        }
    }

    public void sendCountdownTick(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context,
                                  int secondsLeft) {
        for (Player player : context.getPlayers()) {
            context.getSoundsAPI().play(player, coreConfig.getSound("sounds.starting_game.countdown"));

            String title = coreConfig.getLanguage("titles.starting_game.title")
                    .replace("{game_display_name}", moduleInfo.getName())
                    .replace("{time}", String.valueOf(secondsLeft));

            String subtitle = coreConfig.getLanguage("titles.starting_game.subtitle")
                    .replace("{game_display_name}", moduleInfo.getName())
                    .replace("{time}", String.valueOf(secondsLeft));

            context.getTitlesAPI().sendRaw(player, title, subtitle, 0, 20, 5);
        }
    }

    public void sendCountdownFinish(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context) {
        for (Player player : context.getPlayers()) {
            String title = coreConfig.getLanguage("titles.game_started.title")
                    .replace("{game_display_name}", moduleInfo.getName());

            String subtitle = coreConfig.getLanguage("titles.game_started.subtitle")
                    .replace("{game_display_name}", moduleInfo.getName());

            context.getTitlesAPI().sendRaw(player, title, subtitle, 0, 20, 20);
            context.getSoundsAPI().play(player, coreConfig.getSound("sounds.starting_game.start"));
        }
    }

    public void broadcastTaggedHolder(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context,
                                      Player newHolder) {
        String playerName = newHolder.getPlayerRef() != null ? newHolder.getPlayerRef().getUsername() : null;
        if (playerName == null || playerName.isBlank()) {
            playerName = "Unknown";
        }
        String broadcast = moduleConfig.getStringFrom("language.yml", "messages.player_has_the_tnt")
                .replace("{player}", playerName);
        for (Player player : context.getPlayers()) {
            context.getMessagesAPI().sendRaw(player, broadcast);
        }
    }

    public void announceNewRound(GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context,
                                 int round) {
        for (Player player : context.getPlayers()) {
            context.getMessagesAPI().sendRaw(player,
                    moduleConfig.getStringFrom("language.yml", "messages.new_round")
                            .replace("{round}", String.valueOf(round)));
        }
    }
}
