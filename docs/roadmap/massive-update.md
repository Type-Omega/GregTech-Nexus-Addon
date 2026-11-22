# Massive Update — GTNA + Ascension Optimizer

Roadmap de trabalho para a próxima grande versão. Marque `- [x]` ao concluir; mantenha curto (substitua, não acrescente log).
Estado atual: [STATE.md](../STATE.md) · QA manual: [manual-qa.md](../testing/manual-qa.md).

Regra de ouro: **nada de conteúdo novo até as fases 0–2 fecharem.** Toda otimização entra com número de antes/depois.

## Fase 0 — Base de medição e gate

- [x] Gate falha sozinho quando receita `gtna:` não carrega ou GameTest falha (task `gate` no Gradle).
- [ ] Mundo de benchmark reprodutível (dedicated server, pack GTIA completa, mediana de 3 boots, `perf_bench.py`).
- [ ] Baseline salva: tempo de boot, MSPT ocioso, heap após GC, FPS do client e reload do EMI.
- [ ] Cenários fixos de spark (ver manual-qa.md § Performance): 200 hatches wireless ociosos, 50 Pattern Buffers, 2.000 fornalhas LV, 1 EoH rodando, inventário com itens de efeito.

## Fase 1 — Fechar 0.6.1 e definir escopo

- [ ] 0.6.1: requisito TypeOmega Lib, crash sem JEI, 10 receitas, cover check 8,37 → 3,74 ms/tick, cache de preview.
- [ ] Decidir o corte da Massive Update: fases A e B do port (DTPF Convergence, ISA Mill tiers). D–G ficam para depois.
- [ ] Originais WIP (Lance, Stellar Containment, Antimatter Ring, Rift Gate): passam no QA P0 ou ficam atrás de config desligada.
- [ ] PWR: entra completo ou fica fora (não anunciar).

## Fase 2 — Robustez do código

- [ ] Dividir por responsabilidade: `GTNAMachineGameTests` (por sistema), `AnnihilateGeneratorA/B`, `GTNAMachines`, `GTNAGTOPatterns`, `GTNAMachineRecipes`, `GTNALangProvider`.
- [ ] Teste de server dedicado sem client, sem JEI e só com EMI no CI.
- [ ] Avaliar mover a geração dos `AnnihilateGenerator` para o datagen (menos trabalho no boot).

## Fase 3 — Performance do GTNA (guiada pelo spark)

Ordem vinda da auditoria de tick de 2026-11-21. Os custos ainda não foram medidos: medir cada item antes e depois.
- [ ] **WirelessSteamInputHatch** (`:103,160-185`, `SteamNetworkData:211-224`): a contagem de inputs ativos é O(N²) por tick. Cachear por dono e por gameTime e dormir com o tanque cheio. Cenário: 500 hatches.
- [ ] **NexusEnergyNetwork + hatches wireless energy/dynamo**: alocação de BigInteger/Int128 por hatch por tick. Usar Int128 mutável no estado, `reportConnection` só em mudança ou a cada 20 ticks, pular quando não há déficit. Cenário: 200+200 hatches.
- [ ] **NexusNetworkTerminalPartMachine:74**: tica para sempre só para reportar zero.
- [ ] **FuelCellGeneratorMachine:78-88**: cria um GTRecipe novo a cada tick quando ociosa. Fazer a receita estática e rodar a cada 20 ticks.
- [ ] **MEStorageMachine / StorageAccessPart**: `getUsedBytes()` percorre todas as AEKeys a cada tick. Trocar por um contador incremental.
- [ ] **EyeOfHarmonyMachine:244-275**: `cycleProgress` com @DescSynced manda um pacote por tick. Sincronizar só para a GUI.
- [ ] **UniversiumRenderer:193-225**: re-emite os blocos a cada frame. Fazer bake num VertexBuffer por chunk.
- [ ] **ItemEffectRenderer:224-236,358-370**: reconstrói a lista de quads a cada render. Cachear por BakedModel (pesa em grids do JEI/EMI e do AE).
- [ ] **StellarChamberMachine:243**: busca de entidades por AABB a cada tick. Buscar a cada 2–5 ticks.
- [ ] **Pattern Buffer / CraftPattern parts**: ticam só para checar um boolean. Assinar só com `needPatternSync`.
- [ ] **BECAssembler:79**, **PlasmaForge:57**, **InfernalCokeOven:83**, **WirelessSteamHudSync:47**: ticks permanentes de baixo custo. Trocar por callback ou decay lazy e enviar o HUD só quando mudar.
- [ ] **AnnihilateGeneratorA/B**: são só constantes `String[]` do padrão da Artificial Star, lidas por reflexão. Mover para um recurso compactado com parse lazy e cache.
- [ ] Ainda falta auditar: ForgeOfGodsRenderer:300, OriginalsRenderer:430, previews de estrutura e `onElevatorTick` dos módulos.

## Fase 4 — Tooltips, explicações e UX

- [ ] Teste unitário: todo multibloco tem linha `Source:`, chave em en_us/pt_br/zh_cn, tier, parallel e sem placeholder.
- [ ] Padrão por multibloco: o que faz, hatches aceitos, como escala, tier mínimo, dica de uso.
- [ ] Explicação passo a passo (tooltip + wiki) para EoH, Godforge, Nano Forge, BEC, Dyson, Space Elevator.
- [ ] Sistemas próprios (Nexus, wireless steam, Pattern Buffer, módulos, hatches de Thread/Parallel/Overclock): tooltip de como ligar + quest de introdução no GT:IA.
- [ ] UI diz por que a máquina não forma ou não roda (hatch faltando, EU/t insuficiente, módulo ausente).

## Fase 5 — Ascension Optimizer

Cada feature: desligável em `ascopt-mixins.properties`, ignorada sem o mod alvo, com número medido.
- [ ] Hot paths do GTCEu: busca de receita, `RecipeLogic` ocioso, redes de pipe/cabo, `notifyBlockUpdate` em excesso.
- [ ] Alocações por tick nos mods grandes da pack (AE2 crafting, storage).
- [ ] Client: cache de tooltip do EMI, reload de recursos.
- [ ] `/ascopt report`: features ativas, ignoradas e por quê.
- [ ] Benchmark no CI com limite de regressão (falha se MSPT piorar X % em relação à baseline).

## Fase 6 — Lançamento

- [ ] QA completo (manual-qa.md P0 + P1) no client e em server dedicado com a pack inteira.
- [ ] Tabela antes/depois de performance no changelog das duas mods.
- [ ] CHANGELOG.md, STATE.md, wiki en/pt/zh atualizados; commit e push só após aprovação do autor.
