package flash.mounts.mixin.common;

import flash.mounts.common.mount.Mounts;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlyingMob.class)
public class FlyingMobMixin extends Mob {

  protected FlyingMobMixin(EntityType<? extends Mob> p_21368_, Level p_21369_) {
    super(p_21368_, p_21369_);
  }

  private boolean isMount() {
    return this.getTags().contains(Mounts.MOUNT_TAG);
  }

  @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
  public void travel(Vec3 vec3, CallbackInfo ci) {
    if (isMount()) {
      super.travel(vec3);
      ci.cancel();
    }
  }

}
