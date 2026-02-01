package net.blueva.arcade.modules.tnttag.state;

import net.blueva.arcade.api.game.GameContext;
import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;

import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaRuntime {

    private final int arenaId;
    private final GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> context;
    private boolean ended;
    private int roundNumber;
    private int roundTimeLeft;
    private int blinkTicks;
    private boolean blinkState;
    private UUID winner;
    private final Set<UUID> taggedPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> taggedThisRound = ConcurrentHashMap.newKeySet();
    private final List<UUID> eliminationOrder = new ArrayList<>();

    public ArenaRuntime(GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> context) {
        this.context = context;
        this.arenaId = context.getArenaId();
    }

    public int getArenaId() {
        return arenaId;
    }

    public GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> getContext() {
        return context;
    }

    public boolean isEnded() {
        return ended;
    }

    public boolean markEnded() {
        boolean alreadyEnded = ended;
        ended = true;
        return !alreadyEnded;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    public int getRoundTimeLeft() {
        return roundTimeLeft;
    }

    public void setRoundTimeLeft(int roundTimeLeft) {
        this.roundTimeLeft = roundTimeLeft;
    }

    public Set<UUID> getTaggedPlayers() {
        return taggedPlayers;
    }

    public Set<UUID> getTaggedThisRound() {
        return taggedThisRound;
    }

    public void resetTags() {
        taggedPlayers.clear();
        taggedThisRound.clear();
    }

    public void recordElimination(UUID playerId) {
        if (playerId == null || eliminationOrder.contains(playerId)) {
            return;
        }
        eliminationOrder.add(playerId);
    }

    public List<UUID> getEliminationOrder() {
        return Collections.unmodifiableList(eliminationOrder);
    }

    public int incrementBlinkTicks() {
        blinkTicks++;
        return blinkTicks;
    }

    public void setBlinkState(boolean blinkState) {
        this.blinkState = blinkState;
    }

    public void resetBlinkTicks() {
        blinkTicks = 0;
    }

    public boolean isBlinkState() {
        return blinkState;
    }

    public void toggleBlinkState() {
        blinkState = !blinkState;
    }

    public UUID getWinner() {
        return winner;
    }

    public boolean setWinnerIfAbsent(UUID winner) {
        if (this.winner != null) {
            return false;
        }
        this.winner = winner;
        return true;
    }
}
