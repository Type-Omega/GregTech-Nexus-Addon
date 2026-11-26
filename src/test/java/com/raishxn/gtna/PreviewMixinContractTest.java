package com.raishxn.gtna;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The multiblock preview mixins (EMI page and in-world hologram) replace or redirect private GTCEu code that no
 * GameTest can load (client classes). This checks, against the real GTCEu 7.5.3 bytecode, every member and every
 * instruction the mixins aim at, and that they are registered, so a GTCEu update fails here and not at client start.
 */
public final class PreviewMixinContractTest {

    private static final String HOLOGRAM = "com/gregtechceu/gtceu/client/renderer/MultiblockInWorldPreviewRenderer";
    private static final String WIDGET = "com/gregtechceu/gtceu/api/gui/widget/PatternPreviewWidget";
    private static final String PATTERN = "com/gregtechceu/gtceu/api/pattern/BlockPattern";
    private static final String CHECK_PATTERN = "checkPatternAt";
    private static final String CHECK_PATTERN_DESC = "(Lcom/gregtechceu/gtceu/api/pattern/MultiblockState;" +
            "Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/core/Direction;ZZ)Z";

    private PreviewMixinContractTest() {}

    public static void main(String[] args) throws IOException {
        hologramMembers();
        hologramRenderFilterIsSetTwiceInRenderBlocks();
        widgetMembers();
        updateScreenCallsSetPageTwice();
        predicatesMapIsTheFirstGetOrCreate();
        mixinsAreRegistered();
        shadowsAndInjectorsMatchTheirTargets("com/raishxn/gtna/mixin/client/MultiblockInWorldPreviewMixin", HOLOGRAM);
        shadowsAndInjectorsMatchTheirTargets("com/raishxn/gtna/mixin/client/PatternPreviewInitMixin", WIDGET);
        shadowsAndInjectorsMatchTheirTargets("com/raishxn/gtna/mixin/client/PatternPreviewPageMixin", WIDGET);
    }

    /**
     * Every {@code @Shadow} member of the mixin must exist in the target with the same descriptor and staticness, and
     * every injector's {@code method} must name a method the target declares.
     */
    private static void shadowsAndInjectorsMatchTheirTargets(String mixin, String target) throws IOException {
        Set<String> targetFields = new HashSet<>();
        Set<String> targetMethods = new HashSet<>();
        Set<String> targetMethodNames = new HashSet<>();
        new ClassReader(read(target)).accept(new ClassVisitor(Opcodes.ASM9) {

            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                targetFields.add(name + ":" + desc + ":" + ((access & Opcodes.ACC_STATIC) != 0));
                return null;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                             String[] exceptions) {
                targetMethods.add(name + desc + ":" + ((access & Opcodes.ACC_STATIC) != 0));
                targetMethodNames.add(name);
                return null;
            }
        }, ClassReader.SKIP_CODE);
        List<String> problems = new ArrayList<>();
        new ClassReader(read(mixin)).accept(new ClassVisitor(Opcodes.ASM9) {

            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                return new FieldVisitor(Opcodes.ASM9) {

                    @Override
                    public org.objectweb.asm.AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                        if (annotation.equals("Lorg/spongepowered/asm/mixin/Shadow;") &&
                                !targetFields.contains(name + ":" + desc + ":" + ((access & Opcodes.ACC_STATIC) != 0)))
                            problems.add("@Shadow field " + name + " " + desc + " is not in the target");
                        return null;
                    }
                };
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                             String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {

                    @Override
                    public org.objectweb.asm.AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                        if (annotation.equals("Lorg/spongepowered/asm/mixin/Shadow;") &&
                                !targetMethods.contains(name + desc + ":" + ((access & Opcodes.ACC_STATIC) != 0)))
                            problems.add("@Shadow method " + name + desc + " is not in the target");
                        boolean injector = annotation.equals("Lorg/spongepowered/asm/mixin/injection/Inject;") ||
                                annotation.equals("Lorg/spongepowered/asm/mixin/injection/Redirect;");
                        if (!injector) return null;
                        return new org.objectweb.asm.AnnotationVisitor(Opcodes.ASM9) {

                            @Override
                            public org.objectweb.asm.AnnotationVisitor visitArray(String arrayName) {
                                if (!arrayName.equals("method")) return null;
                                return new org.objectweb.asm.AnnotationVisitor(Opcodes.ASM9) {

                                    @Override
                                    public void visit(String n, Object value) {
                                        if (!targetMethodNames.contains((String) value))
                                            problems.add("injector " + name + " names " + value +
                                                    ", which the target does not declare");
                                    }
                                };
                            }
                        };
                    }
                };
            }
        }, ClassReader.SKIP_CODE);
        check(problems.isEmpty(), mixin + ": " + problems);
    }

    private static byte[] read(String internalName) throws IOException {
        try (InputStream stream = PreviewMixinContractTest.class.getClassLoader()
                .getResourceAsStream(internalName + ".class")) {
            if (stream == null) throw new AssertionError("class not on the test classpath: " + internalName);
            return stream.readAllBytes();
        }
    }

    private static Set<String> members(String internalName, boolean fields) throws IOException {
        Set<String> found = new HashSet<>();
        new ClassReader(read(internalName)).accept(new ClassVisitor(Opcodes.ASM9) {

            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                if (fields) found.add(name + ":" + desc + ((access & Opcodes.ACC_STATIC) != 0 ? ":static" : "") +
                        ((access & Opcodes.ACC_FINAL) != 0 ? ":final" : ""));
                return null;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                             String[] exceptions) {
                if (!fields) found.add(name + desc + ((access & Opcodes.ACC_STATIC) != 0 ? ":static" : ""));
                return null;
            }
        }, ClassReader.SKIP_CODE);
        return found;
    }

    private static void hologramMembers() throws IOException {
        Set<String> methods = members(HOLOGRAM, false);
        for (String method : List.of(
                "showPreview(Lnet/minecraft/core/BlockPos;Lcom/gregtechceu/gtceu/api/machine/multiblock/MultiblockControllerMachine;I)V:static",
                "cleanPreview()V:static",
                "getBUFFERS()[Lcom/mojang/blaze3d/vertex/VertexBuffer;:static",
                "renderBlocks(Lcom/lowdragmc/lowdraglib/utils/TrackedDummyWorld;Lcom/mojang/blaze3d/vertex/PoseStack;" +
                        "Lnet/minecraft/client/renderer/block/BlockRenderDispatcher;Lnet/minecraft/client/renderer/RenderType;" +
                        "Lcom/lowdragmc/lowdraglib/client/scene/WorldSceneRenderer$VertexConsumerWrapper;" +
                        "Ljava/util/Collection;)V:static")) {
            check(methods.contains(method), "MultiblockInWorldPreviewRenderer lost " + method);
        }
        Set<String> fields = members(HOLOGRAM, true);
        for (String field : List.of("LEVEL:Lcom/lowdragmc/lowdraglib/utils/TrackedDummyWorld;:static",
                "THREAD:Ljava/lang/Thread;:static", "BLOCK_ENTITIES:Ljava/util/Set;:static",
                "LEFT_TICK:Ljava/util/concurrent/atomic/AtomicInteger;:static:final",
                "CACHE_STATE:Ljava/util/concurrent/atomic/AtomicReference;:static:final",
                "LAST_POS:Lnet/minecraft/core/BlockPos;:static", "LAST_LAYER:I:static")) {
            check(fields.contains(field), "MultiblockInWorldPreviewRenderer lost the field " + field + " in " + fields);
        }
    }

    private static void hologramRenderFilterIsSetTwiceInRenderBlocks() throws IOException {
        int calls = countInvokes(HOLOGRAM, "renderBlocks", "com/lowdragmc/lowdraglib/utils/TrackedDummyWorld",
                "setRenderFilter", "(Ljava/util/function/Predicate;)V");
        check(calls == 2, "renderBlocks calls setRenderFilter " + calls + " times; the redirect expects 2");
    }

    private static void widgetMembers() throws IOException {
        Set<String> methods = members(WIDGET, false);
        for (String method : List.of(
                "initializePattern(Lcom/gregtechceu/gtceu/api/pattern/MultiblockShapeInfo;Ljava/util/HashSet;)" +
                        "Lcom/gregtechceu/gtceu/api/gui/widget/PatternPreviewWidget$MBPattern;",
                "loadControllerFormed(Ljava/util/Collection;Lcom/gregtechceu/gtceu/api/machine/feature/multiblock/IMultiController;)V",
                "locateNextRegion()Lnet/minecraft/core/BlockPos;:static", "updateScreen()V", "setPage(I)V")) {
            check(methods.contains(method), "PatternPreviewWidget lost " + method);
        }
        Set<String> fields = members(WIDGET, true);
        check(fields.contains("LEVEL:Lcom/lowdragmc/lowdraglib/utils/TrackedDummyWorld;:static"), "LEVEL changed");
        check(fields.contains("sceneWidget:Lcom/lowdragmc/lowdraglib/gui/widget/SceneWidget;:final"),
                "sceneWidget changed");
        check(fields.contains("index:I"), "index changed");
        check(members("com/lowdragmc/lowdraglib/gui/widget/SceneWidget", false)
                .contains("getCore()Ljava/util/Set;"), "SceneWidget.getCore() is gone");
        String mbPattern = WIDGET + "$MBPattern";
        check(members(mbPattern, false).contains("<init>(Ljava/util/Map;Ljava/util/List;Ljava/util/Map;" +
                "Lcom/gregtechceu/gtceu/api/machine/feature/multiblock/IMultiController;)V"),
                "MBPattern constructor changed");
    }

    private static void updateScreenCallsSetPageTwice() throws IOException {
        int calls = countInvokes(WIDGET, "updateScreen", WIDGET, "setPage", "(I)V");
        check(calls == 2, "updateScreen calls setPage " + calls + " times (EMI and REI); the redirect expects 2");
    }

    /** The map the mixin swaps is the first getOrCreate of the six-argument checkPatternAt, keyed "predicates". */
    private static void predicatesMapIsTheFirstGetOrCreate() throws IOException {
        List<String> keys = new ArrayList<>();
        new ClassReader(read(PATTERN)).accept(new ClassVisitor(Opcodes.ASM9) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                             String[] exceptions) {
                if (!name.equals(CHECK_PATTERN) || !desc.equals(CHECK_PATTERN_DESC)) return null;
                return new MethodVisitor(Opcodes.ASM9) {

                    private Object lastString;

                    @Override
                    public void visitLdcInsn(Object value) {
                        lastString = value;
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String method, String d, boolean iface) {
                        if (owner.equals("com/gregtechceu/gtceu/api/pattern/util/PatternMatchContext") &&
                                method.equals("getOrCreate") &&
                                d.equals("(Ljava/lang/String;Ljava/util/function/Supplier;)Ljava/lang/Object;")) {
                            keys.add(String.valueOf(lastString));
                        }
                    }
                };
            }
        }, 0);
        check(!keys.isEmpty() && keys.get(0).equals("predicates"), "the first getOrCreate of checkPatternAt is " +
                keys + ", BlockPatternPredicatesMixin (ordinal 0) expects \"predicates\"");
        check(members(PATTERN, false).contains(CHECK_PATTERN + CHECK_PATTERN_DESC), "checkPatternAt is gone");
    }

    private static int countInvokes(String owner, String method, String calledOwner, String called, String calledDesc)
                                                                                                                       throws IOException {
        int[] count = { 0 };
        boolean[] found = { false };
        new ClassReader(read(owner)).accept(new ClassVisitor(Opcodes.ASM9) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                             String[] exceptions) {
                if (!name.equals(method)) return null;
                found[0] = true;
                return new MethodVisitor(Opcodes.ASM9) {

                    @Override
                    public void visitMethodInsn(int opcode, String o, String n, String d, boolean iface) {
                        if (o.equals(calledOwner) && n.equals(called) && d.equals(calledDesc)) count[0]++;
                    }
                };
            }
        }, 0);
        check(found[0], owner + "." + method + " is gone");
        return count[0];
    }

    private static void mixinsAreRegistered() throws IOException {
        String json;
        try (InputStream stream = PreviewMixinContractTest.class.getClassLoader()
                .getResourceAsStream("gtna.mixins.json")) {
            check(stream != null, "gtna.mixins.json is missing");
            json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        int clientStart = json.indexOf("\"client\"");
        String common = json.substring(0, clientStart);
        String client = json.substring(clientStart);
        for (String mixin : List.of("client.PatternPreviewInitMixin", "client.PatternPreviewPageMixin",
                "client.MultiblockInWorldPreviewMixin")) {
            check(client.contains("\"" + mixin + "\""), mixin + " is not in the client mixin list");
        }
        check(common.contains("\"gtceu.BlockPatternPredicatesMixin\""),
                "BlockPatternPredicatesMixin is not registered");
        check(!json.contains("PatternPreviewAirMixin"), "the old PatternPreviewAirMixin is still registered");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError("PreviewMixinContractTest: " + message);
    }
}
