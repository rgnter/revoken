package eu.battleland.revoken.serverside.game.mechanics.common.wearables.model;

import lombok.Builder;
import lombok.Getter;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Represents wearable
 */
@Builder
public class Wearable {

    @Getter
    private final @NotNull String identifier;
    @Getter
    private final @NotNull Material baseMaterial;
    @Getter
    private final @NotNull Integer modelData;
    private final @Nullable org.bukkit.inventory.ItemStack bukkitItem;

    @Getter
    private final @Nullable Impl implementation;

    /**
     * Constructs and returns Native ItemStack
     *
     * @return Native Itemstack
     */
    public @NotNull net.minecraft.world.item.ItemStack getNativeItemStack() {
        var nativeItemStack = bukkitItem != null
                ? CraftItemStack.asNMSCopy(bukkitItem)
                : CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(baseMaterial));

        NBTTagCompound compound = nativeItemStack.getOrCreateTag();
        compound.set("CustomModelData", NBTTagInt.a(modelData));
        nativeItemStack.setTag(compound);

        return nativeItemStack;
    }

    /**
     * Constructs and returns Bukkit ItemStack
     *
     * @return Bukkit Itemstack
     */
    public @NotNull ItemStack getBukkitItemStack() {
        return CraftItemStack.asBukkitCopy(getNativeItemStack());
    }

    /**
     * Compares model data of wearables
     *
     * @param other Other Wearable
     * @return Boolean true if model data are matching
     */
    public boolean hasSameModelData(@NotNull Wearable other) {
        return this.modelData == other.getModelData();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return ((Wearable) o).identifier.equals(this.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, baseMaterial, modelData);
    }

    /**
     * braa-ins
     */
    public static class Impl {
        /**
         * Called upon equip
         */
        public void onEquip() {}

        /**
         * Called upon dequip
         */
        public void onDequip() {}

        /**
         * Called upon tick
         */
        public void onTick() {}
    }
}
