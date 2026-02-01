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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaRegistry {

    private final Map<Integer, ArenaRuntime> arenas = new ConcurrentHashMap<>();

    public ArenaRuntime register(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        ArenaRuntime runtime = new ArenaRuntime(context);
        arenas.put(context.getArenaId(), runtime);
        return runtime;
    }

    public ArenaRuntime get(int arenaId) {
        return arenas.get(arenaId);
    }

    public ArenaRuntime remove(int arenaId) {
        return arenas.remove(arenaId);
    }

    public ArenaRuntime any() {
        return arenas.values().stream().findFirst().orElse(null);
    }

    public Collection<ArenaRuntime> values() {
        return arenas.values();
    }

    public void clear() {
        arenas.clear();
    }
}
