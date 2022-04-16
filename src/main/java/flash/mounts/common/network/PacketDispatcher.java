package flash.mounts.common.network;

import flash.mounts.Main;
import flash.mounts.common.network.game.*;
import flash.mounts.common.network.join.*;
import flash.mounts.common.network.status.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketDispatcher {

  private static int packetId = 0;

  private static final String PROTOCOL_VERSION = "1";
  private static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
          new ResourceLocation(Main.MODID, "main"),
          () -> PROTOCOL_VERSION,
          PROTOCOL_VERSION::equals,
          PROTOCOL_VERSION::equals
  );

  public PacketDispatcher() {}

  public static int nextID() {
    return packetId++;
  }

  public static void registerMessages() {
    INSTANCE.registerMessage(nextID(), ClientboundSendTranslatableStatusMessagePacket.class, ClientboundSendTranslatableStatusMessagePacket::encode, ClientboundSendTranslatableStatusMessagePacket::decode, ClientboundSendTranslatableStatusMessagePacket::handle);
    INSTANCE.registerMessage(nextID(), ClientboundSetAbilityChargeTimePacket.class, ClientboundSetAbilityChargeTimePacket::encode, ClientboundSetAbilityChargeTimePacket::decode, ClientboundSetAbilityChargeTimePacket::handle);
    INSTANCE.registerMessage(nextID(), ClientboundSetAbilityCooldownPacket.class, ClientboundSetAbilityCooldownPacket::encode, ClientboundSetAbilityCooldownPacket::decode, ClientboundSetAbilityCooldownPacket::handle);
    INSTANCE.registerMessage(nextID(), ClientboundSetActiveMountPacket.class, ClientboundSetActiveMountPacket::encode, ClientboundSetActiveMountPacket::decode, ClientboundSetActiveMountPacket::handle);
    INSTANCE.registerMessage(nextID(), ClientboundSetFavoriteMountsPacket.class, ClientboundSetFavoriteMountsPacket::encode, ClientboundSetFavoriteMountsPacket::decode, ClientboundSetFavoriteMountsPacket::handle);
    INSTANCE.registerMessage(nextID(), ClientboundSetCommonConfigPacket.class, ClientboundSetCommonConfigPacket::encode, ClientboundSetCommonConfigPacket::decode, ClientboundSetCommonConfigPacket::handle);
    INSTANCE.registerMessage(nextID(), ClientboundSetUnlockedMountsPacket.class, ClientboundSetUnlockedMountsPacket::encode, ClientboundSetUnlockedMountsPacket::decode, ClientboundSetUnlockedMountsPacket::handle);
    INSTANCE.registerMessage(nextID(), ClientboundSetMountTagPacket.class, ClientboundSetMountTagPacket::encode, ClientboundSetMountTagPacket::decode, ClientboundSetMountTagPacket::handle);
    INSTANCE.registerMessage(nextID(), ClientboundSetMountTimerPacket.class, ClientboundSetMountTimerPacket::encode, ClientboundSetMountTimerPacket::decode, ClientboundSetMountTimerPacket::handle);

    INSTANCE.registerMessage(nextID(), ServerboundRequestMountCapabilityPacket.class, ServerboundRequestMountCapabilityPacket::encode, ServerboundRequestMountCapabilityPacket::decode, ServerboundRequestMountCapabilityPacket::handle);
    INSTANCE.registerMessage(nextID(), ServerboundSetActiveMountPacket.class, ServerboundSetActiveMountPacket::encode, ServerboundSetActiveMountPacket::decode, ServerboundSetActiveMountPacket::handle);
    INSTANCE.registerMessage(nextID(), ServerboundSetDescentStatusPacket.class, ServerboundSetDescentStatusPacket::encode, ServerboundSetDescentStatusPacket::decode, ServerboundSetDescentStatusPacket::handle);
    INSTANCE.registerMessage(nextID(), ServerboundSetFavoriteMountsPacket.class, ServerboundSetFavoriteMountsPacket::encode, ServerboundSetFavoriteMountsPacket::decode, ServerboundSetFavoriteMountsPacket::handle);
    INSTANCE.registerMessage(nextID(), ServerboundSummonMountPacket.class, ServerboundSummonMountPacket::encode, ServerboundSummonMountPacket::decode, ServerboundSummonMountPacket::handle);
    INSTANCE.registerMessage(nextID(), ServerboundTryUnlockMountPacket.class, ServerboundTryUnlockMountPacket::encode, ServerboundTryUnlockMountPacket::decode, ServerboundTryUnlockMountPacket::handle);
    INSTANCE.registerMessage(nextID(), ServerboundUseMountAbilityPacket.class, ServerboundUseMountAbilityPacket::encode, ServerboundUseMountAbilityPacket::decode, ServerboundUseMountAbilityPacket::handle);
  }

  public static <MSG> void sendTo(MSG msg, Player player) {
    INSTANCE.sendTo(msg, ((ServerPlayer)player).connection.getConnection(), NetworkDirection.PLAY_TO_CLIENT);
  }

  public static <MSG> void sendToAllTracking(MSG msg, LivingEntity entityToTrack) {
    INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> entityToTrack), msg);
  }

  public static <MSG> void sendToAll(MSG msg) {
    INSTANCE.send(PacketDistributor.ALL.noArg(), msg);
  }

  public static <MSG> void sendToServer(MSG msg) {
    INSTANCE.sendToServer(msg);
  }
}
