package flash.mounts.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import flash.mounts.common.block.entity.MountBenchBlockEntity;
import flash.mounts.common.mount.Mounts;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Arrays;

import static flash.mounts.client.ClientInit.minecraft;
import static flash.mounts.common.capability.MountCapability.getAmount;
import static flash.mounts.common.capability.MountCapability.matches;

@OnlyIn(Dist.CLIENT)
public class MountBenchBlockEntityRenderer implements BlockEntityRenderer<MountBenchBlockEntity> {

  private float currentRotation;

  private final BlockEntityRendererProvider.Context context;

  public MountBenchBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    this.context = context;
  }

  @Override
  public void render(MountBenchBlockEntity entity, float v, PoseStack stack, MultiBufferSource buffer, int combinedLight, int overlay) {
    if (entity.mount != null) {
      float scale = 0.6f;
      float rotationSpeed = 1.2f;
      this.currentRotation = this.currentRotation + minecraft.getFrameTime()*rotationSpeed;

      Mounts.Mount selected = entity.mount;
      LivingEntity selectedEntity = selected.create();
      if (selectedEntity != null) {
        renderEntity(selectedEntity, scale, stack, buffer, combinedLight);
        float itemOffset = getRenderScale(selectedEntity, scale)*selectedEntity.getBbHeight() + 0.25f;
        ItemStack item = minecraft.player.getMainHandItem();
        renderItem(item, stack, itemOffset, buffer, combinedLight, overlay);

        int amount = getAmount(minecraft.player, item);
        int maxAmount = Arrays.stream(selected.item()).filter(itemStack -> matches(itemStack, item)).findFirst().map(ItemStack::getCount).orElse(0);
        Component component = new TextComponent(amount+"/"+maxAmount).withStyle(amount < maxAmount ? ChatFormatting.RED : ChatFormatting.GREEN);
        renderNameTag(component, stack, itemOffset+0.4f, buffer, combinedLight);
      }
    }
  }

  private float getRenderScale(LivingEntity entity, float scale) {
    return entity.getBbWidth() > scale ? scale/entity.getBbWidth() * scale : scale;
  }

  private Quaternion cameraOrientation() {
    return context.getBlockEntityRenderDispatcher().camera.rotation();
  }

  private void renderEntity(LivingEntity entity, float scale, PoseStack stack, MultiBufferSource buffer, int combinedLight) {
    float renderScale = getRenderScale(entity, scale);

    stack.pushPose();
    stack.translate(0.5f, 0.75f, 0.5f);
    stack.mulPose(Vector3f.YP.rotationDegrees(currentRotation));
    stack.scale(renderScale, renderScale, renderScale);

    EntityRenderDispatcher entityRendererManager = minecraft.getEntityRenderDispatcher();
    entityRendererManager.setRenderShadow(false);
    entityRendererManager.render(entity, 0, 0, 0, minecraft.getFrameTime(), 1, stack, buffer, combinedLight);
    renderPassengers(entity, entityRendererManager, stack, buffer, combinedLight);

    stack.popPose();
  }

  private static void renderPassengers(Entity entity, EntityRenderDispatcher entityRendererManager, PoseStack matrixStack, MultiBufferSource buffer, int combinedLightIn) {
    if (entity.isVehicle()) {
      for(Entity rider : entity.getPassengers()) {
        entity.positionRider(rider);
        entityRendererManager.render(rider, rider.getX(), rider.getY(), rider.getZ(), minecraft.getFrameTime(), 1, matrixStack, buffer, combinedLightIn);
        renderPassengers(rider, entityRendererManager, matrixStack, buffer, combinedLightIn);
      }
    }
  }

  private void renderItem(ItemStack stack, PoseStack poseStack, float yOffset, MultiBufferSource buffer, int combinedLightIn, int combinedOverlayIn) {
    poseStack.pushPose();
    poseStack.translate(0.5f, 0.75f+yOffset, 0.5f);
    poseStack.mulPose(Vector3f.YP.rotationDegrees(currentRotation));
    poseStack.scale(0.35f, 0.35f, 0.35f);
    Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemTransforms.TransformType.FIXED, combinedLightIn, combinedOverlayIn, poseStack, buffer, 0);
    poseStack.popPose();
  }

  private void renderNameTag(Component component, PoseStack poseStack, float yOffset, MultiBufferSource buffer, int combinedLightIn) {
    poseStack.pushPose();
    poseStack.translate(0.5f, 0.75f+yOffset, 0.5f);
    poseStack.mulPose(cameraOrientation());
    poseStack.scale(-0.025F, -0.025F, 0.025F);
    Matrix4f matrix4f = poseStack.last().pose();
    float f1 = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
    int j = (int)(f1 * 255.0F) << 24;
    Font font = minecraft.font;
    float f2 = (float)(-font.width(component) / 2);
    font.drawInBatch(component, f2, 0, component.getStyle().getColor().getValue(), false, matrix4f, buffer, true, j, combinedLightIn);
    font.drawInBatch(component, f2, 0, -1, false, matrix4f, buffer, false, 0, combinedLightIn);

    poseStack.popPose();
  }

}
