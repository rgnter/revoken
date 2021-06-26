package eu.battleland.revoken.serverside.game.controllers.security;

import com.mojang.authlib.GameProfile;
import eu.battleland.revoken.common.Revoken;
import eu.battleland.revoken.common.abstracted.AController;
import eu.battleland.revoken.serverside.RevokenPlugin;
import eu.battleland.revoken.serverside.providers.statics.PermissionStatics;
import eu.battleland.revoken.serverside.providers.statics.PktStatics;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.PlayerInteractManager;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import org.bukkit.craftbukkit.v1_17_R1.CraftServer;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Log4j2(topic = "Admin Controller")
public class AdminController extends AController<RevokenPlugin> implements Listener {

    @Getter
    public static AdminController instance;

    private final Map<UUID, Inventory> liveAdminViews = new ConcurrentHashMap<>();
    @Getter
    private final Set<UUID> frozenPlayers = new HashSet<>();

    public AdminController(@NotNull Revoken<RevokenPlugin> plugin) {
        super(plugin);
        instance = this;
    }

    @Override
    public void initialize() throws Exception {
        PermissionStatics.newPermission("revoken.staff", PermissionDefault.OP);

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
        Bukkit.getCommandMap().register("revoken", new Command("freeze") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
                if (!sender.hasPermission("revoken.admin"))
                    return true;

                if(args.length < 1) {
                    sender.sendMessage("§cSpecify player name.");
                    return true;
                }
                String name = args[0];
                final Player target = Bukkit.getPlayer(name);
                if(target == null) {
                    sender.sendMessage("§cInvalid player");
                    return true;
                }

                if(frozenPlayers.contains(target.getUniqueId())) {
                    frozenPlayers.remove(target.getUniqueId());
                    sender.sendMessage("§aUnfrozen " + name);
                } else {
                    frozenPlayers.add(target.getUniqueId());
                    sender.sendMessage("§aForzen " + name);
                }

                return true;
            }
        });

        Bukkit.getPluginManager().registerEvents(this, getPlugin().instance());
    }

    @Override
    public void terminate() {

    }

    @Override
    public void reload() {

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
        Inventory adminView = null;

        final AdminViewHolder inventoryHolder;
        if (targetPlayer == null) {
            targetPlayer = Bukkit.getOfflinePlayer(target);

            sender.sendMessage("§aPreparing offline inventory");
            EntityPlayer targetNmsPlayer = new EntityPlayer(
                    MinecraftServer.getServer(),
                    worldServer,
                    new GameProfile(targetPlayer.getUniqueId(), "")
            );
            CraftPlayer targetBukkitPlayer = new CraftPlayer(((CraftServer) Bukkit.getServer()), targetNmsPlayer);
            targetBukkitPlayer.loadData();

            originalInventory = enderchest ? targetBukkitPlayer.getEnderChest() : targetBukkitPlayer.getInventory();
            inventoryHolder = new AdminViewHolder() {
                @Override
                public @NotNull Inventory getInventory() {
                    return originalInventory;
                }

                @Override
                public void updateOriginal(@NotNull Inventory adminView) {
                    targetBukkitPlayer.saveData();
                }
            };
            isOnline = false;
        } else {
            sender.sendMessage("§aPreparing online inventory");

            originalInventory = enderchest ? ((Player) targetPlayer).getEnderChest() : ((Player) targetPlayer).getInventory();

            inventoryHolder = new AdminViewHolder() {
                @Override
                public @NotNull Inventory getInventory() {
                    return originalInventory;
                }

                @Override
                public void updateAdminView(@NotNull Inventory adminView) {
                    AdminController.this.updateAdminView(originalInventory, adminView, !enderchest);
                }

                @Override
                public void updateOriginal(@NotNull Inventory adminView) {
                    final int playerInvSize = getInventory().getSize();
                    for (int i = 0; i < playerInvSize; i++) {
                        getInventory().setItem(i, adminView.getItem(i));
                    }
                }
            };
            isOnline = true;
        }

        if (!textOnly) {
            adminView = Bukkit.createInventory(
                    inventoryHolder,
                    enderchest ? originalInventory.getSize() : 54,
                    (enderchest ? "Ender Chest" : "Inventory") + " of " + (isOnline ? "§a" : "§c") + target
            );
            updateAdminView(originalInventory, adminView, !enderchest);
            this.liveAdminViews.put(targetPlayer.getUniqueId(), adminView);
        }

        if (textOnly) {
            sender.sendMessage("§aListing inventory of player " + targetPlayer.getName());
            originalInventory.forEach(item -> {
                if (item == null)
                    return;
                final var itemMeta = item.getItemMeta();
                sender.sendMessage(String.format("    %s§r %s(x%d) %s",
                        itemMeta.getDisplayName(),
                        item.getType().name(),
                        item.getAmount(),
                        (itemMeta.hasLore() ? itemMeta.getLore().stream()
                                .map(ChatColor::stripColor)
                                .collect(Collectors.joining(" ; ", "Lore: [", "] ")) : ""
                        )
                        ));
            });
        } else {
            ((Player) sender).openInventory(adminView);
            this.liveAdminViews.put(targetPlayer.getUniqueId(), adminView);
            sender.sendMessage((isOnline ? "§7Opened online inventory" : "§7Opened offline inventory") + " of player '" + target + "'");
        }
    }

    private void updateAdminView(@NotNull final Inventory originalInventory, @NotNull Inventory adminInventory, boolean addHints) {
        adminInventory.clear();
        final int playerInvSize = originalInventory.getSize();
        for (int i = 0; i < playerInvSize; i++) {
            adminInventory.setItem(i, originalInventory.getItem(i));
        }
        if (addHints)
            addHints(adminInventory);
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

    public boolean hasHintMetadata(ItemStack clicked) {
        if (clicked.getItemMeta().hasCustomModelData())
            return clicked.getItemMeta().getCustomModelData() == 1000;
        return false;
    }


    @EventHandler
    public void handleAdminInventoryHintClickEvent(InventoryClickEvent event) {
        if (!event.getWhoClicked().hasPermission("revoken.admin"))
            return;

        if (event.getClickedInventory() == null || !(event.getClickedInventory().getHolder() instanceof AdminViewHolder))
            return;

        if (event.getCurrentItem() != null)
            if (hasHintMetadata(event.getCurrentItem()))
                event.setCancelled(true);
    }

    @EventHandler
    public <T extends InventoryEvent>void handleOriginalInventoryUpdate(T event) {
        final var inventory = event.getInventory();
        if( inventory == null || inventory.getHolder() == null || !(inventory.getHolder() instanceof CraftPlayer))
            return;
        final var player = (Player) inventory.getHolder();
        if (!this.liveAdminViews.containsKey(player.getUniqueId()))
            return;


        final var adminInventory = this.liveAdminViews.get(player.getUniqueId());
        ((AdminViewHolder) adminInventory.getHolder()).updateAdminView(event.getInventory());
    }

    @EventHandler
    public void handleAdminInventoryUpdate(InventoryCloseEvent event) {
        final var inventory = event.getInventory();
        if(inventory == null)
            return;
        final var player = event.getPlayer();
        if (!player.hasPermission("revoken.admin"))
            return;

        var holder = inventory.getHolder();
        if (!(holder instanceof AdminViewHolder))
            return;
        AdminViewHolder identifiedHolder = (AdminViewHolder) holder;
        identifiedHolder.updateOriginal(event.getInventory());

        player.sendMessage("§aSaved inventory");
    }


    private static interface AdminViewHolder extends InventoryHolder {
        default void updateAdminView(@NotNull Inventory adminView) {
        }

        void updateOriginal(@NotNull Inventory adminView);
    }

}
