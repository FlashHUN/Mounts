package flash.mounts.common.network.status;

import flash.mounts.common.capability.MountCapabilityProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundSetDescentStatusPacket {

  boolean status;

  public ServerboundSetDescentStatusPacket(boolean status) {
    this.status = status;
  }

  public static void encode(ServerboundSetDescentStatusPacket msg, FriendlyByteBuf buf) {
    buf.writeBoolean(msg.status);
  }

  public static ServerboundSetDescentStatusPacket decode(FriendlyByteBuf buf) {
    return new ServerboundSetDescentStatusPacket(buf.readBoolean());
  }

  public static void handle(ServerboundSetDescentStatusPacket msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer sender = ctx.get().getSender();
      MountCapabilityProvider.IMountCapability cap = MountCapabilityProvider.getCapability(sender);
      cap.setDescending(msg.status);
    });
    ctx.get().setPacketHandled(true);
  }
}
