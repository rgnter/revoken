package eu.battleland.revoken.serverside.game.mechanics.magical.alchemy.model;

import eu.battleland.revoken.serverside.game.mechanics.magical.alchemy.nms.BrewingItem;
import lombok.Builder;
import lombok.Getter;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Represents brew recipe
 */
@Builder
public class BrewRecipe {
    @Getter
    private final BrewRecipe.Result brewResult;
    @Getter
    private final Map<Material, BrewingItem> requiredMaterials;

    /**
     * Represents brew result
     */
    public static abstract class Result {
        public abstract void handleResult(@NotNull BrewRecipe brew);
    }
}
