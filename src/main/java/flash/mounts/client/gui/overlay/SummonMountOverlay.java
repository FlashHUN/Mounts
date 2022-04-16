package flash.mounts.client.gui.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import flash.mounts.common.capability.MountCapabilityProvider;
import flash.mounts.common.network.game.ServerboundSummonMountPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.IIngameOverlay;

import static flash.mounts.client.ClientInit.bindTexture;
import static flash.mounts.client.ClientInit.minecraft;

@OnlyIn(Dist.CLIENT)
public class SummonMountOverlay implements IIngameOverlay {

  private static final ResourceLocation BARS_LOCATION = new ResourceLocation("minecraft", "textures/gui/bars.png");

  private static int WAIT_TIMER = 0;
  private static int MAX_WAIT_TIMER = (int)(40 * 1.25f);

  @Override
  public void render(ForgeIngameGui gui, PoseStack mStack, float partialTicks, int width, int height) {
    int barWidth = 55;
    int left = width / 2 - barWidth / 2;
    int top = height / 2 + 20;
    MountCapabilityProvider.IMountCapability cap = MountCapabilityProvider.getCapability(minecraft.player);
    if (cap.getMountSummonTimer() > 0) {
      if (!minecraft.options.hideGui) {
        gui.setupOverlayRenderState(true, false);
        bindTexture(BARS_LOCATION);
        RenderSystem.enableBlend();

        // Draw bar backgrounds
        gui.blit(mStack, left, top, 0, 40, barWidth - 5, 5);
        gui.blit(mStack, left+barWidth-5, top, 177, 40, 5, 5);

        // Draw bar progress
        int currentProgress = (int)(barWidth*(ServerboundSummonMountPacket.MAX_SUMMON_TIMER - cap.getMountSummonTimer())/(float) ServerboundSummonMountPacket.MAX_SUMMON_TIMER);
        if (currentProgress > barWidth) currentProgress = barWidth;
        gui.blit(mStack, left, top, 0, 45, Math.min(currentProgress, barWidth - 5), 5);
        if (currentProgress > barWidth-5) {
          gui.blit(mStack, left+barWidth-5, top, 177, 45, currentProgress-(barWidth-5), 5);
        }

        RenderSystem.disableBlend();

      }
      if (cap.getMountSummonTimer() == 1) {
        WAIT_TIMER = MAX_WAIT_TIMER;
      }
    }
    if (WAIT_TIMER > 0) {
      if (!minecraft.options.hideGui) {
        gui.setupOverlayRenderState(true, false);
        bindTexture(BARS_LOCATION);
        RenderSystem.enableBlend();

        int texY = 35;
        if (MountCapabilityProvider.getCapability(minecraft.player).getActiveMount() == null) texY = 25;

        gui.blit(mStack, left, top, 0, texY, barWidth - 5, 5);
        gui.blit(mStack, left+barWidth-5, top, 177, texY, 5, 5);

        RenderSystem.disableBlend();
      }
      WAIT_TIMER--;
    }
  }

}
