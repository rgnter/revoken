package eu.battleland.revoken.serverside.game.mechanics.gamechanger.wearables;

import eu.battleland.revoken.serverside.game.mechanics.gamechanger.wearables.model.Wearable;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2(topic = "Static Wearable Repo")
public class WearablesRepository {

    private static final Map<String, Wearable> wearables = new ConcurrentHashMap<>();

    /**
     * Registers wearable
     * @param wearable Wearable instance
     */
    public static void registerWearable(@NotNull Wearable wearable) {
        wearables.compute(wearable.getIdentifier(), (identifier, oldData) -> {
            if(oldData != null)
                log.warn("Overriding already existing wearable identifier '{}'({}) with a new wearable '{}'",
                        identifier,
                        oldData.getClass().getSimpleName(),
                        oldData.getClass().getSimpleName()
                );

            return wearable;
        });
    }

    /**
     * Unregisters wearable
     * @param wearable Wearable identifier
     */
    public static void unregisterWearable(@NotNull String wearable) {
        wearables.remove(wearable);
    }

    /**
     * Unregisters wearable
     * @param wearable Wearable instance
     */
    public static void unregisterWearable(@NotNull Wearable wearable) {
        unregisterWearable(wearable.getIdentifier());
    }

    /**
     *
     * @param wearableId Wearable identifier
     * @return Wearable
     */
    public static @Nullable Wearable getWearable(@NotNull String wearableId) {
        return wearables.get(wearableId);
    }


    /**
     * @return Immutable map of all wearables
     */
    public static @NotNull Map<String, Wearable> getWearables() {
        return Collections.unmodifiableMap(wearables);
    }
}
