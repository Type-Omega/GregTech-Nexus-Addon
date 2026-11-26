// Ported from GTLCore (LGPL-3.0), https://github.com/AaAdoniSsS/GTLCore
package com.raishxn.gtna.integration.ae2.wireless;

import com.gregtechceu.gtceu.api.item.tool.ToolHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** The Wireless ME Network Core: its front is the face that plugs into an ME cable or ME block. */
public class WirelessNetworkCoreBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.values());

    /** The order a wrench steps through the six faces. */
    private static final Direction[] WRENCH_ORDER = { Direction.NORTH, Direction.EAST, Direction.SOUTH,
            Direction.WEST, Direction.UP, Direction.DOWN };

    public WirelessNetworkCoreBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(5.0F)
                .requiresCorrectToolForDrops());
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // EN: The front goes against the block that was clicked, so placing the core on an ME cable plugs it in.
        // PT: A frente fica contra o bloco clicado, então colocar o núcleo num cabo ME já o liga.
        // ZH: 正面朝向被点击的方块，因此把核心放在 ME 线缆上就会直接接入。
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return WirelessAeContent.WIRELESS_NETWORK_CORE_BE.create(pos, state);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @NotNull BlockState rotate(@NotNull BlockState state, @NotNull Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    /** Called by GregTech's wrench: steps the front through the six faces and moves the cable face with it. */
    @Override
    public BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation rotation) {
        Direction old = state.getValue(FACING);
        Direction next = WRENCH_ORDER[(indexOf(old) + 1) % WRENCH_ORDER.length];
        BlockState turned = state.setValue(FACING, next);
        level.setBlock(pos, turned, 3);
        if (level.getBlockEntity(pos) instanceof WirelessNetworkCoreBlockEntity core) {
            core.refreshConnectionSide(old, next);
        }
        return turned;
    }

    private static int indexOf(Direction direction) {
        for (int i = 0; i < WRENCH_ORDER.length; i++) {
            if (WRENCH_ORDER[i] == direction) return i;
        }
        return 0;
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                          @NotNull Player player, @NotNull InteractionHand hand,
                                          @NotNull BlockHitResult hit) {
        ItemStack heldStack = player.getItemInHand(hand);
        if (!ToolHelper.getToolTypes(heldStack).isEmpty()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer &&
                level.getBlockEntity(pos) instanceof WirelessNetworkCoreBlockEntity core) {
            WirelessNetworkCoreMenu.open(serverPlayer, core);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state,
                            @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player &&
                level.getBlockEntity(pos) instanceof WirelessNetworkCoreBlockEntity core) {
            core.setOwner(player);
        }
    }

    @Override
    public void onRemove(BlockState state, @NotNull Level level, @NotNull BlockPos pos, BlockState newState,
                         boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level instanceof ServerLevel serverLevel) {
            if (level.getBlockEntity(pos) instanceof WirelessNetworkCoreBlockEntity core) {
                core.removeNetwork(serverLevel.getServer());
            } else {
                for (UUID frequency : WirelessAeSavedData.get(serverLevel.getServer())
                        .removeNetworksAtCore(GlobalPos.of(serverLevel.dimension(), pos))) {
                    WirelessAeNetworkRuntime.disconnectFrequency(frequency);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
