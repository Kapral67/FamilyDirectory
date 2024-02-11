package org.familydirectory.sdk.adminclient.utility;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
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

    private
    SdkClientProvider () {
        super();
        this.clientMap = new ConcurrentHashMap<>();
    }

    public static synchronized
    SdkClientProvider getSdkClientProvider () {
        if (isNull(INSTANCE.get())) {
            INSTANCE.set(new SdkClientProvider());
        }
        return INSTANCE.get();
    }

    @NotNull
    public
    <T extends SdkClient> T getSdkClient (final @NotNull Class<T> clazz) {
        T client;
        try {
            client = clazz.cast(this.clientMap.get(clazz));
            if (isNull(client)) {
                client = clazz.cast(clazz.getMethod("create")
                                         .invoke(null));
                this.clientMap.put(clazz, client);
            }
        } catch (final NoSuchMethodException e) {
            throw new RuntimeException("%s Has No Method: create".formatted(clazz.getName()), e);
        } catch (final InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("SdkClientProvider Failed", e);
        }
        return requireNonNull(client);
    }

    @Override
    public
    void close () {
        for (final SdkClient client : this.clientMap.values()) {
            client.close();
        }
        this.clientMap.clear();
    }
}
