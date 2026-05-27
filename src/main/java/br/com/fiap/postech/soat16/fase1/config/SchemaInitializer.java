package br.com.fiap.postech.soat16.fase1.config;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.vertx.mutiny.sqlclient.Pool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@ApplicationScoped
@RequiredArgsConstructor
public class SchemaInitializer {

    private final Pool pool;

    void onStart(@Observes StartupEvent event) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("db/init.sql")) {
            if (is == null) {
                Log.warn("db/init.sql not found on classpath — skipping schema initialization.");
                return;
            }
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Arrays.stream(content.split(";"))
                    .map(stmt -> stmt.replaceAll("--[^\n]*", "").trim())
                    .filter(stmt -> !stmt.isBlank())
                    .forEach(stmt -> pool.query(stmt).execute().await().indefinitely());
            Log.info("Schema initialized from db/init.sql");
        } catch (IOException e) {
            throw new RuntimeException("Failed to read db/init.sql", e);
        }
    }
}
