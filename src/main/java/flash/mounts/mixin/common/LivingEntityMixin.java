package flash.mounts.mixin.common;

import flash.mounts.Main;
import flash.mounts.common.capability.MountCapabilityProvider;
import flash.mounts.common.mount.Mounts;
import flash.mounts.common.mount.Mounts.Mount;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@SuppressWarnings("ALL")
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

  // region Shadows
  @Shadow public float yHeadRot;

  @Shadow @Nullable public abstract AttributeInstance getAttribute(Attribute p_21052_);
  @Shadow public abstract void calculateEntityAnimation(LivingEntity p_21044_, boolean p_21045_);
  @Shadow public abstract void setSpeed(float p_21320_);
  @Shadow public abstract double getAttributeValue(Attribute p_21134_);
  @Shadow protected abstract SoundEvent getFallDamageSound(int p_21313_);
  @Shadow protected abstract int calculateFallDamage(float p_21237_, float p_21238_);
  @Shadow protected abstract void playBlockFallSound();
  @Shadow protected abstract void jumpFromGround();
  @Shadow public abstract boolean isAlive();
  // endregion Shadows

  private boolean isMountDeath;

  public LivingEntityMixin(EntityType<?> type, Level level) {
    super(type, level);
  }

  // region Helper Methods
  private void setSpeedAttribute(float value) {
    this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(value);
  }

  private boolean isMount() {
    return this.getTags().contains(Mounts.MOUNT_TAG);
  }

  public boolean isFlying() {
    return ((LivingEntity)(Object)this) instanceof FlyingAnimal || ((LivingEntity)(Object)this) instanceof FlyingMob;
  }
  // endregion Helper Methods

  // region Injected Methods

  @ModifyArg(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;travel(Lnet/minecraft/world/phys/Vec3;)V"))
  private Vec3 getTravelVector(Vec3 vec3) {
    if (this.isAlive()) {
      if (this.isVehicle() && isMount()) {
        LivingEntity driver = (LivingEntity) this.getControllingPassenger();
        if (driver != null) {
          boolean isFlying = isFlying();
          double moveSideways = driver.xxa * 0.5F;
          double moveY = (float) vec3.y;
          double moveForward = driver.zza;

          // We're doing this instead of isControlledByLocalInstance() because it just doesn't want to work
          //  even after I used a mixin on it and I can't be arsed to fix it
          if (Main.PROXY.isLocalPlayer(driver)) {
            boolean isDriverJumping = driver.jumping;

            if (isFlying) {
              moveForward = moveForward > 0 ? moveForward : 0;
              if (isDriverJumping) {
                moveY = 1;
              } else {
                moveY = Main.PROXY.shouldDescend((Player)driver) ? -1 : 0;
              }
              moveSideways = 0;
            } else {
              if (moveForward <= 0.0F) moveForward *= 0.5F;
            }

            vec3 = new Vec3(moveSideways, moveY, moveForward);
          }
        }
      }
    }
    return vec3;
  }

  // A lot of this came from https://github.com/Kay9Unit/Dragon-Mounts-Legacy/blob/master/src/main/java/com/github/kay9/dragonmounts/dragon/TameableDragon.java
  // Thank you Kay9Unit for being smarter than me
  @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
  public void travel(Vec3 vec3, CallbackInfo ci) {
    if (this.isAlive()) {
      if (this.isVehicle() && isMount()) {
        LivingEntity driver = (LivingEntity) this.getControllingPassenger();
        if (driver != null) {
          boolean isFlying = isFlying();
          if (driver instanceof Player player) {
            Mount mount = MountCapabilityProvider.getCapability(player).getActiveMount();
            if (mount != null && mount.speed().isPresent()) {
              this.setSpeedAttribute(mount.speed().get());
            }
          }
          float speed = (float) (this.getAttributeValue(Attributes.MOVEMENT_SPEED) * (isFlying() ? Mounts.FLYING_SPEED_FACTOR : Mounts.SPEED_FACTOR));
          this.maxUpStep = Math.max(maxUpStep, 1f);
          this.yHeadRot = driver.yHeadRot;
          this.setXRot(driver.getXRot() * 0.5F);
          this.setYRot(driver.getYRot());

          if (Main.PROXY.isLocalPlayer(driver)) {
            if (driver.jumping && this.onGround) {
              jumpFromGround();
            }

            this.setSpeed(speed);

            this.tryCheckInsideBlocks();
          } else if (driver instanceof Player) {
            calculateEntityAnimation((LivingEntity) (Object) this, false);
            setDeltaMovement(Vec3.ZERO);
          }

          if (isFlying) {
            // Move relative to yaw
            moveRelative(speed, vec3);
            move(MoverType.SELF, getDeltaMovement());
            if (getDeltaMovement().lengthSqr() < 0.1) // we're not actually going anywhere, bob up and down.
              setDeltaMovement(getDeltaMovement().add(0, Math.sin(tickCount / 4f) * 0.03, 0));
            setDeltaMovement(getDeltaMovement().scale(0.9f)); // smoothly slow down

            calculateEntityAnimation((LivingEntity)(Object)this, true);
            ci.cancel();
          }
        }
      }
    }
  }

  @Inject(method = "tick", at = @At("TAIL"))
  public void tick(CallbackInfo ci) {
    if (this.getPassengers().isEmpty() || this.isInWater() || this.isInLava()) {
      if (this.removeTag(Mounts.MOUNT_TAG)) {
        this.isMountDeath = true;
        this.setInvisible(true);
        this.kill();
      }
    }
  }

  @Inject(method = "isEffectiveAi", at = @At("HEAD"), cancellable = true)
  public void isEffectiveAiMixin(CallbackInfoReturnable<Boolean> cir) {
    if (isMount()) cir.setReturnValue(Main.PROXY.isLocalPlayer(this.getControllingPassenger()));
  }

  @Inject(method = "getJumpPower", at = @At("RETURN"), cancellable = true)
  public void getJumpPower(CallbackInfoReturnable<Float> cir) {
    if (isMount() && isFlying()) cir.setReturnValue(cir.getReturnValue() * 3);
  }

  @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
  public void isPushable(CallbackInfoReturnable<Boolean> cir) {
    if (isMount()) cir.setReturnValue(false);
  }

  @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
  public void causeFallDamage(float height, float damageFactor, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
    if (isMount()) {
      if (!isFlying()) {
        int i = this.calculateFallDamage(height, damageFactor);
        if (i > 0) {
          this.playSound(this.getFallDamageSound(i), 1.0F, 1.0F);
          this.playBlockFallSound();
          if (this.isVehicle()) {
            for (Entity entity : this.getIndirectPassengers()) {
              entity.hurt(source, (float) i);
            }
          }
        }
      }
      cir.setReturnValue(false);
    }
  }

  @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
  public void dropAllDeathLoot(DamageSource source, CallbackInfo ci) {
    if (isMountDeath) ci.cancel();
  }

  @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
  public void hurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
    if (isMount()) cir.setReturnValue(false);
  }
  // endregion Injected Methods
}
