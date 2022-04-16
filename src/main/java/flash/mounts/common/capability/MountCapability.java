package flash.mounts.common.capability;


import flash.mounts.Main;
import flash.mounts.common.mount.Abilities;
import flash.mounts.common.mount.Mounts;
import flash.mounts.common.mount.Mounts.Mount;
import flash.mounts.common.network.game.ServerboundSummonMountPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.HashSet;

public class MountCapability implements MountCapabilityProvider.IMountCapability {

  private Mount activeMount;
  private HashSet<Mount> mounts;
  private HashSet<Mount> favoriteMounts;
  private int mountSummonTimer;
  private boolean shouldSummonMount;
  private int abilityCooldown;
  private int abilityChargeTime;
  private boolean isDescending;

  public MountCapability() {
    activeMount = null;
    mounts = new HashSet<>();
    favoriteMounts = new HashSet<>();
    mountSummonTimer = 0;
    shouldSummonMount = false;
    abilityCooldown = 0;
    abilityChargeTime = 0;
    isDescending = false;
  }

  @Override
  public Mount getActiveMount() {
    return activeMount;
  }

  @Override
  public void setActiveMount(Mount mount) {
    activeMount = mount;
    Main.LOGGER.debug("MountCapability#setActiveMount: " + mount);
  }

  @Override
  public HashSet<Mount> getUnlockedMounts() {
    return mounts;
  }

  @Override
  public void setUnlockedMounts(HashSet<Mount> mounts) {
    this.mounts = mounts;
    Main.LOGGER.debug("MountCapability#setUnlockedMounts:");
    for (Mount mount : mounts) {
      Main.LOGGER.debug(String.valueOf(mount));
    }
  }

  @Override
  public HashSet<Mount> getFavoriteMounts() {
    return favoriteMounts;
  }

  @Override
  public void setFavoriteMounts(HashSet<Mount> mounts) {
    this.favoriteMounts = mounts;
    Main.LOGGER.debug("MountCapability#setFavoriteMounts:");
    for (Mount mount : mounts) {
      Main.LOGGER.debug(String.valueOf(mount));
    }
  }

  @Override
  public int getMountSummonTimer() {
    return mountSummonTimer;
  }

  @Override
  public void setMountSummonTimer(int i) {
    mountSummonTimer = Mth.clamp(i, 0, ServerboundSummonMountPacket.MAX_SUMMON_TIMER);
  }

  @Override
  public void tickSummonMount() {
    setMountSummonTimer(mountSummonTimer-1);
  }

  @Override
  public boolean shouldSummonMount() {
    return shouldSummonMount;
  }

  @Override
  public void setShouldSummonMount(boolean shouldSummon) {
    shouldSummonMount = shouldSummon;
  }

  @Override
  public boolean unlockMount(Mount mount) {
    return this.mounts.add(mount);
  }

  @Override
  public boolean tryUnlockMount(Player player, Mount mount) {
    for (ItemStack stack : mount.item()) {
      if (hasAmount(player, stack)) {
        boolean canUnlock = unlockMount(mount);
        if (canUnlock && !player.isCreative()) {
          takeStack(player, stack, stack.getCount());
        }
        return canUnlock;
      }
    }

    return false;
  }

  @Override
  public boolean favoriteMount(Mount mount) {
    if (!mounts.contains(mount)) return false;
    return favoriteMounts.add(mount);
  }

  @Override
  public boolean unfavoriteMount(Mount mount) {
    return favoriteMounts.remove(mount);
  }

  @Override
  public int getAbilityCooldown() {
    return abilityCooldown;
  }

  @Override
  public int getAbilityChargeTime() {
    return abilityChargeTime;
  }

  @Override
  public void setAbilityCooldown(int cooldown) {
    abilityCooldown = cooldown;
  }

  @Override
  public void setAbilityChargeTime(int chargeTime) {
    abilityChargeTime = chargeTime;
  }

  @Override
  public void tickAbilityCooldown() {
    if (abilityCooldown > 0)
      abilityCooldown--;
  }

  @Override
  public void tickAbilityChargeTime() {
    if (abilityChargeTime >= 0)
      abilityChargeTime--;
  }

  @Override
  public void chargeAbility(Abilities.Ability ability) {
    this.abilityChargeTime = ability.getChargeTicks();
  }

  @Override
  public void cooldownAbility(Abilities.Ability ability) {
    this.abilityCooldown = ability.getCooldownTicks();
  }

  @Override
  public boolean isDescending() {
    return isDescending;
  }

  @Override
  public void setDescending(boolean b) {
    isDescending = b;
  }

  public static boolean matches(ItemStack stack1, ItemStack stack2) {
    return ItemStack.isSame(stack1, stack2) && ItemStack.tagMatches(stack1, stack2);
  }

  public static boolean hasAmount(Player sender, ItemStack itemStackIn) {
    if (itemStackIn.isEmpty()) return true;

    int neededCount = itemStackIn.getCount();
    int currentCount = 0;
    for(ItemStack itemStack : sender.getInventory().items) {
      if (matches(itemStackIn, itemStack))
        currentCount += itemStack.getCount();

      if (currentCount >= neededCount) return true;
    }

    return false;
  }

  public static int getAmount(Player sender, ItemStack itemStackIn) {
    if (itemStackIn.isEmpty()) return 0;

    int currentCount = 0;
    for(ItemStack itemStack : sender.getInventory().items) {
      if (matches(itemStackIn, itemStack))
        currentCount += itemStack.getCount();
    }

    return currentCount;
  }

  private static void takeStack(Player player, ItemStack itemStack, int neededCount) {
    if (itemStack.isEmpty()) return;

    for (ItemStack stack : player.getInventory().items) {
      if (matches(itemStack, stack)) {
        int count = stack.getCount();
        stack.shrink(neededCount);
        neededCount -= count;
      }

      if (neededCount <= 0) {
        break;
      }
    }
  }

  @Override
  public CompoundTag serializeNBT() {
    CompoundTag tag = new CompoundTag();

    Main.LOGGER.debug("MountCapability#serializeNBT: activeMount is " + (getActiveMount() == null ? "null" : getActiveMount().toString()));
    if (getActiveMount() != null) {
      tag.putString("activeMount", getActiveMount().mob());
      tag.put("activeMountTag", getActiveMount().tag());
    } else {
      tag.putString("activeMount", "");
      tag.put("activeMountTag", new CompoundTag());
    }

    Main.LOGGER.debug("MountCapability#serializeNBT: Writing Unlocked Mounts");
    tag.put("mounts", writeMountSetToTag(mounts));
    Main.LOGGER.debug("MountCapability#serializeNBT: Writing Favorite Mounts");
    tag.put("favorites", writeMountSetToTag(favoriteMounts));

    return tag;
  }

  private ListTag writeMountSetToTag(HashSet<Mount> set) {
    ListTag returnTag = new ListTag();
    for (Mount mount : set) {
      CompoundTag tag1 = new CompoundTag();
      tag1.putString("mount", mount.mob());
      tag1.put("tag", mount.tag());
      Main.LOGGER.debug("MountCapability#writeMountSetToTag: " + mount);
      returnTag.add(tag1);
    }
    return returnTag;
  }

  @Override
  public void deserializeNBT(CompoundTag tag) {
    String activeMountString = tag.getString("activeMount");
    CompoundTag activeMountTag = tag.getCompound("activeMountTag");
    Main.LOGGER.debug("MountCapability#deserializeNBT: activeMount is " + activeMountString);
    Main.LOGGER.debug("MountCapability#deserializeNBT: activeMountTag is " + activeMountTag.getAsString());

    setActiveMount(Mounts.getMountFromMob(activeMountString, activeMountTag));

    Main.LOGGER.debug("MountCapability#deserializeNBT: Reading Unlocked Mounts, " + tag.get("mounts"));
    setUnlockedMounts(readMountSetFromTag((ListTag) tag.get("mounts")));
    Main.LOGGER.debug("MountCapability#deserializeNBT: Reading Favorite Mounts, " + tag.get("favorites"));
    setFavoriteMounts(readMountSetFromTag((ListTag) tag.get("favorites")));
  }

  private HashSet<Mount> readMountSetFromTag(@Nullable ListTag tag) {
    HashSet<Mount> returnSet = new HashSet<>();
    if (tag != null) {
      for (int i = 0; i < tag.size(); i++) {
        CompoundTag tag1 = tag.getCompound(i);
        String mountString = tag1.getString("mount");
        CompoundTag mountTag = tag1.getCompound("tag");
        Main.LOGGER.debug("MountCapability#readMountSetFromTag: " + mountString + ", " + mountTag.getAsString());

        Mount mount = Mounts.getMountFromMob(mountString, mountTag);
        if (mount != null) {
          returnSet.add(mount);
        }
      }
    }
    return returnSet;
  }
}