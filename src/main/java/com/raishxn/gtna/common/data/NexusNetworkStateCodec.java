package com.raishxn.gtna.common.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import com.raishxn.gtna.common.data.NexusEnergyNetwork.DirectDebit;
import com.raishxn.gtna.common.data.NexusEnergyNetwork.NetworkState;
import com.typeomega.lib.data.Int128;

import java.math.BigInteger;
import java.util.Map;

/** Save format of one Nexus network and the merge of a member's network into a team's (GTIA-D-386). */
final class NexusNetworkStateCodec {

    private NexusNetworkStateCodec() {}

    static NetworkState read(CompoundTag entry) {
        NetworkState state = new NetworkState();
        try {
            state.energy = new BigInteger(entry.getString("Amount")).max(BigInteger.ZERO);
        } catch (NumberFormatException ignored) {
            state.energy = BigInteger.ZERO;
        }
        state.maxCapacity = Int128.fromString(entry.getString("MaxCapacity"), Int128.ZERO());
        state.totalCapacitors = entry.getLong("TotalCapacitors");
        state.averageTier = entry.getInt("AvgTier");
        state.efficiency = entry.getDouble("Efficiency");
        state.transferLimit = Int128.fromString(entry.getString("TransferLimit"), Int128.ZERO());
        state.matrixFormed = entry.getBoolean("MatrixFormed");
        state.unlimited = entry.getBoolean("Unlimited") && state.matrixFormed && state.totalCapacitors == 750 &&
                state.averageTier == 14;
        state.matrixDimension = entry.getString("MatrixDimension");
        state.lossRemainder = Math.max(0, Math.min(9_999, entry.getInt("LossRemainder")));
        readMatrices(entry, state.matrices);
        if (!state.matrices.isEmpty()) applyMatrices(state);
        if (entry.contains("DirectDebit", Tag.TAG_COMPOUND)) {
            var debit = entry.getCompound("DirectDebit");
            var dimension = ResourceLocation.tryParse(debit.getString("Dimension"));
            if (dimension != null) {
                state.lastDirectDebit = new DirectDebit(GlobalPos.of(ResourceKey.create(Registries.DIMENSION,
                        dimension), BlockPos.of(debit.getLong("Position"))), debit.getString("MachineType"),
                        Int128.fromString(debit.getString("Amount"), Int128.ZERO()), debit.getLong("Tick"));
            }
        }
        return state;
    }

    static CompoundTag write(NetworkState state) {
        CompoundTag entry = new CompoundTag();
        entry.putString("Amount", state.energy.toString());
        entry.putString("MaxCapacity", state.maxCapacity.toString());
        entry.putLong("TotalCapacitors", state.totalCapacitors);
        entry.putInt("AvgTier", state.averageTier);
        entry.putDouble("Efficiency", state.efficiency);
        entry.putString("TransferLimit", state.transferLimit.toString());
        entry.putBoolean("MatrixFormed", state.matrixFormed);
        entry.putBoolean("Unlimited", state.unlimited);
        entry.putString("MatrixDimension", state.matrixDimension);
        entry.putInt("LossRemainder", state.lossRemainder);
        writeMatrices(entry, state.matrices);
        if (state.lastDirectDebit != null) {
            var info = state.lastDirectDebit;
            var debit = new CompoundTag();
            debit.putString("Dimension", info.source().dimension().location().toString());
            debit.putLong("Position", info.source().pos().asLong());
            debit.putString("MachineType", info.machineType());
            debit.putString("Amount", info.amount().toString());
            debit.putLong("Tick", info.tick());
            entry.put("DirectDebit", debit);
        }
        return entry;
    }

    /**
     * EN: Stored EU is summed and the member's Flux Matrices join the team's, which then sum (GTIA-D-426). A save
     * from before 0.6.2 has no matrix list: the team keeps its own matrix and takes the member's only when it has
     * none; the next time a matrix forms, the list replaces it. EU above the capacity stays stored and only blocks
     * new input.
     * PT: O EU guardado é somado e as Flux Matrix do membro entram nas do time, que passam a somar (GTIA-D-426). Um
     * save anterior à 0.6.2 não tem a lista de matrizes: o time fica com a própria e só herda a do membro se não
     * tiver nenhuma; na próxima formação de uma matriz, a lista a substitui. EU acima da capacidade continua guardado
     * e só bloqueia entrada nova.
     * ZH: 存储的 EU 相加，成员的 Flux Matrix 并入队伍的矩阵并一起累加（GTIA-D-426）。0.6.2 之前的存档没有矩阵列表：
     * 队伍保留自己的矩阵，只有在没有矩阵时才继承成员的；下次有矩阵成型时，列表会取而代之。超出容量的 EU 仍然保存，
     * 只会阻止新的输入。
     */
    static NetworkState merge(NetworkState team, NetworkState member) {
        team.energy = team.energy.add(member.energy);
        member.matrices.forEach(team.matrices::putIfAbsent);
        if (!team.matrices.isEmpty()) {
            applyMatrices(team);
        } else if (!team.matrixFormed && member.matrixFormed) {
            team.matrixFormed = true;
            team.maxCapacity = member.maxCapacity.copy();
            team.totalCapacitors = member.totalCapacitors;
            team.averageTier = member.averageTier;
            team.efficiency = member.efficiency;
            team.transferLimit = member.transferLimit.copy();
            team.matrixDimension = member.matrixDimension;
            team.unlimited = member.unlimited;
        }
        if (member.lastDirectDebit != null &&
                (team.lastDirectDebit == null || member.lastDirectDebit.tick() > team.lastDirectDebit.tick())) {
            team.lastDirectDebit = member.lastDirectDebit;
        }
        member.connections.forEach(team.connections::putIfAbsent);
        return team;
    }

    /** Sets the network's capacity and stats to the sum of its matrices, or clears them when none is left. */
    static void applyMatrices(NetworkState state) {
        FluxMatrices.Matrix sum = FluxMatrices.sum(state.matrices.values());
        state.matrixFormed = sum != null;
        state.maxCapacity = sum == null ? Int128.ZERO() : sum.capacity().copy();
        state.totalCapacitors = sum == null ? 0 : sum.capacitors();
        state.averageTier = sum == null ? 0 : sum.averageTier();
        state.efficiency = sum == null ? 0.0 : sum.efficiency();
        state.transferLimit = sum == null ? Int128.ZERO() : sum.transferLimit().copy();
        state.matrixDimension = sum == null ? "" : sum.dimension();
        state.unlimited = sum != null && sum.unlimited();
    }

    private static void readMatrices(CompoundTag entry, Map<GlobalPos, FluxMatrices.Matrix> matrices) {
        if (!entry.contains("Matrices", Tag.TAG_LIST)) return;
        ListTag list = entry.getList("Matrices", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            var dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
            if (dimension == null) continue;
            matrices.put(GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimension),
                    BlockPos.of(tag.getLong("Position"))),
                    new FluxMatrices.Matrix(tag.getLong("Capacitors"), tag.getInt("AvgTier"),
                            tag.getDouble("Efficiency"), Int128.fromString(tag.getString("Capacity"), Int128.ZERO()),
                            Int128.fromString(tag.getString("TransferLimit"), Int128.ZERO()),
                            tag.getBoolean("Unlimited"), dimension.toString()));
        }
    }

    private static void writeMatrices(CompoundTag entry, Map<GlobalPos, FluxMatrices.Matrix> matrices) {
        if (matrices.isEmpty()) return;
        ListTag list = new ListTag();
        matrices.forEach((pos, matrix) -> {
            CompoundTag tag = new CompoundTag();
            tag.putString("Dimension", pos.dimension().location().toString());
            tag.putLong("Position", pos.pos().asLong());
            tag.putLong("Capacitors", matrix.capacitors());
            tag.putInt("AvgTier", matrix.averageTier());
            tag.putDouble("Efficiency", matrix.efficiency());
            tag.putString("Capacity", matrix.capacity().toString());
            tag.putString("TransferLimit", matrix.transferLimit().toString());
            tag.putBoolean("Unlimited", matrix.unlimited());
            list.add(tag);
        });
        entry.put("Matrices", list);
    }
}
