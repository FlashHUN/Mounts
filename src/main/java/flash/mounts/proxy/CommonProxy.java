package flash.mounts.proxy;

import com.google.gson.JsonObject;
import flash.mounts.Main;
import flash.mounts.common.KeyBindings;
import flash.mounts.common.capability.MountCapability;
import flash.mounts.common.capability.MountCapabilityProvider;
import flash.mounts.common.mount.Mounts.Mount;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashSet;

public class CommonProxy {

  public void sync(HashSet<Mount> unlockedMounts) {}
  public void syncFavoriteMounts(HashSet<Mount> favorites) {}
  public void syncActiveMount(Mount activeMount) {}
  public void syncMountTag() {}
  public void syncMountTimer(int timer) {}
  public void syncCommonConfig(JsonObject config) {}
  public void setScreen(KeyBindings.Screens screen) {}
  public void syncAbilityCooldown(int timer) {}
  public void syncAbilityChargeTime(int timer) {}
  public void receiveTranslatableMessage(Component component, boolean actionBar) {}

  public Level getLevel() {
    return ServerLifecycleHooks.getCurrentServer().overworld();
  }

  public boolean isLocalPlayer(Entity entity) {
    return true;
  }

  public boolean shouldDescend(Player player) {
    return MountCapabilityProvider.getCapability(player).isDescending();
  }
}
