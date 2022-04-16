package flash.mounts.common.network.game;

import flash.mounts.common.capability.MountCapabilityProvider;
import flash.mounts.common.mount.Abilities;
import flash.mounts.common.mount.Mounts;
import flash.mounts.common.network.PacketDispatcher;
import flash.mounts.common.network.status.ClientboundSetMountTimerPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundUseMountAbilityPacket {

  public ServerboundUseMountAbilityPacket() {}

  public static void encode(ServerboundUseMountAbilityPacket msg, FriendlyByteBuf buf) {

  }

  public static ServerboundUseMountAbilityPacket decode(FriendlyByteBuf buf) {
    return new ServerboundUseMountAbilityPacket();
  }

  public static void handle(ServerboundUseMountAbilityPacket msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer sender = ctx.get().getSender();
      MountCapabilityProvider.IMountCapability cap = MountCapabilityProvider.getCapability(sender);
      Mounts.Mount mount = cap.getActiveMount();
      if (mount != null && sender.isPassenger() && sender.getVehicle().getTags().contains(Mounts.MOUNT_TAG)) {
        Abilities.Ability ability = mount.ability();
        if (ability != null) {
          cap.chargeAbility(ability);
        }
      }
    });
    ctx.get().setPacketHandled(true);
  }
}
