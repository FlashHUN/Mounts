package flash.mounts.common.network.game;

import flash.mounts.Main;
import flash.mounts.common.mount.Mounts;
import flash.mounts.common.mount.Mounts.Mount;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundSetActiveMountPacket {

  Mount mount;

  public ClientboundSetActiveMountPacket(Mount mount) {
    this.mount = mount;
  }

  public static void encode(ClientboundSetActiveMountPacket msg, FriendlyByteBuf buf) {
    buf.writeBoolean(msg.mount != null);
    if (msg.mount != null) {
      buf.writeUtf(msg.mount.mob());
      buf.writeNbt(msg.mount.tag());
    }
  }

  public static ClientboundSetActiveMountPacket decode(FriendlyByteBuf buf) {
    if (buf.readBoolean()) {
      return new ClientboundSetActiveMountPacket(Mounts.getMountFromMob(buf.readUtf(), buf.readNbt()));
    }
    return new ClientboundSetActiveMountPacket(null);
  }

  public static void handle(ClientboundSetActiveMountPacket msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      Main.PROXY.syncActiveMount(msg.mount);
    });
    ctx.get().setPacketHandled(true);
  }
}
