package com.raishxn.gtna.common.data.tooltip;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.network.chat.Component;

import com.raishxn.gtna.common.machine.multiblock.electric.bec.BECAssemblerMachine;
import com.raishxn.gtna.common.machine.multiblock.electric.bec.BECDiodeMachine;
import com.raishxn.gtna.common.machine.multiblock.electric.bec.BECIONodeMachine;
import com.raishxn.gtna.common.machine.multiblock.electric.bec.BECStorageMachine;

import java.util.List;

import static com.raishxn.gtna.common.data.tooltip.Fmt.num;
import static com.raishxn.gtna.common.data.tooltip.Fmt.pct;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.finish;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.own;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.start;
import static com.typeomega.lib.tooltip.TooltipArg.value;

/** The Bose-Einstein Condensate network, from the GTNH BEC tooltip pages (GT5-Unofficial, LGPL-3.0). */
final class BECTooltips {

    private BECTooltips() {}

    static void register() {
        GTNATooltips.register("bec_assembler", BECTooltips::assembler);
        GTNATooltips.register("bec_storage", BECTooltips::storage);
        GTNATooltips.register("bec_generator", BECTooltips::generator);
        GTNATooltips.register("bec_io_node", BECTooltips::ioNode);
        GTNATooltips.register("bec_diode", BECTooltips::diode);
    }

    private static List<Component> assembler() {
        String id = "bec_assembler";
        var b = start("bec_assembler").line(own(id, "purpose")).line(own(id, "nanites"))
                .line(own(id, "lowest")).line(own(id, "max"), value(num(BECAssemblerMachine.MAX_NANITES)))
                .separator()
                .warning(own(id, "stall"))
                .line(own(id, "condensate"))
                .warning(own(id, "void"))
                .line(own(id, "slow"))
                .warning(own(id, "clog"), value(BECIONodeMachine.MAX_UNREQUESTED_CONDENSATES));
        return finish(b, id);
    }

    private static List<Component> storage() {
        String id = "bec_storage";
        var b = start("bec_storage").line(own(id, "purpose")).line(own(id, "route")).separator()
                .line(own(id, "capacity")).line(own(id, "unit"))
                .line(own(id, "split"))
                .separator()
                .warning(own(id, "overfill"), value(pct(BECStorageMachine.OVERFILL_LOSS)))
                .warning(own(id, "shutdown"))
                .line(own(id, "detector"));
        return finish(b, id);
    }

    private static List<Component> generator() {
        String id = "bec_generator";
        var b = start("bec_generator").line(own(id, "purpose")).line(own(id, "transfer"))
                .warning(own(id, "always")).line(own(id, "stored")).separator()
                .line(own(id, "eu")).line(own(id, "time"));
        return finish(b, id);
    }

    private static List<Component> ioNode() {
        String id = "bec_io_node";
        var b = start("bec_io_node").line(own(id, "purpose")).line(own(id, "logic")).line(own(id, "start"))
                .line(own(id, "link")).separator()
                .line(own(id, "slot_tier")).line(own(id, "excess")).warning(own(id, "higher"))
                .line(own(id, "progress"))
                .line(own(id, "eu")).line(own(id, "parallel_eu")).warning(own(id, "no_overclock"))
                .separator()
                .line(own(id, "min_max")).line(own(id, "divisor"))
                .line(own(id, "nanite_detector")).line(own(id, "pause"));
        return finish(b, id);
    }

    private static List<Component> diode() {
        String id = "bec_diode";
        var b = start("bec_diode").line(own(id, "purpose")).line(own(id, "direction")).separator()
                .line(own(id, "filters")).line(own(id, "no_filter")).line(own(id, "fluid_override"))
                .separator()
                .line(own(id, "toggle"))
                .line(own(id, "power"), value(BECDiodeMachine.AMPS + "A " + GTValues.VN[GTValues.UIV]));
        return finish(b, id);
    }
}
