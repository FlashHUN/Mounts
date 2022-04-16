package flash.mounts.common.event;

import flash.mounts.Main;
import flash.mounts.common.block.entity.MountBenchBlockEntity;
import flash.mounts.common.capability.MountCapabilityProvider;
import flash.mounts.common.capability.MountCapabilityProvider.IMountCapability;
import flash.mounts.common.config.ConfigHolder;
import flash.mounts.common.mount.Abilities;
import flash.mounts.common.mount.Mounts;
import flash.mounts.common.network.PacketDispatcher;
import flash.mounts.common.network.game.ClientboundSetActiveMountPacket;
import flash.mounts.common.network.game.ClientboundSetFavoriteMountsPacket;
import flash.mounts.common.network.join.ClientboundSetCommonConfigPacket;
import flash.mounts.common.network.game.ClientboundSetUnlockedMountsPacket;
import flash.mounts.common.network.status.ClientboundSetAbilityChargeTimePacket;
import flash.mounts.common.network.status.ClientboundSetAbilityCooldownPacket;
import flash.mounts.common.network.status.ClientboundSetMountTagPacket;
import flash.mounts.common.network.status.ClientboundSetMountTimerPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;

import static flash.mounts.common.capability.MountCapability.matches;

@Mod.EventBusSubscriber(modid = Main.MODID)
public class ForgeEvents {

  private static MountBenchBlockEntity prevMountBenchBlockEntity;

  // region Events
  @SubscribeEvent(priority = EventPriority.HIGHEST)
  public static void onWorldLoad(WorldEvent.Load event) {
    Main.LOGGER.debug("ForgeEvents#onWorldLoad");
    ConfigHolder.COMMON.reloadConfig();
    ConfigHolder.COMMON.loadAllAbilities();
    Mounts.MOUNTS = ConfigHolder.COMMON.loadAllMounts();
  }

  // region Syncing Capabilities
  @SubscribeEvent
  public static void attach(AttachCapabilitiesEvent<Entity> event) {
    if (event.getObject() instanceof Player) {
      event.addCapability(MountCapabilityProvider.IDENTIFIER, new MountCapabilityProvider());
    }
  }

  @SubscribeEvent
  public static void playerRespawn(PlayerEvent.PlayerRespawnEvent event) {
    syncCapability(event.getPlayer());
  }

  @SubscribeEvent
  public static void playerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
    syncCapability(event.getPlayer());
  }

  @SubscribeEvent
  public static void playerClone(PlayerEvent.Clone event) {
    //if (event.getOriginal().level.isClientSide) return;

    event.getOriginal().reviveCaps();
    IMountCapability oldCap = MountCapabilityProvider.getCapability(event.getOriginal());
    IMountCapability newCap = MountCapabilityProvider.getCapability(event.getPlayer());

    newCap.deserializeNBT(oldCap.serializeNBT());
    event.getOriginal().invalidateCaps();
  }

  @SubscribeEvent
  public static void serverLoginEvent(PlayerEvent.PlayerLoggedInEvent event) {
    Player player = event.getPlayer();
    syncCapability(player);
    if (player != null) {
      if (player.isAlive() && player.isPassenger() && player.getVehicle().getTags().contains(Mounts.MOUNT_TAG)) {
        PacketDispatcher.sendTo(new ClientboundSetMountTagPacket(), player);
      }
      PacketDispatcher.sendTo(new ClientboundSetCommonConfigPacket(ConfigHolder.COMMON.getConfig().getAsJsonObject()), player);
    }
  }
  // endregion Syncing Capabilities

  @SubscribeEvent
  public static void playerTickEvent(TickEvent.PlayerTickEvent event) {
    if (event.player != null && event.player.isAlive()) {
      if (event.side == LogicalSide.SERVER) {
        IMountCapability cap = MountCapabilityProvider.getCapability(event.player);

        if (cap.getMountSummonTimer() > 0) {
          cap.tickSummonMount();
          PacketDispatcher.sendTo(new ClientboundSetMountTimerPacket(cap.getMountSummonTimer()), event.player);
        } else if (cap.shouldSummonMount()) {
          cap.setShouldSummonMount(false);
          Mounts.summonMount((ServerPlayer) event.player);
        }

        if (cap.getAbilityChargeTime() == 0) {
          Mounts.Mount mount = cap.getActiveMount();
          if (mount != null && event.player.isPassenger() && event.player.getVehicle().getTags().contains(Mounts.MOUNT_TAG)) {
            Abilities.Ability ability = mount.ability();
            if (ability != null) {
              ability.use(event.player);
            }
          }
        }
        tickAbilityChargeTime(cap, event.player);
        tickAbilityCooldown(cap, event.player);
      }

      if (event.side == LogicalSide.CLIENT) {
        HitResult hitResult = event.player.pick(10.0D, 1f, false);
        if (hitResult.getType().equals(HitResult.Type.BLOCK)) {
          BlockHitResult lookingAt = (BlockHitResult) hitResult;
          BlockPos pos = lookingAt.getBlockPos();
          if (event.player.level.getBlockEntity(pos) instanceof MountBenchBlockEntity mountBenchBlockEntity) {
            prevMountBenchBlockEntity = mountBenchBlockEntity;
            ItemStack heldItem = event.player.getMainHandItem();
            boolean found = false;
            for (ItemStack stack : Mounts.ITEM_TO_MOUNT_MAP.keySet()) {
              if (matches(stack, heldItem)) {
                mountBenchBlockEntity.mount = Mounts.ITEM_TO_MOUNT_MAP.get(stack);
                found = true;
                break;
              }
            }
            if (!found) {
              mountBenchBlockEntity.mount = null;
            }
          } else if (prevMountBenchBlockEntity != null) {
            prevMountBenchBlockEntity.mount = null;
            prevMountBenchBlockEntity = null;
          }
        } else if (prevMountBenchBlockEntity != null) {
          prevMountBenchBlockEntity.mount = null;
          prevMountBenchBlockEntity = null;
        }
      }
    }
  }

  @SubscribeEvent
  public static void mobGriefingEvent(EntityMobGriefingEvent event) {
    if (event.getEntity() instanceof LivingEntity livingEntity) {
      if (livingEntity.getTags().contains(Mounts.MOUNT_TAG)
              || (livingEntity instanceof Player player && player.isPassenger() &&
                  player.getVehicle().getTags().contains(Mounts.MOUNT_TAG))
      ) {
        event.setResult(Event.Result.DENY);
      }
    }
  }
  // endregion Events

  // region Helper Methods
  private static void tickAbilityChargeTime(IMountCapability cap, Player player) {
    int prevChargeTime = cap.getAbilityChargeTime();
    cap.tickAbilityChargeTime();
    if (prevChargeTime != cap.getAbilityChargeTime())
      PacketDispatcher.sendTo(new ClientboundSetAbilityChargeTimePacket(cap.getAbilityChargeTime()), player);
  }

  private static void tickAbilityCooldown(IMountCapability cap, Player player) {
    int prevCooldown = cap.getAbilityCooldown();
    cap.tickAbilityCooldown();
    if (prevCooldown != cap.getAbilityCooldown())
      PacketDispatcher.sendTo(new ClientboundSetAbilityCooldownPacket(cap.getAbilityCooldown()), player);
  }

  private static void syncCapability(Player player) {
    if (player != null && player.isAlive()) {
      IMountCapability cap = MountCapabilityProvider.getCapability(player);

      String playerName = player.getName().getString();
      Mounts.Mount active = cap.getActiveMount();
      Main.LOGGER.debug("ForgeEvents#syncCapability: " + playerName + "'s Active Mount is " + active);
      Main.LOGGER.debug("ForgeEvents#syncCapability: " + playerName + "'s Unlocked Mounts:");
      HashSet<Mounts.Mount> unlocked = cap.getUnlockedMounts();
      for (Mounts.Mount mount : unlocked) {
        Main.LOGGER.debug(mount.toString());
      }
      Main.LOGGER.debug("ForgeEvents#syncCapability: " + playerName + "'s Favorite Mounts:");
      HashSet<Mounts.Mount> favorites = cap.getFavoriteMounts();
      for (Mounts.Mount mount : favorites) {
        Main.LOGGER.debug(mount.toString());
      }

      PacketDispatcher.sendTo(new ClientboundSetActiveMountPacket(cap.getActiveMount()), player);
      PacketDispatcher.sendTo(new ClientboundSetUnlockedMountsPacket(unlocked), player);
      PacketDispatcher.sendTo(new ClientboundSetFavoriteMountsPacket(favorites), player);
    }
  }
  // endregion Helper Methods

}
