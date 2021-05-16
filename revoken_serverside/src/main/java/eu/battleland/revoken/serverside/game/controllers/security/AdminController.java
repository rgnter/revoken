package eu.battleland.revoken.serverside.game.controllers.security;

import com.mojang.authlib.GameProfile;
import eu.battleland.revoken.common.Revoken;
import eu.battleland.revoken.common.abstracted.AController;
import eu.battleland.revoken.common.providers.api.discord.DiscordWebhook;
import eu.battleland.revoken.serverside.RevokenPlugin;
import eu.battleland.revoken.serverside.providers.statics.PermissionStatics;
import eu.battleland.revoken.serverside.providers.statics.PktStatics;
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

    private final Map<UUID, InventorySeeHolder> modifiedInventories = new ConcurrentHashMap<>();

    public AdminController(@NotNull Revoken<RevokenPlugin> plugin) {
        super(plugin);
    }

    @Override
    public void initialize() throws Exception {
        Bukkit.getPluginManager().registerEvents(this, getPlugin().instance());
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

    public boolean hasHintMetadata(ItemStack clicked) {
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
            if (hasHintMetadata(event.getCurrentItem()))
                event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
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


    private static interface InventorySeeHolder extends InventoryHolder {
        void modifyOriginalInventory();
    }

}
