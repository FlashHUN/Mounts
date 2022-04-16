package flash.mounts.common.network.game;

import flash.mounts.common.capability.MountCapabilityProvider;
import flash.mounts.common.network.PacketDispatcher;
import flash.mounts.common.network.status.ClientboundSetMountTimerPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundSummonMountPacket {

  public static int MAX_SUMMON_TIMER = 30;

  public ServerboundSummonMountPacket() {}

  public static void encode(ServerboundSummonMountPacket msg, FriendlyByteBuf buf) {

  }

  public static ServerboundSummonMountPacket decode(FriendlyByteBuf buf) {
    return new ServerboundSummonMountPacket();
  }

  public static void handle(ServerboundSummonMountPacket msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer sender = ctx.get().getSender();
      MountCapabilityProvider.getCapability(sender).setMountSummonTimer(MAX_SUMMON_TIMER);
      MountCapabilityProvider.getCapability(sender).setShouldSummonMount(true);
      PacketDispatcher.sendTo(new ClientboundSetMountTimerPacket(MAX_SUMMON_TIMER), sender);
    });
    ctx.get().setPacketHandled(true);
  }
}
