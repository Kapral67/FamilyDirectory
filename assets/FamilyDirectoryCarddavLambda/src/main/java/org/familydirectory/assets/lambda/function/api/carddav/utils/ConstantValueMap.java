package org.familydirectory.assets.lambda.function.api.carddav.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableSet;

public final
class ConstantValueMap<K, V> implements Map<K, V> {
    private final HashMap<K, V> delegate = new HashMap<>(1, 1.0f);

    public
    ConstantValueMap (V value) {
        this.delegate.put(null, value);
    }

    @Override
    public
    int size () {
        return 1;
    }

    @Override
    public
    boolean isEmpty () {
        return false;
    }

    @Override
    public
    boolean containsKey (Object key) {
        return true;
    }

    @Override
    public
    boolean containsValue (Object value) {
        return Objects.equals(this.get(null), value);
    }

    @Override
    public
    V get (Object key) {
        return this.delegate.get(null);
    }

    @Override
    @NotNull
    public
    Set<K> keySet () {
        return unmodifiableSet(this.delegate.keySet());
    }

    @Override
    @NotNull
    public
    Collection<V> values () {
        return unmodifiableCollection(this.delegate.values());
    }

    @Override
    @NotNull
    public
    Set<Entry<K, V>> entrySet () {
        return unmodifiableSet(this.delegate.entrySet());
    }

    @Override
    public
    V getOrDefault (Object key, V defaultValue) {
        return this.get(null);
    }

    @Override
    public
    void forEach (BiConsumer<? super K, ? super V> action) {
        action.accept(null, this.get(null));
    }



    //
    // UNSUPPORTED OPERATIONS
    //



    @Override
    @Deprecated
    public
    V put (K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public
    V remove (Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public
    void putAll (@NotNull Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public
    void clear () {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public
    void replaceAll (BiFunction<? super K, ? super V, ? extends V> function) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public
    V putIfAbsent (K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public
    boolean remove (Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public
    boolean replace (K key, V oldValue, V newValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public
    V replace (K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public
    V computeIfAbsent (K key, @NotNull Function<? super K, ? extends V> mappingFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public
    V computeIfPresent (K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public
    V compute (K key, @NotNull BiFunction<? super K, ? super @Nullable V, ? extends V> remappingFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public
    V merge (K key, @NotNull V value, @NotNull BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        throw new UnsupportedOperationException();
    }
}
