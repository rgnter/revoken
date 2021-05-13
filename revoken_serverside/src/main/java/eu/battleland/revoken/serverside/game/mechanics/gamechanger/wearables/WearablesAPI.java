package eu.battleland.revoken.serverside.game.mechanics.gamechanger.wearables;
import eu.battleland.revoken.serverside.game.mechanics.gamechanger.wearables.model.Wearable;
import eu.battleland.revoken.serverside.game.mechanics.gamechanger.wearables.nms.WearableEntity;
import eu.battleland.revoken.serverside.statics.PktStatics;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.rgnt.mth.tuples.Pair;
import xyz.rgnt.mth.tuples.Triple;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2(topic = "Wearables API")
public class WearablesAPI {

    private static WearablesAPI instance;
    @Getter
    private final WearablesMechanic wearablesMechanic;

    public WearablesAPI(@NotNull WearablesMechanic wearablesMechanic) {
        this.wearablesMechanic = wearablesMechanic;
        instance = this;
    }

    /**
     * Adds wearable to player.
     *
     * @param wearable Wearable Object
     * @param player   Player   Object
     * @return Returns false if
     */
    public boolean addWearable(@NotNull Player player, @NotNull Wearable wearable) {
        final var wearableEntity = this.wearablesMechanic.createWearableEntity(player, wearable);
        final var successful = new AtomicBoolean(true);

        this.wearablesMechanic.wardrobe.compute(player.getUniqueId(), (uuid, originData) -> {
            final var playerData =
                    (originData != null ? originData : new ConcurrentHashMap<String, Pair<WearableEntity, Wearable>>()); // player wearab

            var wearableData = playerData.compute(wearable.getIdentifier(), (wearableId, oldWearableData) -> {
                if (oldWearableData != null) {
                    log.warn("Overriding already existing wearable '{}' on player '{}'!", wearableId, player.getName());
                    if(!this.wearablesMechanic.deleteWearableFromWorld(oldWearableData))
                        log.warn("Failed to delete old wearable data from world, wearable id '{}' on player '{}'", wearableId, player.getName());
                }

                return Pair.of(wearableEntity, wearable);
            });

            if(!this.wearablesMechanic.createWearableInWorld(wearableData))
                successful.set(false);

            return playerData;
        });

        this.wearablesMechanic.saveWearablesToStorage(player);
        return successful.get();
    }

    /**
     * Adds all wearables to player
     *
     * @param player    Player
     * @param wearables Wearable variadic args
     */
    public void addAllWearables(@NotNull Player player, @NotNull Wearable... wearables) {
        for (final Wearable wearable : wearables)
            addWearable(player, wearable);
    }

    /**
     * Removes wearable from player.
     *
     * @param player Player
     * @param wearable Wearable
     * @return Returns false if player does not have specified wearable equipped. Otherwise true.
     */
    public boolean remWearable(@NotNull Player player, @NotNull Wearable wearable) {
        AtomicBoolean successful = new AtomicBoolean(true);
        this.wearablesMechanic.wardrobe.compute(player.getUniqueId(), (playerUuid, wearables) -> {
            if(wearables == null) {
                successful.set(false);
                return null;
            }

            final var wearableData = wearables.remove(wearable.getIdentifier());
            if(wearableData != null) {
                this.wearablesMechanic.deleteWearableFromWorld(wearableData); // delete removed wearable from world
            } else
                successful.set(false);

            return wearables;
        });

        this.wearablesMechanic.saveWearablesToStorage(player);
        return successful.get();
    }

    /**
     * Removes all wearables from player.
     *
     * @param player Player
     */
    public void remAllWearables(@NotNull Player player) {
        final var playerData = this.wearablesMechanic.wardrobe.remove(player.getUniqueId());
        if(playerData == null)
            return;

        playerData.forEach((identifier, wearableData) -> {
            this.wearablesMechanic.deleteWearableFromWorld(wearableData); // delete each wearable from world
        });

        this.wearablesMechanic.wipeWearablesInStorage(player);
    }

    /**
     *
     * @param player Player
     * @return boolean true if player has wearables, false otherwise
     */
    public boolean hasWearable(@NotNull Player player) {
        return this.wearablesMechanic.wardrobe.containsKey(player.getUniqueId());
    }

}
