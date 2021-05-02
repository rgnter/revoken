package eu.battleland.common.providers.storage.flatfile;


import eu.battleland.common.Revoken;
import eu.battleland.common.providers.storage.flatfile.store.AStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class StorageProvider {

    private final @NotNull Revoken instance;

    /**
     * Default constructor
     *
     * @param instance Instance of plugin
     */
    public StorageProvider(@NotNull Revoken instance) {
        this.instance = instance;
    }

    /**
     * Constructs YAML Store, and prepares it
     *
     * @param root       root of the resource
     * @param path       Path to resource
     * @param hasDefault Load default if not available in data folder
     * @return YAML Store
     * @throws Exception When something goes wrong
     */
    public @NotNull AStore provideYaml(@Nullable String root, @NotNull String path, boolean hasDefault) throws Exception {
        return AStore.makeYaml(this.instance, root, path, hasDefault).prepare();
    }


    /**
     * Constructs JSON Store, and prepares it
     *
     * @param root       root of the resource
     * @param path       Path to resource
     * @param hasDefault Load default if not available in data folder
     * @return YAML Store
     * @throws Exception When something goes wrong
     */
    public @NotNull AStore provideJson(@Nullable String root, @NotNull String path, boolean hasDefault) throws Exception {
        return AStore.makeJson(this.instance, root, path, hasDefault).prepare();
    }


}
