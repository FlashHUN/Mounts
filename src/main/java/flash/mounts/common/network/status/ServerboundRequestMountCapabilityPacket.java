package flash.mounts.common.network.status;

import flash.mounts.common.capability.MountCapabilityProvider;
import flash.mounts.common.network.PacketDispatcher;
import flash.mounts.common.network.game.ClientboundSetActiveMountPacket;
import flash.mounts.common.network.game.ClientboundSetFavoriteMountsPacket;
import flash.mounts.common.network.game.ClientboundSetUnlockedMountsPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundRequestMountCapabilityPacket {

  public ServerboundRequestMountCapabilityPacket() {}

  public static void encode(ServerboundRequestMountCapabilityPacket msg, FriendlyByteBuf buf) {

  }

  public static ServerboundRequestMountCapabilityPacket decode(FriendlyByteBuf buf) {
    return new ServerboundRequestMountCapabilityPacket();
  }

  public static void handle(ServerboundRequestMountCapabilityPacket msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer sender = ctx.get().getSender();
      MountCapabilityProvider.IMountCapability cap = MountCapabilityProvider.getCapability(sender);
      PacketDispatcher.sendTo(new ClientboundSetActiveMountPacket(cap.getActiveMount()), sender);
      PacketDispatcher.sendTo(new ClientboundSetUnlockedMountsPacket(cap.getUnlockedMounts()), sender);
      PacketDispatcher.sendTo(new ClientboundSetFavoriteMountsPacket(cap.getFavoriteMounts()), sender);
    });
    ctx.get().setPacketHandled(true);
  }
}
