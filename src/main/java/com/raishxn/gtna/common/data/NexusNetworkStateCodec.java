package com.raishxn.gtna.common.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import com.raishxn.gtna.common.data.NexusEnergyNetwork.DirectDebit;
import com.raishxn.gtna.common.data.NexusEnergyNetwork.NetworkState;
import com.typeomega.lib.data.Int128;

import java.math.BigInteger;

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
     * EN: Stored EU is summed. The team keeps its own Flux Matrix; it takes the member's only when it has none
     * formed, so a matrix is never lost. EU above the capacity stays stored and only blocks new input.
     * PT: O EU guardado é somado. O time fica com a própria Flux Matrix; só herda a do membro se não tiver uma
     * formada, então nenhuma matriz se perde. EU acima da capacidade continua guardado e só bloqueia entrada nova.
     * ZH: 存储的 EU 相加。队伍保留自己的 Flux Matrix；只有在自身未成型时才继承成员的矩阵，因此矩阵不会丢失。
     * 超出容量的 EU 仍然保存，只会阻止新的输入。
     */
    static NetworkState merge(NetworkState team, NetworkState member) {
        team.energy = team.energy.add(member.energy);
        if (!team.matrixFormed && member.matrixFormed) {
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
}
