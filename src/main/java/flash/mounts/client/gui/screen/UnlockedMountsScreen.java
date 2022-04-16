package flash.mounts.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import flash.mounts.Main;
import flash.mounts.client.gui.components.ActiveMountWidget;
import flash.mounts.client.gui.components.MountWidget;
import flash.mounts.common.capability.MountCapabilityProvider;
import flash.mounts.common.capability.MountCapabilityProvider.IMountCapability;
import flash.mounts.common.mount.Mounts.Mount;
import flash.mounts.common.network.PacketDispatcher;
import flash.mounts.common.network.game.ServerboundSetActiveMountPacket;
import flash.mounts.common.network.game.ServerboundSetFavoriteMountsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UnlockedMountsScreen extends Screen {

  public static final ResourceLocation MOUNT_MENU_LOCATION = new ResourceLocation(Main.MODID, "textures/gui/unlocked_mounts.png");

  private int x, y, scrollOffset, maxScrollOffset;
  private double scrollY;
  private boolean isScrolling;

  private List<Mount> mounts;
  private List<Mount> favorites;
  private final List<MountWidget> mountWidgets;
  private ActiveMountWidget activeMountWidget;

  public UnlockedMountsScreen() {
    super(TextComponent.EMPTY);
    reorderFavorites();
    mountWidgets = new ArrayList<>();
  }

  @Override
  protected void init() {
    x = width-100;
    y = height/2-173/2;
    recacheActiveMountWidget();
    recacheMountWidgets();
  }

  @Override
  public void tick() {
    for (MountWidget mountWidget : mountWidgets) {
      mountWidget.y = mountWidget.getOrigY() + scrollOffset;
    }
  }

  @Override
  public void render(PoseStack stack, int mouseX, int mouseY, float partialTicks) {
    bindTexture();
    this.blit(stack, x+5, y+28, 100, 0, 83, 130);
    for(MountWidget widget : this.mountWidgets) {
      widget.render(stack, mouseX, mouseY, partialTicks);
    }
    bindTexture();
    this.blit(stack, x, y, 0, 0, 100, 173);
    this.activeMountWidget.render(stack, mouseX, mouseY, partialTicks);

    if (maxScrollOffset > 0) {
      bindTexture();
      stack.pushPose();
      stack.translate(0, scrollY, 0);
      this.blit(stack, x + 89, y + 28, 83 + (isMouseOverScrollbar(mouseX, mouseY) ? 6 : 0), 173, 6, 15);
      stack.popPose();
    }
  }

  private void bindTexture() {
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    RenderSystem.setShaderTexture(0, MOUNT_MENU_LOCATION);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    updateScroll(-delta*4);
    return super.mouseScrolled(mouseX, mouseY, delta);
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int btn) {
    if (isMouseOverScrollbar(mouseX, mouseY) && btn == 0) {
      isScrolling = true;
      this.scrollY = (mouseY - (y+28)) / 130;
      updateScroll(0);
    }
    return super.mouseClicked(mouseX, mouseY, btn);
  }

  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int btn) {
    if (btn == 0) isScrolling = false;
    return super.mouseReleased(mouseX, mouseY, btn);
  }

  @Override
  public boolean mouseDragged(double mouseX, double mouseY, int btn, double deltaX, double deltaY) {
    if (isScrolling) {
      updateScroll(deltaY);
    }
    return super.mouseDragged(mouseX, mouseY, btn, deltaX, deltaY);
  }

  public boolean isMouseOverScrollbar(double mouseX, double mouseY) {
    return mouseX >= x+89 && mouseX <= x+99 && mouseY >= y+28 && mouseY <= y+158;
  }

  private void updateScroll(double delta) {
    boolean canScroll = maxScrollOffset > 0;
    this.scrollY = canScroll ? Mth.clamp(scrollY + delta, 0, 115) : 0;
    this.scrollOffset = canScroll ? -(int) (scrollY * maxScrollOffset / 115) : 0;
  }

  public int getY() {
    return y;
  }

  public void recacheMountWidgets() {
    for (MountWidget mountWidget : mountWidgets) {
      removeWidget(mountWidget);
    }
    mountWidgets.clear();
    int yOffset = 0;
    for (Mount mount : favorites) {
      MountWidget mountWidget = new MountWidget(x + 5, y + 28 + yOffset, true, mount);
      this.mountWidgets.add(this.addRenderableWidget(mountWidget));
      yOffset += 15;
    }
    for (Mount mount : mounts) {
      MountWidget mountWidget = new MountWidget(x + 5, y + 28 + yOffset, false, mount);
      this.mountWidgets.add(this.addRenderableWidget(mountWidget));
      yOffset += 15;
    }
    maxScrollOffset = Math.max(0, yOffset-130);
    scrollY = Mth.clamp(scrollOffset, 0, maxScrollOffset) / (double)maxScrollOffset;
    updateScroll(0);
  }

  public void recacheActiveMountWidget() {
    removeWidget(activeMountWidget);
    this.activeMountWidget = this.addRenderableWidget(new ActiveMountWidget(x+4, y+4));
  }

  public void reorderFavorites() {
    IMountCapability cap = MountCapabilityProvider.getCapability(Minecraft.getInstance().player);
    mounts = cap.getUnlockedMounts().stream().sorted((a, b) -> a.mob().compareToIgnoreCase(b.mob())).collect(Collectors.toList());
    favorites = cap.getFavoriteMounts().stream().sorted((a, b) -> a.mob().compareToIgnoreCase(b.mob())).collect(Collectors.toList());
    mounts = mounts.stream().filter(mount -> !favorites.contains(mount)).collect(Collectors.toList());
  }

  @Override
  public void onClose() {
    IMountCapability cap = MountCapabilityProvider.getCapability(Minecraft.getInstance().player);
    PacketDispatcher.sendToServer(new ServerboundSetFavoriteMountsPacket(cap.getFavoriteMounts()));
    PacketDispatcher.sendToServer(new ServerboundSetActiveMountPacket(cap.getActiveMount()));
    super.onClose();
  }
}
