// Copyright 2022 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.engine.game;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.core.SimpleUri;
import org.terasology.engine.core.TerasologyConstants;
import org.terasology.engine.utilities.gson.CaseInsensitiveEnumTypeAdapterFactory;
import org.terasology.engine.utilities.gson.UriTypeAdapterFactory;
import org.terasology.engine.world.generator.internal.WorldGeneratorManager;
import org.terasology.engine.world.internal.WorldInfo;
import org.terasology.gestalt.entitysystem.component.Component;
import org.terasology.gestalt.naming.Name;
import org.terasology.gestalt.naming.NameVersion;
import org.terasology.gestalt.naming.Version;
import org.terasology.gestalt.naming.gson.NameTypeAdapter;
import org.terasology.gestalt.naming.gson.VersionTypeAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Represents the saved metadata of game, including title, seed, modules,
 * worlds, block family registration, and module configuration settings.
 * <p>
 * This information can be saved to or loaded from a file (usually "manifest.json").
 */
public class GameManifest {

    /** Default file name used for saving a manifest */
    public static final String DEFAULT_FILE_NAME = "manifest.json";

    private static final Logger logger = LoggerFactory.getLogger(GameManifest.class);

    // --- Basic Info ---
    private String title = "";
    private String seed = "";
    private long time;

    // --- Block Data ---
    private List<String> registeredBlockFamilies = Lists.newArrayList();
    private Map<String, Short> blockIdMap = Maps.newHashMap();

    // --- World Info ---
    private Map<String, WorldInfo> worlds = Maps.newHashMap();

    // --- Module Info ---
    private List<NameVersion> modules = Lists.newArrayList();

    // --- Module Configs ---
    private Map<SimpleUri, Map<String, JsonElement>> moduleConfigs = Maps.newHashMap();

    /**
     * Creates a blank GameManifest.
     */
    public GameManifest() {
    }

    /**
     * Creates a GameManifest with specified title, seed, and time.
     *
     * @param title The title of the game.
     * @param seed The world seed.
     * @param time The in-game time.
     */
    public GameManifest(String title, String seed, long time) {
        if (title != null) {
            this.title = title;
        }
        if (seed != null) {
            this.seed = seed;
        }
        this.time = time;
    }

    // --- Basic Info Getters and Setters ---

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSeed() {
        return seed;
    }

    public void setSeed(String seed) {
        this.seed = seed;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    // --- Block Data Getters and Setters ---

    public List<String> getRegisteredBlockFamilies() {
        return registeredBlockFamilies;
    }

    public void setRegisteredBlockFamilies(List<String> registeredBlockFamilies) {
        this.registeredBlockFamilies = registeredBlockFamilies;
    }

    public Map<String, Short> getBlockIdMap() {
        return blockIdMap;
    }

    public void setBlockIdMap(Map<String, Short> blockIdMap) {
        this.blockIdMap = blockIdMap;
    }

    // --- World Info Methods ---

    /**
     * Retrieves information about a specific world.
     *
     * @param name The world name.
     * @return The WorldInfo object for the given name.
     */
    public WorldInfo getWorldInfo(String name) {
        return worlds.get(name);
    }

    public Map<String, WorldInfo> getWorldInfoMap() {
        return worlds;
    }

    /**
     * Adds a world to the manifest.
     *
     * @param worldInfo The world information to add.
     */
    public void addWorld(WorldInfo worldInfo) {
        this.worlds.put(worldInfo.getTitle(), worldInfo);
    }

    /**
     * Returns an iterable collection of all worlds in the manifest.
     *
     * @return The iterable of WorldInfo objects.
     */
    public Iterable<WorldInfo> getWorlds() {
        return this.worlds.values();
    }

    // --- Module Info Methods ---

    /**
     * Returns an immutable list of modules used in the game.
     *
     * @return The list of NameVersion pairs.
     */
    public List<NameVersion> getModules() {
        return ImmutableList.copyOf(modules);
    }

    /**
     * Adds a module (ID and version) to the manifest.
     *
     * @param id The module ID.
     * @param version The module version.
     */
    public void addModule(Name id, Version version) {
        modules.add(new NameVersion(id, version));
    }

    // --- Module Configs Methods ---

    public Map<SimpleUri, Map<String, JsonElement>> getModuleConfigs() {
        return moduleConfigs;
    }

    /**
     * Saves module configuration components for a world generator.
     *
     * @param generatorUri The URI of the generator.
     * @param configs A map of configuration key to component.
     */
    public void setModuleConfigs(SimpleUri generatorUri, Map<String, Component> configs) {
        Gson gson = createGson();
        Map<String, JsonElement> map = Maps.newHashMap();
        for (Map.Entry<String, Component> entry : configs.entrySet()) {
            JsonElement json = gson.toJsonTree(entry.getValue());
            map.put(entry.getKey(), json);
        }
        getModuleConfigs().put(generatorUri, map);
    }

    /**
     * Retrieves a specific module configuration component.
     *
     * @param uri The URI of the generator.
     * @param key The config key to retrieve.
     * @param clazz The component class to deserialize into.
     * @return The component, or {@code null} if not found.
     */
    public <T extends Component> T getModuleConfig(SimpleUri uri, String key, Class<T> clazz) {
        Map<String, JsonElement> map = getModuleConfigs().get(uri);
        if (map == null) {
            return null;
        }

        JsonElement element = map.get(key);
        Gson gson = createGson();
        return gson.fromJson(element, clazz);
    }

    // --- Serialization and Utility Methods ---

    /**
     * Saves the provided GameManifest to the specified file.
     *
     * @param toFile The path to save the file.
     * @param gameManifest The GameManifest to save.
     * @throws IOException if an I/O error occurs.
     */
    public static void save(Path toFile, GameManifest gameManifest) throws IOException {
        try (Writer writer = Files.newBufferedWriter(toFile, TerasologyConstants.CHARSET)) {
            createGson().toJson(gameManifest, writer);
        }
    }

    /**
     * Loads a GameManifest from the specified file path.
     *
     * @param filePath The path to the manifest file.
     * @return The loaded GameManifest.
     * @throws IOException if an I/O error occurs.
     */
    public static GameManifest load(Path filePath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(filePath, TerasologyConstants.CHARSET)) {
            return createGson().fromJson(reader, GameManifest.class);
        }
    }

    /**
     * Creates a configured Gson instance for (de)serializing manifests.
     *
     * @return The configured Gson instance.
     */
    private static Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapterFactory(new CaseInsensitiveEnumTypeAdapterFactory())
                .registerTypeAdapterFactory(new UriTypeAdapterFactory())
                .registerTypeAdapter(Version.class, new VersionTypeAdapter())
                .registerTypeAdapter(Name.class, new NameTypeAdapter())
                .setPrettyPrinting()
                .create();
    }

    /**
     * Returns display name of the main world's generator.
     * <p>
     * If missing, returns an "ERROR:" message.
     *
     * @param manager The WorldGeneratorManager used to look up generator info.
     * @return The display name of the generator, or an error message.
     */
    public String mainWorldDisplayName(WorldGeneratorManager manager) {
        var world = getWorldInfo(TerasologyConstants.MAIN_WORLD);
        if (world == null) {
            logger.warn("{} has no MAIN_WORLD", this);
            return "ERROR: No main world";
        }
        SimpleUri generatorUri = world.getWorldGenerator();
        var generator = manager.getWorldGeneratorInfo(generatorUri);
        if (generator == null) {
            logger.warn("{}: {} has no generator for {}", this, manager, generatorUri);
            return "ERROR: No generator found for " + generatorUri;
        }
        return generator.getDisplayName();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("title", title)
                .toString();
    }
}
