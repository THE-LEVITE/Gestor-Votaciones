package co.edu.sena.cimm.eventos.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Serializacion/deserializacion JSON centralizada (Gson + soporte LocalDateTime). */
public final class JsonUtil {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    private JsonUtil() {
    }

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    static class LocalDateTimeAdapter
            implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

        private static final DateTimeFormatter F = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        public JsonElement serialize(LocalDateTime src, Type type, JsonSerializationContext ctx) {
            return src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.format(F));
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type type, JsonDeserializationContext ctx) {
            if (json == null || json.isJsonNull()) {
                return null;
            }
            String valor = json.getAsString();
            // Los inputs datetime-local envian "yyyy-MM-ddTHH:mm" (sin segundos)
            if (valor.length() == 16) {
                valor = valor + ":00";
            }
            return LocalDateTime.parse(valor, F);
        }
    }
}
