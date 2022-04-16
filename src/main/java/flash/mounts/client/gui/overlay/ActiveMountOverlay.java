package flash.mounts.client.gui.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import flash.mounts.common.capability.MountCapabilityProvider;
import flash.mounts.common.config.ConfigHolder;
import flash.mounts.common.mount.Mounts;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.IIngameOverlay;

import static flash.mounts.client.ClientInit.bindTexture;
import static flash.mounts.client.ClientInit.minecraft;
import static flash.mounts.client.gui.components.MountWidget.renderFixHeightEntityInScreen;
import static net.minecraft.client.gui.components.AbstractWidget.WIDGETS_LOCATION;

@OnlyIn(Dist.CLIENT)
public class ActiveMountOverlay implements IIngameOverlay {

  @Override
  public void render(ForgeIngameGui gui, PoseStack mStack, float partialTicks, int width, int height) {
    if (minecraft.player != null && !minecraft.options.hideGui && ConfigHolder.CLIENT.isOverlayShown()) {
      MountCapabilityProvider.IMountCapability cap = MountCapabilityProvider.getCapability(minecraft.player);
      if (cap.getActiveMount() != null) {
        Mounts.Mount mount = cap.getActiveMount();
        int x = 0; // tried to make these coords configurable, it's just a headache atm.
        int y = 0; // TODO figure this out for an update
        int abilityChargeTime = cap.getAbilityChargeTime();
        int abilityCooldownTime = cap.getAbilityCooldown();

        // Background
        gui.setupOverlayRenderState(true, false);
        bindTexture(WIDGETS_LOCATION);
        RenderSystem.enableBlend();
        gui.blit(mStack, x, y, 24, 23, 22, 22);
        RenderSystem.disableBlend();

        // Entity
        LivingEntity mountEntity = mount.create();
        if (mountEntity != null) {
          int sign = x+11 > width / 2 ? 1 : -1;
          renderFixHeightEntityInScreen(x + 11, y + 18, 14, sign * 30, -10, mountEntity);
        }

        // Ability
        if (mount.ability() != null) {
          if (abilityChargeTime > 0) {
            renderAbilityTime(x, y, 1f - abilityChargeTime / (float) mount.ability().getChargeTicks());
          } else if (abilityCooldownTime > 0) {
            renderAbilityTime(x, y, abilityCooldownTime / (float) mount.ability().getCooldownTicks());
          }
        }
      }
    }
  }

  private void renderAbilityTime(int x, int y, float percent) {
    RenderSystem.disableDepthTest();
    RenderSystem.disableTexture();
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    Tesselator tesselator1 = Tesselator.getInstance();
    BufferBuilder bufferbuilder1 = tesselator1.getBuilder();
    fillRect(bufferbuilder1, x+3, y+3 + Mth.floor(16.0F * (1.0F - percent)), 16, Mth.ceil(16.0F * percent), 255, 255, 255, 127);
    RenderSystem.enableTexture();
    RenderSystem.enableDepthTest();
    RenderSystem.disableBlend();
  }

  public static void fillRect(BufferBuilder p_115153_, int p_115154_, int p_115155_, int p_115156_, int p_115157_, int p_115158_, int p_115159_, int p_115160_, int p_115161_) {
    RenderSystem.setShader(GameRenderer::getPositionColorShader);
    p_115153_.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    p_115153_.vertex((p_115154_), (p_115155_), 0.0D).color(p_115158_, p_115159_, p_115160_, p_115161_).endVertex();
    p_115153_.vertex((p_115154_), (p_115155_ + p_115157_), 0.0D).color(p_115158_, p_115159_, p_115160_, p_115161_).endVertex();
    p_115153_.vertex((p_115154_ + p_115156_), (p_115155_ + p_115157_), 0.0D).color(p_115158_, p_115159_, p_115160_, p_115161_).endVertex();
    p_115153_.vertex((p_115154_ + p_115156_), (p_115155_), 0.0D).color(p_115158_, p_115159_, p_115160_, p_115161_).endVertex();
    p_115153_.end();
    BufferUploader.end(p_115153_);
  }
}
