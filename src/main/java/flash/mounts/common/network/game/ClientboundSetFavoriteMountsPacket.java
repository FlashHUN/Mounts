package flash.mounts.common.network.game;

import flash.mounts.Main;
import flash.mounts.common.mount.Mounts;
import flash.mounts.common.mount.Mounts.Mount;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.function.Supplier;

public class ClientboundSetFavoriteMountsPacket {

  HashSet<Mount> favorites;

  public ClientboundSetFavoriteMountsPacket(HashSet<Mount> favorites) {
    this.favorites = favorites;
  }

  public static void encode(ClientboundSetFavoriteMountsPacket msg, FriendlyByteBuf buf) {
    buf.writeInt(msg.favorites.size());
    for (Mount mount : msg.favorites) {
      buf.writeUtf(mount.mob());
      buf.writeNbt(mount.tag());
    }
  }

  public static ClientboundSetFavoriteMountsPacket decode(FriendlyByteBuf buf) {
    HashSet<Mount> favorites = new HashSet<>();
    int size = buf.readInt();
    for (int i = 0; i < size; i++) {
      Mount mount = Mounts.getMountFromMob(buf.readUtf(), buf.readNbt());
      favorites.add(mount);
    }
    return new ClientboundSetFavoriteMountsPacket(favorites);
  }

  public static void handle(ClientboundSetFavoriteMountsPacket msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      Main.PROXY.syncFavoriteMounts(msg.favorites);
    });
    ctx.get().setPacketHandled(true);
  }
}
