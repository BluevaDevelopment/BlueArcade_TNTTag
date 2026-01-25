package net.blueva.arcade.modules.tnttag.game;

import net.blueva.arcade.api.config.CoreConfigAPI;
import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.game.GameResult;
import net.blueva.arcade.api.module.ModuleInfo;
import net.blueva.arcade.modules.tnttag.state.ArenaRegistry;
import net.blueva.arcade.modules.tnttag.state.ArenaRuntime;
import net.blueva.arcade.modules.tnttag.state.PlayerArenaRegistry;
import net.blueva.arcade.modules.tnttag.state.TagCountTracker;
import net.blueva.arcade.modules.tnttag.support.IndicatorService;
import net.blueva.arcade.modules.tnttag.support.LoadoutService;
import net.blueva.arcade.modules.tnttag.support.MessagingService;
import net.blueva.arcade.modules.tnttag.support.StatsService;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class TNTTagGameManager {

    private final ModuleConfigAPI moduleConfig;
    private final CoreConfigAPI coreConfig;
    private final StatsService statsService;
    private final ArenaRegistry arenaRegistry = new ArenaRegistry();
    private final PlayerArenaRegistry playerArenaRegistry = new PlayerArenaRegistry();
    private final TagCountTracker tagCountTracker = new TagCountTracker();
    private final LoadoutService loadoutService;
    private final IndicatorService indicatorService;
    private final MessagingService messagingService;

    public TNTTagGameManager(ModuleConfigAPI moduleConfig,
                             CoreConfigAPI coreConfig,
                             ModuleInfo moduleInfo,
                             StatsService statsService) {
        this.moduleConfig = moduleConfig;
        this.coreConfig = coreConfig;
        this.statsService = statsService;
        this.loadoutService = new LoadoutService(moduleConfig);
        this.indicatorService = new IndicatorService(moduleConfig);
        this.messagingService = new MessagingService(moduleConfig, coreConfig, moduleInfo);
    }

    public void handleStart(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();

        context.getSchedulerAPI().cancelArenaTasks(arenaId);

        ArenaRuntime runtime = arenaRegistry.register(context);
        runtime.setRoundNumber(0);
        runtime.setRoundTimeLeft(resolveRoundDuration(context));
        runtime.resetTags();
        runtime.resetBlinkTicks();
        runtime.setBlinkState(false);

        playerArenaRegistry.registerPlayers(context.getPlayers(), arenaId);
        tagCountTracker.initializePlayers(context.getPlayers());

        messagingService.sendDescription(context);
    }

    public void handleCountdownTick(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                    int secondsLeft) {
        messagingService.sendCountdownTick(context, secondsLeft);
    }

    public void handleCountdownFinish(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        messagingService.sendCountdownFinish(context);
    }

    public boolean freezePlayersOnCountdown() {
        return false;
    }

    public void handleGameStart(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();

        for (Player player : context.getPlayers()) {
            loadoutService.prepareForStart(player);
            context.getScoreboardAPI().showScoreboard(player, getScoreboardPath());
        }

        beginNextRound(context);

        String timerTaskId = getTimerTaskId(arenaId);
        context.getSchedulerAPI().runTimer(timerTaskId, () -> handleGameTick(context), 0L, 20L);

        String indicatorTaskId = getIndicatorTaskId(arenaId);
        long indicatorPeriod = Math.max(1, moduleConfig.getInt("tagging.inventory_indicator.tick_rate", 5));
        context.getSchedulerAPI().runTimer(indicatorTaskId, () -> updateIndicators(context), 0L, indicatorPeriod);
    }

    private void handleGameTick(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();
        ArenaRuntime runtime = arenaRegistry.get(arenaId);
        if (runtime == null) {
            context.getSchedulerAPI().cancelTask(getTimerTaskId(arenaId));
            return;
        }

        if (runtime.isEnded()) {
            context.getSchedulerAPI().cancelTask(getTimerTaskId(arenaId));
            return;
        }

        int timeLeft = runtime.getRoundTimeLeft() - 1;
        runtime.setRoundTimeLeft(timeLeft);

        List<Player> alivePlayers = context.getAlivePlayers();

        if (alivePlayers.size() <= 1) {
            finishGameIfPossible(context);
            return;
        }

        if (timeLeft <= 0) {
            handleRoundExpiration(context, runtime);
            return;
        }

        sendHudUpdates(context, alivePlayers, timeLeft, runtime);
    }

    private void sendHudUpdates(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                List<Player> actionBarTargets,
                                int timeLeft,
                                ArenaRuntime runtime) {
        List<Player> alivePlayers = context.getAlivePlayers();
        int aliveCount = alivePlayers.size();
        int spectatorsCount = context.getSpectators().size();

        for (Player player : actionBarTargets) {
            if (!player.isOnline()) continue;

            String actionBarTemplate = coreConfig.getLanguage("action_bar.in_game.global");
            Map<String, String> placeholders = getCustomPlaceholders(player);
            placeholders.put("time", String.valueOf(timeLeft));
            placeholders.put("alive", String.valueOf(aliveCount));
            placeholders.put("spectators", String.valueOf(spectatorsCount));

            if (actionBarTemplate != null) {
                String actionBarMessage = actionBarTemplate
                        .replace("{time}", String.valueOf(timeLeft))
                        .replace("{round}", String.valueOf(runtime.getRoundNumber()))
                        .replace("{round_max}", String.valueOf(context.getMaxRounds()));
                context.getMessagesAPI().sendActionBar(player, actionBarMessage);
            }
        }

        for (Player viewer : context.getPlayers()) {
            if (!viewer.isOnline()) continue;

            Map<String, String> placeholders = getCustomPlaceholders(viewer);
            placeholders.put("time", String.valueOf(timeLeft));
            placeholders.put("alive", String.valueOf(aliveCount));
            placeholders.put("spectators", String.valueOf(spectatorsCount));

            context.getScoreboardAPI().update(viewer, getScoreboardPath(), placeholders);
        }

        applySmokeWarning(context, runtime, timeLeft);
    }

    private void handleRoundExpiration(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                       ArenaRuntime runtime) {
        Set<UUID> currentTagged = new HashSet<>(runtime.getTaggedPlayers());

        awardSurvivalStats(context, runtime, currentTagged);
        detonateTaggedPlayers(context, runtime, currentTagged);

        List<Player> alivePlayers = context.getAlivePlayers();
        if (alivePlayers.size() <= 1) {
            finishGameIfPossible(context);
            return;
        }

        beginNextRound(context);
    }

    private void awardSurvivalStats(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                    ArenaRuntime runtime,
                                    Set<UUID> currentTagged) {
        Set<UUID> hadTNT = runtime.getTaggedThisRound();
        for (Player player : context.getAlivePlayers()) {
            UUID uuid = player.getUniqueId();
            if (currentTagged.contains(uuid)) {
                continue;
            }

            statsService.recordSurvival(player);
            if (hadTNT.contains(uuid)) {
                statsService.recordEscape(player);
            }
        }
    }

    private void detonateTaggedPlayers(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                       ArenaRuntime runtime,
                                       Set<UUID> currentTagged) {
        for (Player player : new ArrayList<>(context.getAlivePlayers())) {
            if (!currentTagged.contains(player.getUniqueId())) {
                continue;
            }

            indicatorService.clearIndicator(player);
            context.getMessagesAPI().send(player,
                    moduleConfig.getStringFrom("language.yml", "messages.detonated"));
            context.getSoundsAPI().play(player, coreConfig.getSound("sounds.in_game.dead"));
            player.getWorld().spawnParticle(
                    Particle.EXPLOSION,
                    player.getLocation().add(0, 1, 0),
                    moduleConfig.getInt("explosion.particle_count", 6),
                    0.35, 0.35, 0.35, 0.02
            );
            context.eliminatePlayer(player, moduleConfig.getStringFrom("language.yml", "messages.eliminated"));
            player.getInventory().clear();
            player.setGameMode(GameMode.SPECTATOR);
        }

        sendHudUpdates(context, context.getAlivePlayers(),
                runtime.getRoundTimeLeft(),
                runtime);

        runtime.resetTags();
    }

    private void beginNextRound(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        ArenaRuntime runtime = arenaRegistry.get(context.getArenaId());
        if (runtime == null) {
            return;
        }

        runtime.setRoundNumber(runtime.getRoundNumber() + 1);
        runtime.setRoundTimeLeft(resolveRoundDuration(context));
        runtime.resetBlinkTicks();
        runtime.setBlinkState(false);
        runtime.resetTags();

        selectTaggedPlayers(context, runtime);
        messagingService.announceNewRound(context, runtime.getRoundNumber());

        sendHudUpdates(context, context.getAlivePlayers(), runtime.getRoundTimeLeft(), runtime);
    }

    private int resolveRoundDuration(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        Integer configuredTime = context.getDataAccess().getGameData("basic.time", Integer.class);
        if (configuredTime == null || configuredTime <= 0) {
            configuredTime = moduleConfig.getInt("round.default_seconds", 15);
        }

        if (shouldConvertSetupTime(configuredTime)) {
            configuredTime = Math.max(1, configuredTime / 60);
        }

        return Math.max(1, configuredTime);
    }

    private boolean shouldConvertSetupTime(int configuredSeconds) {
        boolean convert = moduleConfig.getBoolean("round.convert_setup_time_from_minutes", true);
        int detectionThreshold = Math.max(1, moduleConfig.getInt("round.minutes_conversion_minimum_seconds", 60));

        return convert && configuredSeconds >= detectionThreshold && configuredSeconds % 60 == 0;
    }

    private void selectTaggedPlayers(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                     ArenaRuntime runtime) {
        List<Player> alivePlayers = new ArrayList<>(context.getAlivePlayers());
        if (alivePlayers.isEmpty()) {
            return;
        }

        int taggedCount = Math.min(alivePlayers.size(), calculateTaggedPlayers(alivePlayers.size()));
        Collections.shuffle(alivePlayers);

        for (int i = 0; i < taggedCount; i++) {
            Player player = alivePlayers.get(i);
            runtime.getTaggedPlayers().add(player.getUniqueId());
            runtime.getTaggedThisRound().add(player.getUniqueId());
            handleTaggedStatusChange(context, player, true);
        }
    }

    private int calculateTaggedPlayers(int playerCount) {
        List<String> rules = moduleConfig.getStringList("tagging.tagged_players.rules");
        int fallback = Math.max(1, moduleConfig.getInt("tagging.tagged_players.fallback", 1));

        for (String rule : rules) {
            String[] parts = rule.split(":");
            if (parts.length != 2) {
                continue;
            }

            String range = parts[0];
            int tagged;
            try {
                tagged = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                continue;
            }

            if (range.contains("+")) {
                try {
                    int min = Integer.parseInt(range.replace("+", ""));
                    if (playerCount >= min) {
                        return tagged;
                    }
                } catch (NumberFormatException ignored) {
                    // Ignore malformed rule
                }
            } else if (range.contains("-")) {
                String[] bounds = range.split("-");
                if (bounds.length == 2) {
                    try {
                        int min = Integer.parseInt(bounds[0]);
                        int max = Integer.parseInt(bounds[1]);
                        if (playerCount >= min && playerCount <= max) {
                            return tagged;
                        }
                    } catch (NumberFormatException ignored) {
                        // Ignore malformed rule
                    }
                }
            }
        }

        return fallback;
    }

    private void updateIndicators(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();
        ArenaRuntime runtime = arenaRegistry.get(arenaId);
        if (runtime == null) {
            context.getSchedulerAPI().cancelTask(getIndicatorTaskId(arenaId));
            return;
        }

        if (runtime.isEnded()) {
            context.getSchedulerAPI().cancelTask(getIndicatorTaskId(arenaId));
            return;
        }

        int timeLeft = runtime.getRoundTimeLeft();
        int maxTicks = Math.max(1, moduleConfig.getInt("tagging.inventory_indicator.blink.max_ticks", 10));
        int minTicks = Math.max(1, moduleConfig.getInt("tagging.inventory_indicator.blink.min_ticks", 2));
        int criticalSeconds = Math.max(1, moduleConfig.getInt("tagging.inventory_indicator.critical_seconds", 5));

        double normalized = (double) Math.min(timeLeft, criticalSeconds) / criticalSeconds;
        int interval = (int) Math.round(minTicks + (maxTicks - minTicks) * normalized);
        interval = Math.max(minTicks, Math.min(maxTicks, interval));

        int ticks = runtime.incrementBlinkTicks();
        if (ticks >= interval) {
            runtime.resetBlinkTicks();
            runtime.toggleBlinkState();
        }

        for (Player player : context.getAlivePlayers()) {
            if (!isTagged(context, player)) {
                continue;
            }
            indicatorService.applyIndicator(player, runtime);
        }
    }

    private void applySmokeWarning(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                   ArenaRuntime runtime,
                                   int timeLeft) {
        boolean enabled = moduleConfig.getBoolean("round.smoke_warning.enabled", true);
        int atSeconds = moduleConfig.getInt("round.smoke_warning.seconds", 4);

        if (!enabled || timeLeft > atSeconds) {
            return;
        }

        Particle particle = safeParticle("round.smoke_warning.particle", "SMOKE");
        int count = moduleConfig.getInt("round.smoke_warning.count", 8);
        double offset = moduleConfig.getDouble("round.smoke_warning.offset", 0.3D);

        for (Player player : context.getAlivePlayers()) {
            if (!isTagged(context, player)) {
                continue;
            }

            player.getWorld().spawnParticle(
                    particle,
                    player.getLocation().add(0, 0.9, 0),
                    count,
                    offset,
                    offset,
                    offset,
                    0.02
            );
        }
    }

    public void finishGameIfPossible(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();
        ArenaRuntime runtime = arenaRegistry.get(arenaId);
        if (runtime == null) {
            return;
        }

        if (!runtime.markEnded()) {
            return;
        }

        context.getSchedulerAPI().cancelArenaTasks(arenaId);

        List<Player> alivePlayers = new ArrayList<>(context.getAlivePlayers());
        if (alivePlayers.size() == 1) {
            Player winner = alivePlayers.getFirst();
            if (runtime.setWinnerIfAbsent(winner.getUniqueId())) {
                context.setWinner(winner);
                statsService.recordWin(winner);
            }
        }

        context.endGame();
    }

    public void handleEnd(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                          GameResult<Player> result) {
        int arenaId = context.getArenaId();

        context.getSchedulerAPI().cancelArenaTasks(arenaId);
        arenaRegistry.remove(arenaId);

        statsService.recordGamesPlayed(context.getPlayers());
        for (Player player : context.getPlayers()) {
            indicatorService.clearIndicator(player);
        }
        tagCountTracker.removePlayers(context.getPlayers());
        playerArenaRegistry.removeArena(arenaId);
    }

    public void onDisable() {
        ArenaRuntime any = arenaRegistry.any();
        if (any != null) {
            any.getContext().getSchedulerAPI().cancelModuleTasks("tnt_tag");
        }

        arenaRegistry.clear();
        playerArenaRegistry.clear();
        tagCountTracker.clear();
        indicatorService.clearAllIndicators();
    }

    public boolean playerCanTag(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                Player player) {
        return isTagged(context, player);
    }

    public void passTNT(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                        Player from,
                        Player to) {
        int arenaId = context.getArenaId();
        ArenaRuntime runtime = arenaRegistry.get(arenaId);
        if (runtime == null || runtime.isEnded()) {
            return;
        }
        Set<UUID> tagged = runtime.getTaggedPlayers();
        if (!tagged.contains(from.getUniqueId())) {
            return;
        }

        if (from.equals(to)) {
            return;
        }

        if (tagged.contains(to.getUniqueId())) {
            return;
        }

        tagged.remove(from.getUniqueId());
        handleTaggedStatusChange(context, from, false);

        tagged.add(to.getUniqueId());
        runtime.getTaggedThisRound().add(to.getUniqueId());
        handleTaggedStatusChange(context, to, true);

        statsService.recordTag(from);
        tagCountTracker.increment(from);

        messagingService.broadcastTaggedHolder(context, to);
    }

    public GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> getGameContext(Player player) {
        Integer arenaId = playerArenaRegistry.getArenaId(player);
        if (arenaId == null) {
            return null;
        }
        ArenaRuntime runtime = arenaRegistry.get(arenaId);
        return runtime != null ? runtime.getContext() : null;
    }

    public Map<String, String> getCustomPlaceholders(Player player) {
        Map<String, String> placeholders = new HashMap<>();

        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = getGameContext(player);
        if (context != null) {
            ArenaRuntime runtime = arenaRegistry.get(context.getArenaId());
            int roundNumber = runtime != null ? runtime.getRoundNumber() : 1;
            int timeLeft = runtime != null ? runtime.getRoundTimeLeft() : resolveRoundDuration(context);

            placeholders.put("alive", String.valueOf(context.getAlivePlayers().size()));
            placeholders.put("spectators", String.valueOf(context.getSpectators().size()));
            placeholders.put("round", String.valueOf(roundNumber));
            placeholders.put("time", String.valueOf(timeLeft));
            placeholders.put("total_players", String.valueOf(context.getPlayers().size()));

            List<String> taggedNames = getTaggedPlayerNames(context);
            String none = moduleConfig.getStringFrom("language.yml", "placeholders.none");
            if (none == null || none.isEmpty()) {
                none = "None";
            }

            for (int i = 0; i < 5; i++) {
                String value = i < taggedNames.size() ? taggedNames.get(i) : none;
                placeholders.put("tnt_holder_" + (i + 1), value);
            }
        }

        return placeholders;
    }

    private List<String> getTaggedPlayerNames(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        ArenaRuntime runtime = arenaRegistry.get(context.getArenaId());
        if (runtime == null) {
            return Collections.emptyList();
        }
        Set<UUID> tagged = runtime.getTaggedPlayers();
        if (tagged.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> names = new ArrayList<>();
        for (Player player : context.getPlayers()) {
            if (tagged.contains(player.getUniqueId())) {
                names.add(player.getName());
            }
        }

        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    private void handleTaggedStatusChange(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                          Player player,
                                          boolean tagged) {
        if (tagged) {
            context.getMessagesAPI().send(player,
                    moduleConfig.getStringFrom("language.yml", "messages.you_have_tnt"));
            indicatorService.applyIndicator(player, arenaRegistry.get(context.getArenaId()));
            return;
        }

        context.getMessagesAPI().send(player,
                moduleConfig.getStringFrom("language.yml", "messages.you_no_have_tnt"));
        indicatorService.clearIndicator(player);
    }

    private boolean isTagged(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                             Player player) {
        ArenaRuntime runtime = arenaRegistry.get(context.getArenaId());
        return runtime != null && runtime.getTaggedPlayers().contains(player.getUniqueId());
    }

    private String getScoreboardPath() {
        return "scoreboard.main";
    }

    private String getTimerTaskId(int arenaId) {
        return "tnt_tag_timer_" + arenaId;
    }

    private String getIndicatorTaskId(int arenaId) {
        return "tnt_tag_indicator_" + arenaId;
    }

    private Particle safeParticle(String path, String def) {
        String value = moduleConfig.getString(path, def);
        if (value == null || value.isEmpty()) {
            value = def;
        }

        String normalized = value.toUpperCase(Locale.ROOT);
        Particle particle = parseParticle(normalized);
        if (particle != null) {
            return particle;
        }

        Particle fallback = parseParticle(def.toUpperCase(Locale.ROOT));
        if (fallback != null) {
            return fallback;
        }

        return Particle.CLOUD;
    }

    private Particle parseParticle(String name) {
        try {
            return Particle.valueOf(name);
        } catch (Exception e) {
            return null;
        }
    }
}
