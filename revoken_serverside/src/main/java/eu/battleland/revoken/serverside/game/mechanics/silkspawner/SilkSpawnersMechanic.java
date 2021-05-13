package eu.battleland.revoken.serverside.game.mechanics.silkspawner;

import eu.battleland.revoken.common.Revoken;
import eu.battleland.revoken.common.abstracted.AMechanic;
import eu.battleland.revoken.common.providers.statics.ProgressStatics;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.ICodec;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.meta.CodecKey;
import eu.battleland.revoken.common.providers.storage.flatfile.store.AStore;
import eu.battleland.revoken.serverside.RevokenPlugin;
import eu.battleland.revoken.serverside.statics.PermissionStatics;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftCreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Log4j2(topic = "SilkSpawners Mechanic")
public class SilkSpawnersMechanic extends AMechanic<RevokenPlugin> implements Listener {

    public static final NamespacedKey SPAWNER_DURABILITY_DATA_KEY = new NamespacedKey("battleland", "spawner_durability");
    public static final NamespacedKey SPAWNER_TYPE_DATA_KEY = new NamespacedKey("battleland", "spawner_type");


    private Optional<AStore> configuration = Optional.empty();
    public Settings settings = new Settings();


    public SilkSpawnersMechanic(@NotNull Revoken<RevokenPlugin> plugin) {
        super(plugin);
    }

    @Override
    public void initialize() throws Exception {
        PermissionStatics.permissionFromString("revoken.silkspawner.admin", PermissionDefault.OP);
        PermissionStatics.permissionFromString("revoken.silkspawner.bypass", PermissionDefault.OP);

        settings.setup();

        Bukkit.getPluginManager().registerEvents(this, getPlugin().instance());
        Bukkit.getCommandMap().register("revoken", new Command("givespawner", "", "", Arrays.asList("spawner", "silkspawner")) {
            @Override
            public boolean execute(@NotNull CommandSender commandSender, @NotNull String alias, @NotNull String[] args) {
                if (!commandSender.hasPermission("revoken.silkspawner.admin"))
                    return true;

                if (args.length < 2) {
                    commandSender.sendMessage("§cSpecify target player name and entity name");
                    return true;
                }
                String targetName = args[0];
                Player target = Bukkit.getPlayer(targetName);
                if (target == null) {
                    commandSender.sendMessage("§cInvalid player '" + targetName + "'");
                    return true;
                }
                String entityName = args[1];
                EntityType entity;
                try {
                    entity = EntityType.valueOf(entityName.toUpperCase());
                } catch (Exception x) {
                    commandSender.sendMessage("§cInvalid entity '" + entityName + "'");
                    return true;
                }

                String spawnerCountStr = args.length >= 3 ? args[2] : "1";
                int spawnerCount;
                try {
                    spawnerCount = Integer.parseInt(spawnerCountStr);
                } catch (Exception x) {
                    commandSender.sendMessage("§cInvalid number '" + spawnerCountStr + "'");
                    return true;
                }

                final var itemStack = getSpawnerItemStack(Material.SPAWNER, entity, 3);
                itemStack.setAmount(spawnerCount);
                target.getInventory().addItem(itemStack);
                commandSender.sendMessage("§aSpawner type '" + entityName + "'(" + spawnerCount + "x) given to player '" + targetName + "'");
                return true;
            }
        });
    }

    @Override
    public void terminate() {

    }

    @Override
    public void reload() {
        settings.setup();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        final var blockState = event.getBlock().getState();
        final var tool = event.getPlayer().getInventory().getItemInMainHand();
        if (!(blockState instanceof CraftCreatureSpawner))
            return;
        if (!this.settings.requiredTools.contains(tool.getType()))
            return;
        for (final var requiredEnchant : this.settings.requiredEnchantments) {
            if (!tool.containsEnchantment(requiredEnchant))
                return;
        }

        final var bypass = event.getPlayer().hasPermission("revoken.silkspawner.bypass");
        final CraftCreatureSpawner spawnerState = (CraftCreatureSpawner) blockState;
        final PersistentDataContainer spawnerData = spawnerState.getPersistentDataContainer();

        int durability = spawnerData.getOrDefault(SPAWNER_DURABILITY_DATA_KEY, PersistentDataType.INTEGER, settings.defaultSpawnerDurability);
        if (durability <= 0)
            return;

        blockState.getWorld().dropItem(
                blockState.getLocation(),
                getSpawnerItemStack(
                        blockState.getType(),
                        spawnerState.getSpawnedType(),
                        bypass ? durability : --durability
                ));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        final var itemStack = event.getPlayer().getInventory().getItem(event.getHand());
        if (itemStack == null || !itemStack.getType().equals(Material.SPAWNER))
            return;
        final var itemMeta = itemStack.getItemMeta();
        final var itemData = itemMeta.getPersistentDataContainer();

        int durability = itemData.getOrDefault(SPAWNER_DURABILITY_DATA_KEY, PersistentDataType.INTEGER, settings.defaultSpawnerDurability);
        if (!itemData.has(SPAWNER_TYPE_DATA_KEY, PersistentDataType.STRING))
            return;
        final String typeName = itemData.get(SPAWNER_TYPE_DATA_KEY, PersistentDataType.STRING);
        final EntityType type;

        try {
            type = EntityType.valueOf(typeName);
        } catch (IllegalArgumentException x) {
            log.error("Player '{}' has bad spawner with entity type '{}'", event.getPlayer().getName(), typeName);
            event.getPlayer().sendMessage("§cPoskodeny typ spawneru, kontaktuj administratorov");
            event.setCancelled(true);
            return;
        }

        final CraftCreatureSpawner spawnerState = (CraftCreatureSpawner) event.getBlockPlaced().getState();
        spawnerState.setSpawnedType(type);
        final PersistentDataContainer spawnerData = spawnerState.getPersistentDataContainer();
        spawnerData.set(SPAWNER_DURABILITY_DATA_KEY, PersistentDataType.INTEGER, durability);
        spawnerState.update(true);
    }


    public @NotNull ItemStack getSpawnerItemStack(@NotNull Material material, @NotNull EntityType entity, int newDurability) {
        final ItemStack itemStack = new ItemStack(material);
        final var meta = itemStack.getItemMeta();
        final var data = meta.getPersistentDataContainer();

        String progressBar = ProgressStatics.getProgressBar(settings.progressBarSettings, newDurability - 1, settings.defaultSpawnerDurability);
        String entityName = entity.name().toLowerCase().replace("_", " ");

        final var nameComponent = Component.text(settings.itemStackName.replaceAll("(?i)\\{entity\\}", entityName));
        final var loreComponent = Component.text(settings.itemStackLore.replaceAll("(?i)\\{durability\\}", progressBar));
        meta.displayName(nameComponent);
        meta.lore(Collections.singletonList(loreComponent));

        data.set(SPAWNER_DURABILITY_DATA_KEY, PersistentDataType.INTEGER, newDurability);
        data.set(SPAWNER_TYPE_DATA_KEY, PersistentDataType.STRING, entity.name());
        itemStack.setItemMeta(meta);

        return itemStack;
    }

    class Settings implements ICodec {
        @CodecKey("spawners.required-tools")
        public Set<Material> requiredTools = new HashSet<>() {{
            add(Material.IRON_PICKAXE);
            add(Material.DIAMOND_PICKAXE);
            add(Material.NETHERITE_PICKAXE);
        }};

        @CodecKey("spawners.required-enchantments")
        public Set<Enchantment> requiredEnchantments = new HashSet<>() {{
            add(Enchantment.SILK_TOUCH);
        }};

        @CodecKey("spawners.item.name")
        public String itemStackName = "§fSpawner §e{entity}";
        @CodecKey("spawners.item.lore")
        public String itemStackLore = "§7Durabilita §r{durability}";

        @CodecKey("spawners.durability")
        public int defaultSpawnerDurability = 3;
        @CodecKey("spawners.show-durability")
        public boolean showDurability = true;

        @CodecKey("durability-progress-bar")
        public ProgressStatics.BarSettings progressBarSettings
                = ProgressStatics.BarSettings.builder().stepMax(3).build();


        public Settings() {
        }

        protected void setup() {
            configuration.or(() -> {
                try {
                    configuration = Optional.of(getPlugin().instance().getStorageProvider()
                            .provideYaml("resources", "configs/mechanics/silkspawners/silk_spawners.yaml", true));
                } catch (Exception e) {
                    log.error("Failed to provide configuration", e);
                }
                return configuration;
            }).ifPresent((config) -> {
                try {
                    config.prepare();
                } catch (Exception e) {
                    log.error("Failed to prepare configuration", e);
                }

                try {
                    config.getData().decode(this);
                } catch (Exception e) {
                    log.error("Failed to decode settings: {}", e.getMessage());
                }
                log.info("Loaded settings");
            });

        }

        @Override
        public Class<?> type() {
            return this.getClass();
        }

        @Override
        public ICodec instance() {
            return new Settings();
        }
    }

}
