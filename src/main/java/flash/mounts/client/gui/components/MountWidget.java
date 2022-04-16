package flash.mounts.client.gui.components;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import flash.mounts.Main;
import flash.mounts.client.gui.screen.UnlockedMountsScreen;
import flash.mounts.common.capability.MountCapabilityProvider;
import flash.mounts.common.capability.MountCapabilityProvider.IMountCapability;
import flash.mounts.common.mount.Mounts;
import flash.mounts.common.mount.Mounts.Mount;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Consumer;

import static flash.mounts.client.ClientInit.minecraft;

public class MountWidget extends AbstractButton {

  public static final TranslatableComponent UNKNOWN_ENTITY_COMPONENT = new TranslatableComponent("entity."+Main.MODID+".unknown");

  private boolean isFavorite;
  private double speedInBPS;
  private final Mount mount;
  private OnTooltip onTooltip;
  @Nullable
  private LivingEntity mountEntity;

  private final int origY;

  public MountWidget(int x, int y, boolean isFavorite, Mount mount) {
    super(x, y, 83, 15, UNKNOWN_ENTITY_COMPONENT);
    this.origY = y;
    this.isFavorite = isFavorite;
    this.mount = mount;
    if (mount != null) {
      if (EntityType.byString(mount.mob()).isPresent()) {
        CompoundTag tag = mount.tag().copy();
        tag.putString("id", mount.mob());
        this.mountEntity = (LivingEntity) EntityType.loadEntityRecursive(tag, minecraft.player.getLevel(), mob -> mob);
        this.setMessage(mountEntity.getName());
        float speedFactor = mountEntity instanceof FlyingMob ? Mounts.FLYING_SPEED_FACTOR : Mounts.SPEED_FACTOR;
        if (mount.speed().isPresent()) {
          this.speedInBPS = mount.speed().get() * speedFactor;
        } else if (mountEntity != null && mountEntity.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
          this.speedInBPS = mountEntity.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue() * speedFactor;
        }
        this.onTooltip = new OnTooltip() {
          private final Component text = new TranslatableComponent("widget." + Main.MODID + ".mount.speed", new TextComponent(String.valueOf(MountWidget.this.speedInBPS)).setStyle(Style.EMPTY).withStyle(ChatFormatting.GREEN));

          @Override
          public void onTooltip(MountWidget widget, PoseStack stack, int mouseX, int mouseY) {
            stack.pushPose();
            stack.translate(0, 0, 2048);
            minecraft.screen.renderTooltip(stack, text, mouseX, mouseY);
            stack.popPose();
          }

          @Override
          public void narrateTooltip(Consumer<Component> p_169456_) {
            p_169456_.accept(this.text);
          }
        };
      }
    }
  }

  public int getOrigY() {
    return origY;
  }

  public void setFavorite(boolean favorite) {
    isFavorite = favorite;
    IMountCapability cap = MountCapabilityProvider.getCapability(minecraft.player);
    if (favorite) {
      cap.favoriteMount(this.mount);
    } else {
      cap.unfavoriteMount(this.mount);
    }
    ((UnlockedMountsScreen)minecraft.screen).reorderFavorites();
    ((UnlockedMountsScreen)minecraft.screen).recacheMountWidgets();
  }

  @Override
  public void renderButton(PoseStack stack, int mouseX, int mouseY, float partialTicks) {
    Minecraft minecraft = Minecraft.getInstance();
    UnlockedMountsScreen screen = (UnlockedMountsScreen) minecraft.screen;
    if (y > screen.getY()+13 && y < screen.getY()+158) {
      Font font = minecraft.font;
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderTexture(0, UnlockedMountsScreen.MOUNT_MENU_LOCATION);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
      int i = this.getYImage(this.isHoveredOrFocused());
      if (Objects.equals(MountCapabilityProvider.getCapability(minecraft.player).getActiveMount(), this.mount)) {
        i = 2;
      }
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableDepthTest();
      this.blit(stack, this.x, y, 0, 173 + i * 15, this.width, this.height);

      int j = getFGColor();
      drawString(stack, font, ActiveMountWidget.cutOffTextByWidth(font, this.getMessage(), 65), this.x+15, y + (this.height - 8) / 2, j | Mth.ceil(this.alpha * 255.0F) << 24);

      if (this.mountEntity != null) {
        renderFixHeightEntityInScreen(this.x + 7, y + 13, 11, -30, -10, this.mountEntity);
      }

      if (isFavorite) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, UnlockedMountsScreen.MOUNT_MENU_LOCATION);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
        stack.pushPose();
        stack.translate(x+width-13, y+2, 0);
        stack.scale(0.65f, 0.65f, 1f);
        this.blit(stack, 0, 0, 0, 218, 17, 17);
        stack.popPose();
      }

      if (this.isHoveredOrFocused()) {
        this.renderToolTip(stack, mouseX, mouseY);
      }
    }
  }

  public static void renderFixHeightEntityInScreen(float x, float y, float scale, float mouseX, float mouseY, LivingEntity entity) {
    float f = (float)Math.atan((mouseX / 40.0F));
    float f1 = (float)Math.atan((mouseY / 40.0F));
    PoseStack posestack = RenderSystem.getModelViewStack();
    posestack.pushPose();
    posestack.translate(x, y, 1050.0D);
    posestack.scale(1.0F, 1.0F, -1.0F);
    RenderSystem.applyModelViewMatrix();
    PoseStack posestack1 = new PoseStack();
    posestack1.translate(0.0D, 0.0D, 1000.0D);
    float renderScale = Math.min(scale/entity.getBbHeight(), scale/entity.getBbWidth());
    posestack1.scale(renderScale, renderScale, renderScale);
    Quaternion quaternion = Vector3f.ZP.rotationDegrees(180.0F);
    Quaternion quaternion1 = Vector3f.XP.rotationDegrees(f1 * 20.0F);
    quaternion.mul(quaternion1);
    posestack1.mulPose(quaternion);
    float f2 = entity.yBodyRot;
    float f3 = entity.getYRot();
    float f4 = entity.getXRot();
    float f5 = entity.yHeadRotO;
    float f6 = entity.yHeadRot;
    entity.yBodyRot = 180.0F + f * 20.0F;
    entity.setYRot(180.0F + f * 40.0F);
    entity.setXRot(-f1 * 20.0F);
    entity.yHeadRot = entity.getYRot();
    entity.yHeadRotO = entity.getYRot();
    Lighting.setupForEntityInInventory();
    EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
    quaternion1.conj();
    entityrenderdispatcher.overrideCameraOrientation(quaternion1);
    entityrenderdispatcher.setRenderShadow(false);
    MultiBufferSource.BufferSource multibuffersource$buffersource = Minecraft.getInstance().renderBuffers().bufferSource();
    RenderSystem.runAsFancy(() -> {
      entityrenderdispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, posestack1, multibuffersource$buffersource, 15728880);
    });
    multibuffersource$buffersource.endBatch();
    entityrenderdispatcher.setRenderShadow(true);
    entity.yBodyRot = f2;
    entity.setYRot(f3);
    entity.setXRot(f4);
    entity.yHeadRotO = f5;
    entity.yHeadRot = f6;
    posestack.popPose();
    RenderSystem.applyModelViewMatrix();
    Lighting.setupFor3DItems();
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int btn) {
    if (this.active && this.visible) {
      boolean flag = this.clicked(mouseX, mouseY);
      if (flag) {
        if (this.isValidClickButton(btn)) {
          this.playDownSound(Minecraft.getInstance().getSoundManager());
          this.onClick(mouseX, mouseY);
          return true;
        } else if (btn == 1) {
          this.playDownSound(Minecraft.getInstance().getSoundManager());
          this.setFavorite(!isFavorite);
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void renderToolTip(PoseStack stack, int mouseX, int mouseY) {
    this.onTooltip.onTooltip(this, stack, mouseX, mouseY);
  }

  @Override
  public void onPress() {
    if (this.mountEntity != null) {
      IMountCapability cap = MountCapabilityProvider.getCapability(minecraft.player);
      cap.setActiveMount(this.mount);
      ((UnlockedMountsScreen) minecraft.screen).recacheActiveMountWidget();
    }
  }

  @Override
  public void updateNarration(NarrationElementOutput narrationOutput) {
    this.defaultButtonNarrationText(narrationOutput);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MountWidget that = (MountWidget) o;
    return isFavorite == that.isFavorite && speedInBPS == that.speedInBPS && mount.equals(that.mount) && Objects.equals(mountEntity, that.mountEntity);
  }

  @OnlyIn(Dist.CLIENT)
  public interface OnTooltip {
    void onTooltip(MountWidget widget, PoseStack stack, int mouseX, int mouseY);

    default void narrateTooltip(Consumer<Component> narrationComponents) {
    }
  }
}
