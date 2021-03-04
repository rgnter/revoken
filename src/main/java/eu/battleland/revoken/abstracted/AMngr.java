package eu.battleland.revoken.abstracted;

import eu.battleland.revoken.RevokenPlugin;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public abstract class AMngr implements IComponent {

    @Getter(AccessLevel.PROTECTED)
    private final RevokenPlugin plugin;

    /**
     * Default constructor for Manager
     * @param plugin Plugin instance
     */
    public AMngr(@NotNull RevokenPlugin plugin) {
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
