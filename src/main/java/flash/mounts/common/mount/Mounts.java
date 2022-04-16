package flash.mounts.common.mount;

import flash.mounts.Main;
import flash.mounts.common.capability.MountCapabilityProvider;
import flash.mounts.common.mount.Abilities.Ability;
import flash.mounts.common.network.PacketDispatcher;
import flash.mounts.common.network.status.ClientboundSetMountTagPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.*;

public class Mounts {

  public static final float SPEED_FACTOR = 0.35f;
  public static final float FLYING_SPEED_FACTOR = 0.225f;

  public static final String MOUNT_TAG = "summonable_mount";
  public static final String FLYING_TAG = "flying";

  public record Mount(String mob, ItemStack[] item, Optional<Float> speed, @Nullable Ability ability, CompoundTag tag) {
    public static final HashMap<Mount, LivingEntity> CACHED_ENTITIES = new HashMap<>();

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Mount mount = (Mount) o;
      return mob.equals(mount.mob) && tag.equals(mount.tag);
    }

    @Override
    public int hashCode() {
      return Objects.hash(mob, tag);
    }

    @Override
    public String toString() {
      return mob() + tag().getAsString();
    }

    @Nullable
    public LivingEntity create() {
      if (CACHED_ENTITIES.containsKey(this)) {
        LivingEntity entity = CACHED_ENTITIES.get(this);
        entity.level = Main.PROXY.getLevel();
        return entity;
      }

      Optional<EntityType<?>> type = EntityType.byString(mob);
      if (type.isPresent()) {
        CompoundTag mobTag = tag.copy();
        mobTag.putString("id", mob);
        LivingEntity entity = (LivingEntity) EntityType.loadEntityRecursive(mobTag, Main.PROXY.getLevel(), mob -> mob);
        CACHED_ENTITIES.put(this, entity);
        return entity;
      }

      return null;
    }
  }

  public static HashSet<Mount> MOUNTS = new HashSet<>();
  public static HashMap<ItemStack, Mount> ITEM_TO_MOUNT_MAP = new HashMap<>();

  @Nullable
  public static Mount getMountFromMob(String mob, CompoundTag tag) {
    if (mob == null || tag == null) return null;
    if (mob.isEmpty()) return null;

    for (Mount mount : MOUNTS) {
      if (mount.mob().equals(mob) && mount.tag().equals(tag)) {
        return mount;
      }
    }
    return null;
  }

  public static void summonMount(ServerPlayer player) {
    Mount mount = MountCapabilityProvider.getCapability(player).getActiveMount();
    if (mount != null) {
      Optional<EntityType<?>> type = EntityType.byString(mount.mob());
      if (type.isPresent()) {
        CompoundTag tag = mount.tag().copy();
        tag.putString("id", mount.mob());
        Entity entity = EntityType.loadEntityRecursive(tag, player.getLevel(), mob -> {
          mob.moveTo(player.getX(), player.getY(), player.getZ(), mob.getYRot(), mob.getXRot());
          return mob;
        });
        if (entity != null) {
          if (entity instanceof Mob mob) {
            if (mount.tag().isEmpty()) {
              mob.finalizeSpawn(player.getLevel(), player.getLevel().getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.COMMAND, null, null);
            }
            mob.goalSelector.removeAllGoals();
            mob.targetSelector.removeAllGoals();
          }

          if (player.getLevel().tryAddFreshEntityWithPassengers(entity)) {
            entity.setYRot(player.getYRot());
            entity.setXRot(player.getXRot());
            entity.addTag(MOUNT_TAG);
            player.startRiding(entity);
            PacketDispatcher.sendTo(new ClientboundSetMountTagPacket(), player);
          }
        }
      }
    }
  }
}
