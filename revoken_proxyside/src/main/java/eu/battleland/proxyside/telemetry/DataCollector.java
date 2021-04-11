package eu.battleland.proxyside.telemetry;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import eu.battleland.common.providers.api.ApiConnector;
import lombok.Getter;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;


public class DataCollector {

    /**
     * Gets various player data.
     *
     * @param player Player
     * @return Data
     */
    public @NotNull Result getPlayerData(@NotNull ProxiedPlayer player) throws IOException {
        InetSocketAddress address = (InetSocketAddress) player.getSocketAddress();
        if (address == null)
            return Result.empty();
        JsonObject object = ApiConnector.getHttpApiResponse("http://ip-api.com/json/" + address.getAddress().getHostAddress());
        String status = object.get("status").getAsString();
        if (!status.equalsIgnoreCase("success"))
            return Result.empty();

        String brand = "not implemented";

        return new Result(object.get("countryCode").getAsString(), object.get("country").getAsString(), object.get("isp").getAsString(),
                object.get("as").getAsString(),object.get("org").getAsString(), object.get("timezone").getAsString(), brand);
    }

    @Getter
    public static class Result {
        private @NotNull String countryCode = "unknown";
        private @NotNull String countryName = "unknown";
        private @NotNull String isp = "unknown";
        private @NotNull String as  = "unknown";
        private @NotNull String ispOrg = "unknown";
        private @NotNull String timezone = "unknown";

        private @NotNull String clientBrand = "unknown";

        public Result(@NotNull String countryCode, @NotNull String countryName, @NotNull String isp, @NotNull String as,  @NotNull String ispOrg, @NotNull String timezone, @NotNull String clientBrand) {
            this.countryCode = countryCode;
            this.countryName = countryName;
            this.isp = isp;
            this.as = as;
            this.ispOrg = ispOrg;
            this.timezone = timezone;
            this.clientBrand = clientBrand;
        }

        public Result() {
        }

        public static @NotNull Result empty() {
            return new Result();
        }
    }

}
