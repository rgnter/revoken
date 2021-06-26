package eu.battleland.revoken.serverside.game.mechanics.magical.alchemy;

import eu.battleland.revoken.common.Revoken;
import eu.battleland.revoken.common.abstracted.AMechanic;
import eu.battleland.revoken.common.providers.storage.data.codec.ICodec;
import eu.battleland.revoken.common.providers.storage.flatfile.store.AStore;
import eu.battleland.revoken.serverside.RevokenPlugin;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.TreeMap;

@Log4j2(topic = "Alchemy Mechanic")
public class AlchemyMechanic extends AMechanic<RevokenPlugin> implements Listener {

    @Getter
    private Optional<AStore> configuration = Optional.empty();
    @Getter
    private Settings settings = new Settings();


    private final TreeMap<Long, Item> items = new TreeMap<>();


    public AlchemyMechanic(@NotNull Revoken<RevokenPlugin> plugin) {
        super(plugin);
        setTickableAsync(true);
    }

    @Override
    public void initialize() throws Exception {
        this.settings.setup();
        Bukkit.getPluginManager().registerEvents(this, getPlugin().instance());
    }

    @Override
    public void terminate() {

    }

    @Override
    public void reload() {
        this.settings.setup();
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        final var item = event.getItemDrop();
        final var itemStack = event.getItemDrop().getItemStack();

    }

    @Override
    public void asyncTick() {

    }

    /**
     * Settings for Alchemy
     */
    class Settings implements ICodec {
        private void setup() {
            configuration.or(() -> {
                try {
                    configuration = Optional.of(getPlugin().instance().getStorageProvider()
                            .provideYaml("resources", "configs/plugin_config.yaml", true));
                    return configuration;
                } catch (Exception e) {
                    log.error("Couldn't provide config", e);
                }
                return Optional.empty();
            }).ifPresent((config) -> {
                try {
                    config.prepare();
                } catch (Exception e) {
                    log.error("Couldn't prepare config", e);
                }

                final var data = config.getData();
                try {
                    data.decode(this);
                } catch (Exception e) {
                    log.error("Couldn't decode settings", e);
                    log.info("Running on defaults");
                }
                log.info("Loaded settings");
            });
        }
    }
}
