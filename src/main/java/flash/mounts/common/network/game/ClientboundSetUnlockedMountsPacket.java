package flash.mounts.common.network.game;

import flash.mounts.Main;
import flash.mounts.common.mount.Mounts;
import flash.mounts.common.mount.Mounts.Mount;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.function.Supplier;

public class ClientboundSetUnlockedMountsPacket {

  HashSet<Mount> unlockedMounts;

  public ClientboundSetUnlockedMountsPacket(HashSet<Mount> unlockedMounts) {
    this.unlockedMounts = unlockedMounts;
  }

  public static void encode(ClientboundSetUnlockedMountsPacket msg, FriendlyByteBuf buf) {
    buf.writeInt(msg.unlockedMounts.size());
    for (Mount mount : msg.unlockedMounts) {
      buf.writeUtf(mount.mob());
      buf.writeNbt(mount.tag());
    }
  }

  public static ClientboundSetUnlockedMountsPacket decode(FriendlyByteBuf buf) {
    HashSet<Mount> unlockedMounts = new HashSet<>();
    int setSize = buf.readInt();
    for (int i = 0; i < setSize; i++) {
      Mount mount = Mounts.getMountFromMob(buf.readUtf(), buf.readNbt());
      unlockedMounts.add(mount);
    }
    return new ClientboundSetUnlockedMountsPacket(unlockedMounts);
  }

  public static void handle(ClientboundSetUnlockedMountsPacket msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      Main.PROXY.sync(msg.unlockedMounts);
    });
    ctx.get().setPacketHandled(true);
  }
}
