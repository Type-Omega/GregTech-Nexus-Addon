package com.raishxn.gtna.preview;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;

import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import com.typeomega.lib.data.BlockPosHashing;
import com.typeomega.lib.preview.BlockInfoCache;
import com.typeomega.lib.preview.PreviewCells;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The block map of the in-world multiblock hologram: the preview shape turned to the controller's facing and moved
 * to its position. GTCEu's {@code MultiblockInWorldPreviewRenderer.showPreview} does this for every cell of the
 * bounding box, air included (up to 6.7 million cells for 80 000 blocks), with several {@code BlockPos} each; here
 * air is skipped before anything is allocated and the map only holds real blocks.
 *
 * <p>
 * The rotation follows GTCEu 7.5.3 ({@code showPreview} and {@code rotateByFrontAxis}, LGPL-3.0) cell for cell.
 */
public final class HologramBuild {

    /**
     * @param blocks        real blocks by world position; the controller itself is not in it, like in GTCEu
     * @param controller    the controller found in the shape, bound to the level, or null when the layer has none
     * @param blockEntities positions of the blocks that have a block entity
     */
    public record Result(Object2ObjectLinkedOpenCustomHashMap<BlockPos, BlockInfo> blocks,
                         @Nullable IMultiController controller, List<BlockPos> blockEntities) {}

    private HologramBuild() {}

    /**
     * @param layer {@code y} index of the only layer to build, or {@code -1} for all of them
     * @param level the level the controller's block entity is bound to
     */
    public static Result build(BlockInfo[][][] shape, BlockPos controllerCell, Direction front, Direction up,
                               BlockPos origin, int layer, Level level) {
        Object2ObjectLinkedOpenCustomHashMap<BlockPos, BlockInfo> blocks = BlockPosHashing.newMap();
        List<BlockPos> entities = new ArrayList<>();
        IMultiController[] controller = { null };
        Rotation upRotation = up == Direction.NORTH ? Rotation.NONE : up == Direction.EAST ? Rotation.CLOCKWISE_90 :
                up == Direction.SOUTH ? Rotation.CLOCKWISE_180 :
                        up == Direction.WEST ? Rotation.COUNTERCLOCKWISE_90 : Rotation.NONE;
        PreviewCells.forEachSolid(shape, layer, (x, y, z, state, info) -> {
            BlockPos offset = new BlockPos(x - controllerCell.getX(), y - controllerCell.getY(),
                    z - controllerCell.getZ());
            offset = switch (front) {
                case NORTH, UP, DOWN -> offset.rotate(Rotation.NONE);
                case SOUTH -> offset.rotate(Rotation.CLOCKWISE_180);
                case EAST -> offset.rotate(Rotation.COUNTERCLOCKWISE_90);
                case WEST -> offset.rotate(Rotation.CLOCKWISE_90);
            };
            offset = rotateByFrontAxis(offset, front, upRotation);
            BlockState rotated = withFacing(state, front);
            BlockPos real = origin.offset(offset);
            if (info.hasBlockEntity() && info.getBlockEntity(real) instanceof IMachineBlockEntity holder &&
                    holder.getMetaMachine() instanceof IMultiController found) {
                holder.getSelf().setLevel(level);
                controller[0] = found;
                return;
            }
            BlockInfo cell = BlockInfoCache.of(rotated);
            blocks.put(real, cell);
            if (cell.hasBlockEntity()) entities.add(real);
        });
        return new Result(blocks, controller[0], entities);
    }

    private static BlockState withFacing(BlockState state, Direction front) {
        if (!(state.getBlock() instanceof MetaMachineBlock machineBlock)) return state;
        RotationState rotationState = machineBlock.getRotationState();
        if (rotationState == RotationState.NONE) return state;
        Direction face = state.getValue(rotationState.property);
        if (face.getAxis() != Direction.Axis.Y) {
            face = switch (front) {
                case NORTH, UP, DOWN -> front;
                case SOUTH -> face.getOpposite();
                case WEST -> face.getCounterClockWise();
                case EAST -> face.getClockWise();
            };
        }
        return rotationState.test(face) ? state.setValue(rotationState.property, face) : state;
    }

    private static BlockPos rotateByFrontAxis(BlockPos pos, Direction front, Rotation rotation) {
        int step = front.getAxisDirection().getStep();
        if (front.getAxis() == Direction.Axis.X) {
            return switch (rotation) {
                default -> new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
                case CLOCKWISE_90 -> new BlockPos(-pos.getX(), -step * pos.getZ(), step * -pos.getY());
                case CLOCKWISE_180 -> new BlockPos(-pos.getX(), -pos.getY(), pos.getZ());
                case COUNTERCLOCKWISE_90 -> new BlockPos(-pos.getX(), step * pos.getZ(), step * pos.getY());
            };
        } else if (front.getAxis() == Direction.Axis.Y) {
            return switch (rotation) {
                default -> new BlockPos(-step * pos.getX(), -step * pos.getZ(), -pos.getY());
                case CLOCKWISE_90 -> new BlockPos(pos.getY(), -step * pos.getZ(), -step * pos.getX());
                case CLOCKWISE_180 -> new BlockPos(step * pos.getX(), -step * pos.getZ(), pos.getY());
                case COUNTERCLOCKWISE_90 -> new BlockPos(-pos.getY(), -step * pos.getZ(), step * pos.getX());
            };
        } else if (front.getAxis() == Direction.Axis.Z) {
            return switch (rotation) {
                default -> pos;
                case CLOCKWISE_90 -> new BlockPos(step * pos.getY(), -step * pos.getX(), pos.getZ());
                case CLOCKWISE_180 -> new BlockPos(-pos.getX(), -pos.getY(), pos.getZ());
                case COUNTERCLOCKWISE_90 -> new BlockPos(step * -pos.getY(), step * pos.getX(), pos.getZ());
            };
        }
        return pos;
    }
}
