package net.blueva.arcade.modules.tnttag.state;

import net.blueva.arcade.api.game.GameContext;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaRuntime {

    private final int arenaId;
    private final GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context;
    private boolean ended;
    private int roundNumber;
    private int roundTimeLeft;
    private int blinkTicks;
    private boolean blinkState;
    private UUID winner;
    private final Set<UUID> taggedPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> taggedThisRound = ConcurrentHashMap.newKeySet();

    public ArenaRuntime(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        this.context = context;
        this.arenaId = context.getArenaId();
    }

    public int getArenaId() {
        return arenaId;
    }

    public GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> getContext() {
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
