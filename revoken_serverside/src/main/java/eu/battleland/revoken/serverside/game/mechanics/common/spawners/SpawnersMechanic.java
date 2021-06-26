package eu.battleland.revoken.serverside.game.mechanics.common.spawners;

import eu.battleland.revoken.common.Revoken;
import eu.battleland.revoken.common.abstracted.AMechanic;
import eu.battleland.revoken.common.providers.statics.ProgressStatics;
import eu.battleland.revoken.common.providers.storage.data.codec.ICodec;
import eu.battleland.revoken.common.providers.storage.data.codec.meta.CodecKey;
import eu.battleland.revoken.common.providers.storage.flatfile.store.AStore;
import eu.battleland.revoken.serverside.RevokenPlugin;
import eu.battleland.revoken.serverside.providers.data.codecs.BlockStateCodec;
import eu.battleland.revoken.serverside.providers.statics.PermissionStatics;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftCreatureSpawner;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Log4j2(topic = "Spawners Mechanic")
public class SpawnersMechanic extends AMechanic<RevokenPlugin> implements Listener {

    public static final NamespacedKey SPAWNER_DURABILITY_DATA_KEY = new NamespacedKey("battleland", "spawner_durability");
    public static final NamespacedKey SPAWNER_TYPE_DATA_KEY = new NamespacedKey("battleland", "spawner_type");


    private Optional<AStore> configuration = Optional.empty();
    public Settings settings = new Settings();


    public SpawnersMechanic(@NotNull Revoken<RevokenPlugin> plugin) {
        super(plugin);
    }

    @Override
    public void initialize() throws Exception {
        PermissionStatics.newPermission("revoken.silkspawner.admin", PermissionDefault.OP);
        PermissionStatics.newPermission("revoken.silkspawner.bypass", PermissionDefault.OP);

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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if(event.isCancelled())
            return;

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

        final var location = event.getBlock().getLocation();
        final var bypass = event.getPlayer().hasPermission("revoken.silkspawner.bypass");
        final CraftCreatureSpawner spawnerState = (CraftCreatureSpawner) blockState;
        final PersistentDataContainer spawnerData = spawnerState.getPersistentDataContainer();


        final EntityType type = spawnerState.getSpawnedType();
        final int durability = spawnerData.getOrDefault(SPAWNER_DURABILITY_DATA_KEY, PersistentDataType.INTEGER, settings.defaultSpawnerDurability);
        Bukkit.getScheduler().runTaskAsynchronously(getPlugin().instance(), () -> {
            log.info("Player '{}' broke spawner of type '{}' with durability of {} at {} {} {}[{}]",
                    event.getPlayer().getName(),
                    type.name(), durability - 1,
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ(),
                    location.getWorld().getName());
        });
        if (durability <= 0)
            return;

        blockState.getWorld().dropItem(
                blockState.getLocation(),
                getSpawnerItemStack(
                        blockState.getType(),
                        type,
                        bypass ? durability : durability - 1
                ));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if(event.isCancelled())
            return;

        final var itemStack = event.getPlayer().getInventory().getItem(event.getHand());
        if (itemStack == null || !itemStack.getType().equals(Material.SPAWNER))
            return;
        final var itemMeta = itemStack.getItemMeta();
        final var itemData = itemMeta.getPersistentDataContainer();
        final var location = event.getBlock().getLocation();

        // get spawner type
        EntityType type = null;
        final String typeName = itemData.get(SPAWNER_TYPE_DATA_KEY, PersistentDataType.STRING);

        try {
            // battleland type
            type = EntityType.valueOf(typeName);
            log.info("battleland spawner");
        } catch (Exception ignored) {
            final var nmsItem = CraftItemStack.asNMSCopy(itemStack);
            // mineablespawners type
            final var nbtTag = nmsItem.getOrCreateTag();
            if(nbtTag.hasKey("ms_mob"))
                type = EntityType.valueOf(nbtTag.getString("ms_mob"));
            else {
                // vanilla type
                if (itemMeta instanceof BlockStateMeta)
                    type = ((CraftCreatureSpawner) ((BlockStateMeta) itemMeta).getBlockState()).getSpawnedType();
            }
        }


        if (type == null) {
            log.info("Player '{}' tried placing spawner without entity information at {} {} {}[{}]",
                    event.getPlayer().getName(),
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ(),
                    location.getWorld().getName());
            event.getPlayer().sendMessage("§cPoskodeny spawner!");
            event.setCancelled(true);
            return;
        }


        int durability = itemData.getOrDefault(
                SPAWNER_DURABILITY_DATA_KEY,
                PersistentDataType.INTEGER, settings.defaultSpawnerDurability);

        // modify placed block state
        final CraftCreatureSpawner spawnerState = (CraftCreatureSpawner) event.getBlockPlaced().getState();
        final PersistentDataContainer spawnerData = spawnerState.getPersistentDataContainer();
        spawnerState.setSpawnedType(type);
        if (settings.applyStateModifier)
            applyStateModifier(spawnerState);
        spawnerData.set(SPAWNER_DURABILITY_DATA_KEY, PersistentDataType.INTEGER, durability);
        spawnerState.update(true, false);

        log.info("Player '{}' placed spawner of type '{}' with durability of {} at {} {} {}[{}]",
                event.getPlayer().getName(),
                type.name(), durability,
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                location.getWorld().getName());
    }


    public @NotNull ItemStack getSpawnerItemStack(@NotNull Material material, @NotNull EntityType entity,
                                                  int newDurability) {
        final ItemStack itemStack = new ItemStack(material);
        final var meta = itemStack.getItemMeta();
        final var data = meta.getPersistentDataContainer();

        String progressBar = ProgressStatics.getProgressBar(settings.progressBarSettings, newDurability - 1, settings.defaultSpawnerDurability);
        String entityName = entity.name().toLowerCase().replace("_", " ");

        final var nameComponent = settings.itemStackName.replaceAll("(?i)\\{entity\\}", entityName);
        final var loreComponent = settings.itemStackLore.replaceAll("(?i)\\{durability\\}", progressBar);
        meta.setDisplayName(nameComponent);
        meta.setLore(Collections.singletonList(loreComponent));

        data.set(SPAWNER_DURABILITY_DATA_KEY, PersistentDataType.INTEGER, newDurability);
        data.set(SPAWNER_TYPE_DATA_KEY, PersistentDataType.STRING, entity.name());
        itemStack.setItemMeta(meta);

        return itemStack;
    }

    private void applyStateModifier(@NotNull CraftCreatureSpawner spawner) {
        spawner.setDelay(this.settings.stateModifier.delay);
        spawner.setMaxNearbyEntities(this.settings.stateModifier.maxNearbyEntities);
        spawner.setMaxSpawnDelay(this.settings.stateModifier.maxSpawnDelay);
        spawner.setMinSpawnDelay(this.settings.stateModifier.minSpawnDelay);
        spawner.setRequiredPlayerRange(this.settings.stateModifier.requiredPlayerRange);
        spawner.setSpawnCount(this.settings.stateModifier.spawnCount);
        spawner.setSpawnRange(this.settings.stateModifier.spawnRange);
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

        @CodecKey("spawner.state-modifier.apply")
        public boolean applyStateModifier = false;
        @CodecKey("spawners.state-modifier.data")
        public BlockStateCodec stateModifier = new BlockStateCodec();

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
                            .provideYaml("resources", "configs/mechanics/spawners/spawners.yaml", true));
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

    }

}
