package flash.mounts.common.block;

import flash.mounts.Main;
import flash.mounts.common.block.entity.MountBenchBlockEntity;
import flash.mounts.common.mount.Mounts.Mount;
import flash.mounts.common.network.PacketDispatcher;
import flash.mounts.common.network.game.ClientboundSetUnlockedMountsPacket;
import flash.mounts.common.network.game.ServerboundTryUnlockMountPacket;
import flash.mounts.common.network.status.ClientboundSendTranslatableStatusMessagePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class MountBenchBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {

  protected static final VoxelShape SLAB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);

  public MountBenchBlock() {
    super(BlockBehaviour.Properties.of(Material.STONE, DyeColor.LIME)
            .requiresCorrectToolForDrops()
            .strength(1.8F)
            .noOcclusion()
            .isSuffocating((state, level, pos) -> false)
            .lightLevel(state -> 4)
    );
    this.registerDefaultState(this.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, Boolean.FALSE));
  }

  @Override
  public boolean useShapeForLightOcclusion(BlockState p_60576_) {
    return true;
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> state) {
    state.add(BlockStateProperties.WATERLOGGED);
  }

  @Nullable
  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
    return this.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, fluidState.getType() == Fluids.WATER);
  }

  @Override
  public VoxelShape getShape(BlockState p_60555_, BlockGetter p_60556_, BlockPos p_60557_, CollisionContext p_60558_) {
    return SLAB;
  }

  @Override
  public RenderShape getRenderShape(BlockState p_49232_) {
    return RenderShape.MODEL;
  }

  @Override
  public FluidState getFluidState(BlockState state) {
    return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
  }

  @Override
  public boolean isPathfindable(BlockState p_60475_, BlockGetter p_60476_, BlockPos p_60477_, PathComputationType p_60478_) {
    return false;
  }

  @Nullable
  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new MountBenchBlockEntity(pos, state);
  }

  @Override
  public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
    if (level.isClientSide()) {
      ItemStack heldItem = player.getMainHandItem();
      if (!heldItem.isEmpty()) {
        if (level.getBlockEntity(pos) instanceof MountBenchBlockEntity blockEntity) {
          if (blockEntity.mount != null) {
            Mount selected = blockEntity.mount;
            LivingEntity mount = selected.create();
            if (mount != null) {
              PacketDispatcher.sendToServer(new ServerboundTryUnlockMountPacket(selected));
            }
          }
        }
      }
    }

    return InteractionResult.SUCCESS;
  }
}
