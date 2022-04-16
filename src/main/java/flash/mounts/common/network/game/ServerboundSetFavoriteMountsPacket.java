package flash.mounts.common.network.game;

import flash.mounts.common.capability.MountCapabilityProvider;
import flash.mounts.common.mount.Mounts;
import flash.mounts.common.mount.Mounts.Mount;
import flash.mounts.common.network.PacketDispatcher;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.function.Supplier;

public class ServerboundSetFavoriteMountsPacket {

  HashSet<Mount> favorites;

  public ServerboundSetFavoriteMountsPacket(HashSet<Mount> favorites) {
    this.favorites = favorites;
  }

  public static void encode(ServerboundSetFavoriteMountsPacket msg, FriendlyByteBuf buf) {
    buf.writeInt(msg.favorites.size());
    for (Mount mount : msg.favorites) {
      buf.writeUtf(mount.mob());
      buf.writeNbt(mount.tag());
    }
  }

  public static ServerboundSetFavoriteMountsPacket decode(FriendlyByteBuf buf) {
    HashSet<Mount> favorites = new HashSet<>();
    int size = buf.readInt();
    for (int i = 0; i < size; i++) {
      Mount mount = Mounts.getMountFromMob(buf.readUtf(), buf.readNbt());
      favorites.add(mount);
    }
    return new ServerboundSetFavoriteMountsPacket(favorites);
  }

  public static void handle(ServerboundSetFavoriteMountsPacket msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer sender = ctx.get().getSender();
      MountCapabilityProvider.IMountCapability cap = MountCapabilityProvider.getCapability(sender);
      boolean canSet = true;
      for (Mount mount : msg.favorites) {
        if (!cap.getUnlockedMounts().contains(mount)) {
          canSet = false;
          break;
        }
      }

      if (canSet) {
        cap.setFavoriteMounts(msg.favorites);
      }
      PacketDispatcher.sendTo(new ClientboundSetFavoriteMountsPacket(msg.favorites), sender);
    });
    ctx.get().setPacketHandled(true);
  }
}
