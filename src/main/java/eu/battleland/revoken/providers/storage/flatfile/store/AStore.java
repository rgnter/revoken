package eu.battleland.revoken.providers.storage.flatfile.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.battleland.revoken.providers.storage.flatfile.data.FriendlyData;
import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.libs.org.apache.commons.io.FilenameUtils;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Store owns a file and a data accessor
 */
public abstract class AStore {

    @Getter
    protected @NotNull JavaPlugin instance;

    @Getter
    protected File file;

    @Getter
    private @NotNull
    final String root;
    @Getter
    private @NotNull
    final String path;

    @Getter
    protected boolean hasDefault;

    /**
     * Default constructor
     *
     * @param instance   Instance to plugin
     * @param path       Relative path to file
     * @param hasDefault Default
     */
    public AStore(@NotNull JavaPlugin instance, @Nullable String root, @NotNull String path, boolean hasDefault) {
        this.instance = instance;
        this.root = root != null ? root : "";
        this.path = FilenameUtils.separatorsToUnix(path);
        this.hasDefault = hasDefault;
    }

    /**
     * Creates file and prepares data accessor
     *
     * @throws Exception Exception
     */
    public AStore prepare() throws Exception {
        if (!file.exists()) {
            if (hasDefault)
                provideDefault();
        } else
            load();

        return this;
    }

    /**
     * Loads from disk
     */
    public abstract void load() throws Exception;

    /**
     * Creates file
     *
     * @throws Exception Exception
     */
    protected void create() throws Exception {
        if (!file.exists()) {
            if (!file.getParentFile().exists())
                file.getParentFile().mkdirs();
            file.createNewFile();
        }
    }

    /**
     * Deletes file
     *
     * @throws Exception Exception
     */
    protected void delete() throws Exception {
        if (file.exists())
            file.delete();
    }


    /**
     * Saves on disk
     */
    public abstract void save() throws Exception;

    /**
     * Loads default
     */
    public void provideDefault() throws Exception {
        if (hasDefault) {
            if (!file.exists())
                create();
            // write default file
            try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(Objects.requireNonNull(instance.getResource(getResourcePath())),
                                 StandardCharsets.UTF_8));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(this.file, StandardCharsets.UTF_8))) {

                String line;
                do {
                    line = reader.readLine();
                    if (line != null)
                        writer.write(line + "\n");
                } while (line != null);
            } catch (Exception x) {
                throw new Exception("Failed to load '" + getResourcePath() + "': " + x.getMessage(), x);
            }
            load();
        }
    }

    public @NotNull String getResourcePath() {
        return root + "/" + path;
    }

    /**
     * @return Friendly Data
     */
    public abstract @NotNull FriendlyData getData();


    public static @NotNull AStore makeYaml(@NotNull JavaPlugin plugin, @Nullable String root, @NotNull String path, boolean hasDefault) {
        return new YamlImpl(plugin, root, path, hasDefault);
    }

    public static @NotNull AStore makeJson(@NotNull JavaPlugin plugin, @Nullable String root, @NotNull String path, boolean hasDefault) {
        return new JsonImpl(plugin, root, path, hasDefault);
    }

    /**
     * JSON Implementation of store
     */
    private static class JsonImpl extends AStore {

        private FriendlyData data;
        private JsonObject jsonData;

        public JsonImpl(@NotNull JavaPlugin instance, @Nullable String root, @NotNull String path, boolean hasDefault) {
            super(instance, root, path, hasDefault);

            this.file = new File(instance.getDataFolder(), path);
        }

        @Override
        public void load() throws Exception {
            try (Reader reader = new FileReader(this.file)) {
                this.jsonData = new JsonParser().parse(reader).getAsJsonObject();
                this.data = FriendlyData.fromJson(this.jsonData);
            } catch (Exception x) {
                throw new Exception("Failed to load '" + getResourcePath() + "': " + x.getMessage(), x);
            }
        }

        @Override
        public void save() throws Exception {
            super.create();

            try (Writer writer = new FileWriter(this.file)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();

                String serialized = gson.toJson(this.jsonData);
                writer.write(serialized);

            } catch (Exception x) {
                throw new Exception("Failed to load '" + getResourcePath() + "': " + x.getMessage(), x);
            }
        }

        @Override
        public void provideDefault() throws Exception {
            if (hasDefault) {
                try (Reader reader = new InputStreamReader(Objects.requireNonNull(instance.getResource(getResourcePath())))) {
                    this.jsonData = new JsonParser().parse(reader).getAsJsonObject();
                    this.data = FriendlyData.fromJson(this.jsonData);
                    save();
                } catch (Exception x) {
                    throw new Exception("Failed to load '" + getResourcePath() + "': " + x.getMessage(), x);
                }
            }
        }

        @Override
        public @NotNull FriendlyData getData() {
            return data;
        }
    }

    /**
     * YAML Implementation of store
     */
    private static class YamlImpl extends AStore {

        private FriendlyData data;
        private YamlConfiguration yamlData;

        public YamlImpl(@NotNull JavaPlugin instance, @Nullable String root, @NotNull String path, boolean hasDefault) {
            super(instance, root, path, hasDefault);

            this.file = new File(instance.getDataFolder(), path);
        }

        @Override
        public void load() throws Exception {
            try (Reader reader = new FileReader(this.file, StandardCharsets.UTF_8)) {
                this.yamlData = new YamlConfiguration();
                this.yamlData.load(reader);

                this.data = FriendlyData.fromYaml(this.yamlData);
            } catch (Exception x) {
                throw new Exception("Failed to load '" + getResourcePath() + "': " + x.getMessage(), x);
            }
        }

        @Override
        public void save() throws Exception {
            super.create();
            yamlData.save(file);
        }

        @Override
        public @NotNull FriendlyData getData() {
            return data;
        }
    }
}
