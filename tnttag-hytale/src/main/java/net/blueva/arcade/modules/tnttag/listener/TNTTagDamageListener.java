package net.blueva.arcade.modules.tnttag.listener;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.game.GamePhase;
import net.blueva.arcade.modules.tnttag.game.TNTTagGameManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

public class TNTTagDamageListener extends EntityEventSystem<EntityStore, Damage> {

    private final TNTTagGameManager gameManager;

    public TNTTagDamageListener(TNTTagGameManager gameManager) {
        super(Damage.class);
        this.gameManager = gameManager;
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {
        if (damage.isCancelled()) {
            return;
        }

        Ref<EntityStore> victimRef = archetypeChunk.getReferenceTo(index);
        if (victimRef == null) {
            return;
        }

        PlayerRef victimRefComponent = store.getComponent(victimRef, PlayerRef.getComponentType());
        Player victimPlayer = store.getComponent(victimRef, Player.getComponentType());
        if (victimRefComponent == null || victimPlayer == null) {
            return;
        }

        GameContext<Player, Location, World, String, ItemStack, String, Holder, Entity> context =
                gameManager.getGameContext(victimPlayer);
        if (context == null || !context.isPlayerPlaying(victimPlayer)) {
            return;
        }

        if (context.getPhase() != GamePhase.PLAYING) {
            cancelDamage(damage, victimRef, store, commandBuffer);
            return;
        }

        Player attacker = resolveAttacker(damage);
        if (attacker == null || !context.isPlayerPlaying(attacker)) {
            cancelDamage(damage, victimRef, store, commandBuffer);
            return;
        }

        if (attacker.equals(victimPlayer)) {
            cancelDamage(damage, victimRef, store, commandBuffer);
            return;
        }

        if (!gameManager.playerCanTag(context, attacker)) {
            cancelDamage(damage, victimRef, store, commandBuffer);
            return;
        }

        gameManager.passTNT(context, attacker, victimPlayer);
        cancelDamage(damage, victimRef, store, commandBuffer);
    }

    @Nullable
    private Player resolveAttacker(@Nonnull Damage damage) {
        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return null;
        }
        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (attackerRef == null || !attackerRef.isValid()) {
            return null;
        }
        Store<EntityStore> attackerStore = attackerRef.getStore();
        if (attackerStore == null) {
            return null;
        }
        return attackerStore.getComponent(attackerRef, Player.getComponentType());
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Collections.singleton(RootDependency.first());
    }

    private void cancelDamage(Damage damage,
                              Ref<EntityStore> ref,
                              Store<EntityStore> store,
                              CommandBuffer<EntityStore> commandBuffer) {
        damage.setCancelled(true);
        damage.removeMetaObject(Damage.KNOCKBACK_COMPONENT);
        KnockbackComponent knockback = store.getComponent(ref, KnockbackComponent.getComponentType());
        if (knockback != null) {
            knockback.setVelocity(com.hypixel.hytale.math.vector.Vector3d.ZERO);
            knockback.setDuration(0.0F);
        }
        commandBuffer.tryRemoveComponent(ref, KnockbackComponent.getComponentType());
    }
}
