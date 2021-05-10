package eu.battleland.revoken.common.abstracted;

import eu.battleland.revoken.common.Revoken;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public abstract class AMngr<T, X extends IComponent> implements IComponent {

    @Getter(AccessLevel.PROTECTED)
    private final Revoken<T> plugin;

    @Getter
    private final ConcurrentHashMap<Class<X>, X> registeredComponents = new ConcurrentHashMap<>();

    /**
     * Default constructor for Manager
     *
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


    public void callForComponents(@NotNull BiConsumer<Class<X>, X> call) {
        this.registeredComponents.forEach(call);
    }

    public void registerComponents(@NotNull X... components) {
        for (X component : components) {
            registerComponent(component);
        }
    }


    public void registerComponent(@NotNull X component) {
        this.registeredComponents.put((Class<X>) component.getClass(), component);
    }

    public void unregisterComponent(@NotNull X component) {
        this.registeredComponents.remove((Class<X>) component.getClass());
    }

    public X obtainComponent(@NotNull Class<X> clazz) {
        return this.registeredComponents.get(clazz);
    }

}
