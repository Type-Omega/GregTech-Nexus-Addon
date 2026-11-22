# QA manual do GTNA

Roteiro de teste manual montado a partir do código (registros em `src/main/java/com/raishxn/gtna/common/data/`, nomes em `src/generated/resources/assets/gtna/lang/en_us.json`, fontes em `GTNASources`, cobertura de GameTest em `src/main/java/com/raishxn/gtna/gametest/`). Os IDs das máquinas ficam em inglês.

Contagem: **@@NT@@ controladores multibloco** registrados. P0 = @@N0@@ máquinas + sistemas · P1 = @@N1@@ (sem GameTest que cite a máquina) · P2 = @@N2@@ (com GameTest, só smoke check).

Observações:
- Todo multibloco GTNA já passa por dois GameTests genéricos (`everyGtnaMachineTooltipBuilds` e `everyMultiblockUsesTheStandardTooltip` em `GTNAMachineGameTests`), que checam que o tooltip monta e tem o texto padrão. Eles **não** checam formação nem receita, por isso a classificação P1/P2 usa só os GameTests que citam a máquina pelo ID ou pela constante.
- As receitas do GTNA são geradas em Java (não existe `data/gtna/recipe` em `src/`). Para "rodar uma receita", use a categoria do JEI/EMI indicada em cada item.

---

## 1. Preparação

- [ ] `./gradlew runClient --offline` na raiz do repositório.
- [ ] Criar mundo **Creative**, cheats ligados, tipo Superflat (menos lag para estruturas grandes).
- [ ] `/gamerule doDaylightCycle false`, `/gamerule doWeatherCycle false`, `/gamerule doMobSpawning false`.
- [ ] Item principal: **Nexus Structure Terminal** (`gtna:nexus_structure_terminal`). Clique direito abre a configuração dos blocos; **Shift + clique direito no controlador monta a estrutura** (no creative não consome itens).
- [ ] Preview da estrutura: JEI/EMI > aba do multibloco (preview GTCEu). Para conferir o que foi montado, use os comandos de debug abaixo.

### Comandos úteis (cliente, só singleplayer, `client/debug/PreviewBenchCommand.java`)

| Comando | O que faz |
|---|---|
| `/gtna_dev_build <id>` | Coloca o controlador 8 blocos à frente e roda o auto-build do Nexus Terminal como o jogador |
| `/gtna_dev_open` | Abre a UI do controlador da última build |
| `/gtna_dev_mismatch` | Lista posições erradas da última build de um original |
| `/gtna_dev_swap <id>` | Quebra um casing da última build e coloca aquela máquina (hatch) no buraco |
| `/gtna_dev_info` | Abre/fecha o painel de informação da tela de um original |
| `/gtna_dev_fps <s>` | Registra FPS do cliente e MSPT do servidor por s segundos no log |
| `/gtna_dev_wireless`, `/gtna_dev_endgame [collapse]`, `/gtna_dev_showcase`, `/gtna_dev_core`, `/gtna_dev_fire` | Bancadas de debug (ver o código antes de usar) |
| `/gtna_rift_preview [off]`, `/gtna_star_preview [hole\|nolens\|off]` | Previews de render do Rift Gate e da estrela |
| `/gtna_preview_open <id>` | Abre o preview do JEI direto (só com JEI) |

Comandos vanilla: `/give @s gtna:<id>`, `/tp`, `/time set day`, `/forceload add ~ ~` (para teste de chunk), `/kill @e[type=item]`.

### Como ler o log

- Arquivo: `run/logs/latest.log` (cliente) e `run/logs/latest.log` do servidor dedicado.
- Procurar: `grep -nE "ERROR|Exception|gtna" run/logs/latest.log | tail -100`.
- Sinais de bug: `Parsing error loading recipe gtna:`, `Missing translation`, stack trace com `com.raishxn.gtna`, `Can't keep up!` frequente.

### Modelo de relato de bug

```
Máquina (id): gtna:<id>
Versão/commit: <git rev-parse --short HEAD>
Lado: cliente singleplayer / servidor dedicado
Passos:
1.
2.
Esperado:
Obtido:
Trecho do log:
<cole as linhas do latest.log>
Screenshot: <anexo>
```

### Checklist padrão (aplicar em todo P0/P1)

1. Forma com o terminal e não há posição errada.
2. Tooltip correto em **en_us, pt_br e zh_cn** (trocar em Opções > Idioma), sem chave crua, e com a linha **Source:**.
3. Uma receita roda até o fim e a saída sai no hatch.
4. Paralelos/velocidade/consumo mostrados na UI batem com o tooltip.
5. Desformar (quebrar 1 casing) e reformar.
6. Descarregar/recarregar o chunk (afastar 20+ chunks ou `/forceload remove`) com receita rodando.
7. Salvar e sair, voltar: estado mantido.
8. Servidor dedicado (`./gradlew runServer --offline`, `eula=true` em `run/eula.txt`): forma, roda, sem crash no cliente que conecta.
9. Nenhum erro novo no log.

---

## 2. P0 — originais WIP, endgame e sistemas

### 2.1 Originais WIP (fonte GTNA, `GTNAOriginals.java`, tipo `dummy`, cobertos parcialmente por `GTNAOriginalsGameTests`)

Para os quatro:
- [ ] `/gtna_dev_build <id>` e depois `/gtna_dev_mismatch`: lista vazia.
- [ ] `/gtna_dev_open` abre a tela própria; `/gtna_dev_info` abre e fecha o painel sem sobrepor a moldura.
- [ ] Tooltip en/pt/zh com `Source: GTNA`.
- [ ] Servidor dedicado: conectar e abrir a tela (as telas usam render cliente; o servidor não pode carregar classe `net.minecraft.client`).
- [ ] FPS com o render ligado: `/gtna_dev_fps 30`, anotar mínimo.

#### `kinetic_orbital_lance` — Kinetic Orbital Lance (WIP)
- [ ] Mira com até 2^20 CWU/t (CHANGELOG): ligar fonte de computação, conferir que o valor máximo aceito na UI é 1.048.576 CWU/t e que acima disso trava.
- [ ] Disparo/efeito visual não deixa entidades ou partículas presas após `/gtna_dev_fps`.
- Esperado: operação estável, MSPT sem pico ao disparar.

#### `stellar_containment_chamber` — Stellar Containment Chamber (WIP)
- [ ] `/gtna_star_preview`, `/gtna_star_preview hole`, `/gtna_star_preview nolens`, `/gtna_star_preview off`: render liga e desliga sem resto.
- [ ] Ciclo da estrela anda conforme o painel de informação.

#### `antimatter_collider_ring` — Antimatter Collider Ring (WIP)
- [ ] Feixes e+e- são um toggle limitado a 100 GeV: ligar o toggle, tentar passar de 100 GeV, esperado travar em 100.
- [ ] Desligar o toggle volta ao modo normal sem perder a receita em andamento.

#### `dimensional_rift_gate` — Dimensional Rift Gate (WIP)
- [ ] Profundidade cresce com os harmônicos: subir o número de harmônicos e verificar que a profundidade na tela aumenta.
- [ ] `/gtna_rift_preview` e `/gtna_rift_preview off`.

### 2.2 Endgame

#### `eye_of_harmony` — Eye of Harmony (GTNH, `cosmos_simulation`)
- [ ] Montar com o terminal (estrutura grande: subir alto no superflat). Cobertos: `EyeOfHarmonyStructureGameTests`, `EyeOfHarmonyOperationGameTests`, `EyeOfHarmonyPlanetGameTests`, `EyeOfHarmonyContentGameTests`.
- [ ] Rodar uma receita de `cosmos_simulation` do JEI (um planeta qualquer); verificar consumo de hidrogênio/hélio e saída.
- [ ] Ver o tempo e a chance de sucesso na UI contra o tooltip.
- [ ] Salvar/sair no meio da receita, chunk unload, servidor dedicado.

#### `forge_of_gods` + módulos (GTNH)
IDs: `forge_of_gods`, `godforge_smelting_module` (blast/furnace), `godforge_molten_module` (`godforge_molten`), `godforge_plasma_module` (`godforge_plasma`), `godforge_exotic_module`.
- [ ] Montar a Forge e cada módulo; o módulo só liga com a Forge formada e conectada.
- [ ] Rodar uma receita de `blast` no Smelting Module, uma de `godforge_molten` e uma de `godforge_plasma`.
- [ ] Upgrades/combustível da Forge alteram paralelos do módulo como no tooltip.
- [ ] Desformar a Forge para os módulos; reformar religa. Cobertos: `ForgeOfGodsGameTests`, `GTNAGodforgeChainGameTests`.

#### `nano_forge` — Nano Forge (GTNH, `nano_forge`)
- [ ] Montar T1, depois expandir até T4 (módulos/tier no terminal). Conferir tier mostrado.
- [ ] Rodar uma receita de `nano_forge` de cada tier; receita de tier maior não roda em tier menor. Coberto: `GTNANanoForgeGameTests`.

#### Rede BEC (GTNH)
IDs: `bec_generator` (`condensate_generator`), `bec_storage`, `bec_assembler` (`condensate_assembler`), `bec_io_node`, `bec_diode`.
- [ ] Montar gerador + armazenamento + assembler; ligar pela rede.
- [ ] Gerador produz condensado e o armazenamento mostra o valor; assembler consome.
- [ ] IO node e diode: transferência em um sentido só no diode.
- [ ] Quebrar o storage com condensado dentro e recolocar: comportamento igual ao tooltip. Coberto: `GTNABECGameTests`.

#### `dimensionally_transcendent_plasma_forge` — DTPF (GTNH, `plasma_forge`)
- [ ] Montar, colocar bobinas; rodar uma receita de `plasma_forge` do JEI.
- [ ] Desconto de catalisador/tempo conforme o tooltip. Coberto: `GTNAPlasmaForgeGameTests`.

#### `space_elevator` + `assembler_module` + `resource_collection_module` (GTO)
- [ ] Montar o elevador e os módulos (base `space_elevator_module_base`, power modules 1–5).
- [ ] Rodar uma receita de `assembler_module` e uma de `miner_module`/`drilling_module`.
- [ ] Power module maior libera mais módulos/paralelos conforme tooltip.

#### Dyson: `dyson_sphere_launch_silo` + `dyson_sphere_receiving_station` (GTO)
- [ ] Silo roda receita `dyson_sphere`; estação recebe energia; desmontar o silo e ver a estação manter o estado salvo (`DysonSphereData`).
- [ ] Salvar/sair e conferir que o progresso da esfera persiste.

### 2.3 Sistemas

#### Rede de energia Nexus (`nexus_flux_matrix`, capacitores `nexus_capacitor_*`, terminais `wireless_energy_network_input_terminal`/`output_terminal`, hatches `wireless_energy_hatch_*`/`wireless_dynamo_hatch_*`)
- [ ] Montar `nexus_flux_matrix` com capacitores; ver capacidade na UI.
- [ ] Gerador com `wireless_dynamo_hatch_1a_lv` envia; máquina com `wireless_energy_hatch_*` recebe do mesmo dono.
- [ ] Perdas configuráveis (`NexusWirelessLoss`) aplicadas como na config.
- [ ] Valores acima de 2^63 (128-bit) aparecem certos na UI e no Quantum Network Terminal (`gtna:quantum_network_terminal`).
- [ ] Salvar/sair: saldo da rede mantido. Servidor dedicado com 2 jogadores: redes separadas. Cobertos: `NexusMatrixUnlimitedGameTests`, `NexusNetworkTerminalGameTests`.

#### Vapor sem fio (`wireless_steam_input_hatch[_steel]`, `wireless_steam_output_hatch[_steel]`, `SteamNetworkData`)
- [ ] Caldeira com output hatch, máquina steam grande com input hatch: vapor chega.
- [ ] Salvar/sair: vapor da rede mantido.

#### ME Pattern Buffers (`me_pattern_buffer`, `me_mini_pattern_buffer`, `me_advanced_pattern_buffer`, `me_ultimate_pattern_buffer`, `me_pattern_buffer_proxy`, cartas `pattern_buffer_copy_card`/`pattern_buffer_cut_card`)
- [ ] Ligar a uma rede AE2, codificar 1 padrão, pedir craft: o multibloco recebe e devolve.
- [ ] Trocar modos; capacidade conforme config.
- [ ] Copy/cut card copia a configuração entre buffers; proxy espelha o buffer.
- [ ] Layout da UI: nada desenhado fora da moldura em nenhuma página. Coberto: `GTNAMEExportBufferGameTests` (parcial).

#### Sistema de módulos (`module_connector`, `GTNAModules`)
- [ ] Multibloco com sub-padrão: adicionar o módulo libera capacidade ou tipo de receita citado no tooltip; remover tira. Coberto: `GTNAModuleLogicGameTests`.

#### Thread / Parallel / Overclock hatches (`thread_hatch_zpm…max`, `parallel_hatch_uhv…opv`, `overclock_hatch_uv…max`)
- [ ] Em uma máquina que aceita, trocar com `/gtna_dev_swap thread_hatch_uv` etc.
- [ ] Threads = receitas diferentes simultâneas; paralelos e overclock batem com o tooltip do hatch.
- [ ] Hatch de tier acima do permitido é recusado (`GTNARestrictedPartTierGameTests`).

#### Void Miner (`electric_void_miner` GTL, `void_miner_steam_gate_aged` GTO, `void_fluid_drilling_rig`)
- [ ] Electric: rodar `electric_void_mining` e `random_void_mining`; minérios conforme a dimensão.
- [ ] Steam Gate Aged: `steam_void_ore`; coberto por `SteamVoidOreGameTests`.
- [ ] Trocar de dimensão muda a lista (`GTNAVoidMinerLookupGameTests`).

---

## 3. P1 — sem GameTest que cite a máquina

Aplicar o checklist padrão. Itens gerados do registro:

@@P1@@

---

## 4. P2 — cobertos por GameTest (smoke check)

Para cada um: forma com o terminal, tooltip en/pt/zh com Source, uma receita roda.

@@P2@@

---

## 5. Teste de desempenho (spark)

Comandos: `/spark profiler start --timeout 60` (gera link do relatório ao fim), `/spark tps`, `/spark health`. Antes de cada cenário: mundo superflat novo, `/spark tps` de base; depois anotar TPS, MSPT médio/máx e as 5 entradas `gtna` mais pesadas no relatório.

- [ ] **200 wireless hatches ociosos**: 100 `wireless_energy_hatch_*` + 100 `wireless_dynamo_hatch_*` sem máquina rodando. Esperado: MSPT quase igual à base.
- [ ] **50 pattern buffers**: 50 `me_pattern_buffer` ligados à mesma rede AE2 com padrões, sem crafts. Depois 1 craft grande.
- [ ] **2.000 fornos LV com covers GTNA**: fornos elétricos LV com covers infinitos (`InfiniteElectricSingleblockCover`, `infinite_steam_singleblock_cover`) ou covers hidráulicos (`hydraulic_pump`, `hydraulic_conveyor`, `hydraulic_arm`, `hydraulic_regulator`), todos rodando.
- [ ] **1 Eye of Harmony formado rodando**: perfil durante a receita e no fim (saída grande).
- [ ] **FPS do cliente com efeitos de item**: inventário cheio de itens com modelos `gtna:halo`, `glow_edge`, `cosmic`, `eternal`, `unstable`; `/gtna_dev_fps 30` com o inventário aberto e fechado.

Relato: TPS, MSPT, link do spark, hardware, cenário.

---

## 6. Inventário completo

@@INV@@
