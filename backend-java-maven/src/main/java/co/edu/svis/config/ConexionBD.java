package co.edu.svis.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gestor centralizado del Pool de Conexiones JDBC utilizando HikariCP.
 * Provee conexiones optimizadas para transacciones concurrentes de alta demanda en MySQL.
 */
public class ConexionBD {

    private static final Logger LOGGER = Logger.getLogger(ConexionBD.class.getName());
    private static HikariDataSource dataSource;

    static {
        try {
            Properties props = new Properties();
            try (InputStream is = ConexionBD.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (is != null) {
                    props.load(is);
                } else {
                    LOGGER.warning("db.properties no encontrado en classpath, usando valores por defecto");
                }
            }

            // Prioridad a variables de entorno si existen
            String envUrl = System.getenv("SVIS_DB_URL");
            String envUser = System.getenv("SVIS_DB_USER");
            String envPass = System.getenv("SVIS_DB_PASSWORD");

            String url = envUrl != null ? envUrl : props.getProperty("db.url", 
                    "jdbc:mysql://localhost:3306/svis_db?serverTimezone=America/Bogota&useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8");
            String user = envUser != null ? envUser : props.getProperty("db.user", "root");
            String password = envPass != null ? envPass : props.getProperty("db.password", "");
            int poolSize = Integer.parseInt(props.getProperty("db.poolSize", "10"));

            // Cargar explícitamente el driver MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(user);
            config.setPassword(password);
            config.setMaximumPoolSize(poolSize);
            config.setMinimumIdle(Integer.parseInt(props.getProperty("db.minIdle", "2")));
            config.setConnectionTimeout(Long.parseLong(props.getProperty("db.connectionTimeout", "10000")));
            config.setIdleTimeout(Long.parseLong(props.getProperty("db.idleTimeout", "300000")));
            config.setPoolName("SVIS-HikariCP-Pool");

            // Optimizaciones de rendimiento JDBC
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);
            LOGGER.info("Pool de conexiones HikariCP inicializado correctamente hacia: " + url);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error crítico al inicializar el pool de conexiones de base de datos", e);
            throw new RuntimeException("Error en conexión a Base de Datos SVIS: " + e.getMessage(), e);
        }
    }

    private ConexionBD() {
        // Constructor privado para evitar instanciación
    }

    /**
     * Obtiene una conexión activa del pool.
     * Recuerde invocar conn.close() en un bloque try-with-resources o finally.
     *
     * @return Connection lista para transacciones JDBC.
     * @throws SQLException Si ocurre un fallo en el pool o en el servidor MySQL.
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("El DataSource HikariCP de SVIS no está disponible.");
        }
        return dataSource.getConnection();
    }

    /**
     * Cierra el pool al detener el contenedor web.
     */
    public static void cerrarPool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("Pool de conexiones HikariCP cerrado con éxito.");
        }
    }
}
