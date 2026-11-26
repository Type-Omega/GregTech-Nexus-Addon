// Ported from GTLCore (LGPL-3.0), https://github.com/AaAdoniSsS/GTLCore
package com.raishxn.gtna.integration.ae2.wireless;

import net.minecraft.core.BlockPos;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

final class WirelessReflection {

    private WirelessReflection() {}

    static final Map<RuntimeReflectionKey, Optional<Method>> RUNTIME_METHOD_CACHE = new ConcurrentHashMap<>();

    record RuntimeReflectionKey(Class<?> type, String methodName) {}

    static Object invokeObject(Object target, String methodName) {
        if (target == null) return null;
        try {
            Method method = findPublicNoArgMethod(target.getClass(), methodName);
            if (method == null) {
                return null;
            }
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    static BlockPos invokeBlockPos(Object target, String methodName) {
        Object result = invokeObject(target, methodName);
        return result instanceof BlockPos pos ? pos : null;
    }

    static boolean hasTypeName(Class<?> type, String typeName) {
        if (type == null) {
            return false;
        }
        if (type.getName().equals(typeName)) {
            return true;
        }
        for (Class<?> interfaceType : type.getInterfaces()) {
            if (hasTypeName(interfaceType, typeName)) {
                return true;
            }
        }
        return hasTypeName(type.getSuperclass(), typeName);
    }

    static boolean hasNoArgMethod(Class<?> type, String methodName) {
        return findPublicNoArgMethod(type, methodName) != null;
    }

    static Method findPublicNoArgMethod(Class<?> type, String methodName) {
        if (type == null) {
            return null;
        }
        return RUNTIME_METHOD_CACHE.computeIfAbsent(
                new RuntimeReflectionKey(type, methodName),
                WirelessReflection::findPublicNoArgMethodUncached)
                .orElse(null);
    }

    static Optional<Method> findPublicNoArgMethodUncached(RuntimeReflectionKey key) {
        try {
            Method method = key.type().getMethod(key.methodName());
            method.setAccessible(true);
            return Optional.of(method);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // ponytail: LinkageError catches NoClassDefFoundError when a method
            // signature references a class from an uninstalled optional mod
            return Optional.empty();
        }
    }
}
