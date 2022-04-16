package flash.mounts.client;

import com.mojang.blaze3d.systems.RenderSystem;
import flash.mounts.client.gui.overlay.ActiveMountOverlay;
import flash.mounts.client.render.blockentity.MountBenchBlockEntityRenderer;
import flash.mounts.Main;
import flash.mounts.client.event.ClientEvents;
import flash.mounts.client.gui.overlay.SummonMountOverlay;
import flash.mounts.common.KeyBindings;
import flash.mounts.common.config.ConfigHolder;
import flash.mounts.common.init.BlockInit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.gui.IIngameOverlay;
import net.minecraftforge.client.gui.OverlayRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientInit {

  public static final Minecraft minecraft = Minecraft.getInstance();

  public static final IIngameOverlay summonMountOverlay = new SummonMountOverlay();
  public static final IIngameOverlay activeMountOverlay = new ActiveMountOverlay();

  @SubscribeEvent
  public static void onClientSetup(final FMLClientSetupEvent event) {
    OverlayRegistry.registerOverlayTop(Main.MODID+".summon", summonMountOverlay);
    OverlayRegistry.registerOverlayTop(Main.MODID+".active", activeMountOverlay);

    BlockEntityRenderers.register(BlockInit.MOUNT_BENCH_ENTITY.get(), MountBenchBlockEntityRenderer::new);

    ConfigHolder.initClient();

    MinecraftForge.EVENT_BUS.register(new ClientEvents());
  }

  public static void bindTexture(ResourceLocation texture) {
    RenderSystem.setShaderTexture(0, texture);
  }
}
