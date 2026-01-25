package net.blueva.arcade.modules.tnttag.state;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TagCountTracker {

    private final Map<UUID, Integer> tagsGiven = new ConcurrentHashMap<>();

    public void initializePlayers(Collection<Player> players) {
        for (Player player : players) {
            tagsGiven.put(player.getUniqueId(), 0);
        }
    }

    public void increment(Player player) {
        if (player == null) {
            return;
        }
        tagsGiven.merge(player.getUniqueId(), 1, Integer::sum);
    }

    public void removePlayers(Collection<Player> players) {
        for (Player player : players) {
            tagsGiven.remove(player.getUniqueId());
        }
    }

    public void clear() {
        tagsGiven.clear();
    }
}
