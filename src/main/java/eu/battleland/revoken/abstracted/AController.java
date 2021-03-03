package eu.battleland.revoken.abstracted;

import eu.battleland.revoken.RevokenPlugin;
import eu.battleland.revoken.providers.storage.flatfile.store.AStore;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

public abstract class AController {

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
     * Called upon reload request of resources
     */
    public abstract void reloadResources();

}
