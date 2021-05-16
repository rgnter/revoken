package eu.battleland.revoken.serverside.game.controllers.security;

import eu.battleland.revoken.common.Revoken;
import eu.battleland.revoken.common.abstracted.AController;
import eu.battleland.revoken.common.providers.api.discord.DiscordWebhook;
import eu.battleland.revoken.common.providers.storage.data.codec.ICodec;
import eu.battleland.revoken.common.providers.storage.data.codec.meta.CodecKey;
import eu.battleland.revoken.common.providers.storage.flatfile.store.AStore;
import eu.battleland.revoken.serverside.RevokenPlugin;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

@Log4j2(topic = "BattleSec Controller")
public class BattleSecController extends AController<RevokenPlugin> implements Listener, ICodec {

    private final @NotNull DiscordWebhook webhook
            = new DiscordWebhook("");

    @CodecKey("disallowed.materials")
    private final Set<Material> disallowedMaterials = new HashSet<>() {{
        add(Material.BEDROCK);
        add(Material.BARRIER);
        add(Material.END_PORTAL_FRAME);
    }};

    @CodecKey("disallowed.item-names")
    private final Set<String> disallowedItemNames = new HashSet<>() {{
        add("renit");
        add("magická je tráva,");
    }};


    private final ExecutorService cachedThreadPool = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(@NotNull Runnable r) {
            return new Thread(r, "SecurityAnalysis");
        }
    });

    @Getter
    private Optional<AStore> configuration = Optional.empty();

    /**
     * Constructor requiring RevokenPlugin instance
     *
     * @param plugin Revoken Plugin
     */
    public BattleSecController(@NotNull Revoken<RevokenPlugin> plugin) {
        super(plugin);
    }

    @Override
    public void initialize() throws Exception {
        setup();
        Bukkit.getPluginManager().registerEvents( this, getPlugin().instance() );
    }

    @Override
    public void terminate() {
        this.cachedThreadPool.shutdown();
    }

    @Override
    public void reload() {
        setup();
    }

    private void setup() {
        configuration.or(() -> {
            try {
                configuration = Optional.of(getPlugin().instance().getStorageProvider()
                        .provideYaml("resources", "configs/controllers/security/battlesec.yaml", true));
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

        });
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        final Player player = event.getPlayer();
        if (player.hasPermission("revoken.staff"))
            return;

        final Item droppedItem = event.getItemDrop();
        final ItemStack item = droppedItem.getItemStack();

        runAsyncAnalysis(player.getName(), player.getLocation(), (isEvil) -> {
            if (isEvil) {
                if (!droppedItem.isDead())
                    droppedItem.remove();

                player.banPlayer("§c§lBattleSec §7- §fSi podozrelý z podvádzania. Stala sa niekde chyba? \n §fNapíš nám na §9discord.battleland.eu");
            }
        }, true, item);
    }

    @EventHandler
    public void onItemPick(PlayerAttemptPickupItemEvent event) {
        final Player player = event.getPlayer();
        if (player.hasPermission("revoken.staff"))
            return;

        final Item pickedUpItem = event.getItem();
        final ItemStack item = pickedUpItem.getItemStack();

        runAsyncAnalysis(player.getName(), player.getLocation(), (isEvil) -> {
            if (isEvil) {
                if (!pickedUpItem.isDead())
                    pickedUpItem.remove();
                player.getInventory().remove(item);
            }
        }, true, item);
    }

    @EventHandler
    public void onItemMove(InventoryMoveItemEvent event) {
        final var item = event.getItem();
        final var source = event.getSource();
        final var destination = event.getDestination();
        final var initiator = event.getInitiator();

        Location location = null;
        if (source.getLocation() != null)
            location = source.getLocation();
        if (destination.getLocation() != null)
            location = destination.getLocation();
        if (initiator.getLocation() != null)
            location = initiator.getLocation();


        runAsyncAnalysis("", location, (isEvil) -> {
            if (isEvil) {
                source.remove(item);
                destination.remove(item);
                initiator.remove(item);
            }
        }, true, item);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        final Player player = (Player) event.getWhoClicked();
        if (player.hasPermission("revoken.staff"))
            return;

        final Inventory clickedInventory = event.getClickedInventory();

        final var currentItem = event.getCurrentItem();
        final var cursorItem = event.getCursor();

        runAsyncAnalysis(player.getName(), player.getLocation(), (isEvil) -> {
            if (isEvil) {
                if (clickedInventory != null)
                    clickedInventory.clear();
                player.banPlayer("§c§lBattleSec §7- §fSi podozrelý z podvádzania. Stala sa niekde chyba? \n §fNapíš nám na §9discord.battleland.eu");
            }
        }, false, currentItem, cursorItem);
    }

    @EventHandler
    public void onCreativeClick(InventoryCreativeEvent event) {
        final Player player = (Player) event.getWhoClicked();
        if (player.hasPermission("revoken.staff"))
            return;

        final Inventory clickedInventory = event.getClickedInventory();

        final var currentItem = event.getCurrentItem();
        final var cursorItem = event.getCursor();

        runAsyncAnalysis(player.getName(), player.getLocation(), (isEvil) -> {
            if (isEvil) {
                if (clickedInventory != null)
                    clickedInventory.clear();
                player.getInventory().clear();

                player.banPlayer("§c§lBattleSec §7- §fSi podozrelý z podvádzania. Stala sa niekde chyba? \n §fNapíš nám na §9discord.battleland.eu");
            }
        }, true, currentItem, cursorItem);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        if (player.hasPermission("revoken.staff"))
            return;

        final var item = event.getItem();

        if (item != null)
            runAsyncAnalysis(player.getName(), player.getLocation(), (isEvil) -> {
                if (isEvil) {
                    player.getInventory().remove(item);
                    player.banPlayer("§c§lBattleSec §7- §fSi podozrelý z podvádzania. Stala sa niekde chyba? \n §fNapíš nám na §9discord.battleland.eu");
                }
            }, true, item);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        if (player.hasPermission("revoken.staff"))
            return;

        final Location location = event.getBlock().getLocation();
        final var item = event.getItemInHand();
        runAsyncAnalysis(player.getName(), player.getLocation(), (isEvil) -> {
            if (isEvil) {
                player.getInventory().remove(item);
                player.banPlayer("§c§lBattleSec §7- §fSi podozrelý z podvádzania. Stala sa niekde chyba? \n §fNapíš nám na §9discord.battleland.eu");
            }
        }, true, item);


        Bukkit.getScheduler().runTaskAsynchronously(getPlugin().instance(), () -> {
            if (item.getItemMeta() instanceof BlockStateMeta) {
                BlockStateMeta meta = ((BlockStateMeta) item.getItemMeta());
                BlockState blockState = meta.getBlockState();

                if (blockState instanceof ShulkerBox) {
                    ShulkerBox shulkerBox = (ShulkerBox) blockState;

                    runAsyncAnalysis(player.getName(), player.getLocation(), (isEvil) -> {
                        if (isEvil) {
                            player.getInventory().remove(item);
                            player.banPlayer("§c§lBattleSec §7- §fSi podozrelý z podvádzania. Stala sa niekde chyba? \n §fNapíš nám na §9discord.battleland.eu");
                        }
                    }, true, shulkerBox.getInventory().getContents());

                }
            }
        });

        if (this.disallowedMaterials.contains(event.getBlockPlaced().getType())) {
            if (event.getPlayer().hasPermission("revoken.staff")) {
                log.info("Player {} bypasses place check", event.getPlayer().getName());
                return;
            }

            notifyStaff("Player '{}' tried placing disallowed block: {}({},{},{})",
                    player.getName(), "bedrock", location.getBlockX(), location.getBlockY(), location.getBlockZ()
            );

            event.setCancelled(true);
            event.getPlayer().getInventory().clear();
            player.banPlayer("§c§lBattleSec §7- §fSi podozrelý z podvádzania. Stala sa niekde chyba? \n §fNapíš nám na §9discord.battleland.eu");
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        final Player player = event.getPlayer();
        if (player.hasPermission("revoken.staff"))
            return;

        final Location fromLoc = event.getFrom();
        final Location toLoc = event.getTo();

        if (false) {
            log.warn("Player {} suspected of flying", player.getName());
            player.banPlayer("§c§lBattleSec §7- §fSi podozrelý z podvádzania. Stala sa niekde chyba? \n §fNapíš nám na §9discord.battleland.eu");
        }

        log.info("Player '{}' teleported from ({} {} {}) to ({} {} {})",
                player.getName(),
                fromLoc.getBlockX(), fromLoc.getBlockY(), fromLoc.getBlockZ(),
                toLoc.getBlockX(), toLoc.getBlockY(), toLoc.getBlockZ()
        );
    }

    public void runAsyncAnalysis(@NotNull String suspect, @NotNull Location location, @NotNull Consumer<Boolean> result, boolean materialCheck, @Nullable final ItemStack... items) {
        if (items == null)
            return;
        this.cachedThreadPool.submit(() -> {
            for (final ItemStack item : items) {
                boolean isItemEvil = false;
                if (item == null)
                    continue;

                final var material = item.getType();
                if (!item.hasItemMeta())
                    continue;
                final var itemMetadata = item.getItemMeta();
                final String displayName = itemMetadata.hasDisplayName() ? ChatColor.stripColor(itemMetadata.getDisplayName()) : "";

                if (itemMetadata.hasAttributeModifiers()) {
                    var attributeModifiers = itemMetadata.getAttributeModifiers(Attribute.GENERIC_MAX_HEALTH);
                    for (AttributeModifier attrMod : attributeModifiers) {
                        if (attrMod.getName().toLowerCase().contains("ei-id")) {
                            isItemEvil = true;
                            log.warn("Item '{}({})§r' for suspect '{}§r' has evil attrmod", material.name().toLowerCase(), displayName, suspect);
                            break;
                        }
                    }
                }

                if (itemMetadata.hasEnchants()) {
                    var enchants = itemMetadata.getEnchants();
                    for (Integer value : enchants.values()) {
                        if (value > 20) {
                            isItemEvil = true;
                            log.warn("Item '{}({})§r' for suspect '{}§r' has bad enchant value", material.name().toLowerCase(), displayName, suspect);
                            break;
                        }
                    }
                }

                if (!isItemEvil && materialCheck) {
                    isItemEvil = this.disallowedMaterials.contains(material);
                }

                if (isItemEvil || this.disallowedItemNames.stream().anyMatch(name -> displayName.matches("(?i).*" + name + ".*"))) {
                    notifyStaff("Suspect '%s'(%d %d %d/%s) has disallowed item in inventory: %s(%s)",
                            suspect, location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                            location.getWorld().getName(), material.name().toLowerCase(), displayName
                    );

                    Bukkit.getScheduler().runTask(getPlugin().instance(), () -> result.accept(true));
                    return;
                } else {
                    Bukkit.getScheduler().runTask(getPlugin().instance(), () -> result.accept(false));
                }

            }
        });

    }

    private void notifyStaff(String message, Object... format) {
        String str = String.format(message, format);
        webhook.setContent(str);
        log.warn(str);
        try {
            webhook.execute();
        } catch (IOException e) {
        }
    }


}
