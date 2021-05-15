package eu.battleland.revoken.serverside.game.mechanics.spawners;

import eu.battleland.revoken.common.Revoken;
import eu.battleland.revoken.common.abstracted.AMechanic;
import eu.battleland.revoken.common.providers.statics.ProgressStatics;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.ICodec;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.meta.CodecKey;
import eu.battleland.revoken.common.providers.storage.flatfile.store.AStore;
import eu.battleland.revoken.serverside.RevokenPlugin;
import eu.battleland.revoken.serverside.providers.data.codecs.BlockStateCodec;
import eu.battleland.revoken.serverside.providers.statics.PermissionStatics;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

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

                final var itemStack = getSpawnerItemStack(Material.SPAWNER, null, 3);
                itemStack.setAmount(spawnerCount);
                target.getInventory().addItem(itemStack);
                commandSender.sendMessage("§aSpawner type '" + entityName + "'(" + spawnerCount + "x) given to player '" + targetName + "'");
                return true;
            }

            @Override
            public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
                if(args.length == 1)
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                if(args.length == 2)
                    return Arrays.stream(EntityType.values()).map(EntityType::name).map(String::toLowerCase).collect(Collectors.toList());
                return Collections.emptyList();
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

    @EventHandler(priority = EventPriority.HIGHEST)
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

        final var bypass = event.getPlayer().hasPermission("revoken.silkspawner.bypass") || event.getPlayer().getGameMode().equals(GameMode.CREATIVE);
        final CraftCreatureSpawner spawnerState = (CraftCreatureSpawner) blockState;
        final PersistentDataContainer spawnerData = spawnerState.getPersistentDataContainer();

        int durability = spawnerData.getOrDefault(SPAWNER_DURABILITY_DATA_KEY, PersistentDataType.INTEGER, settings.defaultSpawnerDurability);
        if (durability <= 0)
            return;

        blockState.getWorld().dropItem(
                blockState.getLocation(),
                getSpawnerItemStack(
                        blockState.getType(),
                        spawnerState,
                        bypass ? durability : --durability
                ));
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

    public @NotNull ItemStack getSpawnerItemStack(final @NotNull Material material, @NotNull CraftCreatureSpawner spawnerState, final int newDurability) {
        final ItemStack itemStack = new ItemStack(material);
        final var meta = (BlockStateMeta) itemStack.getItemMeta();
        final var data = spawnerState.getPersistentDataContainer();
        final var entity = spawnerState.getSpawnedType();

        applyStateModifier(spawnerState);

        String progressBar = ProgressStatics.getProgressBar(settings.progressBarSettings, newDurability, settings.defaultSpawnerDurability);
        String entityName = entity.name().toLowerCase().replace("_", " ");

        final var nameComponent = settings.itemStackName.replaceAll("(?i)\\{entity\\}", entityName);
        final var loreComponent = settings.itemStackLore.replaceAll("(?i)\\{durability\\}", progressBar);
        meta.setDisplayName(nameComponent);
        meta.setLore(Collections.singletonList(loreComponent));
        meta.setBlockState(spawnerState);

        data.set(SPAWNER_DURABILITY_DATA_KEY, PersistentDataType.INTEGER, newDurability-1);
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

    }

}
