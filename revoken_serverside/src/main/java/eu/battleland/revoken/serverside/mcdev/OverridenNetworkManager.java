package eu.battleland.revoken.serverside.mcdev;

import net.minecraft.server.v1_16_R3.EnumProtocolDirection;
import net.minecraft.server.v1_16_R3.NetworkManager;

public class OverridenNetworkManager extends NetworkManager {

    public OverridenNetworkManager(EnumProtocolDirection enumprotocoldirection) {
        super(enumprotocoldirection);
    }


}
