package flash.mounts.common.init;

import flash.mounts.Main;
import flash.mounts.common.block.MountBenchBlock;
import flash.mounts.common.block.entity.MountBenchBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockInit {

  public static final DeferredRegister<Block> BLOCK_REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCKS, Main.MODID);
  public static final DeferredRegister<Item> BLOCK_ITEM_REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, Main.MODID);
  public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, Main.MODID);

  public static final RegistryObject<MountBenchBlock> MOUNT_BENCH = BLOCK_REGISTRY.register("mount_bench", MountBenchBlock::new);

  public static final RegistryObject<BlockEntityType<MountBenchBlockEntity>> MOUNT_BENCH_ENTITY = BLOCK_ENTITY_REGISTRY.register(
          "mount_bench",
          () -> BlockEntityType.Builder.of(MountBenchBlockEntity::new, MOUNT_BENCH.get()).build(null)
  );

  public static final RegistryObject<BlockItem> MOUNT_BENCH_ITEM = BLOCK_ITEM_REGISTRY.register(
          "mount_bench",
          () -> new BlockItem(MOUNT_BENCH.get(), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS).rarity(Rarity.UNCOMMON))
  );

}
