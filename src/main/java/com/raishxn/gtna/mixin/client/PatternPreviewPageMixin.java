package com.raishxn.gtna.mixin.client;

import com.gregtechceu.gtceu.api.gui.widget.PatternPreviewWidget;

import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * EN: The first tick of the EMI/REI screen calls {@code setPage(0)} again after the constructor did it, and every
 * EN: such call restarts the scene compile from zero. GTCEu needs it only when the constructor ran off the render
 * EN: thread, where LDLib's {@code setRenderedCore} does nothing and the scene has no blocks; so the repeat is
 * EN: skipped only when the scene already holds its blocks. Page buttons call {@code setPage} directly and are not
 * EN: affected.
 * PT: No primeiro tick da tela do EMI/REI o {@code setPage(0)} é chamado de novo depois do construtor, e cada chamada
 * PT: reinicia a compilação da cena do zero. O GTCEu só precisa dele quando o construtor rodou fora da thread de
 * PT: render, onde o {@code setRenderedCore} da LDLib não faz nada e a cena fica sem blocos; então a repetição só é
 * PT: pulada quando a cena já tem os blocos. Os botões de página chamam {@code setPage} direto e não mudam.
 * ZH: EMI/REI 界面的第一个 tick 会在构造函数之后再次调用 {@code setPage(0)}，每次调用都会让场景编译从零重来。
 * ZH: GTCEu 只在构造函数不在渲染线程运行时才需要它（此时 LDLib 的 {@code setRenderedCore} 无效，场景没有方块），
 * ZH: 因此仅当场景已有方块时才跳过重复调用。翻页按钮直接调用 {@code setPage}，不受影响。
 */
@Mixin(value = PatternPreviewWidget.class, remap = false)
public abstract class PatternPreviewPageMixin {

    @Shadow
    @Final
    private SceneWidget sceneWidget;
    @Shadow
    private int index;

    @Redirect(method = "updateScreen",
              at = @At(value = "INVOKE",
                       target = "Lcom/gregtechceu/gtceu/api/gui/widget/PatternPreviewWidget;setPage(I)V"),
              require = 2)
    private void gtna$setPageOnce(PatternPreviewWidget widget, int page) {
        if (page == index && !sceneWidget.getCore().isEmpty()) return;
        widget.setPage(page);
    }
}
