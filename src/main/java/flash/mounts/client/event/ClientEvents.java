package flash.mounts.client.event;

import flash.mounts.common.KeyBindings;
import flash.mounts.common.config.ConfigHolder;
import flash.mounts.common.mount.Mounts;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import static flash.mounts.client.ClientInit.minecraft;
import static flash.mounts.common.capability.MountCapability.matches;

@OnlyIn(Dist.CLIENT)
public class ClientEvents {

  @SubscribeEvent
  public void onKeyInput(InputEvent.KeyInputEvent event) {
    Player player = minecraft.player;
    if (minecraft.isWindowActive() && player != null && player.isAlive()) {
      if (event.getAction() == GLFW.GLFW_PRESS) {
        for (KeyBindings keyBind : KeyBindings.values()) {
          if (!keyBind.wasDown() && keyBind.isDown())
            keyBind.onPress();

          keyBind.setWasDown(keyBind.isDown());
        }
      } else if (event.getAction() == GLFW.GLFW_RELEASE) {
        for (KeyBindings keyBind : KeyBindings.values()) {
          if (keyBind.wasDown() && !keyBind.isDown())
            keyBind.onRelease();

          keyBind.setWasDown(keyBind.isDown());
        }
      }
    }
  }

  @SubscribeEvent
  public void itemTooltip(ItemTooltipEvent event) {
    if (ConfigHolder.COMMON.allowTooltips() && ConfigHolder.CLIENT.areTooltipsShown()) {
      for (ItemStack stack : Mounts.ITEM_TO_MOUNT_MAP.keySet()) {
        if (matches(stack, event.getItemStack())) {
          Mounts.Mount mount = Mounts.ITEM_TO_MOUNT_MAP.get(stack);
          LivingEntity entity = mount.create();
          if (entity != null) {
            event.getToolTip().add(new TranslatableComponent("tooltip.mounts.unlock", entity.getName()).withStyle(ChatFormatting.GRAY));
          }
        }
      }
    }
  }
}
