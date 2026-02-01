package net.blueva.arcade.modules.tnttag.state;

import net.blueva.arcade.api.game.GameContext;
import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaRegistry {

    private final Map<Integer, ArenaRuntime> arenas = new ConcurrentHashMap<>();

    public ArenaRuntime register(GameContext<Player, Location, World, String, ItemStack, String, BlockState, Entity> context) {
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
