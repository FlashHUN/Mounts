package flash.mounts.common.block.entity;

import flash.mounts.common.init.BlockInit;
import flash.mounts.common.mount.Mounts.Mount;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class MountBenchBlockEntity extends BlockEntity {

  public Mount mount;

  public MountBenchBlockEntity(BlockEntityType<MountBenchBlockEntity> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  public MountBenchBlockEntity(BlockPos pos, BlockState state) {
    this(BlockInit.MOUNT_BENCH_ENTITY.get(), pos, state);
  }
}
