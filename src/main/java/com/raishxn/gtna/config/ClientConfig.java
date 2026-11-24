package com.raishxn.gtna.config;

import dev.toma.configuration.config.Configurable;
import dev.toma.configuration.config.Configurable.Comment;
import dev.toma.configuration.config.Configurable.Range;

/** Client settings of GTNA ({@code gtna} config, section {@code client}). */
public class ClientConfig {

    @Configurable
    @Comment({ "En: Disable fly inertia when the player stops moving in the air.",
            "Pt: Remove a inercia do voo quando o jogador para de se mover no ar." })
    public boolean disableFlyInertia = true;

    @Configurable
    @Comment({ "En: Show the wireless steam network HUD (balance, flow and connected hatches).",
            "Pt: Mostra o HUD da rede wireless de vapor (saldo, fluxo e hatches conectados)." })
    public boolean wirelessSteamHud = false;

    @Configurable
    @Comment({ "En: Show the Nexus wireless energy HUD.", "Pt: Mostra o HUD de energia wireless do Nexus." })
    public boolean wirelessEnergyHud = false;

    @Configurable
    @Range(min = 0, max = 100)
    public int wirelessEnergyHudX = 5;

    @Configurable
    @Range(min = 0, max = 100)
    public int wirelessEnergyHudY = 60;

    @Configurable
    @Range(min = 0, max = 100)
    @Comment({ "En: Wireless steam HUD horizontal position: 0 = left edge, 100 = right edge.",
            "Pt: Posicao horizontal do HUD de vapor: 0 = esquerda, 100 = direita." })
    public int wirelessSteamHudX = 5;

    @Configurable
    @Range(min = 0, max = 100)
    @Comment({ "En: Wireless steam HUD vertical position: 0 = top edge, 100 = bottom edge.",
            "Pt: Posicao vertical do HUD de vapor: 0 = topo, 100 = baixo." })
    public int wirelessSteamHudY = 75;

    @Configurable
    @Range(min = 0, max = 600)
    @Comment({ "En: Seconds of balance history drawn as a graph in the HUD (0 hides the graph).",
            "Pt: Segundos de historico de saldo no grafico do HUD (0 esconde o grafico)." })
    public int wirelessSteamHudHistorySeconds = 60;

    @Configurable
    @Comment({ "En: Draw item halos (soft disc behind the icon) in inventories.",
            "Pt: Desenha halos de item (disco suave atras do icone) nos inventarios." })
    public boolean itemHalo = true;

    @Configurable
    @Comment({ "En: Draw item glow edges (tinted outline behind the icon) in inventories.",
            "Pt: Desenha bordas brilhantes de item (contorno colorido atras do icone) nos inventarios." })
    public boolean itemGlowEdge = true;

    @Configurable
    @Comment({ "En: Draw animated item shader effects (cosmic, eternal, unstable).",
            "Pt: Desenha efeitos animados de shader nos itens (cosmic, eternal, unstable)." })
    public boolean itemShaderEffects = true;

    @Configurable
    @Range(min = 0, max = 400)
    @Comment({ "En: Item effect animation speed in percent (0 freezes the animation).",
            "Pt: Velocidade da animacao dos efeitos de item em porcentagem (0 congela a animacao)." })
    public int itemEffectSpeed = 100;

    @Configurable
    @Comment({ "En: Level of the GTNA machine renders (stars, beams, holograms, fields).",
            "En: OFF hides the effects only: parts taken out of the world (Forge of Gods rings, collider ring,",
            "En: lance gun) stay drawn, and the Stellar Chamber's gravity and sound stay on.",
            "En: LOW draws the effects up to half the distance and skips the expensive passes.",
            "Pt: Nivel dos renders das maquinas do GTNA (estrelas, feixes, hologramas, campos).",
            "Pt: OFF esconde so os efeitos: pecas tiradas do mundo continuam desenhadas.",
            "Pt: LOW desenha os efeitos ate metade da distancia e pula as passadas caras." })
    public MachineRenderLevel machineRenders = MachineRenderLevel.HIGH;

    public enum MachineRenderLevel {
        OFF,
        LOW,
        HIGH
    }
}
