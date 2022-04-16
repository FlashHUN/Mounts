package flash.mounts.common.capability;

import flash.mounts.Main;
import flash.mounts.common.mount.Abilities.Ability;
import flash.mounts.common.mount.Mounts.Mount;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;

public class MountCapabilityProvider implements ICapabilitySerializable<CompoundTag> {

  public interface IMountCapability extends INBTSerializable<CompoundTag> {
    Mount getActiveMount();
    void setActiveMount(Mount mount);
    HashSet<Mount> getUnlockedMounts();
    void setUnlockedMounts(HashSet<Mount> mounts);
    HashSet<Mount> getFavoriteMounts();
    void setFavoriteMounts(HashSet<Mount> mounts);
    int getMountSummonTimer();
    void setMountSummonTimer(int i);
    void tickSummonMount();
    boolean shouldSummonMount();
    void setShouldSummonMount(boolean shouldSummon);
    boolean unlockMount(Mount mount);
    boolean tryUnlockMount(Player player, Mount mount);
    boolean favoriteMount(Mount mount);
    boolean unfavoriteMount(Mount mount);
    int getAbilityCooldown();
    int getAbilityChargeTime();
    void setAbilityCooldown(int cooldown);
    void setAbilityChargeTime(int chargeTime);
    void tickAbilityCooldown();
    void tickAbilityChargeTime();
    void chargeAbility(Ability ability);
    void cooldownAbility(Ability ability);
    boolean isDescending();
    void setDescending(boolean b);
  }

  public static final Capability<IMountCapability> MOUNT_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
  public static final ResourceLocation IDENTIFIER = new ResourceLocation(Main.MODID, "mounts");

  private LazyOptional<IMountCapability> instance = LazyOptional.of(MountCapability::new);

  @NotNull
  @Override
  public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
    if (cap == MOUNT_CAPABILITY)
      return this.instance.cast();

    return LazyOptional.empty();
  }

  @Override
  public CompoundTag serializeNBT() {
    return this.instance.map(IMountCapability::serializeNBT).orElse(new CompoundTag());
  }

  @Override
  public void deserializeNBT(CompoundTag nbt) {
    this.instance.ifPresent(capability -> capability.deserializeNBT(nbt));
  }

  public static IMountCapability getCapability(Player player) {
    return player.getCapability(MOUNT_CAPABILITY).orElse(new MountCapability());
  }
}
