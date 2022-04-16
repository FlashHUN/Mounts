package flash.mounts.mixin.common;

import flash.mounts.common.mount.Mounts;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobMixin extends LivingEntity {
  protected MobMixin(EntityType<? extends LivingEntity> p_20966_, Level p_20967_) {
    super(p_20966_, p_20967_);
  }

  private boolean isMount() {
    return this.getTags().contains(Mounts.MOUNT_TAG);
  }

  @Inject(method = "canBeControlledByRider", at = @At("HEAD"), cancellable = true)
  public void canBeControlledByRider(CallbackInfoReturnable<Boolean> cir) {
    if (isMount()) cir.setReturnValue(true);
  }

  @Inject(method = "shouldDespawnInPeaceful", at = @At("HEAD"), cancellable = true)
  public void shouldDespawnInPeaceful(CallbackInfoReturnable<Boolean> cir) {
    if (isMount()) cir.setReturnValue(false);
  }

  @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
  public void isSunBurnTick(CallbackInfoReturnable<Boolean> cir) {
    if (isMount()) cir.setReturnValue(false);
  }
}
