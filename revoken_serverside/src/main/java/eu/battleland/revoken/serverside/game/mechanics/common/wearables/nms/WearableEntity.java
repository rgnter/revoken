package eu.battleland.revoken.serverside.game.mechanics.common.wearables.nms;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import net.minecraft.world.level.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Log4j2(topic = "WearableEntity")
public class WearableEntity extends EntityArmorStand {

    @Getter
    private final EntityPlayer owner;

    public WearableEntity(@NotNull EntityPlayer owner, World world, double d0, double d1, double d2) {
        super(world, d0, d1, d2);
        this.owner = owner;
        this.setHealth(1);
    }

    @Override
    public void tick() {

    }

    @Override
    public @NotNull World getWorld() {
        return owner.getWorld();
    }

    public void synchronize() {
        this.teleportAndSync(owner.locX(), owner.locY(), owner.locZ());
        this.a((Entity) this.getOwner(), true);
    }

    @Override
    public float getHeadRotation() {
        return owner.getHeadRotation();
    }

    public byte getHeadRotationNetwork() {
        return (byte)((int)(this.getHeadRotation() * 256.0F / 360.0F));
    }


}
