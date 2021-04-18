package eu.battleland.revoken.serverside.game.mechanics.wearables.model;

import lombok.Builder;
import lombok.Getter;
import net.minecraft.server.v1_16_R3.*;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Builder
public class Wearable {

    private @Nullable org.bukkit.inventory.ItemStack bukkitItem;

    @Getter
    private final @NotNull String identifier;

    @Getter
    private final @NotNull Material baseMaterial;

    @Getter
    private final @NotNull int modelData;

    /**
     * Constructs and returns Native ItemStack
     *
     * @return Native Itemstack
     */
    public @NotNull net.minecraft.server.v1_16_R3.ItemStack getNativeItemStack() {
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
}
