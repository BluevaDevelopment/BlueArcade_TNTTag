package net.blueva.arcade.modules.tnttag.listener;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.modules.tnttag.game.TNTTagGameManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TNTTagItemDropListener extends EntityEventSystem<EntityStore, DropItemEvent.PlayerRequest> {

    private final TNTTagGameManager gameManager;

    public TNTTagItemDropListener(TNTTagGameManager gameManager) {
        super(DropItemEvent.PlayerRequest.class);
        this.gameManager = gameManager;
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull DropItemEvent.PlayerRequest event) {
        if (event.isCancelled()) {
            return;
        }
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        if (ref == null) {
            return;
        }
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context =
                gameManager.getGameContext(player);
        if (context == null || !context.isPlayerPlaying(player)) {
            return;
        }
        event.setCancelled(true);
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
