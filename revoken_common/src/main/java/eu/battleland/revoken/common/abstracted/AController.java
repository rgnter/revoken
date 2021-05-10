package eu.battleland.revoken.common.abstracted;


import eu.battleland.revoken.common.Revoken;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Abstraction of Game Controller
 */
public abstract class AController<T> implements IComponent {

    @Getter
    private final @NotNull Revoken<T> plugin;

    /**
     * Constructor requiring RevokenPlugin instance
     *
     * @param plugin Revoken Plugin
     */
    public AController(@NotNull Revoken<T> plugin) {
        this.plugin = plugin;
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
