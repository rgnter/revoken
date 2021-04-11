package eu.battleland.revoken.providers.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.log4j.Log4j2;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

@Log4j2(topic = "Revoken - Api Connector")
public class ApiConnector {

    public static @Nullable JsonObject getHttpApiResponse(@NotNull String rawUrl) {
        try {
            URL url = new URL(rawUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            if(connection.getResponseCode() != 200)
                return null;

            StringBuilder response = new StringBuilder();
            int b;
            do {
                b = connection.getInputStream().read();
                if (b != -1) {
                    response.append((char) b);
                }
            } while (b != -1);

            connection.disconnect();
            return new Gson().fromJson(response.toString(), JsonObject.class);
        } catch (MalformedURLException e) {
            log.error("Malformed url: " + rawUrl);
            return null;
        } catch (IOException e) {
            log.error("IOP Exception: " + e, e);
            return null;
        }
    }

    public static @NotNull String getSkinHeadUrl(@NotNull Player player) {
        return "https://minotar.net/avatar/" + player.getName();
    }

}
