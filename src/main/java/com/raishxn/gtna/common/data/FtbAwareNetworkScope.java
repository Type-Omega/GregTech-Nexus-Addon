package com.raishxn.gtna.common.data;

import com.gregtechceu.gtceu.GTCEu;

import com.raishxn.gtna.integration.ftb.FTBWirelessTeams;
import com.raishxn.gtna.research.KnowledgeScope;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** The scope the saved networks use in game: the effective FTB team, or the player without FTB Teams. */
final class FtbAwareNetworkScope implements WirelessNetworkScope {

    static final WirelessNetworkScope INSTANCE = new FtbAwareNetworkScope();

    private FtbAwareNetworkScope() {}

    @Override
    public UUID networkOf(UUID owner) {
        // EN: Checked first on purpose: without FTB Teams, GTCEu may answer with an Argonauts guild id, and
        // D-386 keeps those worlds per player.
        // PT: Testado primeiro de propósito: sem FTB Teams o GTCEu pode responder com o id de uma guilda do
        // Argonauts, e a D-386 mantém esses mundos por jogador.
        // ZH: 故意先检查：没有 FTB Teams 时 GTCEu 可能返回 Argonauts 公会 id，而 D-386 让这些世界保持按玩家划分。
        if (owner == null || !GTCEu.Mods.isFTBTeamsLoaded()) return owner;
        try {
            UUID scope = KnowledgeScope.of(owner);
            return scope == null ? owner : scope;
        } catch (RuntimeException | LinkageError e) {
            return owner;
        }
    }

    @Override
    public Collection<UUID> membersOf(UUID network) {
        if (network == null || !GTCEu.Mods.isFTBTeamsLoaded()) return List.of();
        try {
            return FTBWirelessTeams.members(network);
        } catch (RuntimeException | LinkageError e) {
            return List.of();
        }
    }
}
