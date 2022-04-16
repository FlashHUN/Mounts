package flash.mounts.common;


import flash.mounts.Main;
import flash.mounts.common.network.PacketDispatcher;
import flash.mounts.common.network.game.ServerboundSummonMountPacket;
import flash.mounts.common.network.game.ServerboundUseMountAbilityPacket;
import flash.mounts.common.network.status.ServerboundSetDescentStatusPacket;
import flash.mounts.common.network.status.ServerboundRequestMountCapabilityPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.fml.loading.FMLLoader;
import org.lwjgl.glfw.GLFW;

import java.util.function.BooleanSupplier;

public enum KeyBindings {
  SUMMON_MOUNT("summon_mount", GLFW.GLFW_KEY_Z) {
    @Override
    public void onPress() {
      PacketDispatcher.sendToServer(new ServerboundRequestMountCapabilityPacket());
      PacketDispatcher.sendToServer(new ServerboundSummonMountPacket());
    }
  },
  MOUNT_MENU("mount_menu", GLFW.GLFW_KEY_PERIOD) {
    @Override
    public void onPress() {
      Main.PROXY.setScreen(Screens.UNLOCKED_MOUNTS);
    }
  },
  USE_ABILITY("use_ability", GLFW.GLFW_KEY_G) {
    @Override
    public void onPress() {
      PacketDispatcher.sendToServer(new ServerboundUseMountAbilityPacket());
    }
  },
  DESCENT("descent", GLFW.GLFW_KEY_C) {
    @Override
    public void onPress() {
      PacketDispatcher.sendToServer(new ServerboundSetDescentStatusPacket(true));
    }

    @Override
    public void onRelease() {
      PacketDispatcher.sendToServer(new ServerboundSetDescentStatusPacket(false));
    }
  };

  private BooleanSupplier down;
  private BooleanSupplier consumeClick;
  private boolean wasDown;

  KeyBindings(String name, int keyCode) {
    keymap("key."+Main.MODID+"."+name, keyCode, "key."+Main.MODID+".category");
  }

  private void keymap(String name, int defaultMapping, String category)
  {
    if (FMLLoader.getDist().isClient() && Minecraft.getInstance() != null) // instance is null during datagen
    {
      var keymap = new KeyMapping(name, defaultMapping, category);
      ClientRegistry.registerKeyBinding(keymap);
      down = keymap::isDown;
      consumeClick = keymap::consumeClick;
    }
  }

  public void setWasDown(boolean b) {
    this.wasDown = b;
  }

  public boolean wasDown() {
    return wasDown;
  }

  public boolean isDown() {
    return down.getAsBoolean();
  }

  public boolean consumeClick() {
    return consumeClick.getAsBoolean();
  }

  public void onPress() {}
  public void onRelease() {}

  public enum Screens {
    UNLOCKED_MOUNTS
  }
}