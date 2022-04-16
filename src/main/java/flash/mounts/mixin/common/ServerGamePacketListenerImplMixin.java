package flash.mounts.mixin.common;

import flash.mounts.common.mount.Mounts;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.FlyingAnimal;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

  @Shadow private boolean clientVehicleIsFloating;

  @Shadow private int aboveGroundVehicleTickCount;

  private boolean isEntityFlying(LivingEntity entity) {
    return entity.getTags().contains("flying");
  }

  private boolean isEntityMount(LivingEntity entity) {
    return entity.getTags().contains(Mounts.MOUNT_TAG);
  }

  // https://github.com/Kay9Unit/Dragon-Mounts-Legacy/blob/master/src/main/java/com/github/kay9/dragonmounts/mixins/EnsureSafeFlyingVehicleMixin.java
  @Redirect(method = "handleMoveVehicle", at = @At(value = "FIELD", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;clientVehicleIsFloating:Z", opcode = Opcodes.PUTFIELD))
  public void handleMoveVehicle(ServerGamePacketListenerImpl instance, boolean value) {
    clientVehicleIsFloating = (!(instance.getPlayer().getRootVehicle() instanceof FlyingAnimal a) || !a.isFlying())
            || (!(instance.getPlayer().getVehicle() instanceof FlyingMob mob) || !isEntityFlying(mob))
            && value;
  }

  @Redirect(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;aboveGroundVehicleTickCount:I", opcode = Opcodes.PUTFIELD))
  public void tick(ServerGamePacketListenerImpl instance, int value) {
    aboveGroundVehicleTickCount = instance.getPlayer().getVehicle() instanceof LivingEntity entity && isEntityMount(entity) ? 0 : value;
  }

}
