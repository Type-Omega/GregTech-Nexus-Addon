import re,json,glob,os
B='src/main/java/com/raishxn/gtna/common/data/'
lang=json.load(open('src/generated/resources/assets/gtna/lang/en_us.json'))
pt=json.load(open('src/main/resources/assets/gtna/lang/pt_br.json'))
zh=json.load(open('src/main/resources/assets/gtna/lang/zh_cn.json'))
src=open(B+'GTNASources.java').read()
srcmap=dict(re.findall(r'Map\.entry\("([a-z0-9_]+)",\s*([A-Z_]+)\)',src))
gt=''.join(open(f).read() for f in glob.glob('src/main/java/com/raishxn/gtna/gametest/*.java'))
tt=open('src/main/java/com/raishxn/gtna/data/GTNALangProvider.java').read()+''.join(open(f).read() for f in glob.glob('src/main/resources/assets/gtna/tooltip_lang/*.json'))
pat=re.compile(r'(?:\.multiblock|registerElevatorModule|\bbec)\(\s*"([a-z0-9_]+)"')
out=[]
for f in sorted(glob.glob(B+'*.java')):
  t=open(f).read()
  ms=list(pat.finditer(t))
  for i,m in enumerate(ms):
    end=ms[i+1].start() if i+1<len(ms) else min(len(t),m.start()+6000)
    body=t[m.start():end]
    rt=sorted(set(re.findall(r'recipeTypes?\(([^;]*?)\)\s*\n',body)))
    rts=sorted(set(re.findall(r'(?:GTNARecipeType|GTRecipeTypes|GTNANaquadahRecipeTypes)\.([A-Z0-9_]+)',body)))
    tier=re.search(r'\.tier\(([^)]*)\)',body)
    id=m.group(1)
    k='block.gtna.'+id
    out.append(dict(id=id,file=os.path.basename(f),name=lang.get(k,'MISSING'),pt=k in pt,zh=k in zh,tier=tier.group(1) if tier else '',rt=rts,src=srcmap.get(id,'(GTNA default)'),gt=len(re.findall(r'\b'+id+r'\b',gt)) + len(re.findall(re.escape(id.upper()),gt)),tt=('"'+id+'"' in tt) or (id.upper() in tt)))
json.dump(out,open(__import__('os').path.join(__import__('os').path.dirname(__import__('os').path.abspath(__file__)),'m.json'),'w'),indent=0)
print(len(out))
for o in out: print(o['id'],'|',o['name'],'|',o['file'],'|',o['tier'],'|',','.join(o['rt']),'|',o['src'],'|gt',o['gt'],'|pt',o['pt'],'zh',o['zh'],'tt',o['tt'])
