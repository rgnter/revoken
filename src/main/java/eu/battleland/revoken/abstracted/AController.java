package eu.battleland.revoken.abstracted;

import eu.battleland.revoken.RevokenPlugin;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Abstraction of Game Controller
 */
public abstract class AController implements IComponent {

    @Getter
    private final @NotNull RevokenPlugin instance;

    /**
     * Constructor requiring RevokenPlugin instance
     * @param plugin Revoken Plugin
     */
    public AController(@NotNull RevokenPlugin plugin) {
        this.instance = plugin;
    }

    /**
     * Called upon initialization of Controller
     */
    public abstract void initialize() throws Exception;
    /**
     * Called upon termination of Controller
     */
    public abstract void terminate();

    /**
     * Called upon reload request
     */
    public abstract void reload();

}
