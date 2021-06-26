package eu.battleland.revoken.serverside.mcdev;


import net.minecraft.network.NetworkManager;
import net.minecraft.network.protocol.EnumProtocolDirection;

public class OverridenNetworkManager extends NetworkManager {

    public OverridenNetworkManager(EnumProtocolDirection enumprotocoldirection) {
        super(enumprotocoldirection);
    }


}
