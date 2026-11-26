package com.raishxn.gtna.mixin.gtceu;

import com.gregtechceu.gtceu.api.pattern.BlockPattern;

import com.typeomega.lib.data.BlockPosHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Supplier;

/**
 * EN: With {@code savePredicate} (the EMI preview) {@code checkPatternAt} stores one entry per structure cell in a
 * EN: plain {@code HashMap<BlockPos, ...>} under "predicates"; vanilla's position hash collides by the thousand on a
 * EN: big structure. The first {@code getOrCreate} of the method is that map; it now starts as a position-hashed one
 * EN: (still a {@code HashMap}, because GTCEu casts to it).
 * PT: Com {@code savePredicate} (a prévia do EMI) o {@code checkPatternAt} guarda uma entrada por célula num
 * PT: {@code HashMap<BlockPos, ...>} comum em "predicates"; o hash de posição do vanilla colide aos milhares numa
 * PT: estrutura grande. O primeiro {@code getOrCreate} do método é esse mapa; agora ele nasce com hash de posição
 * PT: (ainda um {@code HashMap}, porque o GTCEu faz cast para ele).
 * ZH: 使用 {@code savePredicate}（EMI 预览）时，{@code checkPatternAt} 在 "predicates" 下用普通
 * ZH: {@code HashMap<BlockPos, ...>} 为每个结构单元存一项；原版位置哈希在大型结构上会大量碰撞。该方法中的第一个
 * ZH: {@code getOrCreate} 就是这个映射，现在改用位置哈希（仍是 {@code HashMap}，因为 GTCEu 会强制转换）。
 */
@Mixin(value = BlockPattern.class, remap = false)
public abstract class BlockPatternPredicatesMixin {

    @ModifyArg(method = "checkPatternAt(Lcom/gregtechceu/gtceu/api/pattern/MultiblockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/core/Direction;ZZ)Z",
               at = @At(value = "INVOKE",
                        target = "Lcom/gregtechceu/gtceu/api/pattern/util/PatternMatchContext;getOrCreate(Ljava/lang/String;Ljava/util/function/Supplier;)Ljava/lang/Object;",
                        ordinal = 0),
               index = 1)
    private Supplier<?> gtna$fastPredicates(Supplier<?> original) {
        return BlockPosHashMap::new;
    }
}
