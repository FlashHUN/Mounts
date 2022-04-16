package flash.mounts.common.network.status;

import flash.mounts.Main;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundSetAbilityCooldownPacket {

  int timer;

  public ClientboundSetAbilityCooldownPacket(int timer) {
    this.timer = timer;
  }

  public static void encode(ClientboundSetAbilityCooldownPacket msg, FriendlyByteBuf buf) {
    buf.writeInt(msg.timer);
  }

  public static ClientboundSetAbilityCooldownPacket decode(FriendlyByteBuf buf) {
    return new ClientboundSetAbilityCooldownPacket(buf.readInt());
  }

  public static void handle(ClientboundSetAbilityCooldownPacket msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      Main.PROXY.syncAbilityCooldown(msg.timer);
    });
    ctx.get().setPacketHandled(true);
  }
}
