package flash.mounts.common.network.status;

import flash.mounts.Main;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundSetAbilityChargeTimePacket {

  int timer;

  public ClientboundSetAbilityChargeTimePacket(int timer) {
    this.timer = timer;
  }

  public static void encode(ClientboundSetAbilityChargeTimePacket msg, FriendlyByteBuf buf) {
    buf.writeInt(msg.timer);
  }

  public static ClientboundSetAbilityChargeTimePacket decode(FriendlyByteBuf buf) {
    return new ClientboundSetAbilityChargeTimePacket(buf.readInt());
  }

  public static void handle(ClientboundSetAbilityChargeTimePacket msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      Main.PROXY.syncAbilityChargeTime(msg.timer);
    });
    ctx.get().setPacketHandled(true);
  }
}
