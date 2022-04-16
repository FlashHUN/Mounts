package flash.mounts.client.gui.components;

import com.mojang.blaze3d.vertex.PoseStack;
import flash.mounts.Main;
import flash.mounts.common.capability.MountCapabilityProvider;
import flash.mounts.common.mount.Mounts;
import flash.mounts.common.mount.Mounts.Mount;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import javax.annotation.Nullable;

public class ActiveMountWidget extends AbstractButton {

  private double speedInBPS;
  @Nullable
  private LivingEntity mountEntity;

  public ActiveMountWidget(int x, int y) {
    super(x, y, 92, 22, TextComponent.EMPTY);
    updateMount();
  }

  private void updateMount() {
    Minecraft minecraft = Minecraft.getInstance();
    MountCapabilityProvider.IMountCapability cap = MountCapabilityProvider.getCapability(minecraft.player);
    Mount mount = cap.getActiveMount();
    if (mount != null) {
      if (EntityType.byString(mount.mob()).isPresent()) {
        CompoundTag tag = mount.tag().copy();
        tag.putString("id", mount.mob());
        this.mountEntity = (LivingEntity) EntityType.loadEntityRecursive(tag, minecraft.player.getLevel(), mob -> mob);
        float speedFactor = mountEntity instanceof FlyingMob ? Mounts.FLYING_SPEED_FACTOR : Mounts.SPEED_FACTOR;
        if (mount.speed().isPresent()) {
          this.speedInBPS = mount.speed().get() * speedFactor;
        } else if (mountEntity != null && mountEntity.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
          this.speedInBPS = mountEntity.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue() * speedFactor;
        }
        this.setMessage(new TranslatableComponent("widget."+Main.MODID+".mount.speed", new TextComponent(String.valueOf(this.speedInBPS)).setStyle(Style.EMPTY).withStyle(ChatFormatting.GREEN)));
      }
      else {
        this.setMessage(MountWidget.UNKNOWN_ENTITY_COMPONENT);
      }
    } else {
      this.mountEntity = null;
      this.setMessage(TextComponent.EMPTY);
    }
  }

  @Override
  public void renderButton(PoseStack stack, int mouseX, int mouseY, float partialTicks) {
    Minecraft minecraft = Minecraft.getInstance();
    Font font = minecraft.font;
    if (mountEntity != null) {
      MountWidget.renderFixHeightEntityInScreen(x+11, y+20, 18, -30, -10, this.mountEntity);

      font.draw(stack, cutOffTextByWidth(font, mountEntity.getName(), 69), x+29, y+1, 0x8b8b8b);
      font.draw(stack, cutOffTextByWidth(font, getMessage(), 69), x+29, y+12, 0x8b8b8b);
    }
    else {
      font.draw(stack, cutOffTextByWidth(font, getMessage(), 69), x+29, y+7, 0x8b8b8b);
    }
  }

  public static String cutOffTextByWidth(Font font, FormattedText text, int maxWidth) {
    return cutOffTextByWidth(font, text.getString(), maxWidth);
  }

  public static String cutOffTextByWidth(Font font, String text, int maxWidth) {
    if (font.width(text) > maxWidth) {
      return font.plainSubstrByWidth(text, maxWidth-font.width("..."))+"...";
    } else {
      return text;
    }
  }

  @Override
  public void updateNarration(NarrationElementOutput narrationOutput) {
    this.defaultButtonNarrationText(narrationOutput);
  }

  @Override
  public void onPress() {
    MountCapabilityProvider.getCapability(Minecraft.getInstance().player).setActiveMount(null);
    updateMount();
  }
}
