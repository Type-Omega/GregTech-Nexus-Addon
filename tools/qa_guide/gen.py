import json
S=__import__('os').path.dirname(__import__('os').path.abspath(__file__))+'/'
m=json.load(open(S+'m.json'))
P0=set('kinetic_orbital_lance stellar_containment_chamber antimatter_collider_ring dimensional_rift_gate eye_of_harmony forge_of_gods godforge_smelting_module godforge_molten_module godforge_plasma_module godforge_exotic_module nano_forge bec_generator bec_storage bec_assembler bec_io_node bec_diode dimensionally_transcendent_plasma_forge space_elevator assembler_module resource_collection_module dyson_sphere_launch_silo dyson_sphere_receiving_station nexus_flux_matrix electric_void_miner void_miner_steam_gate_aged'.split())
def rt(o): return ', '.join(x.lower() for x in o['rt']) or '(sem tipo, máquina de suporte/módulo)'
p1=[o for o in m if o['id'] not in P0 and o['gt']==0]
p2=[o for o in m if o['id'] not in P0 and o['gt']>0]
L=[]
for o in p1:
  L.append(f"#### `{o['id']}` — {o['name']}\n\n- Arquivo: `{o['file']}` · Fonte: {o['src']} · Tipos de receita: `{rt(o)}`\n- [ ] Pegar o controlador no creative (aba GTNA ou `/give @s gtna:{o['id']}`), colocar e montar com o Nexus Structure Terminal (Shift + clique direito) ou `/gtna_dev_build {o['id']}`.\n- [ ] Preview do JEI/EMI mostra a mesma estrutura que o terminal montou; `/gtna_dev_mismatch` não lista posições erradas.\n- [ ] Tooltip em en/pt/zh sem chave crua, com a linha `Source: {o['src']}`.\n- [ ] Rodar a primeira receita da categoria `{rt(o).split(', ')[0]}` no JEI (ou, se não tiver tipo, a função descrita no tooltip). Esperado: progresso avança e a saída aparece no hatch.\n- [ ] Paralelos/velocidade na UI batem com o tooltip.\n- [ ] Quebrar um casing desforma; recolocar reforma. Salvar e sair, voltar: continua formada e a receita segue.\n- [ ] Sem erro no `latest.log`.\n")
P1T='\n'.join(L)
P2T='| OK | id | Nome | Fonte | Tipos de receita | GameTest refs |\n|---|---|---|---|---|---|\n'+'\n'.join(f"| [ ] | `{o['id']}` | {o['name']} | {o['src']} | {rt(o)} | {o['gt']} |" for o in p2)
INV='| id | Nome (en_us) | Registro | Fonte | Prioridade |\n|---|---|---|---|---|\n'+'\n'.join(f"| `{o['id']}` | {o['name']}{' (WIP)' if o['id'] in ('kinetic_orbital_lance','stellar_containment_chamber','antimatter_collider_ring','dimensional_rift_gate') else ''} | {o['file']} | {o['src']} | {'P0' if o['id'] in P0 else ('P1' if o['gt']==0 else 'P2')} |" for o in m)
t=open(S+'tpl.md').read()
t=t.replace('@@P1@@',P1T).replace('@@P2@@',P2T).replace('@@INV@@',INV).replace('@@N0@@',str(sum(o['id'] in P0 for o in m))).replace('@@N1@@',str(len(p1))).replace('@@N2@@',str(len(p2))).replace('@@NT@@',str(len(m)))
open('docs/testing/manual-qa.md','w').write(t)
print(sum(o['id'] in P0 for o in m),len(p1),len(p2))
