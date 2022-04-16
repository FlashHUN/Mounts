package flash.mounts.common.mount;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import flash.mounts.Main;
import flash.mounts.common.capability.MountCapabilityProvider;
import net.minecraft.commands.arguments.MobEffectArgument;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class Abilities {

  private static final Map<String, Ability> ABILITIES = new HashMap<>();

  public static Ability getAbility(String name) {
    return ABILITIES.get(name);
  }

  public static void clear() {
    ABILITIES.clear();
  }

  public static void registerAbility(JsonObject json) {
    try {
      String name = json.get("name").getAsString();
      String type = json.get("type").getAsString();
      int chargeTicks = json.has("charge_time") ? json.get("charge_time").getAsInt() : 0;
      int cooldown = json.has("cooldown") ? json.get("cooldown").getAsInt() : 0;
      Ability ability;
      switch (type) {
        case "fireball" -> {
          int explosionPower = json.has("explosion_power") ? json.get("explosion_power").getAsInt() : 1;
          double speed = json.has("speed") ? json.get("speed").getAsDouble() : 0.01;
          ability = new FireballAbility(chargeTicks, cooldown, explosionPower, speed);
        }
        case "mount_effect" -> {
          String effect = json.has("effect") ? json.get("effect").getAsString() : null;
          int duration = json.has("duration") ? json.get("duration").getAsInt() : 1;
          int amplifier = json.has("amplifier") ? json.get("amplifier").getAsInt() : 0;
          ability = new MountPotionEffectAbility(chargeTicks, cooldown, effect, duration, amplifier);
        }
        case "player_effect" -> {
          String effect = json.has("effect") ? json.get("effect").getAsString() : null;
          int duration = json.has("duration") ? json.get("duration").getAsInt() : 1;
          int amplifier = json.has("amplifier") ? json.get("amplifier").getAsInt() : 0;
          ability = new PlayerPotionEffectAbility(chargeTicks, cooldown, effect, duration, amplifier);
        }
        default -> ability = null;
      }
      if (ability != null) {
        ABILITIES.put(name, ability);
        Main.LOGGER.debug("Abilities#registerAbility: Registered Ability " + name);
      } else {
        Main.LOGGER.warn("Abilities#registerAbility: Ability "+name+" has invalid type, skipping it");
      }
    } catch (Exception e) {
      Main.LOGGER.warn("Abilities#registerAbility: Ability has invalid formatting, skipping it");
    }
  }

  public static abstract class Ability {

    private final int chargeTicks;
    private final int cooldownTicks;

    public Ability(int chargeTicks, int cooldownTicks) {
      this.chargeTicks = chargeTicks;
      this.cooldownTicks = cooldownTicks;
    }

    public int getChargeTicks() {
      return chargeTicks;
    }

    public int getCooldownTicks() {
      return cooldownTicks;
    }

    public void use(Player player) {
      if (getPlayerMount(player) != null) {
        MountCapabilityProvider.IMountCapability cap = MountCapabilityProvider.getCapability(player);
        if (cap.getAbilityCooldown() == 0) {
          onUse(player);
          cap.cooldownAbility(this);
        }
      }
    }

    protected abstract void onUse(Player player);

    @Nullable
    protected LivingEntity getPlayerMount(Player player) {
      if (player != null && player.isPassenger() && player.getVehicle().getTags().contains(Mounts.MOUNT_TAG)) {
        return (LivingEntity) player.getVehicle();
      }
      return null;
    }

  }

  static class FireballAbility extends Ability {
    int explosionPower;
    double speed;

    public FireballAbility(int chargeTicks, int cooldownTicks, int explosionPower, double speed) {
      super(chargeTicks, cooldownTicks);
      this.explosionPower = explosionPower;
      this.speed = speed;
    }

    @Override
    public void onUse(Player player) {
      LivingEntity livingentity = getPlayerMount(player);
      Vec3 lookVec = livingentity.getViewVector(1.0F);
      double x = livingentity.getX() + lookVec.x * 4.0D;
      double y = 0.5D + livingentity.getY(0.5D);
      double z = livingentity.getZ() + lookVec.z * 4.0D;

      LargeFireball largefireball = new LargeFireball(livingentity.level, livingentity, lookVec.x * speed, lookVec.y * speed, lookVec.z * speed, explosionPower);
      largefireball.setPos(x, y, z);
      livingentity.level.addFreshEntity(largefireball);
    }
  }

  static class MountPotionEffectAbility extends Ability {

    int duration, amplifier;
    MobEffect effect;

    public MountPotionEffectAbility(int chargeTicks, int cooldownTicks, String effect, int duration, int amplifier) throws CommandSyntaxException {
      this(chargeTicks, cooldownTicks, MobEffectArgument.effect().parse(new StringReader(effect)), duration, amplifier);
    }

    public MountPotionEffectAbility(int chargeTicks, int cooldownTicks, MobEffect effect, int duration, int amplifier) {
      super(chargeTicks, cooldownTicks);
      this.duration = duration;
      this.amplifier = amplifier;
      this.effect = effect;
    }

    @Override
    protected void onUse(Player player) {
      LivingEntity livingentity = getPlayerMount(player);
      livingentity.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false));
    }
  }

  static class PlayerPotionEffectAbility extends MountPotionEffectAbility {

    public PlayerPotionEffectAbility(int chargeTicks, int cooldownTicks, String effect, int duration, int amplifier) throws CommandSyntaxException {
      super(chargeTicks, cooldownTicks, effect, duration, amplifier);
    }

    public PlayerPotionEffectAbility(int chargeTicks, int cooldownTicks, MobEffect effect, int duration, int amplifier) {
      super(chargeTicks, cooldownTicks, effect, duration, amplifier);
    }

    @Override
    protected void onUse(Player player) {
      player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false));
    }
  }
}
