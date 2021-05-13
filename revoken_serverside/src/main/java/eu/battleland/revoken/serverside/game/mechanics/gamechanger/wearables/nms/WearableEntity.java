package eu.battleland.revoken.serverside.game.mechanics.gamechanger.wearables.nms;

import lombok.Getter;
import net.minecraft.server.v1_16_R3.*;
import org.jetbrains.annotations.NotNull;

public class WearableEntity extends EntityArmorStand {

    @Getter
    private EntityPlayer owner;

    public WearableEntity(@NotNull EntityPlayer owner, World world, double d0, double d1, double d2) {
        super(world, d0, d1, d2);
        this.owner = owner;
    }


    @Override
    public void tick() {
        this.teleportAndSync(this.owner.locX(), this.owner.locY()+this.owner.getHeadHeight(), this.owner.locZ());
        this.setHeadRotation(this.owner.getHeadRotation());

        super.tick();
    }

    @Override
    public boolean canPortal() {
        return true;
    }
}
