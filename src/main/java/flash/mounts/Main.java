package flash.mounts;

import flash.mounts.common.capability.MountCapabilityProvider;
import flash.mounts.common.init.BlockInit;
import flash.mounts.common.network.PacketDispatcher;
import flash.mounts.proxy.ClientProxy;
import flash.mounts.proxy.CommonProxy;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Main.MODID)
public class Main {

  public static final String MODID = "mounts";
  public static final Logger LOGGER = LogManager.getLogger(Main.MODID);

  public static CommonProxy PROXY;

  public Main() {
    IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
    modEventBus.addListener(this::setup);
    modEventBus.addListener(this::registerCapabilites);

    BlockInit.BLOCK_REGISTRY.register(modEventBus);
    BlockInit.BLOCK_ENTITY_REGISTRY.register(modEventBus);
    BlockInit.BLOCK_ITEM_REGISTRY.register(modEventBus);

    PROXY = DistExecutor.runForDist(() -> ClientProxy::new, () -> CommonProxy::new);

    MinecraftForge.EVENT_BUS.register(this);
  }

  private void setup(final FMLCommonSetupEvent event) {
    PacketDispatcher.registerMessages();
  }

  public void registerCapabilites(RegisterCapabilitiesEvent event) {
    event.register(MountCapabilityProvider.IMountCapability.class);
  }
}
