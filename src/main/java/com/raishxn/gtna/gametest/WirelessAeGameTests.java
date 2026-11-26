package com.raishxn.gtna.gametest;

import com.gregtechceu.gtceu.common.data.machines.GTAEMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import appeng.api.stacks.AEItemKey;
import appeng.core.definitions.AEBlocks;
import com.raishxn.gtna.integration.ae2.wireless.WirelessAeContent;
import com.raishxn.gtna.integration.ae2.wireless.WirelessAeSavedData;
import com.raishxn.gtna.integration.ae2.wireless.WirelessNetworkBindingToolItem;

/**
 * The wireless ME network (D-444): a part or an AE2 block bound to a Wireless ME Network Core joins the core's grid
 * and sees its storage; an unbound one does not; the binding survives reloads and world saves; sneak-placing binds;
 * removing the core disconnects everything. The grid is a real powered network.
 */
@GameTestHolder("gtna")
@PrefixGameTestTemplate(false)
public final class WirelessAeGameTests {

    private static final String TEMPLATE = "empty_12";
    private static final AEItemKey IRON = AEItemKey.of(Items.IRON_INGOT);

    private WirelessAeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void wirelessGregTechMePartBoundToTheCoreJoinsItsGridAndSeesTheStorage(GameTestHelper h) {
        var rig = new WirelessAeTestRig(h);
        rig.place(WirelessAeTestRig.FAR, GTAEMachines.ITEM_IMPORT_BUS_ME);
        h.startSequence()
                .thenWaitUntil(() -> {
                    rig.ready();
                    h.assertTrue(rig.hasNode(WirelessAeTestRig.FAR), "the part has no node yet");
                })
                .thenExecute(() -> {
                    h.assertTrue(!rig.inTheGrid(WirelessAeTestRig.FAR), "an unbound part is already in the grid");
                    rig.select();
                    h.assertTrue(WirelessNetworkBindingToolItem.savedNetwork(rig.tool) != null,
                            "the tool did not save the network of the core");
                    rig.bind(WirelessAeTestRig.FAR);
                    h.assertTrue(rig.memberNetwork(WirelessAeTestRig.FAR) != null, "the part was not bound");
                    rig.stock(IRON, 7);
                })
                .thenWaitUntil(() -> {
                    h.assertTrue(rig.inTheGrid(WirelessAeTestRig.FAR), "the bound part did not join the grid");
                    h.assertTrue(rig.nodeAt(WirelessAeTestRig.FAR).isOnline(), "the bound part has no channel");
                })
                .thenExecute(() -> {
                    var storage = rig.nodeAt(WirelessAeTestRig.FAR).getGrid().getStorageService().getInventory();
                    h.assertTrue(storage.getAvailableStacks().get(IRON) == 7, "the part does not see the storage");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void wirelessAeInterfaceAndPatternProviderBoundToTheCoreJoinTheGrid(GameTestHelper h) {
        var rig = new WirelessAeTestRig(h);
        h.setBlock(WirelessAeTestRig.FAR, AEBlocks.INTERFACE.block());
        h.setBlock(WirelessAeTestRig.FAR_2, AEBlocks.PATTERN_PROVIDER.block());
        h.startSequence()
                .thenWaitUntil(() -> {
                    rig.ready();
                    h.assertTrue(rig.hasNode(WirelessAeTestRig.FAR) && rig.hasNode(WirelessAeTestRig.FAR_2),
                            "the AE2 blocks have no node yet");
                })
                .thenExecute(() -> {
                    h.assertTrue(!rig.inTheGrid(WirelessAeTestRig.FAR) && !rig.inTheGrid(WirelessAeTestRig.FAR_2),
                            "an unbound AE2 block is already in the grid");
                    rig.select();
                    rig.bind(WirelessAeTestRig.FAR);
                    rig.bind(WirelessAeTestRig.FAR_2);
                    h.assertTrue(rig.memberNetwork(WirelessAeTestRig.FAR) != null &&
                            rig.memberNetwork(WirelessAeTestRig.FAR_2) != null, "the AE2 blocks were not bound");
                })
                .thenWaitUntil(() -> h.assertTrue(
                        rig.inTheGrid(WirelessAeTestRig.FAR) && rig.inTheGrid(WirelessAeTestRig.FAR_2),
                        "the bound interface and pattern provider did not join the grid"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void wirelessUnboundTargetDoesNotJoinAndAnInvalidBlockIsRefused(GameTestHelper h) {
        var rig = new WirelessAeTestRig(h);
        rig.place(WirelessAeTestRig.FAR, GTAEMachines.ITEM_IMPORT_BUS_ME);
        rig.place(WirelessAeTestRig.FAR_2, GTAEMachines.ITEM_IMPORT_BUS_ME);
        h.setBlock(WirelessAeTestRig.FAR_3, Blocks.STONE);
        h.startSequence()
                .thenWaitUntil(() -> {
                    rig.ready();
                    h.assertTrue(rig.hasNode(WirelessAeTestRig.FAR) && rig.hasNode(WirelessAeTestRig.FAR_2),
                            "no nodes yet");
                })
                .thenExecute(() -> {
                    rig.select();
                    rig.bind(WirelessAeTestRig.FAR);
                    rig.bind(WirelessAeTestRig.FAR_3);
                    h.assertTrue(rig.memberNetwork(WirelessAeTestRig.FAR_3) == null,
                            "a block without a grid node was bound");
                })
                .thenWaitUntil(() -> h.assertTrue(rig.inTheGrid(WirelessAeTestRig.FAR), "the bound part did not join"))
                .thenExecute(() -> {
                    rig.stock(IRON, 3);
                    h.assertTrue(!rig.inTheGrid(WirelessAeTestRig.FAR_2), "the unbound part joined the grid");
                    h.assertTrue(rig.memberNetwork(WirelessAeTestRig.FAR_2) == null, "the unbound part has a binding");
                    var own = rig.nodeAt(WirelessAeTestRig.FAR_2).getGrid();
                    h.assertTrue(own == null || own.getStorageService().getInventory().getAvailableStacks()
                            .get(IRON) == 0, "the unbound part sees the storage");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 700)
    public static void wirelessBindingSurvivesAPartReloadAndTheLinkComesBack(GameTestHelper h) {
        var rig = new WirelessAeTestRig(h);
        rig.place(WirelessAeTestRig.FAR, GTAEMachines.ITEM_IMPORT_BUS_ME);
        h.startSequence()
                .thenWaitUntil(() -> {
                    rig.ready();
                    h.assertTrue(rig.hasNode(WirelessAeTestRig.FAR), "the part has no node yet");
                })
                .thenExecute(() -> {
                    rig.select();
                    rig.bind(WirelessAeTestRig.FAR);
                })
                .thenWaitUntil(() -> h.assertTrue(rig.inTheGrid(WirelessAeTestRig.FAR), "the part did not join"))
                .thenExecute(() -> {
                    // EN: A reload builds a new machine from its saved tag: its node and the old link are gone.
                    // PT: Um recarregamento cria uma máquina nova a partir da tag salva: o nó e o link antigo somem.
                    // ZH: 重新加载会根据保存的标签创建新机器：节点和旧链接都已消失。
                    CompoundTag saved = h.getBlockEntity(WirelessAeTestRig.FAR).saveWithFullMetadata();
                    rig.reloadBlockEntity(WirelessAeTestRig.FAR, saved);
                })
                .thenWaitUntil(() -> {
                    h.assertTrue(rig.hasNode(WirelessAeTestRig.FAR), "the new machine has no node yet");
                    h.assertTrue(rig.memberNetwork(WirelessAeTestRig.FAR) != null, "the binding was lost");
                    h.assertTrue(rig.inTheGrid(WirelessAeTestRig.FAR), "the link did not come back after the reload");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 700)
    public static void wirelessBindingSurvivesACoreReloadAndAWorldSave(GameTestHelper h) {
        var rig = new WirelessAeTestRig(h);
        rig.place(WirelessAeTestRig.FAR, GTAEMachines.ITEM_IMPORT_BUS_ME);
        var frequency = new java.util.UUID[1];
        h.startSequence()
                .thenWaitUntil(() -> {
                    rig.ready();
                    h.assertTrue(rig.hasNode(WirelessAeTestRig.FAR), "the part has no node yet");
                })
                .thenExecute(() -> {
                    rig.select();
                    rig.bind(WirelessAeTestRig.FAR);
                    frequency[0] = rig.core().getFrequency();
                })
                .thenWaitUntil(() -> h.assertTrue(rig.inTheGrid(WirelessAeTestRig.FAR), "the part did not join"))
                .thenExecute(() -> {
                    var server = h.getLevel().getServer();
                    var before = WirelessAeSavedData.get(server);
                    var after = WirelessAeSavedData.load(before.save(new CompoundTag()));
                    h.assertTrue(after.getMembers(frequency[0]).equals(before.getMembers(frequency[0])) &&
                            after.getMemberCount(frequency[0]) == 1, "the members did not survive a world save");
                    h.assertTrue(after.getNetworkOwner(frequency[0]).equals(rig.player.getUUID()) &&
                            GlobalPos.of(h.getLevel().dimension(), h.absolutePos(WirelessAeTestRig.CORE))
                                    .equals(after.getCore(frequency[0])),
                            "the owner and the core did not survive a world save");
                    // EN: The core's own block entity is rebuilt from its tag, as when its chunk loads again.
                    // PT: O bloco-entidade do núcleo é recriado a partir da tag, como quando o chunk carrega de novo.
                    // ZH: 核心自身的方块实体会根据标签重建，就像其区块再次加载时一样。
                    rig.reloadCore(rig.core().saveWithFullMetadata());
                })
                .thenWaitUntil(() -> {
                    h.assertTrue(rig.core().getFrequency().equals(frequency[0]), "the core changed its network id");
                    h.assertTrue(rig.core().isLinkedToAeNetwork(), "the reloaded core is not plugged in");
                    h.assertTrue(rig.memberNetwork(WirelessAeTestRig.FAR) != null, "the binding was lost");
                    h.assertTrue(rig.inTheGrid(WirelessAeTestRig.FAR),
                            "the member did not rejoin after the core reload");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void wirelessSneakPlacingWithTheToolInTheInventoryBindsTheNewPart(GameTestHelper h) {
        var rig = new WirelessAeTestRig(h);
        h.startSequence()
                .thenWaitUntil(rig::ready)
                .thenExecute(() -> {
                    rig.select();
                    // The tool goes to a backpack slot: only the main hand intercepts the click, the rest binds on
                    // placement.
                    rig.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    rig.player.getInventory().setItem(5, rig.tool);
                    rig.player.setShiftKeyDown(false);
                    place(h, rig, WirelessAeTestRig.FAR_2);
                    rig.player.setShiftKeyDown(true);
                    place(h, rig, WirelessAeTestRig.FAR);
                })
                .thenWaitUntil(() -> {
                    h.assertTrue(rig.hasNode(WirelessAeTestRig.FAR), "the placed part has no node yet");
                    h.assertTrue(rig.memberNetwork(WirelessAeTestRig.FAR) != null,
                            "sneak-placing did not bind the part");
                    h.assertTrue(rig.inTheGrid(WirelessAeTestRig.FAR), "the sneak-placed part did not join the grid");
                })
                .thenExecute(() -> {
                    h.assertTrue(rig.memberNetwork(WirelessAeTestRig.FAR_2) == null,
                            "placing without sneaking bound the part");
                    h.assertTrue(!rig.inTheGrid(WirelessAeTestRig.FAR_2), "the part placed standing joined the grid");
                })
                .thenSucceed();
    }

    /** Places an ME bus and posts the event the game posts when a player places a block. */
    private static void place(GameTestHelper h, WirelessAeTestRig rig, BlockPos at) {
        rig.place(at, GTAEMachines.ITEM_IMPORT_BUS_ME);
        BlockPos abs = h.absolutePos(at);
        var snapshot = BlockSnapshot.create(h.getLevel().dimension(), h.getLevel(), abs);
        MinecraftForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(snapshot, Blocks.AIR.defaultBlockState(),
                rig.player));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 500)
    public static void wirelessRemovingTheCoreDisconnectsItsMembersCleanly(GameTestHelper h) {
        var rig = new WirelessAeTestRig(h);
        rig.place(WirelessAeTestRig.FAR, GTAEMachines.ITEM_IMPORT_BUS_ME);
        h.setBlock(WirelessAeTestRig.FAR_2, AEBlocks.INTERFACE.block());
        var frequency = new java.util.UUID[1];
        h.startSequence()
                .thenWaitUntil(() -> {
                    rig.ready();
                    h.assertTrue(rig.hasNode(WirelessAeTestRig.FAR) && rig.hasNode(WirelessAeTestRig.FAR_2),
                            "no nodes yet");
                })
                .thenExecute(() -> {
                    frequency[0] = rig.core().getFrequency();
                    rig.select();
                    rig.bind(WirelessAeTestRig.FAR);
                    rig.bind(WirelessAeTestRig.FAR_2);
                })
                .thenWaitUntil(() -> h.assertTrue(
                        rig.inTheGrid(WirelessAeTestRig.FAR) && rig.inTheGrid(WirelessAeTestRig.FAR_2),
                        "the members did not join"))
                .thenExecute(() -> h.setBlock(WirelessAeTestRig.CORE, Blocks.AIR))
                .thenWaitUntil(() -> {
                    h.assertTrue(!rig.inTheGrid(WirelessAeTestRig.FAR) && !rig.inTheGrid(WirelessAeTestRig.FAR_2),
                            "the members are still in the grid after the core was removed");
                    var data = WirelessAeSavedData.get(h.getLevel().getServer());
                    h.assertTrue(data.getCore(frequency[0]) == null && data.getMemberCount(frequency[0]) == 0,
                            "the removed core left its network behind");
                    h.assertTrue(rig.memberNetwork(WirelessAeTestRig.FAR) == null &&
                            rig.memberNetwork(WirelessAeTestRig.FAR_2) == null,
                            "the members kept a binding to the removed core");
                })
                .thenExecute(() -> {
                    // A new core is a new network: the old members must not come back to it.
                    h.setBlock(WirelessAeTestRig.CORE, WirelessAeContent.WIRELESS_NETWORK_CORE.get());
                })
                .thenWaitUntil(() -> {
                    h.assertTrue(rig.core().isLinkedToAeNetwork(), "the new core is not plugged in");
                    h.assertTrue(!rig.core().getFrequency().equals(frequency[0]), "the new core reused the old id");
                })
                .thenExecute(() -> h.assertTrue(
                        !rig.inTheGrid(WirelessAeTestRig.FAR) && !rig.inTheGrid(WirelessAeTestRig.FAR_2),
                        "an old member joined the new core"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void wirelessPlayerOfAnotherTeamCannotSelectOrBindToTheNetwork(GameTestHelper h) {
        var rig = new WirelessAeTestRig(h);
        rig.place(WirelessAeTestRig.FAR, GTAEMachines.ITEM_IMPORT_BUS_ME);
        var stranger = WirelessAeTestRig.newPlayer(h);
        var strangerTool = new ItemStack(WirelessAeContent.WIRELESS_NETWORK_BINDING_TOOL.get());
        h.startSequence()
                .thenWaitUntil(() -> {
                    rig.ready();
                    h.assertTrue(rig.hasNode(WirelessAeTestRig.FAR), "the part has no node yet");
                })
                .thenExecute(() -> {
                    rig.select(stranger, strangerTool);
                    h.assertTrue(WirelessNetworkBindingToolItem.savedNetwork(strangerTool) == null,
                            "a player of another team saved the network of the core");
                    // A tool that already carries the network (a copy of the owner's) is no key either.
                    rig.select();
                    var copied = rig.tool.copy();
                    h.assertTrue(WirelessNetworkBindingToolItem.savedNetwork(copied) != null, "owner's tool is empty");
                    rig.click(stranger, copied, WirelessAeTestRig.FAR, false);
                    h.assertTrue(rig.memberNetwork(WirelessAeTestRig.FAR) == null,
                            "a player of another team bound a part to the network");
                })
                .thenExecute(() -> rig.stock(IRON, 2))
                .thenExecute(() -> {
                    h.assertTrue(!rig.inTheGrid(WirelessAeTestRig.FAR), "the refused part joined the grid");
                    rig.bind(WirelessAeTestRig.FAR);
                    h.assertTrue(rig.memberNetwork(WirelessAeTestRig.FAR) != null,
                            "the owner could not bind after the stranger was refused");
                })
                .thenSucceed();
    }
}
