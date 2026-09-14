package co.edu.sena.cimm.eventos.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Proveedor unico del pool de conexiones (HikariCP).
 *
 * La configuracion se toma, en orden de prioridad:
 *   1. Variables de entorno (EVENTOS_DB_URL, EVENTOS_DB_USER, ...)
 *   2. Archivo db.properties en el classpath
 *   3. Valores por defecto
 */
public final class Database {

    private static volatile DataSource dataSource;

    private Database() {
    }

    public static DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (Database.class) {
                if (dataSource == null) {
                    dataSource = construir();
                }
            }
        }
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    private static DataSource construir() {
        Properties p = cargarPropiedades();

        try { Class.forName("com.mysql.cj.jdbc.Driver"); } catch (Exception e) {}
        HikariConfig cfg = new HikariConfig();
        cfg.setDriverClassName("com.mysql.cj.jdbc.Driver");
        cfg.setJdbcUrl(valor(p, "db.url", "EVENTOS_DB_URL",
                "jdbc:mysql://localhost:3306/eventos_qr?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true"));
        cfg.setUsername(valor(p, "db.user", "EVENTOS_DB_USER", "root"));
        cfg.setPassword(valor(p, "db.password", "EVENTOS_DB_PASSWORD", ""));
        cfg.setMaximumPoolSize(Integer.parseInt(valor(p, "db.poolSize", "EVENTOS_DB_POOL", "5")));
        cfg.setPoolName("eventos-pool");
        cfg.setConnectionTimeout(10000);

        return new HikariDataSource(cfg);
    }

    private static Properties cargarPropiedades() {
        Properties p = new Properties();
        try (InputStream in = Database.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                p.load(in);
            }
        } catch (IOException e) {
            // Si no hay archivo, se usan variables de entorno o valores por defecto.
        }
        return p;
    }

    private static String valor(Properties p, String clave, String envKey, String porDefecto) {
        String v = System.getenv(envKey);
        if (v == null || v.trim().isEmpty()) {
            v = p.getProperty(clave);
        }
        if (v == null || v.trim().isEmpty()) {
            v = porDefecto;
        }
        return v;
    }
}

