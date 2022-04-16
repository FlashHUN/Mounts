package flash.mounts.mixin.common;

import flash.mounts.Main;
import flash.mounts.common.mount.Mounts;
import net.minecraft.commands.CommandSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Set;

@Mixin(Entity.class)
public abstract class EntityMixin extends net.minecraftforge.common.capabilities.CapabilityProvider<Entity> implements Nameable, EntityAccess, CommandSource, net.minecraftforge.common.extensions.IForgeEntity {
  @Shadow @Nullable public abstract Entity getFirstPassenger();

  @Shadow public abstract Set<String> getTags();

  @Shadow @Nullable public abstract Entity getControllingPassenger();

  protected EntityMixin(Class<Entity> baseClass) {
    super(baseClass);
  }

  private boolean isMount() {
    return this.getTags().contains(Mounts.MOUNT_TAG);
  }

  @Inject(method = "getControllingPassenger", at = @At("HEAD"), cancellable = true)
  public void getControllingPassenger(CallbackInfoReturnable<Entity> cir) {
    if (isMount()) cir.setReturnValue(this.getFirstPassenger());
  }

  @Inject(method = "isControlledByLocalInstance", at = @At("HEAD"), cancellable = true)
  public void isControlledByLocalInstance(CallbackInfoReturnable<Boolean> cir) {
    if (isMount()) cir.setReturnValue(Main.PROXY.isLocalPlayer(this.getControllingPassenger()));
  }

  @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
  public void interact(Player p_19978_, InteractionHand p_19979_, CallbackInfoReturnable<InteractionResult> cir) {
    if (isMount()) cir.setReturnValue(InteractionResult.PASS);
  }

  @Inject(method = "interactAt", at = @At("HEAD"), cancellable = true)
  public void interactAt(Player p_19980_, Vec3 p_19981_, InteractionHand p_19982_, CallbackInfoReturnable<InteractionResult> cir) {
    if (isMount()) cir.setReturnValue(InteractionResult.PASS);
  }

  @Inject(method = "fireImmune", at = @At("HEAD"), cancellable = true)
  public void fireImmune(CallbackInfoReturnable<Boolean> cir) {
    if (isMount()) cir.setReturnValue(true);
  }

  @Inject(method = "getRemainingFireTicks", at = @At("HEAD"), cancellable = true)
  public void getRemainingFireTicks(CallbackInfoReturnable<Integer> cir) {
    if (isMount()) cir.setReturnValue(0);
  }
}
