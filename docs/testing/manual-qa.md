# QA manual do GTNA

Roteiro de teste manual montado a partir do código (registros em `src/main/java/com/raishxn/gtna/common/data/`, nomes em `src/generated/resources/assets/gtna/lang/en_us.json`, fontes em `GTNASources`, cobertura de GameTest em `src/main/java/com/raishxn/gtna/gametest/`). Os IDs das máquinas ficam em inglês.

Contagem: **170 controladores multibloco** registrados. P0 = 25 máquinas + sistemas · P1 = 55 (sem GameTest que cite a máquina) · P2 = 90 (com GameTest, só smoke check).

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

#### `large_steam_crusher` — Large Steam Crusher

- Arquivo: `GTNAMachines.java` · Fonte: GTNL · Tipos de receita: `macerator_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:large_steam_crusher`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build large_steam_crusher`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `macerator_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `large_steam_furnace` — Large Steam Furnace

- Arquivo: `GTNAMachines.java` · Fonte: GTNL · Tipos de receita: `furnace_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:large_steam_furnace`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build large_steam_furnace`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `furnace_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `large_steam_hammer` — Large Steam Hammer

- Arquivo: `GTNAMachines.java` · Fonte: GTNL · Tipos de receita: `forge_hammer_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:large_steam_hammer`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build large_steam_hammer`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `forge_hammer_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `large_steam_compressor` — Large Steam Compressor

- Arquivo: `GTNAMachines.java` · Fonte: GTNL · Tipos de receita: `compressor_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:large_steam_compressor`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build large_steam_compressor`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `compressor_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `large_steam_extractor` — Large Steam Extractor

- Arquivo: `GTNAMachines.java` · Fonte: GTNL · Tipos de receita: `extractor_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:large_steam_extractor`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build large_steam_extractor`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `extractor_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `large_steam_ore_washer` — Large Steam Ore Washer

- Arquivo: `GTNAMachines.java` · Fonte: GTNL · Tipos de receita: `ore_washer_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:large_steam_ore_washer`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build large_steam_ore_washer`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `ore_washer_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `large_steam_circuit_assembler` — Large Steam Circuit Assembler

- Arquivo: `GTNAMachines.java` · Fonte: GTNL · Tipos de receita: `circuit_assembler_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:large_steam_circuit_assembler`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build large_steam_circuit_assembler`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `circuit_assembler_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `large_steam_mixer` — Large Steam Mixer

- Arquivo: `GTNAMachines.java` · Fonte: GTNL · Tipos de receita: `mixer_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:large_steam_mixer`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build large_steam_mixer`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `mixer_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `large_steam_centrifuge` — Large Steam Centrifuge

- Arquivo: `GTNAMachines.java` · Fonte: GTNL · Tipos de receita: `centrifuge_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:large_steam_centrifuge`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build large_steam_centrifuge`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `centrifuge_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `large_steam_thermal_centrifuge` — Large Steam Thermal Centrifuge

- Arquivo: `GTNAMachines.java` · Fonte: GTNL · Tipos de receita: `thermal_centrifuge_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:large_steam_thermal_centrifuge`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build large_steam_thermal_centrifuge`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `thermal_centrifuge_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `large_steam_bath` — Large Steam Bath

- Arquivo: `GTNAMachines.java` · Fonte: GTNL · Tipos de receita: `chemical_bath_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:large_steam_bath`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build large_steam_bath`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `chemical_bath_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `primitive_distillation_tower` — Primitive Distillation Tower

- Arquivo: `GTNAMachines.java` · Fonte: GTO · Tipos de receita: `distillation_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:primitive_distillation_tower`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build primitive_distillation_tower`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTO`.
- [ ] Rodar a primeira receita da categoria `distillation_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `large_steam_lathe` — Large Steam Lathe

- Arquivo: `GTNAMachines.java` · Fonte: GTNL · Tipos de receita: `lathe_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:large_steam_lathe`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build large_steam_lathe`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `lathe_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `large_steam_cutting` — Large Steam Cutting Machine

- Arquivo: `GTNAMachines.java` · Fonte: GTNL · Tipos de receita: `cutter_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:large_steam_cutting`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build large_steam_cutting`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `cutter_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_cactus_wonder` — Cactus Wonder

- Arquivo: `GTNAMachines.java` · Fonte: GTNL · Tipos de receita: `cactus_wonder_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_cactus_wonder`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_cactus_wonder`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `cactus_wonder_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_cracking` — Steam Cracker

- Arquivo: `GTNAMachines.java` · Fonte: GTNL · Tipos de receita: `cracking_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_cracking`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_cracking`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `cracking_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_mega_compressor` — Steam Supercompressor

- Arquivo: `GTNAMachines.java` · Fonte: GTNL · Tipos de receita: `compressor_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_mega_compressor`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_mega_compressor`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `compressor_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `large_steam_forming_press` — Large Steam Forming Press

- Arquivo: `GTNAMachines.java` · Fonte: GTNL · Tipos de receita: `forming_press_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:large_steam_forming_press`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build large_steam_forming_press`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `forming_press_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `large_steam_storage_tank` — Large Steam Storage Tank

- Arquivo: `GTNAMachines.java` · Fonte: GTO · Tipos de receita: `dummy_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:large_steam_storage_tank`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build large_steam_storage_tank`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTO`.
- [ ] Rodar a primeira receita da categoria `dummy_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `dimensionally_transcendent_steam_boiler` — Dimensionally Transcendent Steam Boiler

- Arquivo: `GTNAMachines.java` · Fonte: GTL · Tipos de receita: `large_boiler_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:dimensionally_transcendent_steam_boiler`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build dimensionally_transcendent_steam_boiler`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTL`.
- [ ] Rodar a primeira receita da categoria `large_boiler_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `dimensionally_transcendent_steam_oven` — Dimensionally Transcendent Steam Oven

- Arquivo: `GTNAMachines.java` · Fonte: GTL · Tipos de receita: `furnace_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:dimensionally_transcendent_steam_oven`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build dimensionally_transcendent_steam_oven`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTL`.
- [ ] Rodar a primeira receita da categoria `furnace_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_cobbler` — Steam Cobbler

- Arquivo: `GTNAMachines.java` · Fonte: GTNL · Tipos de receita: `rock_breaker_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_cobbler`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_cobbler`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `rock_breaker_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `stone_superheater` — Stone SuperHeater

- Arquivo: `GTNAMachines.java` · Fonte: GTNL · Tipos de receita: `(sem tipo, máquina de suporte/módulo)`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:stone_superheater`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build stone_superheater`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `(sem tipo` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_manufacturer` — Steam Manufacturer

- Arquivo: `GTNAMachines.java` · Fonte: GTNL · Tipos de receita: `hydraulic_manufacturing`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_manufacturer`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_manufacturer`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `hydraulic_manufacturing` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_woodcutter` — Steam Woodcutter

- Arquivo: `GTNAMachines.java` · Fonte: GTNL · Tipos de receita: `woodcutter_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_woodcutter`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_woodcutter`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `woodcutter_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `leap_forward_one_blast_furnace` — Leap Forward One Blast Furnace

- Arquivo: `GTNAMachines.java` · Fonte: GTO · Tipos de receita: `primitive_blast_furnace_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:leap_forward_one_blast_furnace`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build leap_forward_one_blast_furnace`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTO`.
- [ ] Rodar a primeira receita da categoria `primitive_blast_furnace_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `infernal_coke_oven` — Infernal Coke Oven

- Arquivo: `GTNAMachines.java` · Fonte: GTNL · Tipos de receita: `infernal_coke_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:infernal_coke_oven`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build infernal_coke_oven`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `infernal_coke_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `hyper_pressure_reactor` — Hyper Pressure Reactor

- Arquivo: `GTNAMachines.java` · Fonte: GTNL · Tipos de receita: `(sem tipo, máquina de suporte/módulo)`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:hyper_pressure_reactor`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build hyper_pressure_reactor`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `(sem tipo` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `compact_hyper_pressure_reactor` — Compact Hyper Pressure Reactor

- Arquivo: `GTNAMachines.java` · Fonte: GTNL · Tipos de receita: `(sem tipo, máquina de suporte/módulo)`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:compact_hyper_pressure_reactor`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build compact_hyper_pressure_reactor`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `(sem tipo` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `integrated_ore_processor` — Integrated Ore Processor

- Arquivo: `GTNAMachines.java` · Fonte: GTL · Tipos de receita: `ore_processing_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:integrated_ore_processor`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build integrated_ore_processor`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTL`.
- [ ] Rodar a primeira receita da categoria `ore_processing_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `advanced_integrated_ore_processor` — Advanced Integrated Ore Processor

- Arquivo: `GTNAMachines.java` · Fonte: GTL · Tipos de receita: `ore_processing_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:advanced_integrated_ore_processor`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build advanced_integrated_ore_processor`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTL`.
- [ ] Rodar a primeira receita da categoria `ore_processing_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `eye_of_wood` — Eye of Wood

- Arquivo: `GTNAMachines.java` · Fonte: TST · Tipos de receita: `dummy_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:eye_of_wood`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build eye_of_wood`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: TST`.
- [ ] Rodar a primeira receita da categoria `dummy_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `me_storage` — ME Storage

- Arquivo: `GTNAMachines.java` · Fonte: GTO · Tipos de receita: `dummy_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:me_storage`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build me_storage`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTO`.
- [ ] Rodar a primeira receita da categoria `dummy_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_elevator_flight_module_i` — Steam Flight Module I

- Arquivo: `GTNAMachines2.java` · Fonte: GTNL · Tipos de receita: `(sem tipo, máquina de suporte/módulo)`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_elevator_flight_module_i`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_elevator_flight_module_i`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `(sem tipo` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_elevator_flight_module_ii` — Steam Flight Module II

- Arquivo: `GTNAMachines2.java` · Fonte: GTNL · Tipos de receita: `(sem tipo, máquina de suporte/módulo)`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_elevator_flight_module_ii`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_elevator_flight_module_ii`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `(sem tipo` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_elevator_flight_module_iii` — Steam Flight Module III

- Arquivo: `GTNAMachines2.java` · Fonte: GTNL · Tipos de receita: `(sem tipo, máquina de suporte/módulo)`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_elevator_flight_module_iii`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_elevator_flight_module_iii`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `(sem tipo` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_elevator_weather_module_i` — Steam Weather Module I

- Arquivo: `GTNAMachines2.java` · Fonte: GTNL · Tipos de receita: `(sem tipo, máquina de suporte/módulo)`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_elevator_weather_module_i`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_elevator_weather_module_i`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `(sem tipo` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_elevator_weather_module_ii` — Steam Weather Module II

- Arquivo: `GTNAMachines2.java` · Fonte: GTNL · Tipos de receita: `(sem tipo, máquina de suporte/módulo)`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_elevator_weather_module_ii`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_elevator_weather_module_ii`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `(sem tipo` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_elevator_weather_module_iii` — Steam Weather Module III

- Arquivo: `GTNAMachines2.java` · Fonte: GTNL · Tipos de receita: `(sem tipo, máquina de suporte/módulo)`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_elevator_weather_module_iii`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_elevator_weather_module_iii`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `(sem tipo` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_elevator_greenhouse_module` — Steam Greenhouse Planting Module

- Arquivo: `GTNAMachines2.java` · Fonte: GTNL · Tipos de receita: `(sem tipo, máquina de suporte/módulo)`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_elevator_greenhouse_module`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_elevator_greenhouse_module`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `(sem tipo` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_elevator_oil_drill_module_i` — Steam Oil Drill Module I

- Arquivo: `GTNAMachines2.java` · Fonte: GTNL · Tipos de receita: `(sem tipo, máquina de suporte/módulo)`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_elevator_oil_drill_module_i`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_elevator_oil_drill_module_i`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `(sem tipo` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_elevator_oil_drill_module_ii` — Steam Oil Drill Module II

- Arquivo: `GTNAMachines2.java` · Fonte: GTNL · Tipos de receita: `(sem tipo, máquina de suporte/módulo)`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_elevator_oil_drill_module_ii`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_elevator_oil_drill_module_ii`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `(sem tipo` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_elevator_oil_drill_module_iii` — Steam Oil Drill Module III

- Arquivo: `GTNAMachines2.java` · Fonte: GTNL · Tipos de receita: `(sem tipo, máquina de suporte/módulo)`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_elevator_oil_drill_module_iii`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_elevator_oil_drill_module_iii`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `(sem tipo` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_elevator_entity_crusher_module_i` — Steam Entity Crusher Module I

- Arquivo: `GTNAMachines2.java` · Fonte: GTNL · Tipos de receita: `(sem tipo, máquina de suporte/módulo)`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_elevator_entity_crusher_module_i`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_elevator_entity_crusher_module_i`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `(sem tipo` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_elevator_entity_crusher_module_ii` — Steam Entity Crusher Module II

- Arquivo: `GTNAMachines2.java` · Fonte: GTNL · Tipos de receita: `(sem tipo, máquina de suporte/módulo)`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_elevator_entity_crusher_module_ii`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_elevator_entity_crusher_module_ii`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `(sem tipo` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_elevator_entity_crusher_module_iii` — Steam Entity Crusher Module III

- Arquivo: `GTNAMachines2.java` · Fonte: GTNL · Tipos de receita: `(sem tipo, máquina de suporte/módulo)`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_elevator_entity_crusher_module_iii`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_elevator_entity_crusher_module_iii`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `(sem tipo` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_elevator_ore_processor_module` — Steam Ore Processing Module

- Arquivo: `GTNAMachines2.java` · Fonte: GTNL · Tipos de receita: `ore_processing_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_elevator_ore_processor_module`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_elevator_ore_processor_module`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `ore_processing_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_elevator_monster_repellent_module_i` — Steam Monster Repellator Module I

- Arquivo: `GTNAMachines2.java` · Fonte: GTNL · Tipos de receita: `(sem tipo, máquina de suporte/módulo)`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_elevator_monster_repellent_module_i`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_elevator_monster_repellent_module_i`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `(sem tipo` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_elevator_monster_repellent_module_ii` — Steam Monster Repellator Module II

- Arquivo: `GTNAMachines2.java` · Fonte: GTNL · Tipos de receita: `(sem tipo, máquina de suporte/módulo)`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_elevator_monster_repellent_module_ii`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_elevator_monster_repellent_module_ii`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `(sem tipo` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_elevator_monster_repellent_module_iii` — Steam Monster Repellator Module III

- Arquivo: `GTNAMachines2.java` · Fonte: GTNL · Tipos de receita: `(sem tipo, máquina de suporte/módulo)`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_elevator_monster_repellent_module_iii`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_elevator_monster_repellent_module_iii`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `(sem tipo` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_elevator_beacon_module_i` — Steam Elevator Beacon Module I

- Arquivo: `GTNAMachines2.java` · Fonte: GTNL · Tipos de receita: `(sem tipo, máquina de suporte/módulo)`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_elevator_beacon_module_i`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_elevator_beacon_module_i`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `(sem tipo` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_elevator_beacon_module_ii` — Steam Elevator Beacon Module II

- Arquivo: `GTNAMachines2.java` · Fonte: GTNL · Tipos de receita: `(sem tipo, máquina de suporte/módulo)`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_elevator_beacon_module_ii`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_elevator_beacon_module_ii`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `(sem tipo` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_elevator_beacon_module_iii` — Steam Elevator Beacon Module III

- Arquivo: `GTNAMachines2.java` · Fonte: GTNL · Tipos de receita: `(sem tipo, máquina de suporte/módulo)`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_elevator_beacon_module_iii`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_elevator_beacon_module_iii`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `(sem tipo` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_elevator_apiary_module` — Steam-Powered Apiary Module

- Arquivo: `GTNAMachines2.java` · Fonte: GTNL · Tipos de receita: `(sem tipo, máquina de suporte/módulo)`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_elevator_apiary_module`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_elevator_apiary_module`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `(sem tipo` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.

#### `steam_elevator_bee_breeding_module` — Steam Bee Breeding Module

- Arquivo: `GTNAMachines2.java` · Fonte: GTNL · Tipos de receita: `dummy_recipes`
- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:steam_elevator_bee_breeding_module`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build steam_elevator_bee_breeding_module`.
- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.
- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: GTNL`.
- [ ] Rodar a primeira receita da categoria `dummy_recipes` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.
- [ ] Paralelos/velocidade na UI batem com o tooltip.
- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.
- [ ] Sem erro no `latest.log`.


---

## 4. P2 — cobertos por GameTest (smoke check)

Para cada um: forma com o terminal, tooltip en/pt/zh com Source, uma receita roda.

| OK | id | Nome | Fonte | Tipos de receita | GameTest refs |
|---|---|---|---|---|---|
| [ ] | `advanced_fusion_reactor` | Advanced Fusion Reactor MK-I | GTO | fusion_recipes | 1 |
| [ ] | `large_steam_alloy_smelter` | Large Steam Alloy Smelter | GTNL | alloy_smelter_recipes | 10 |
| [ ] | `large_steam_bending` | Large Steam Bending Machine | GTNL | bender_recipes | 2 |
| [ ] | `large_steam_extruder` | Large Steam Extruder | GTNL | extruder_recipes | 2 |
| [ ] | `large_steam_wiremill` | Large Steam Wiremill | GTNL | wiremill_recipes | 2 |
| [ ] | `large_steam_sifter` | Large Steam Sifter | GTNL | sifter_recipes | 4 |
| [ ] | `steam_lava_maker` | Steam Lava Maker | GTNL | lava_maker_recipes | 6 |
| [ ] | `steam_item_vault` | Steam Item Vault | GTNL | (sem tipo, máquina de suporte/módulo) | 4 |
| [ ] | `steam_elevator` | Steam Elevator | GTNL | dummy_recipes | 2 |
| [ ] | `large_steam_solar_boiler` | Large Steam Solar Boiler | GTO | dummy_recipes | 1 |
| [ ] | `dimensionally_transcendent_dirt_forge` | Dimensionally Transcendent Dirt Forge | GTL | primitive_blast_furnace_recipes | 3 |
| [ ] | `industrial_slaughterhouse` | Industrial Slaughterhouse | GTO | slaughterhouse_recipes | 3 |
| [ ] | `annihilate_generator` | Artificial Star | GTO | artificial_star_recipes | 1 |
| [ ] | `nexus_molecular_forge` | Nexus Assembly Forge | GTO | dummy_recipes | 1 |
| [ ] | `nexus_me_hypercore` | Nexus ME Hypercore | (GTNA default) | dummy_recipes | 1 |
| [ ] | `universal_factory` | Universal Factory | GTLSUPB | arc_furnace_recipes, assembler_recipes, autoclave_recipes, bender_recipes, brewing_recipes, canner_recipes, centrifuge_recipes, chemical_bath_recipes, chemical_recipes, circuit_assembler_recipes, compressor_recipes, cutter_recipes, distillery_recipes, electrolyzer_recipes, extractor_recipes, extruder_recipes, fermenting_recipes, fluid_heater_recipes, fluid_solidfication_recipes, forge_hammer_recipes, forming_press_recipes, large_chemical_recipes, laser_engraver_recipes, lathe_recipes, macerator_recipes, mixer_recipes, ore_washer_recipes, packer_recipes, polarizer_recipes, sifter_recipes, thermal_centrifuge_recipes, wiremill_recipes | 43 |
| [ ] | `primitive_stone_furnace` | Primitive Stone Furnace | GTLSUPB | furnace_recipes | 6 |
| [ ] | `brick_kiln` | Brick Kiln | GTO | brick_furnace_recipes | 7 |
| [ ] | `thermal_power_pump` | Thermal Power Pump | GTO | dummy_recipes | 7 |
| [ ] | `liquefaction_furnace` | Liquefaction Furnace | GTO | liquefaction_furnace_recipes | 11 |
| [ ] | `duration_tester` | Duration Tester | (GTNA default) | assembler_recipes, circuit_assembler_recipes | 27 |
| [ ] | `void_fluid_drilling_rig` | Void Fluid Drilling Rig | GTL | void_fluid_drilling_recipes, world_data_scanner_recipes | 2 |
| [ ] | `incubator` | Incubator | GTL | dehydrator_recipes, incubator_recipes, rocket_engine_fuels | 23 |
| [ ] | `generator_array` | Generator Array | GTO | combustion_generator_fuels, gas_turbine_fuels, steam_turbine_fuels | 4 |
| [ ] | `fishing_ground` | Fishing Ground | GTO | fishing_ground_recipes | 2 |
| [ ] | `evaporation_plant` | Evaporation Plant | GTO | evaporation_recipes | 3 |
| [ ] | `greenhouse` | Greenhouse | GTO | greenhouse_recipes | 43 |
| [ ] | `component_assembler` | Component Assembler | GTO | component_assembly_recipes | 13 |
| [ ] | `large_greenhouse` | Large Greenhouse | GTO | greenhouse_recipes, tree_growth_recipes | 3 |
| [ ] | `blaze_blast_furnace` | Blaze Blast Furnace | GTO | blast_recipes | 2 |
| [ ] | `cold_ice_freezer` | Cold Ice Freezer | GTO | atomization_condensation_recipes, vacuum_recipes | 3 |
| [ ] | `chemical_plant` | Chemical Plant | GTO | large_chemical_recipes | 7 |
| [ ] | `mega_alloy_blast_smelter` | Mega Alloy Blast Smelter | GTO | (sem tipo, máquina de suporte/módulo) | 7 |
| [ ] | `isa_mill` | ISA Mill | GTO | isa_mill_recipes | 6 |
| [ ] | `rocket_large_turbine` | Rocket Large Turbine | GTO | rocket_engine_fuels | 3 |
| [ ] | `supercritical_steam_turbine` | Supercritical Steam Turbine | GTO | supercritical_steam_turbine_fuels | 6 |
| [ ] | `industrial_flotation_cell` | Industrial Flotation Cell | GTO | flotating_beneficiation_recipes | 5 |
| [ ] | `vacuum_drying_furnace` | Vacuum Drying Furnace | GTO | dehydrator_recipes, vacuum_drying_recipes | 7 |
| [ ] | `component_assembly_line` | Component Assembly Line | GTO | component_assembly_recipes | 21 |
| [ ] | `large_pyrolyse_oven` | Large Pyrolyse Oven | GTO | pyrolyse_recipes | 2 |
| [ ] | `cracker_hub` | Cracker Hub | GTO | cracking_recipes, pyrolyse_recipes | 3 |
| [ ] | `mega_brewer` | Mega Brewer | GTO | brewing_recipes, fermenting_recipes | 2 |
| [ ] | `mega_vacuum_drying_furnace` | Mega Vacuum Drying Furnace | GTO | vacuum_drying_recipes | 3 |
| [ ] | `giant_flotation_tank` | Giant Flotation Tank | GTO | flotating_beneficiation_recipes | 2 |
| [ ] | `phase_change_cube` | Phase Change Cube | GTO | extractor_recipes, fluid_solidfication_recipes | 3 |
| [ ] | `large_incubator` | Large Incubator | GTO | incubator_recipes | 1 |
| [ ] | `engraving_laser_plant` | Engraving Laser Plant | GTO | laser_engraver_recipes, precision_laser_engraver_recipes | 3 |
| [ ] | `infinity_fluid_drilling_rig` | Infinity Fluid Drilling Rig | GTO | dummy_recipes | 1 |
| [ ] | `mega_distillery` | Mega Distillery | GTL | distillation_recipes, distillery_recipes, evaporation_recipes | 2 |
| [ ] | `super_blast_smelter` | Super Blast Smelter | GTO | blast_recipes | 4 |
| [ ] | `large_cracker` | Large Cracker | GTO | cracking_recipes | 2 |
| [ ] | `integrated_assembler` | Integrated Assembler | GTO | assembler_recipes | 3 |
| [ ] | `neutronium_wire_cutting` | Neutronium Wire Cutting | GTO | cutter_recipes | 2 |
| [ ] | `holy_separator` | Holy Separator | GTO | centrifuge_recipes | 3 |
| [ ] | `superconducting_electromagnetism` | Superconducting Electromagnetism | GTO | electromagnetic_separator_recipes, polarizer_recipes | 3 |
| [ ] | `superconducting_magnetic_presser` | Superconducting Magnetic Presser | GTO | forming_press_recipes | 3 |
| [ ] | `magnetic_energy_reaction_furnace` | Magnetic Energy Reaction Furnace | GTO | arc_furnace_recipes | 3 |
| [ ] | `particle_stream_matrix_filling_machine` | Particle Stream Matrix Filling Machine | GTO | canner_recipes | 3 |
| [ ] | `nanites_circuit_assembly_factory` | Nanites Circuit Assembly Factory | GTO | circuit_assembler_recipes | 3 |
| [ ] | `mega_wiremill` | Mega Wiremill | GTO | wiremill_recipes | 3 |
| [ ] | `crystal_builder` | Crystal Builder | GTO | autoclave_recipes | 3 |
| [ ] | `high_energy_laser_lathe` | High Energy Laser Lathe | GTO | lathe_recipes | 3 |
| [ ] | `extreme_compressor` | Extreme Compressor | GTO | compressor_recipes | 3 |
| [ ] | `hyperdimensional_plasma_fusion_core` | Hyperdimensional Plasma Fusion Core | GTO | plasma_forge_recipes | 3 |
| [ ] | `gemini_containment_system` | Gemini Containment System | GTO | packer_recipes | 3 |
| [ ] | `giant_electrochemical_workstation` | Giant Electrochemical Workstation | GTO | electrolyzer_recipes | 3 |
| [ ] | `desulfurizer` | Desulfurizer | GTO | desulfurizer_recipes | 4 |
| [ ] | `dimensional_focus_engraving_array` | Dimensional Focus Engraving Array | GTO | dimensional_focus_engraving_array_recipes | 3 |
| [ ] | `star_ultimate_material_forge_factory` | Star Ultimate Material Forge Factory | GTO | ultimate_material_forge_recipes | 2 |
| [ ] | `kerr_newman_homogenizer` | Kerr Newman Homogenizer | GTO | mixer_recipes | 1 |
| [ ] | `nyarlathoteps_tentacle` | Nyarlathoteps Tentacle | GTO | assembly_line_recipes | 1 |
| [ ] | `giant_sintering_array` | Giant Sintering Array | GTO | sintering_furnace_recipes | 1 |
| [ ] | `integrated_vapor_deposition_system` | Integrated Vapor Deposition System | GTO | chemical_vapor_deposition_recipes, physical_vapor_deposition_recipes | 1 |
| [ ] | `reaction_furnace` | Reaction Furnace | GTO | reaction_furnace_recipes | 4 |
| [ ] | `hand_of_arachne` | Hand Of Arachne | GTO | loom_recipes | 2 |
| [ ] | `rare_earth_centrifugal` | Rare Earth Centrifugal | GTO | rare_earth_centrifugal_recipes | 3 |
| [ ] | `heavy_rolling` | Heavy Rolling | GTO | bender_recipes | 3 |
| [ ] | `lightning_rod` | Lightning Rod | GTO | arc_generator_recipes | 2 |
| [ ] | `space_probe_surface_reception` | Space Probe Receiver | GTO | space_probe_surface_reception_recipes | 2 |
| [ ] | `pcb_factory` | Pcb Factory | GTO | pcb_factory_recipes | 4 |
| [ ] | `compound_extreme_cooling_unit` | Compound Extreme Cooling Unit | GTO | atomization_condensation_recipes, plasma_condenser_recipes, vacuum_recipes | 6 |
| [ ] | `hyper_reactor` | Hyper Reactor | GTO | hyper_reactor_recipes | 6 |
| [ ] | `advanced_hyper_reactor` | Advanced Hyper Reactor | GTO | advanced_hyper_reactor_recipes | 1 |
| [ ] | `fuel_cell_generator` | Fuel Cell Generator | GTO | fuel_cell_energy_absorption_recipes, fuel_cell_energy_release_recipes, fuel_cell_energy_transfer_recipes | 2 |
| [ ] | `supercomputing_center` | Supercomputing Center | GTO | dummy_recipes | 3 |
| [ ] | `data_center` | Data Center | GTO | dummy_recipes | 1 |
| [ ] | `analysis_and_research_center` | Analysis And Research Center | GTO | data_analysis_recipes, data_integration_recipes | 1 |
| [ ] | `large_naquadah_reactor` | Large Naquadah Reactor | GTO | large_naquadah_reactor_recipes | 2 |
| [ ] | `naquadah_hyper_reactor` | Naquadah Hyper Reactor | GTNA | naquadah_hyper_reactor_recipes | 2 |
| [ ] | `transcendent_plasma_mixer` | Transcendent Plasma Mixer | GTNH | transcendent_plasma_mixer_recipes | 1 |

---

## 5. Teste de desempenho (spark)

Comandos: `/spark profiler start --timeout 60` (gera link do relatório ao fim), `/spark tps`, `/spark health`. Antes de cada cenário: mundo superflat novo, `/spark tps` de base; depois anotar TPS, MSPT médio/máx e as 5 entradas `gtna` mais pesadas no relatório.

- [ ] **200 wireless hatches ociosos**: 100 `wireless_energy_hatch_*` + 100 `wireless_dynamo_hatch_*` sem máquina rodando. Esperado: MSPT quase igual à base.
- [ ] **50 pattern buffers**: 50 `me_pattern_buffer` ligados à mesma rede AE2 com padrões, sem crafts. Depois 1 craft grande.
- [ ] **2.000 fornos LV com covers GTNA**: fornos elétricos LV com covers infinitos (`infinite_electric_singleblock_cover`, `infinite_steam_singleblock_cover`) ou covers hidráulicos (`hydraulic_pump`, `hydraulic_conveyor`, `hydraulic_arm`, `hydraulic_regulator`), todos rodando.
- [ ] **1 Eye of Harmony formado rodando**: perfil durante a receita e no fim (saída grande).
- [ ] **FPS do cliente com efeitos de item**: inventário cheio de itens com modelos `gtna:halo`, `glow_edge`, `cosmic`, `eternal`, `unstable`; `/gtna_dev_fps 30` com o inventário aberto e fechado.

Relato: TPS, MSPT, link do spark, hardware, cenário.

---

## 6. Inventário completo

| id | Nome (en_us) | Registro | Fonte | Prioridade |
|---|---|---|---|---|
| `advanced_fusion_reactor` | Advanced Fusion Reactor MK-I | GTNAAdvancedFusion.java | GTO | P2 |
| `bec_generator` | Bose-Einstein Condensate Entanglement Apparatus | GTNABEC.java | GTNH | P0 |
| `bec_storage` | Bose-Einstein Condensate Containment Field | GTNABEC.java | GTNH | P0 |
| `bec_assembler` | Bose-Einstein Condensate Observation Array | GTNABEC.java | GTNH | P0 |
| `bec_io_node` | Bose-Einstein Condensate Teleportation Node | GTNABEC.java | GTNH | P0 |
| `bec_diode` | Bose-Einstein Condensate Maxwell Gate | GTNABEC.java | GTNH | P0 |
| `nexus_flux_matrix` | Nexus Flux Matrix | GTNAEnergyHatches.java | (GTNA default) | P0 |
| `large_steam_crusher` | Large Steam Crusher | GTNAMachines.java | GTNL | P1 |
| `large_steam_furnace` | Large Steam Furnace | GTNAMachines.java | GTNL | P1 |
| `large_steam_alloy_smelter` | Large Steam Alloy Smelter | GTNAMachines.java | GTNL | P2 |
| `large_steam_hammer` | Large Steam Hammer | GTNAMachines.java | GTNL | P1 |
| `large_steam_compressor` | Large Steam Compressor | GTNAMachines.java | GTNL | P1 |
| `large_steam_extractor` | Large Steam Extractor | GTNAMachines.java | GTNL | P1 |
| `large_steam_ore_washer` | Large Steam Ore Washer | GTNAMachines.java | GTNL | P1 |
| `large_steam_circuit_assembler` | Large Steam Circuit Assembler | GTNAMachines.java | GTNL | P1 |
| `large_steam_mixer` | Large Steam Mixer | GTNAMachines.java | GTNL | P1 |
| `large_steam_centrifuge` | Large Steam Centrifuge | GTNAMachines.java | GTNL | P1 |
| `large_steam_thermal_centrifuge` | Large Steam Thermal Centrifuge | GTNAMachines.java | GTNL | P1 |
| `large_steam_bath` | Large Steam Bath | GTNAMachines.java | GTNL | P1 |
| `primitive_distillation_tower` | Primitive Distillation Tower | GTNAMachines.java | GTO | P1 |
| `large_steam_lathe` | Large Steam Lathe | GTNAMachines.java | GTNL | P1 |
| `large_steam_cutting` | Large Steam Cutting Machine | GTNAMachines.java | GTNL | P1 |
| `large_steam_bending` | Large Steam Bending Machine | GTNAMachines.java | GTNL | P2 |
| `large_steam_extruder` | Large Steam Extruder | GTNAMachines.java | GTNL | P2 |
| `large_steam_wiremill` | Large Steam Wiremill | GTNAMachines.java | GTNL | P2 |
| `large_steam_sifter` | Large Steam Sifter | GTNAMachines.java | GTNL | P2 |
| `steam_lava_maker` | Steam Lava Maker | GTNAMachines.java | GTNL | P2 |
| `steam_item_vault` | Steam Item Vault | GTNAMachines.java | GTNL | P2 |
| `steam_cactus_wonder` | Cactus Wonder | GTNAMachines.java | GTNL | P1 |
| `steam_cracking` | Steam Cracker | GTNAMachines.java | GTNL | P1 |
| `steam_mega_compressor` | Steam Supercompressor | GTNAMachines.java | GTNL | P1 |
| `steam_elevator` | Steam Elevator | GTNAMachines.java | GTNL | P2 |
| `large_steam_forming_press` | Large Steam Forming Press | GTNAMachines.java | GTNL | P1 |
| `large_steam_storage_tank` | Large Steam Storage Tank | GTNAMachines.java | GTO | P1 |
| `large_steam_solar_boiler` | Large Steam Solar Boiler | GTNAMachines.java | GTO | P2 |
| `dimensionally_transcendent_dirt_forge` | Dimensionally Transcendent Dirt Forge | GTNAMachines.java | GTL | P2 |
| `dimensionally_transcendent_steam_boiler` | Dimensionally Transcendent Steam Boiler | GTNAMachines.java | GTL | P1 |
| `dimensionally_transcendent_steam_oven` | Dimensionally Transcendent Steam Oven | GTNAMachines.java | GTL | P1 |
| `steam_cobbler` | Steam Cobbler | GTNAMachines.java | GTNL | P1 |
| `stone_superheater` | Stone SuperHeater | GTNAMachines.java | GTNL | P1 |
| `steam_manufacturer` | Steam Manufacturer | GTNAMachines.java | GTNL | P1 |
| `steam_woodcutter` | Steam Woodcutter | GTNAMachines.java | GTNL | P1 |
| `leap_forward_one_blast_furnace` | Leap Forward One Blast Furnace | GTNAMachines.java | GTO | P1 |
| `infernal_coke_oven` | Infernal Coke Oven | GTNAMachines.java | GTNL | P1 |
| `hyper_pressure_reactor` | Hyper Pressure Reactor | GTNAMachines.java | GTNL | P1 |
| `compact_hyper_pressure_reactor` | Compact Hyper Pressure Reactor | GTNAMachines.java | GTNL | P1 |
| `void_miner_steam_gate_aged` | Void Miner SteamGate Aged | GTNAMachines.java | GTO | P0 |
| `electric_void_miner` | Electric Void Miner | GTNAMachines.java | GTL | P0 |
| `industrial_slaughterhouse` | Industrial Slaughterhouse | GTNAMachines.java | GTO | P2 |
| `integrated_ore_processor` | Integrated Ore Processor | GTNAMachines.java | GTL | P1 |
| `advanced_integrated_ore_processor` | Advanced Integrated Ore Processor | GTNAMachines.java | GTL | P1 |
| `annihilate_generator` | Artificial Star | GTNAMachines.java | GTO | P2 |
| `eye_of_harmony` | Eye of Harmony | GTNAMachines.java | GTNH | P0 |
| `eye_of_wood` | Eye of Wood | GTNAMachines.java | TST | P1 |
| `nexus_molecular_forge` | Nexus Assembly Forge | GTNAMachines.java | GTO | P2 |
| `nexus_me_hypercore` | Nexus ME Hypercore | GTNAMachines.java | (GTNA default) | P2 |
| `me_storage` | ME Storage | GTNAMachines.java | GTO | P1 |
| `universal_factory` | Universal Factory | GTNAMachines.java | GTLSUPB | P2 |
| `primitive_stone_furnace` | Primitive Stone Furnace | GTNAMachines.java | GTLSUPB | P2 |
| `brick_kiln` | Brick Kiln | GTNAMachines.java | GTO | P2 |
| `thermal_power_pump` | Thermal Power Pump | GTNAMachines.java | GTO | P2 |
| `liquefaction_furnace` | Liquefaction Furnace | GTNAMachines.java | GTO | P2 |
| `duration_tester` | Duration Tester | GTNAMachines2.java | (GTNA default) | P2 |
| `steam_elevator_flight_module_i` | Steam Flight Module I | GTNAMachines2.java | GTNL | P1 |
| `steam_elevator_flight_module_ii` | Steam Flight Module II | GTNAMachines2.java | GTNL | P1 |
| `steam_elevator_flight_module_iii` | Steam Flight Module III | GTNAMachines2.java | GTNL | P1 |
| `steam_elevator_weather_module_i` | Steam Weather Module I | GTNAMachines2.java | GTNL | P1 |
| `steam_elevator_weather_module_ii` | Steam Weather Module II | GTNAMachines2.java | GTNL | P1 |
| `steam_elevator_weather_module_iii` | Steam Weather Module III | GTNAMachines2.java | GTNL | P1 |
| `steam_elevator_greenhouse_module` | Steam Greenhouse Planting Module | GTNAMachines2.java | GTNL | P1 |
| `steam_elevator_oil_drill_module_i` | Steam Oil Drill Module I | GTNAMachines2.java | GTNL | P1 |
| `steam_elevator_oil_drill_module_ii` | Steam Oil Drill Module II | GTNAMachines2.java | GTNL | P1 |
| `steam_elevator_oil_drill_module_iii` | Steam Oil Drill Module III | GTNAMachines2.java | GTNL | P1 |
| `steam_elevator_entity_crusher_module_i` | Steam Entity Crusher Module I | GTNAMachines2.java | GTNL | P1 |
| `steam_elevator_entity_crusher_module_ii` | Steam Entity Crusher Module II | GTNAMachines2.java | GTNL | P1 |
| `steam_elevator_entity_crusher_module_iii` | Steam Entity Crusher Module III | GTNAMachines2.java | GTNL | P1 |
| `steam_elevator_ore_processor_module` | Steam Ore Processing Module | GTNAMachines2.java | GTNL | P1 |
| `steam_elevator_monster_repellent_module_i` | Steam Monster Repellator Module I | GTNAMachines2.java | GTNL | P1 |
| `steam_elevator_monster_repellent_module_ii` | Steam Monster Repellator Module II | GTNAMachines2.java | GTNL | P1 |
| `steam_elevator_monster_repellent_module_iii` | Steam Monster Repellator Module III | GTNAMachines2.java | GTNL | P1 |
| `steam_elevator_beacon_module_i` | Steam Elevator Beacon Module I | GTNAMachines2.java | GTNL | P1 |
| `steam_elevator_beacon_module_ii` | Steam Elevator Beacon Module II | GTNAMachines2.java | GTNL | P1 |
| `steam_elevator_beacon_module_iii` | Steam Elevator Beacon Module III | GTNAMachines2.java | GTNL | P1 |
| `steam_elevator_apiary_module` | Steam-Powered Apiary Module | GTNAMachines2.java | GTNL | P1 |
| `steam_elevator_bee_breeding_module` | Steam Bee Breeding Module | GTNAMachines2.java | GTNL | P1 |
| `void_fluid_drilling_rig` | Void Fluid Drilling Rig | GTNAMachines3.java | GTL | P2 |
| `incubator` | Incubator | GTNAMachines3.java | GTL | P2 |
| `generator_array` | Generator Array | GTNAMachines3.java | GTO | P2 |
| `fishing_ground` | Fishing Ground | GTNAMachines3.java | GTO | P2 |
| `evaporation_plant` | Evaporation Plant | GTNAMachines3.java | GTO | P2 |
| `greenhouse` | Greenhouse | GTNAMachines3.java | GTO | P2 |
| `component_assembler` | Component Assembler | GTNAMachines3.java | GTO | P2 |
| `large_greenhouse` | Large Greenhouse | GTNAMachines3.java | GTO | P2 |
| `blaze_blast_furnace` | Blaze Blast Furnace | GTNAMachines3.java | GTO | P2 |
| `cold_ice_freezer` | Cold Ice Freezer | GTNAMachines3.java | GTO | P2 |
| `chemical_plant` | Chemical Plant | GTNAMachines3.java | GTO | P2 |
| `mega_alloy_blast_smelter` | Mega Alloy Blast Smelter | GTNAMachines3.java | GTO | P2 |
| `isa_mill` | ISA Mill | GTNAMachines3.java | GTO | P2 |
| `forge_of_gods` | Forge of the Gods | GTNAMachines3.java | GTNH | P0 |
| `godforge_smelting_module` | Helioflare Power Forge | GTNAMachines3.java | GTNH | P0 |
| `godforge_molten_module` | Helioflux Melting Core | GTNAMachines3.java | GTNH | P0 |
| `godforge_plasma_module` | Heliothermal Plasma Fabricator | GTNAMachines3.java | GTNH | P0 |
| `godforge_exotic_module` | Heliofusion Exoticizer | GTNAMachines3.java | GTNH | P0 |
| `rocket_large_turbine` | Rocket Large Turbine | GTNAMachines3.java | GTO | P2 |
| `supercritical_steam_turbine` | Supercritical Steam Turbine | GTNAMachines3.java | GTO | P2 |
| `industrial_flotation_cell` | Industrial Flotation Cell | GTNAMachines3.java | GTO | P2 |
| `vacuum_drying_furnace` | Vacuum Drying Furnace | GTNAMachines3.java | GTO | P2 |
| `component_assembly_line` | Component Assembly Line | GTNAMachines3.java | GTO | P2 |
| `large_pyrolyse_oven` | Large Pyrolyse Oven | GTNAMegaMachines.java | GTO | P2 |
| `cracker_hub` | Cracker Hub | GTNAMegaMachines.java | GTO | P2 |
| `mega_brewer` | Mega Brewer | GTNAMegaMachines.java | GTO | P2 |
| `mega_vacuum_drying_furnace` | Mega Vacuum Drying Furnace | GTNAMegaMachines.java | GTO | P2 |
| `giant_flotation_tank` | Giant Flotation Tank | GTNAMegaMachines.java | GTO | P2 |
| `phase_change_cube` | Phase Change Cube | GTNAMegaMachines.java | GTO | P2 |
| `large_incubator` | Large Incubator | GTNAMegaMachines.java | GTO | P2 |
| `engraving_laser_plant` | Engraving Laser Plant | GTNAMegaMachines.java | GTO | P2 |
| `infinity_fluid_drilling_rig` | Infinity Fluid Drilling Rig | GTNAMegaMachines.java | GTO | P2 |
| `mega_distillery` | Mega Distillery | GTNAMegaMachines.java | GTL | P2 |
| `super_blast_smelter` | Super Blast Smelter | GTNAMegaMachines.java | GTO | P2 |
| `large_cracker` | Large Cracker | GTNAMegaMachines.java | GTO | P2 |
| `integrated_assembler` | Integrated Assembler | GTNAMegaMachines.java | GTO | P2 |
| `neutronium_wire_cutting` | Neutronium Wire Cutting | GTNAMegaMachines.java | GTO | P2 |
| `holy_separator` | Holy Separator | GTNAMegaMachines.java | GTO | P2 |
| `superconducting_electromagnetism` | Superconducting Electromagnetism | GTNAMegaMachines.java | GTO | P2 |
| `superconducting_magnetic_presser` | Superconducting Magnetic Presser | GTNAMegaMachines.java | GTO | P2 |
| `magnetic_energy_reaction_furnace` | Magnetic Energy Reaction Furnace | GTNAMegaMachines.java | GTO | P2 |
| `particle_stream_matrix_filling_machine` | Particle Stream Matrix Filling Machine | GTNAMegaMachines.java | GTO | P2 |
| `nanites_circuit_assembly_factory` | Nanites Circuit Assembly Factory | GTNAMegaMachines.java | GTO | P2 |
| `mega_wiremill` | Mega Wiremill | GTNAMegaMachines.java | GTO | P2 |
| `crystal_builder` | Crystal Builder | GTNAMegaMachines.java | GTO | P2 |
| `high_energy_laser_lathe` | High Energy Laser Lathe | GTNAMegaMachines.java | GTO | P2 |
| `extreme_compressor` | Extreme Compressor | GTNAMegaMachines.java | GTO | P2 |
| `hyperdimensional_plasma_fusion_core` | Hyperdimensional Plasma Fusion Core | GTNAMegaMachines.java | GTO | P2 |
| `gemini_containment_system` | Gemini Containment System | GTNAMegaMachines.java | GTO | P2 |
| `giant_electrochemical_workstation` | Giant Electrochemical Workstation | GTNAMegaMachines.java | GTO | P2 |
| `desulfurizer` | Desulfurizer | GTNAMegaMachines.java | GTO | P2 |
| `dimensional_focus_engraving_array` | Dimensional Focus Engraving Array | GTNAMegaMachines.java | GTO | P2 |
| `star_ultimate_material_forge_factory` | Star Ultimate Material Forge Factory | GTNAMegaMachines.java | GTO | P2 |
| `kerr_newman_homogenizer` | Kerr Newman Homogenizer | GTNAMegaMachines.java | GTO | P2 |
| `nyarlathoteps_tentacle` | Nyarlathoteps Tentacle | GTNAMegaMachines.java | GTO | P2 |
| `giant_sintering_array` | Giant Sintering Array | GTNAMegaMachines.java | GTO | P2 |
| `integrated_vapor_deposition_system` | Integrated Vapor Deposition System | GTNAMegaMachines.java | GTO | P2 |
| `reaction_furnace` | Reaction Furnace | GTNAMegaMachines.java | GTO | P2 |
| `dyson_sphere_launch_silo` | Dyson Sphere Launch Silo | GTNAMegaMachines.java | GTO | P0 |
| `dyson_sphere_receiving_station` | Dyson Sphere Receiving Station | GTNAMegaMachines.java | GTO | P0 |
| `assembler_module` | Assembler Module | GTNAMegaMachines.java | GTO | P0 |
| `resource_collection_module` | Resource Collection Module | GTNAMegaMachines.java | GTO | P0 |
| `space_elevator` | Space Elevator | GTNAMegaMachines.java | GTO | P0 |
| `hand_of_arachne` | Hand Of Arachne | GTNAMegaMachines.java | GTO | P2 |
| `rare_earth_centrifugal` | Rare Earth Centrifugal | GTNAMegaMachines.java | GTO | P2 |
| `heavy_rolling` | Heavy Rolling | GTNAMegaMachines.java | GTO | P2 |
| `lightning_rod` | Lightning Rod | GTNAMegaMachines.java | GTO | P2 |
| `space_probe_surface_reception` | Space Probe Receiver | GTNAMegaMachines.java | GTO | P2 |
| `pcb_factory` | Pcb Factory | GTNAMegaMachines.java | GTO | P2 |
| `compound_extreme_cooling_unit` | Compound Extreme Cooling Unit | GTNAMegaMachines.java | GTO | P2 |
| `hyper_reactor` | Hyper Reactor | GTNAMegaMachines.java | GTO | P2 |
| `advanced_hyper_reactor` | Advanced Hyper Reactor | GTNAMegaMachines.java | GTO | P2 |
| `fuel_cell_generator` | Fuel Cell Generator | GTNAMegaMachines.java | GTO | P2 |
| `supercomputing_center` | Supercomputing Center | GTNAMegaMachines.java | GTO | P2 |
| `data_center` | Data Center | GTNAMegaMachines.java | GTO | P2 |
| `analysis_and_research_center` | Analysis And Research Center | GTNAMegaMachines.java | GTO | P2 |
| `nano_forge` | Nano Forge | GTNANanoForge.java | GTNH | P0 |
| `large_naquadah_reactor` | Large Naquadah Reactor | GTNANaquadahReactors.java | GTO | P2 |
| `naquadah_hyper_reactor` | Naquadah Hyper Reactor | GTNANaquadahReactors.java | GTNA | P2 |
| `stellar_containment_chamber` | Stellar Containment Chamber (WIP) | GTNAOriginals.java | (GTNA default) | P0 |
| `antimatter_collider_ring` | Antimatter Collider Ring (WIP) | GTNAOriginals.java | (GTNA default) | P0 |
| `dimensional_rift_gate` | Dimensional Rift Gate (WIP) | GTNAOriginals.java | (GTNA default) | P0 |
| `kinetic_orbital_lance` | Kinetic Orbital Lance (WIP) | GTNAOriginals.java | (GTNA default) | P0 |
| `dimensionally_transcendent_plasma_forge` | Dimensionally Transcendent Plasma Forge | GTNAPlasmaForge.java | GTNH | P0 |
| `transcendent_plasma_mixer` | Transcendent Plasma Mixer | GTNATranscendentPlasmaMixer.java | GTNH | P2 |
