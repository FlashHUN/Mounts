package flash.mounts.common.network.status;

import flash.mounts.Main;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundSetMountTimerPacket {

  int timer;

  public ClientboundSetMountTimerPacket(int timer) {
    this.timer = timer;
  }

  public static void encode(ClientboundSetMountTimerPacket msg, FriendlyByteBuf buf) {
    buf.writeInt(msg.timer);
  }

  public static ClientboundSetMountTimerPacket decode(FriendlyByteBuf buf) {
    return new ClientboundSetMountTimerPacket(buf.readInt());
  }

  public static void handle(ClientboundSetMountTimerPacket msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      Main.PROXY.syncMountTimer(msg.timer);
    });
    ctx.get().setPacketHandled(true);
  }
}
