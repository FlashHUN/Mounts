package flash.mounts.common.network.game;

import flash.mounts.common.capability.MountCapabilityProvider;
import flash.mounts.common.mount.Mounts;
import flash.mounts.common.mount.Mounts.Mount;
import flash.mounts.common.network.PacketDispatcher;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundSetActiveMountPacket {

  Mount mount;

  public ServerboundSetActiveMountPacket(Mount mount) {
    this.mount = mount;
  }

  public static void encode(ServerboundSetActiveMountPacket msg, FriendlyByteBuf buf) {
    buf.writeBoolean(msg.mount != null);
    if (msg.mount != null) {
      buf.writeUtf(msg.mount.mob());
      buf.writeNbt(msg.mount.tag());
    }
  }

  public static ServerboundSetActiveMountPacket decode(FriendlyByteBuf buf) {
    if (buf.readBoolean()) {
      return new ServerboundSetActiveMountPacket(Mounts.getMountFromMob(buf.readUtf(), buf.readNbt()));
    }
    return new ServerboundSetActiveMountPacket(null);
  }

  public static void handle(ServerboundSetActiveMountPacket msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer sender = ctx.get().getSender();
      MountCapabilityProvider.IMountCapability cap = MountCapabilityProvider.getCapability(sender);
      if (msg.mount == null || cap.getUnlockedMounts().contains(msg.mount)) {
        cap.setActiveMount(msg.mount);
        if (sender.isPassenger() && sender.getVehicle().getTags().contains(Mounts.MOUNT_TAG)) {
          sender.getVehicle().ejectPassengers();
        }
      }
      PacketDispatcher.sendTo(new ClientboundSetActiveMountPacket(msg.mount), sender);
    });
    ctx.get().setPacketHandled(true);
  }
}
