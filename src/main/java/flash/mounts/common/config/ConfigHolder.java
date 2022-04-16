package flash.mounts.common.config;

import com.google.gson.*;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import flash.mounts.Main;
import flash.mounts.common.mount.Abilities;
import flash.mounts.common.mount.Abilities.Ability;
import flash.mounts.common.mount.Mounts;
import flash.mounts.common.mount.Mounts.Mount;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;

public class ConfigHolder {

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  private enum ConfigType {
    CLIENT, // should only be loaded on clients
    COMMON, // should be loaded on both sides and synced from the server to all clients
    SERVER  // should only be loaded on logical servers, this is per-world
  }

  private static abstract class JsonConfig {

    private JsonElement config;
    private ConfigType type;

    JsonConfig(ConfigType type) {
      this.type = type;
      reloadConfig();
    }

    public void reloadConfig() {
      JsonElement config;
      if (type == ConfigType.SERVER) {
        config = ConfigHolder.loadPerWorldJsonConfigOrDefault(Main.MODID + "-" + type.name().toLowerCase(), getDefaultConfig());
      } else {
        config = ConfigHolder.loadJsonConfigOrDefault(Main.MODID + "-" + type.name().toLowerCase(), getDefaultConfig());
      }
      if (isConfigRightType(config) && getConfigValidator().test(config.toString())) {
        this.config = config;
      } else {
        Main.LOGGER.warn("Config file "+Main.MODID+"-"+type.name().toLowerCase()+".json has invalid data, returning default config instead.");
        this.config = getDefaultConfig();
      }
    }

    abstract boolean isConfigRightType(JsonElement config);

    JsonElement prevalidateConfig(Object text) {
      if (text == null) return null;
      if (!text.getClass().isAssignableFrom(String.class)) return null;
      JsonElement jsonElement = GSON.fromJson(String.valueOf(text), JsonElement.class);
      if (isConfigRightType(jsonElement)) return jsonElement;
      return null;
    }

    abstract Predicate<Object> getConfigValidator();

    abstract JsonElement getDefaultConfig();

    public JsonElement getConfig() {
      return config.deepCopy();
    }

    // In case we need to sync the server's config to clients
    @OnlyIn(Dist.CLIENT)
    public void setConfig(JsonElement config) {
      this.config = config;
    }

  }

  @OnlyIn(Dist.CLIENT)
  public static class Client extends JsonConfig {

    public Client() {
      super(ConfigType.CLIENT);
    }

    @Override
    boolean isConfigRightType(JsonElement config) {
      return config.isJsonObject();
    }

    @Override
    Predicate<Object> getConfigValidator() {
      return (text) -> {
        JsonElement jsonElement = prevalidateConfig(text);
        if (jsonElement == null) return false;
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        return jsonObject.has("active_mount_overlay") && jsonObject.has("item_tooltips_shown");
      };
    }

    @Override
    JsonElement getDefaultConfig() {
      JsonObject config = new JsonObject();

      config.addProperty("item_tooltips_shown", true);
      config.addProperty("active_mount_overlay", true);

      return config;
    }

    public boolean areTooltipsShown() {
      return this.getConfig().getAsJsonObject().get("item_tooltips_shown").getAsBoolean();
    }

    public boolean isOverlayShown() {
      return this.getConfig().getAsJsonObject().get("active_mount_overlay").getAsBoolean();
    }
  }

  public static class Common extends JsonConfig {

    public Common() {
      super(ConfigType.COMMON);
    }

    @Override
    boolean isConfigRightType(JsonElement config) {
      return config.isJsonObject();
    }

    @Override
    Predicate<Object> getConfigValidator() {
      return (text) -> {
        try {
          JsonElement jsonElement = prevalidateConfig(text);
          if (jsonElement == null) return false;
          JsonObject config = jsonElement.getAsJsonObject();
          if (!config.has("mounts") || !config.get("mounts").isJsonArray()) return false;
          JsonArray mountsArray = config.get("mounts").getAsJsonArray();
          for (int i = 0; i < mountsArray.size(); i++) {
            if (!mountsArray.get(i).isJsonObject()) return false;
            JsonObject jsonObject = mountsArray.get(i).getAsJsonObject();
            if (!jsonObject.has("mob") || !jsonObject.has("items")
                    || !jsonObject.get("items").isJsonArray() || jsonObject.get("items").getAsJsonArray().size() == 0) return false;
          }

          if (config.has("abilities") && config.get("abilities").isJsonArray()) {
            JsonArray jsonArray = config.get("abilities").getAsJsonArray();
            for (int i = 0; i < jsonArray.size(); i++) {
              if (!jsonArray.get(i).isJsonObject()) return false;
              JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
              if (!jsonObject.has("name") || !jsonObject.has("type")) return false;
            }
          }

          return true;
        } catch (Exception e) {
          return false;
        }
      };
    }

    @Override
    JsonElement getDefaultConfig() {
      JsonObject config = new JsonObject();

      config.addProperty("allow_item_tooltips_shown", true);

      JsonArray defaultMounts = new JsonArray();
      {
        JsonObject pigMount = new JsonObject();
        pigMount.addProperty("mob", "minecraft:pig");
        JsonArray items = new JsonArray();
        JsonObject item = new JsonObject();
        item.addProperty("item", "minecraft:golden_carrot");
        item.addProperty("count", 1);
        items.add(item);
        pigMount.add("items", items);
        pigMount.addProperty("ability", "mount_speed");
        pigMount.addProperty("speed", 0.2f);
        defaultMounts.add(pigMount);
      } // Cow
      {
        JsonObject phantomMount = new JsonObject();
        phantomMount.addProperty("mob", "minecraft:phantom");
        JsonArray items = new JsonArray();
        JsonObject item = new JsonObject();
        item.addProperty("item", "minecraft:elytra");
        items.add(item);
        phantomMount.add("items", items);
        phantomMount.addProperty("ability", "small_fireball");
        defaultMounts.add(phantomMount);
      } // Phantom with Fireball
      {
        JsonObject zombieMount = new JsonObject();
        zombieMount.addProperty("mob", "minecraft:zombie");
        JsonArray items = new JsonArray();
        JsonObject item = new JsonObject();
        item.addProperty("item", "minecraft:diamond_sword{display:{Name:'[{\"text\":\"Excalibur\"}]'}}");
        item.addProperty("count", 1);
        items.add(item);
        zombieMount.add("items", items);
        zombieMount.addProperty("ability", "player_resistance");
        zombieMount.addProperty("tag", "{HandItems:[{id:diamond_sword,tag:{display:{Name:'[{\"text\":\"Excalibur\"}]'}},Count:1}]}");
        defaultMounts.add(zombieMount);
      } // Zombie with Sword

      config.add("mounts", defaultMounts);

      JsonArray defaultAbilities = new JsonArray();
      {
        JsonObject fireballAbility = new JsonObject();
        fireballAbility.addProperty("name", "small_fireball");
        fireballAbility.addProperty("type", "fireball");
        fireballAbility.addProperty("explosion_power", 1);
        fireballAbility.addProperty("speed", 0.08);
        fireballAbility.addProperty("cooldown", 80);
        fireballAbility.addProperty("charge_time", 0);
        defaultAbilities.add(fireballAbility);
      } // Small Fireball
      {
        JsonObject mountSpeedAbility = new JsonObject();
        mountSpeedAbility.addProperty("name", "mount_speed");
        mountSpeedAbility.addProperty("type", "mount_effect");
        mountSpeedAbility.addProperty("effect", "minecraft:speed");
        mountSpeedAbility.addProperty("duration", 40);
        mountSpeedAbility.addProperty("amplifier", 1);
        mountSpeedAbility.addProperty("cooldown", 120);
        mountSpeedAbility.addProperty("charge_time", 0);
        defaultAbilities.add(mountSpeedAbility);
      } // Mount Speed
      {
        JsonObject playerResistanceAbility = new JsonObject();
        playerResistanceAbility.addProperty("name", "player_resistance");
        playerResistanceAbility.addProperty("type", "player_effect");
        playerResistanceAbility.addProperty("effect", "minecraft:resistance");
        playerResistanceAbility.addProperty("duration", 40);
        playerResistanceAbility.addProperty("amplifier", 0);
        playerResistanceAbility.addProperty("cooldown", 200);
        playerResistanceAbility.addProperty("charge_time", 0);
        defaultAbilities.add(playerResistanceAbility);
      } // Player Resistance

      config.add("abilities", defaultAbilities);

      return config;
    }

    public void loadAllAbilities() {
      Abilities.clear();
      if (getConfig().getAsJsonObject().has("abilities")) {
        JsonArray config = getConfig().getAsJsonObject().get("abilities").getAsJsonArray();
        for (int i = 0; i < config.size(); i++) {
          Abilities.registerAbility(config.get(i).getAsJsonObject());
        }
      }
    }

    public HashSet<Mount> loadAllMounts() {
      Mounts.ITEM_TO_MOUNT_MAP.clear();
      Mounts.Mount.CACHED_ENTITIES.clear();
      HashSet<Mount> returnSet = new HashSet<>();
      JsonArray config = getConfig().getAsJsonObject().get("mounts").getAsJsonArray();
      for (int i = 0; i < config.size(); i++) {
        JsonObject jsonObject = config.get(i).getAsJsonObject();
        String mob = jsonObject.get("mob").getAsString();
        if (!mob.contains(":")) {
          mob = "minecraft:"+mob;
        }
        Optional<Float> speed = jsonObject.has("speed") ? Optional.of(jsonObject.get("speed").getAsFloat()) : Optional.empty();

        Ability ability = jsonObject.has("ability") ? Abilities.getAbility(jsonObject.get("ability").getAsString()) : null;

        CompoundTag tag;
        try {
          tag = jsonObject.has("tag") ? new TagParser(new StringReader(jsonObject.get("tag").getAsString())).readStruct() : new CompoundTag();
        } catch (Exception e) {
          tag = new CompoundTag();
        }

        if (jsonObject.has("items")) {
          JsonArray itemArray = jsonObject.get("items").getAsJsonArray();
          List<ItemStack> itemList = new ArrayList<>();
          for (int j = 0; j < itemArray.size(); j++) {
            JsonObject item = itemArray.get(j).getAsJsonObject();
            ItemStack stack = stackFromString(item.get("item").getAsString());
            if (stack != null) {
              if (item.has("count"))
                stack.setCount(item.get("count").getAsInt());
              else
                stack.setCount(1);
              itemList.add(stack);
            }
          }
          ItemStack[] items = itemList.toArray(new ItemStack[0]);
          if (items.length > 0) {
            Mount mount = new Mount(mob, items, speed, ability, tag);
            addMountToItemMap(mount);
            returnSet.add(mount);
            Main.LOGGER.debug("ConfigHolder#loadAllMounts: Loaded Mount " + mount);
          } else {
            Main.LOGGER.warn("ConfigHolder#loadAllMounts: Failed to load Mount " + mob + tag.getAsString());
          }
        } else {
          Main.LOGGER.warn("ConfigHolder#loadAllMounts: Failed to load Mount " + mob + tag.getAsString());
        }
      }

      return returnSet;
    }

    private void addMountToItemMap(Mount mount) {
      for (ItemStack stack : mount.item()) {
        Mounts.ITEM_TO_MOUNT_MAP.put(stack, mount);
        Main.LOGGER.debug("ConfigHolder#addMountToItemMap: Added " + stack.toString() + ", " + mount + " to map");
      }
    }

    public boolean allowTooltips() {
      JsonObject config = getConfig().getAsJsonObject();
      return !config.has("allow_item_tooltips_shown") || config.get("allow_item_tooltips_shown").getAsBoolean();
    }

    @Nullable
    private static ItemStack stackFromString(String s) {
      ItemArgument itemArgument = new ItemArgument();
      try {
        ItemInput itemInput = itemArgument.parse(new StringReader(s));
        return itemInput.createItemStack(1, false);
      } catch (CommandSyntaxException e) {
        return null;
      }
    }
  }

  public static class Server extends JsonConfig {

    public Server() {
      super(ConfigType.SERVER);
    }

    @Override
    boolean isConfigRightType(JsonElement config) {
      return true;
    }

    @Override
    Predicate<Object> getConfigValidator() {
      return (text) -> {
        JsonElement jsonElement = prevalidateConfig(text);
        if (jsonElement == null) return false;
        return true;
      };
    }

    @Override
    JsonElement getDefaultConfig() {
      return null;
    }
  }

  private static JsonElement buildJsonConfig(String name, JsonElement config) {
    try {
      File jsonFile = getJsonConfigFile("", name);
      Writer fw = new OutputStreamWriter(new FileOutputStream(jsonFile), StandardCharsets.UTF_8);
      GSON.toJson(config, fw);
      fw.flush();
      fw.close();
    } catch (Exception e) {
      Main.LOGGER.warn("Could not create config file " + name + ".json");
      e.printStackTrace();
    }
    return config;
  }

  private static JsonElement loadJsonConfig(String path, String name) {
    try {
      InputStreamReader is = new InputStreamReader(new FileInputStream(getJsonConfigFile(path, name)), StandardCharsets.UTF_8);
      return GSON.fromJson(is, JsonElement.class);
    } catch (FileNotFoundException e) {
      Main.LOGGER.warn("Could not find config file " + name + ".json");
    }
    return null;
  }

  private static JsonElement loadPerWorldJsonConfigOrDefault(String name, JsonElement defaultConfig) {
    JsonElement loadedConfig = null;
    try {
      InputStreamReader is = new InputStreamReader(new FileInputStream(getPerWorldJsonConfigFile("", name)), StandardCharsets.UTF_8);
      loadedConfig = GSON.fromJson(is, JsonElement.class);
    } catch (Exception ignored) {}

    if (loadedConfig == null)  {
      Main.LOGGER.warn("Creating new config file " + name + ".json");
      return buildJsonConfig(name, defaultConfig);
    }
    else {
      return loadedConfig;
    }
  }

  private static JsonElement loadJsonConfigOrDefault(String name, JsonElement defaultConfig) {
    JsonElement loadedConfig = loadJsonConfig("", name);
    if (loadedConfig == null)  {
      Main.LOGGER.warn("Creating new config file " + name + ".json");
      return buildJsonConfig(name, defaultConfig);
    }
    else {
      return loadedConfig;
    }
  }

  @Nullable
  private static File readFileFrom(String path, String name) {
    File directory = readDirectory(path);
    try {
      File file = new File(directory.getCanonicalPath(), name);
      return file;
    } catch (IOException e) {
      Main.LOGGER.warn("Could not read file " + path + "/" + name);
    }
    return null;
  }

  @Nullable
  private static File readDirectory(String path) {
    File directory = new File(".", path);
    if (!directory.exists()) {
      directory.mkdirs();
    }
    return directory;
  }

  private static File getJsonConfigFile(String path, String name) {
    path = "config/"+path;
    File jsonFile = readFileFrom(path, name+".json");
    return jsonFile;
  }

  private static File getPerWorldJsonConfigFile(String path, String name) {
    path = getWorldName()+"/serverconfig/"+path;
    File jsonFile = readFileFrom(path, name+".json");
    return jsonFile;
  }

  private static String getWorldName() {
    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
    String worldName = server.getWorldData().getLevelName();
    if (server.isDedicatedServer()) {
      return worldName;
    }
    return "saves/"+worldName;
  }


  @OnlyIn(Dist.CLIENT)
  public static Client CLIENT;

  public static Common COMMON;
  public static Server SERVER;

  static {
    COMMON = new Common();
  }

  /**
   * Call from WorldEvent.Load
   */
  public static void initServer() {
    SERVER = new Server();
  }

  /**
   * Call from FMLClientSetupEvent
   */
  @OnlyIn(Dist.CLIENT)
  public static void initClient() {
    if (CLIENT == null) {
      CLIENT = new Client();
    }
  }
}
