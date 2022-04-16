package flash.mounts.common.network.game;

import flash.mounts.Main;
import flash.mounts.common.capability.MountCapabilityProvider;
import flash.mounts.common.mount.Abilities;
import flash.mounts.common.mount.Mounts;
import flash.mounts.common.mount.Mounts.Mount;
import flash.mounts.common.network.PacketDispatcher;
import flash.mounts.common.network.status.ClientboundSendTranslatableStatusMessagePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundTryUnlockMountPacket {

  Mount selected;

  public ServerboundTryUnlockMountPacket(Mount selected) {
    this.selected = selected;
  }

  public static void encode(ServerboundTryUnlockMountPacket msg, FriendlyByteBuf buf) {
    buf.writeUtf(msg.selected.mob());
    buf.writeNbt(msg.selected.tag());
  }

  public static ServerboundTryUnlockMountPacket decode(FriendlyByteBuf buf) {
    return new ServerboundTryUnlockMountPacket(Mounts.getMountFromMob(buf.readUtf(), buf.readNbt()));
  }

  public static void handle(ServerboundTryUnlockMountPacket msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer sender = ctx.get().getSender();
      MountCapabilityProvider.IMountCapability cap = MountCapabilityProvider.getCapability(sender);
      LivingEntity mount = msg.selected.create();
      if (mount != null) {
        if (cap.tryUnlockMount(sender, msg.selected)) {
          Component unlockMessage = new TranslatableComponent("msg." + Main.MODID + ".unlocked_mount", mount.getName()).withStyle(ChatFormatting.GREEN);
          sender.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1f, 1f);
          PacketDispatcher.sendTo(new ClientboundSendTranslatableStatusMessagePacket(unlockMessage, true), sender);
          PacketDispatcher.sendTo(new ClientboundSetUnlockedMountsPacket(cap.getUnlockedMounts()), sender);
        }
      }
    });
    ctx.get().setPacketHandled(true);
  }
}
