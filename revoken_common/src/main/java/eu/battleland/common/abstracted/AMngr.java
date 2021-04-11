package eu.battleland.common.abstracted;

import eu.battleland.common.Revoken;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public abstract class AMngr<T> implements IComponent {

    @Getter(AccessLevel.PROTECTED)
    private final Revoken<T> plugin;

    /**
     * Default constructor for Manager
     * @param plugin Plugin instance
     */
    public AMngr(@NotNull Revoken<T> plugin) {
        this.plugin = plugin;
    }

    /**
     * Called upon initialization of Manager
     */
    public abstract void initialize() throws Exception;
    /**
     * Called upon termination of Manager
     */
    public abstract void terminate();

    /**
     * Called upon reload request
     */
    public abstract void reload();


}
