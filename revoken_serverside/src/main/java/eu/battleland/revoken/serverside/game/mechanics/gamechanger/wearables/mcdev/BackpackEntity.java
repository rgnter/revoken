package eu.battleland.revoken.serverside.game.mechanics.gamechanger.wearables.mcdev;

import eu.battleland.revoken.serverside.statics.PktStatics;
import io.netty.buffer.Unpooled;
import net.minecraft.server.v1_16_R3.*;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class BackpackEntity extends EntityArmorStand {

    private EntityPlayer owner;


    public BackpackEntity(EntityPlayer owner, World world, double d0, double d1, double d2) {
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
