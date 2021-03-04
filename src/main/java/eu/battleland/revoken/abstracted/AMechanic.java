package eu.battleland.revoken.abstracted;

import eu.battleland.revoken.RevokenPlugin;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

/**
 * Abstraction of Game Mechanic
 * Mechanics are game changing components
 */
public abstract class AMechanic implements IComponent {

    @Getter
    private final @NotNull RevokenPlugin instance;

    @Getter @Setter(AccessLevel.PROTECTED)
    private boolean tickable = false;
    @Getter @Setter(AccessLevel.PROTECTED)
    private boolean tickableAsync = false;

    /**
     * Constructor requiring RevokenPlugin instance
     * @param plugin Revoken Plugin
     */
    public AMechanic(@NotNull RevokenPlugin plugin) {
        this.instance = plugin;
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
    public abstract void tick();
}
