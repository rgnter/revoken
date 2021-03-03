package eu.battleland.revoken.providers.storage.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import eu.battleland.revoken.RevokenPlugin;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;

@Log4j2
public class DatabaseProvider {

    private RevokenPlugin plugin;

    private HikariDataSource dataSource;
    private final HikariConfig     config;
    {
        this.config = new HikariConfig();
    }

    private Connection connection;

    /**
     * Default constructor
     */
    public DatabaseProvider(@NotNull RevokenPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Connects to database
     * @param host Hostname
     * @param username Username
     * @param password Password
     * @param databaseName Databse name
     * @param port port
     */
    public void connectToMySQL(@NotNull String host, @NotNull String username, @NotNull String password, @NotNull String databaseName, int port) {
        this.config.setJdbcUrl("jdbc:mysql://" + host +":" + port + "/" + databaseName);
        this.config.setUsername(username);
        this.config.setPassword(password);
        this.dataSource = new HikariDataSource(config);
    }

    /**
     * Connects to database
     * @param path Path to file
     */
    public void connectToSQLite(@NotNull String path) {
        final File file = new File(plugin.getDataFolder(), path);
        if(!file.exists()) {
            if (!file.getParentFile().exists())
                file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                log.error("Couldn't create SQLite .db file", e);
            }
        }
        this.config.setJdbcUrl("jdbc:sqlite:" + file.getPath());
        this.config.setDriverClassName("org.sqlite.JDBC");

        this.dataSource = new HikariDataSource(config);
    }

}
