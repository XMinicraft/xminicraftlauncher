package org.xminicraft.xminicraftlauncher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.xminicraft.xminicraftlauncher.util.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Language {
    private static Language instance;

    public final Map<String, String> translations = new HashMap<>();

    public static Language getInstance() {
        if (instance == null) instance = new Language();
        return instance;
    }

    public void load(String langCode) {
        this.translations.clear();

        Path langPath = FileUtils.getJarResourcesPath().resolve("lang/" + langCode + ".json");

        if (!Files.exists(langPath)) {
            return;
        }

        ObjectMapper mapper = new ObjectMapper();

        try  {
            JsonNode rootNode = mapper.readTree(FileUtils.readLines(langPath, StandardCharsets.UTF_8));

            for (Map.Entry<String, JsonNode> entry : rootNode.properties()) {
                this.translations.put(entry.getKey(), entry.getValue().asText());
            }
        } catch (IOException ignored) {
        }
    }

    public String translateKey(String key) {
        return this.translations.getOrDefault(key, key);
    }

    public static String translate(String key) {
        return Language.getInstance().translateKey(key);
    }
}
