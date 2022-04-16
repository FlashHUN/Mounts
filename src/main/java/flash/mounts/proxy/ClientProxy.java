package flash.mounts.proxy;

import com.google.gson.JsonObject;
import flash.mounts.common.KeyBindings;
import flash.mounts.Main;
import flash.mounts.client.gui.screen.UnlockedMountsScreen;
import flash.mounts.common.capability.MountCapabilityProvider;
import flash.mounts.common.capability.MountCapabilityProvider.IMountCapability;
import flash.mounts.common.config.ConfigHolder;
import flash.mounts.common.mount.Mounts;
import flash.mounts.common.mount.Mounts.Mount;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashSet;

import static flash.mounts.client.ClientInit.minecraft;

public class ClientProxy extends CommonProxy {

  private IMountCapability getPlayerCapability() {
    return MountCapabilityProvider.getCapability(minecraft.player);
  }

  public void sync(HashSet<Mount> unlockedMounts) {
    Main.LOGGER.debug("ClientProxy#syncFavoriteMounts: Player's Unlocked Mounts:");
    getPlayerCapability().setUnlockedMounts(unlockedMounts);
    getPlayerCapability().getUnlockedMounts().forEach(mount -> Main.LOGGER.debug(mount.toString()));
  }

  public void syncFavoriteMounts(HashSet<Mount> favorites) {
    getPlayerCapability().setFavoriteMounts(favorites);
    Main.LOGGER.debug("ClientProxy#syncFavoriteMounts: Player's Favorite Mounts:");
    getPlayerCapability().getFavoriteMounts().forEach(mount -> Main.LOGGER.debug(mount.toString()));
  }

  public void syncActiveMount(Mount activeMount) {
    getPlayerCapability().setActiveMount(activeMount);
    Main.LOGGER.debug("ClientProxy#syncActiveMount: Player's Active Mount is " + getPlayerCapability().getActiveMount());
  }

  public void syncMountTag() {
    if (minecraft.player.isPassenger() && !minecraft.player.getVehicle().getTags().contains(Mounts.MOUNT_TAG)) {
      minecraft.player.getVehicle().addTag(Mounts.MOUNT_TAG);
    }
  }

  public void syncMountTimer(int timer) {
    IMountCapability cap = MountCapabilityProvider.getCapability(minecraft.player);
    cap.setMountSummonTimer(timer);
  }

  public void syncAbilityCooldown(int timer) {
    IMountCapability cap = MountCapabilityProvider.getCapability(minecraft.player);
    cap.setAbilityCooldown(timer);
  }

  public void syncAbilityChargeTime(int timer) {
    IMountCapability cap = MountCapabilityProvider.getCapability(minecraft.player);
    cap.setAbilityChargeTime(timer);
  }

  public void syncCommonConfig(JsonObject config) {
    ConfigHolder.COMMON.setConfig(config);
    ConfigHolder.COMMON.loadAllAbilities();
    Mounts.MOUNTS = ConfigHolder.COMMON.loadAllMounts();
  }

  public void setScreen(KeyBindings.Screens screen) {
    if (screen == KeyBindings.Screens.UNLOCKED_MOUNTS) {
      minecraft.setScreen(new UnlockedMountsScreen());
    }
  }

  public void receiveTranslatableMessage(Component component, boolean actionBar) {
    if (minecraft.player != null) {
      minecraft.player.displayClientMessage(component, actionBar);
    }
  }

  public Level getLevel() {
    return minecraft.level;
  }

  public boolean isLocalPlayer(Entity entity) {
    if (entity == null) return false;

    return minecraft.player != null && entity.getUUID().equals(minecraft.player.getUUID());
  }

  public boolean shouldDescend(Player player) {
    return KeyBindings.DESCENT.isDown();
  }
}
