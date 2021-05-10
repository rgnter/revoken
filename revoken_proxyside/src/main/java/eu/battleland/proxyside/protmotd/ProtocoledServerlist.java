package eu.battleland.proxyside.protmotd;

import eu.battleland.proxyside.ProxyPlugin;
import eu.battleland.proxyside.protmotd.model.Serverlist;
import eu.battleland.revoken.common.providers.storage.flatfile.store.AStore;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import xyz.rgnt.mth.tuples.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Perprotocol serverlist
 */
public class ProtocoledServerlist implements Listener {

    private ProxyPlugin plugin;
    private AStore configFile;

    private Map<Pair<Integer, Integer>, Serverlist> serverlists = new HashMap<>();

    public ProtocoledServerlist(ProxyPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        try {
            configFile = plugin.getStorageProvider().provideYaml("resources", "motd.yaml", true);
            reloadConfig();
            plugin.getProxy().getPluginManager().registerListener(plugin, this);


        } catch (Exception x) {
            plugin.getLogger().warning("Cant load config; " + x);
            x.printStackTrace();
        }

    }

    public void terminate() {

    }

    public void reloadConfig() {
        try {
            configFile.prepare();
        } catch (Exception e) {
            plugin.getLogger().warning("Cant prepare config file; " + e);
            e.printStackTrace();
        }
        var config = configFile.getData();
        this.serverlists.clear();

        var serverlists = config.getKeys("serverlist");
        for (String serverlistName : serverlists) {
            var root = config.getSector("serverlist." + serverlistName);
            if (root == null)
                continue;

            Serverlist serverlist = new Serverlist();
            Pair<Integer, Integer> protCond = parseProtocolCondition(root.getString("protocol", "~;~"));
            serverlist.setMotdLines(root.getStringList("motd", Collections.emptyList()));
            serverlist.setSampleLines(root.getStringList("hover", Collections.emptyList()));

            this.serverlists.put(protCond, serverlist);
        }
    }

    @EventHandler
    public void onPing(ProxyPingEvent event) {
        var pingData = event.getResponse();
        int requestedProtocol = event.getConnection().getVersion();

        this.serverlists.forEach((protCond, serverlist) -> {
            if (requestedProtocol >= protCond.getFirst()
                    && requestedProtocol <= protCond.getSecond()) {
                serverlist.processServerPing(pingData);
            }
        });
    }


    private Pair<Integer, Integer> parseProtocolCondition(@NotNull String data) {
        String[] raw = data.split(";");
        if (raw == null)
            return Pair.of(0, 0);
        int least;
        int most;

        if (raw[0].equals("~"))
            least = 0;
        else
            least = Integer.parseInt(raw[0]);
        if (raw[1].equals("~"))
            most = ProxyServer.getInstance().getProtocolVersion();
        else
            most = Integer.parseInt(raw[1]);
        return Pair.of(least, most);
    }
}
