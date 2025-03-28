package org.familydirectory.sdk.adminclient.utility;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

@ThreadSafe
public final
class SdkClientProvider implements SdkAutoCloseable {
    @NotNull
    private static final AtomicReference<SdkClientProvider> INSTANCE = new AtomicReference<>(null);
    @NotNull
    private final ConcurrentHashMap<Class<? extends SdkClient>, SdkClient> clientMap;
    private final ReadWriteLock clientReadWriteLock = new ReentrantReadWriteLock();

    private
    SdkClientProvider () {
        super();
        this.clientMap = new ConcurrentHashMap<>();
    }

    public static
    SdkClientProvider getSdkClientProvider () {
        return INSTANCE.updateAndGet(provider -> {
            if (isNull(provider)) {
                return new SdkClientProvider();
            }
            return provider;
        });
    }

    @NotNull
    public
    <T extends SdkClient> T getSdkClient (final @NotNull Class<T> clazz) {
        this.clientReadWriteLock.readLock().lock();
        try {
            return requireNonNull(clazz.cast(this.clientMap.computeIfAbsent(clazz, key -> {
                try {
                    return key.cast(key.getMethod("create").invoke(null));
                } catch (final NoSuchMethodException e) {
                    throw new RuntimeException("%s Has No Method: create".formatted(clazz.getName()), e);
                } catch (final InvocationTargetException | IllegalAccessException e) {
                    throw new RuntimeException("SdkClientProvider Failed", e);
                }
            })));
        } finally {
            this.clientReadWriteLock.readLock().unlock();
        }
    }

    @Override
    public
    void close () {
        this.clientReadWriteLock.writeLock().lock();
        try {
            this.clientMap.forEachValue(1, client -> client.close());
            this.clientMap.clear();
        } finally {
            this.clientReadWriteLock.writeLock().unlock();
        }
    }
}
