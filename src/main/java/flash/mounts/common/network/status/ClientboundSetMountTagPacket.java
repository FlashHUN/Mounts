package flash.mounts.common.network.status;

import flash.mounts.Main;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundSetMountTagPacket {

  public ClientboundSetMountTagPacket() {}

  public static void encode(ClientboundSetMountTagPacket msg, FriendlyByteBuf buf) {
  }

  public static ClientboundSetMountTagPacket decode(FriendlyByteBuf buf) {
    return new ClientboundSetMountTagPacket();
  }

  public static void handle(ClientboundSetMountTagPacket msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      Main.PROXY.syncMountTag();
    });
    ctx.get().setPacketHandled(true);
  }
}
