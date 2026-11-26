package com.raishxn.gtna.preview;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;

import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.ItemStackKey;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The parts list under an EMI/JEI multiblock preview (what GTCEu's private {@code gatherBlockDrops} and
 * {@code PartInfo} do): one entry per distinct item with how many of it the structure needs, controller first, then
 * block entities, then the higher block id, then the larger amount. The material item of a block without a block
 * entity is computed once per state.
 */
public final class PatternParts {

    private static final Map<BlockState, ItemStack> CLONES = new IdentityHashMap<>();

    private PatternParts() {}

    private static final class Part {

        final ItemStackKey key;
        final boolean controller;
        final boolean tile;
        final int blockId;
        int amount;

        Part(ItemStackKey key, BlockInfo info) {
            this.key = key;
            this.blockId = Block.getId(info.getBlockState());
            this.tile = info.hasBlockEntity();
            this.controller = info.getBlockState().getBlock() instanceof MetaMachineBlock block &&
                    block.definition instanceof MultiblockMachineDefinition;
        }

        List<ItemStack> stacks() {
            return Arrays.stream(key.getItemStack()).map(stack -> stack.copyWithCount(amount))
                    .filter(stack -> !stack.isEmpty()).toList();
        }
    }

    /** @param drops every distinct item key, for the controller definition's drop set */
    public record Result(List<List<ItemStack>> parts, java.util.Set<ItemStackKey> drops) {}

    public static Result gather(Map<BlockPos, BlockInfo> blocks, BlockGetter level) {
        Map<ItemStackKey, Part> parts = new Object2ObjectOpenHashMap<>();
        for (Map.Entry<BlockPos, BlockInfo> entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue().getBlockState();
            ItemStack stack = cloneOf(level, pos, state);
            if (stack.isEmpty() && !state.getFluidState().isEmpty()) {
                Fluid fluid = state.getFluidState().getType();
                stack = fluid.getBucket().getDefaultInstance();
            }
            parts.computeIfAbsent(new ItemStackKey(stack), key -> new Part(key, entry.getValue())).amount++;
        }
        List<List<ItemStack>> sorted = parts.values().stream().sorted((one, two) -> {
            if (one.controller) return -1;
            if (two.controller) return +1;
            if (one.tile && !two.tile) return -1;
            if (two.tile && !one.tile) return +1;
            if (one.blockId != two.blockId) return two.blockId - one.blockId;
            return two.amount - one.amount;
        }).map(Part::stacks).filter(list -> !list.isEmpty()).collect(Collectors.toList());
        return new Result(sorted, parts.keySet());
    }

    private static ItemStack cloneOf(BlockGetter level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (state.hasBlockEntity()) return block.getCloneItemStack(level, pos, state);
        synchronized (CLONES) {
            return CLONES.computeIfAbsent(state, s -> block.getCloneItemStack(level, pos, s)).copy();
        }
    }
}
