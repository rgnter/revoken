package eu.battleland.revoken.serverside.game.mechanics.gamechanger.wearables.nms;

import lombok.Getter;
import net.minecraft.server.v1_16_R3.*;
import org.jetbrains.annotations.NotNull;

public class WearableEntity extends EntityArmorStand {

    @Getter
    private final EntityPlayer owner;

    public WearableEntity(@NotNull EntityPlayer owner, World world, double d0, double d1, double d2) {
        super(world, d0, d1, d2);
        this.owner = owner;
    }

    @Override
    public boolean isTicking() {
        return false;
    }

    @Override
    public void passengerTick() {
        super.passengerTick();
    }

    @Override
    public void tick() {
    }

    @Override
    public boolean canPortal() {
        return true;
    }

    @Override
    public float getBukkitYaw() {
        return owner.getBukkitYaw();
    }

    @Override
    public boolean isInWater() {
        return owner.isInWater();
    }

    @Override
    public Vec3D getMot() {
        return owner.getMot();
    }

    /*
       prilis velky overhead entity trackeru robi lagy v synchronizacii rotacie!
     */

    @Override
    public float getHeadRotation() {
        return owner.getHeadRotation();
    }
}
