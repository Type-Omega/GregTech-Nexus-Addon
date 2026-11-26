package com.raishxn.gtna.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IDataInfoProvider;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableLaserContainer;
import com.gregtechceu.gtceu.common.item.PortableScannerBehavior;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.SelectorWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Debug Energy Hatch and Debug Laser Target Hatch, for testing multiblocks. They follow GTMThings' Creative Energy
 * Hatch and Creative Laser Hatch (Hepdd, LGPL-3.0, see THIRD_PARTY_NOTICES.md): a real input-only hatch part with a
 * receiver buffer of 16 ticks (energy) or 64 ticks (laser) of voltage x amperage. Recipes draw from that buffer and
 * it is topped up on every server tick; it never pushes energy into its neighbours.
 *
 * <p>
 * The GUI has the same controls as GTMThings: tier selector, free voltage, amperage with -/+. Unlike GTMThings, a
 * change is applied at once and a formed controller is re-formed, because it caches the hatch voltage.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DebugEnergyHatchPartMachine extends TieredIOPartMachine implements IDataInfoProvider {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            DebugEnergyHatchPartMachine.class, TieredIOPartMachine.MANAGED_FIELD_HOLDER);

    /** Cap on the buffer so a multiblock summing hatches cannot overflow a long. */
    private static final long CAPACITY_LIMIT = Long.MAX_VALUE >> 8;
    private static final int MAX_AMPERAGE = 67_108_864;

    @Persisted
    public final NotifiableEnergyContainer energyContainer;
    private final boolean laser;
    @Persisted
    @DescSynced
    private long voltage = GTValues.V[GTValues.MAX];
    @Persisted
    @DescSynced
    private int amperage;
    @Persisted
    @DescSynced
    private int setTier = GTValues.MAX;
    private TickableSubscription refillSub;

    public DebugEnergyHatchPartMachine(IMachineBlockEntity holder, boolean laser) {
        super(holder, GTValues.MAX, IO.IN);
        this.laser = laser;
        this.amperage = laser ? 4096 : 2;
        // EN: a laser hatch is an ILaserContainer so lasers can feed it; both are plain receivers (no output).
        // PT: a escotilha laser e um ILaserContainer para lasers poderem alimenta-la; ambas so recebem (sem saida).
        // ZH: 激光仓是 ILaserContainer，激光可以向其供能；两者都只接收，不输出。
        long capacity = capacity(voltage, amperage);
        this.energyContainer = laser ?
                NotifiableLaserContainer.receiverContainer(this, capacity, voltage, amperage) :
                NotifiableEnergyContainer.receiverContainer(this, capacity, voltage, amperage);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        applySettings();
        refillSub = subscribeServerTick(refillSub, this::refill);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (refillSub != null) {
            refillSub.unsubscribe();
            refillSub = null;
        }
    }

    public boolean isLaser() {
        return laser;
    }

    public int getVoltageTier() {
        return setTier;
    }

    public long getVoltage() {
        return voltage;
    }

    public int getDebugAmperage() {
        return amperage;
    }

    private int minTier() {
        return laser ? GTValues.IV : GTValues.ULV;
    }

    private long minVoltage() {
        return laser ? GTValues.V[GTValues.IV] : GTValues.V[GTValues.ULV];
    }

    private int minAmperage() {
        return laser ? 256 : 1;
    }

    /** Picks a tier and its full voltage, like the tier selector. */
    public void setVoltageTier(int tier) {
        setTier = Math.max(minTier(), Math.min(GTValues.MAX, tier));
        voltage = GTValues.V[setTier];
        changed();
    }

    public void setVoltage(long value) {
        voltage = Math.max(minVoltage(), value);
        setTier = Math.max(minTier(), Math.min(GTValues.MAX, GTUtil.getTierByVoltage(voltage)));
        changed();
    }

    public void setDebugAmperage(int value) {
        amperage = Math.max(minAmperage(), Math.min(MAX_AMPERAGE, value));
        changed();
    }

    private void changed() {
        applySettings();
        reformControllers();
    }

    /** Sets the input voltage, amperage and buffer, and fills the buffer. */
    private void applySettings() {
        energyContainer.resetBasicInfo(capacity(voltage, amperage), voltage, amperage, 0, 0);
        energyContainer.setEnergyStored(energyContainer.getEnergyCapacity());
    }

    /** The server tick: keeps the settings applied and tops the buffer up to full after recipes drew from it. */
    public void refill() {
        if (energyContainer.getInputVoltage() != voltage || energyContainer.getInputAmperage() != amperage) {
            applySettings();
            return;
        }
        long capacity = energyContainer.getEnergyCapacity();
        if (energyContainer.getEnergyStored() < capacity) {
            energyContainer.setEnergyStored(capacity);
        }
    }

    /** Ticks of input the buffer holds: 16 for an energy hatch, 64 for a laser hatch; saturating. */
    private long capacity(long volts, long amps) {
        long ticks = laser ? 64L : 16L;
        if (volts <= 0 || amps <= 0) return 0;
        if (volts > CAPACITY_LIMIT / ticks / amps) return CAPACITY_LIMIT;
        return volts * ticks * amps;
    }

    /**
     * The controllers cache the hatch voltage and its recipe handlers when they form, so a formed multiblock is
     * re-formed at once. GTCEu's {@code onPartUnload} alone only drops the handlers and waits for the next
     * structure check, and the machine would not see the hatch as an energy input until then.
     */
    private void reformControllers() {
        if (isRemote()) return;
        for (var controller : new ArrayList<>(getControllers())) {
            if (!controller.isFormed()) continue;
            controller.onStructureInvalid();
            if (controller.checkPatternWithLock()) {
                if (controller instanceof MultiblockControllerMachine machine) {
                    machine.setFlipped(machine.getMultiblockState().isNeededFlip());
                }
                controller.onStructureFormed();
            } else {
                controller.onPartUnload();
            }
        }
    }

    @Override
    public int tintColor(int index) {
        if (index == 2) {
            return GTValues.VC[getTier()];
        }
        return super.tintColor(index);
    }

    @Override
    public boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        return true;
    }

    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 176, 136);
        group.addWidget(new LabelWidget(7, 32, "gtceu.creative.energy.voltage"));
        group.addWidget(new TextFieldWidget(9, 47, 152, 16, () -> String.valueOf(voltage),
                value -> setVoltage(Long.parseLong(value))).setNumbersOnly(minVoltage(), Long.MAX_VALUE));
        group.addWidget(new LabelWidget(7, 74, "gtceu.creative.energy.amperage"));
        group.addWidget(new ButtonWidget(7, 87, 20, 20,
                new GuiTextureGroup(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("-")),
                cd -> setDebugAmperage(amperage - 1)));
        group.addWidget(new TextFieldWidget(31, 89, 114, 16, () -> String.valueOf(amperage),
                value -> setDebugAmperage(Integer.parseInt(value))).setNumbersOnly(minAmperage(), MAX_AMPERAGE));
        group.addWidget(new ButtonWidget(149, 87, 20, 20,
                new GuiTextureGroup(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("+")),
                cd -> setDebugAmperage(amperage + 1)));
        group.addWidget(new LabelWidget(7, 112, () -> Component.translatable("gtna.machine.debug_energy_hatch.rate",
                FormattingUtil.formatNumbers(voltage * (long) amperage)).getString()));
        var tiers = Arrays.asList(GTValues.VNF).subList(minTier(), GTValues.MAX + 1);
        group.addWidget(new SelectorWidget(7, 7, 50, 20, tiers, -1)
                .setOnChanged(tier -> setVoltageTier(minTier() + tiers.indexOf(tier)))
                .setSupplier(() -> GTValues.VNF[setTier])
                .setButtonBackground(ResourceBorderTexture.BUTTON_COMMON)
                .setBackground(ColorPattern.BLACK.rectTexture())
                .setValue(GTValues.VNF[setTier]));
        return group;
    }

    @Override
    public List<Component> getDataInfo(PortableScannerBehavior.DisplayMode mode) {
        if (mode != PortableScannerBehavior.DisplayMode.SHOW_ALL &&
                mode != PortableScannerBehavior.DisplayMode.SHOW_ELECTRICAL_INFO) {
            return new ArrayList<>();
        }
        return Collections.singletonList(Component.literal(
                String.format("%d/%d EU", energyContainer.getEnergyStored(), energyContainer.getEnergyCapacity())));
    }
}
