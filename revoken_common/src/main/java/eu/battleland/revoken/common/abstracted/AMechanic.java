package eu.battleland.revoken.common.abstracted;

import eu.battleland.revoken.common.Revoken;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

/**
 * Abstraction of Game Mechanic
 * Mechanics are game changing components
 */
public abstract class AMechanic<T> implements IComponent {

    @Getter
    private final @NotNull Revoken<T> plugin;

    @Getter
    @Setter(AccessLevel.PROTECTED)
    private boolean tickable = false;
    @Getter
    @Setter(AccessLevel.PROTECTED)
    private boolean tickableAsync = false;

    /**
     * Constructor accepting {@link Revoken} instance
     *
     * @param plugin Revoken instance
     */
    public AMechanic(@NotNull Revoken<T> plugin) {
        this.plugin = plugin;
    }

    /**
     * Called upon initialization of Mechanic
     */
    public abstract void initialize() throws Exception;

    /**
     * Called upon termination of Mechanic
     */
    public abstract void terminate();

    /**
     * Called upon reload request
     */
    public abstract void reload();

    /**
     * Called on tick
     */
    public void tick() {
    }

    /**
     * Called on tick (Running in another thread)
     */
    public void asyncTick() {
    }
}
