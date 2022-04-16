package flash.mounts.common.network.join;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import flash.mounts.Main;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundSetCommonConfigPacket {
  private static final Gson GSON = new Gson();

  JsonObject config;

  public ClientboundSetCommonConfigPacket(JsonObject config) {
    this.config = config;
  }

  public static void encode(ClientboundSetCommonConfigPacket msg, FriendlyByteBuf buf) {
    JsonObject config = msg.config;
    JsonArray mounts = config.get("mounts").getAsJsonArray();
    buf.writeInt(mounts.size());
    for (int i = 0; i < mounts.size(); i++) {
      buf.writeUtf(mounts.get(i).toString());
    }
    boolean hasAbilities = config.has("abilities");
    buf.writeBoolean(hasAbilities);
    if (hasAbilities) {
      JsonArray abilities = config.get("abilities").getAsJsonArray();
      buf.writeInt(abilities.size());
      for (int i = 0; i < abilities.size(); i++) {
        buf.writeUtf(abilities.get(i).toString());
      }
    }
  }

  public static ClientboundSetCommonConfigPacket decode(FriendlyByteBuf buf) {
    JsonObject config = new JsonObject();
    JsonArray mounts = new JsonArray();
    int size = buf.readInt();
    for (int i = 0; i < size; i++) {
      String jsonString = buf.readUtf();
      JsonObject jsonObject = GSON.fromJson(jsonString, JsonObject.class);
      mounts.add(jsonObject);
    }
    config.add("mounts", mounts);
    if (buf.readBoolean()) {
      JsonArray abilities = new JsonArray();
      int size2 = buf.readInt();
      for (int i = 0; i < size2; i++) {
        String jsonString = buf.readUtf();
        JsonObject jsonObject = GSON.fromJson(jsonString, JsonObject.class);
        abilities.add(jsonObject);
      }
      config.add("abilities", abilities);
    }
    return new ClientboundSetCommonConfigPacket(config);
  }

  public static void handle(ClientboundSetCommonConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      Main.PROXY.syncCommonConfig(msg.config);
    });
    ctx.get().setPacketHandled(true);
  }
}
