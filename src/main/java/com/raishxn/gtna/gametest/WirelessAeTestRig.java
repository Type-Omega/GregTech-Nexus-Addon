package com.raishxn.gtna.gametest;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPartHost;
import appeng.api.stacks.AEItemKey;
import appeng.api.util.AEColor;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import appeng.me.helpers.IGridConnectedBlockEntity;
import com.mojang.authlib.GameProfile;
import com.raishxn.gtna.integration.ae2.wireless.WirelessAeContent;
import com.raishxn.gtna.integration.ae2.wireless.WirelessNetworkCoreBlock;
import com.raishxn.gtna.integration.ae2.wireless.WirelessNetworkCoreBlockEntity;

import java.util.UUID;

/**
 * A powered AE2 network for the wireless GameTests: a cable with a Creative Energy Cell to its west and a Drive with
 * a 64k cell to its east, a Wireless ME Network Core on its south side with its front against the cable, free spots
 * for ME parts away from every cable, and a player who holds the binding tool.
 */
final class WirelessAeTestRig {

    static final BlockPos CABLE = new BlockPos(2, 2, 2);
    static final BlockPos ENERGY = new BlockPos(1, 2, 2);
    static final BlockPos DRIVE = new BlockPos(3, 2, 2);
    static final BlockPos CORE = new BlockPos(2, 2, 3);
    static final BlockPos FAR = new BlockPos(6, 2, 6);
    static final BlockPos FAR_2 = new BlockPos(9, 2, 6);
    static final BlockPos FAR_3 = new BlockPos(6, 2, 9);

    final GameTestHelper h;
    final ServerPlayer player;
    final ItemStack tool = new ItemStack(WirelessAeContent.WIRELESS_NETWORK_BINDING_TOOL.get());

    WirelessAeTestRig(GameTestHelper h) {
        this(h, newPlayer(h));
    }

    WirelessAeTestRig(GameTestHelper h, ServerPlayer owner) {
        this.h = h;
        h.setBlock(CABLE, AEBlocks.CABLE_BUS.block());
        ((IPartHost) h.getBlockEntity(CABLE)).addPart(AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT), null, null);
        h.setBlock(ENERGY, AEBlocks.CREATIVE_ENERGY_CELL.block());
        h.setBlock(DRIVE, AEBlocks.DRIVE.block());
        drive().getInternalInventory().setItemDirect(0, AEItems.ITEM_CELL_64K.stack());
        h.setBlock(CORE, WirelessAeContent.WIRELESS_NETWORK_CORE.get());
        player = owner;
        var block = (WirelessNetworkCoreBlock) WirelessAeContent.WIRELESS_NETWORK_CORE.get();
        block.setPlacedBy(h.getLevel(), h.absolutePos(CORE), h.getBlockState(CORE), player,
                new ItemStack(block.asItem()));
    }

    static ServerPlayer newPlayer(GameTestHelper h) {
        return newPlayer(h, UUID.randomUUID());
    }

    static ServerPlayer newPlayer(GameTestHelper h, UUID id) {
        return FakePlayerFactory.get(h.getLevel(), new GameProfile(id, "wireless-" + id.toString().substring(0, 8)));
    }

    DriveBlockEntity drive() {
        return (DriveBlockEntity) h.getBlockEntity(DRIVE);
    }

    WirelessNetworkCoreBlockEntity core() {
        return (WirelessNetworkCoreBlockEntity) h.getBlockEntity(CORE);
    }

    IGrid grid() {
        IGrid grid = drive().getMainNode().getGrid();
        h.assertTrue(grid != null, "the drive is not on a grid yet");
        return grid;
    }

    /** Throws until the cell is mounted and the core is plugged into the cable. */
    void ready() {
        h.assertTrue(drive().getMainNode().isActive(), "the drive is not on a powered grid yet");
        AEItemKey probe = AEItemKey.of(net.minecraft.world.item.Items.STICK);
        long in = grid().getStorageService().getInventory().insert(probe, 1, Actionable.MODULATE,
                IActionSource.empty());
        h.assertTrue(in == 1, "the cell is not mounted yet");
        grid().getStorageService().getInventory().extract(probe, 1, Actionable.MODULATE, IActionSource.empty());
        h.assertTrue(core().isLinkedToAeNetwork(), "the core is not plugged into the cable yet");
    }

    void stock(AEItemKey what, long amount) {
        long put = grid().getStorageService().getInventory().insert(what, amount, Actionable.MODULATE,
                IActionSource.empty());
        h.assertTrue(put == amount, "the network took only " + put + " of " + amount);
    }

    void place(BlockPos at, MachineDefinition definition) {
        h.setBlock(at, definition.getBlock());
    }

    /** The AE2 node of a GregTech machine or of an AE2 block; null while it has none yet. */
    IGridNode nodeAt(BlockPos at) {
        BlockEntity entity = h.getBlockEntity(at);
        Object host = entity instanceof MetaMachineBlockEntity machine ? machine.getMetaMachine() : entity;
        return host instanceof IGridConnectedBlockEntity connected ? connected.getMainNode().getNode() : null;
    }

    boolean hasNode(BlockPos at) {
        return nodeAt(at) != null;
    }

    boolean inTheGrid(BlockPos at) {
        IGridNode node = nodeAt(at);
        return node != null && node.getGrid() != null && node.getGrid() == grid();
    }

    /** Sneak + right-click on the core with the tool: saves the core's network in the tool. */
    InteractionResult select(ServerPlayer by, ItemStack stack) {
        return click(by, stack, CORE, true);
    }

    InteractionResult select() {
        return select(player, tool);
    }

    /** Right-click a block with the tool in the main hand. */
    InteractionResult click(ServerPlayer by, ItemStack stack, BlockPos at, boolean sneaking) {
        by.setShiftKeyDown(sneaking);
        by.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = h.absolutePos(at);
        var hit = new BlockHitResult(new Vec3(abs.getX() + 0.5, abs.getY() + 0.5, abs.getZ() + 0.5), Direction.UP, abs,
                false);
        return stack.getItem().onItemUseFirst(stack, new UseOnContext(h.getLevel(), by, InteractionHand.MAIN_HAND,
                stack, hit));
    }

    InteractionResult bind(BlockPos at) {
        return click(player, tool, at, false);
    }

    UUID memberNetwork(BlockPos at) {
        return com.raishxn.gtna.integration.ae2.wireless.WirelessAeSavedData.get(h.getLevel().getServer())
                .getMemberNetwork(net.minecraft.core.GlobalPos.of(h.getLevel().dimension(), h.absolutePos(at)));
    }

    /** What a chunk reload does to a machine: the block entity is rebuilt from its saved tag, node included. */
    void reloadBlockEntity(BlockPos at, CompoundTag saved) {
        BlockPos abs = h.absolutePos(at);
        var state = h.getLevel().getBlockState(abs);
        h.getLevel().removeBlockEntity(abs);
        h.getLevel().setBlock(abs, Blocks.AIR.defaultBlockState(), 2);
        h.getLevel().setBlock(abs, state, 2);
        h.getLevel().getBlockEntity(abs).load(saved);
    }

    /** The same for the core, without the block removal that would delete its network. */
    void reloadCore(CompoundTag saved) {
        BlockPos abs = h.absolutePos(CORE);
        var state = h.getLevel().getBlockState(abs);
        h.getLevel().removeBlockEntity(abs);
        var fresh = (WirelessNetworkCoreBlockEntity) ((WirelessNetworkCoreBlock) state.getBlock())
                .newBlockEntity(abs, state);
        fresh.load(saved);
        h.getLevel().setBlockEntity(fresh);
        fresh.onLoad();
    }
}
