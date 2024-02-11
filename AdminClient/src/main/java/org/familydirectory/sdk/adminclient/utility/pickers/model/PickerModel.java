package org.familydirectory.sdk.adminclient.utility.pickers.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.sdk.adminclient.AdminClientTui;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

public abstract
class PickerModel extends Thread implements AutoCloseable {
    @NotNull
    protected static final Comparator<MemberRecord> FIRST_NAME_COMPARATOR = Comparator.comparing(memberRecord -> memberRecord.member()
                                                                                                                             .getFirstName());

    @NotNull
    protected final List<MemberRecord> entriesList;
    @NotNull
    private final BlockingQueue<Boolean> processingQueue;
    @NotNull
    private final CountDownLatch closedLatch;
    @NotNull
    private final AtomicBoolean isClosed;

    protected
    PickerModel () {
        super();
        this.isClosed = new AtomicBoolean(false);
        this.entriesList = new ArrayList<>();
        this.processingQueue = new ArrayBlockingQueue<>(1);
        this.closedLatch = new CountDownLatch(1);
    }

    public final synchronized
    boolean isEmpty () {
        this.blockUntilReady();
        return this.is_empty();
    }

    protected
    boolean is_empty () {
        return this.entriesList.isEmpty();
    }

    protected final
    void blockUntilReady () {
        if (this.isClosed()) {
            throw new PickerClosedException();
        }
        synchronized (this) {
            while (this.processingQueue.contains(true)) {
                this.safeWait();
            }
        }
    }

    protected final synchronized
    void safeWait () {
        try {
            this.wait();
        } catch (final InterruptedException x) {
            Thread.currentThread()
                  .interrupt();
            this.close();
        } catch (final Exception e) {
            this.close();
            throw e;
        }
    }

    @Override
    public final
    void close () {
        if (!this.isClosed()) {
            synchronized (this.processingQueue) {
                this.processingQueue.clear();
                this.processingQueue.add(false);
            }
            try {
                this.closedLatch.await();
            } catch (final InterruptedException e) {
                Thread.currentThread()
                      .interrupt();
                throw new RuntimeException(e);
            }
        }
    }

    public final
    boolean isClosed () {
        if (!this.isAlive() && !this.isClosed.get()) {
            this.isClosed.set(true);
        }
        return this.isClosed.get();
    }

    public final synchronized
    void removeEntry (final @NotNull MemberRecord memberRecord) {
        this.blockUntilReady();
        this.remove_entry(memberRecord);
    }

    protected
    void remove_entry (final @NotNull MemberRecord memberRecord) {
        this.entriesList.remove(memberRecord);
    }

    public final synchronized
    void addEntry (final @NotNull MemberRecord memberRecord) {
        this.blockUntilReady();
        this.add_entry(memberRecord);
    }

    protected
    void add_entry (final @NotNull MemberRecord memberRecord) {
        this.remove_entry(memberRecord);
        this.entriesList.add(memberRecord);
        this.entriesList.sort(FIRST_NAME_COMPARATOR);
    }

    @Contract(pure = true)
    @NotNull
    @UnmodifiableView
    public final synchronized
    List<MemberRecord> getEntries () {
        this.blockUntilReady();
        return Collections.unmodifiableList(this.entriesList);
    }

    @Override
    public final
    void run () {
        try {
            if (this.isClosed()) {
                throw new PickerClosedException();
            }
            try {
                do {
                    if (this.isInterrupted()) {
                        return;
                    }
                    synchronized (this) {
                        this.entriesList.clear();
                        this.syncRun();
                        this.entriesList.sort(FIRST_NAME_COMPARATOR);
                        this.notify();
                    }
                } while (this.processingQueue.take());
            } catch (final InterruptedException ignored) {
                Thread.currentThread()
                      .interrupt();
            }
        } catch (final Throwable e) {
            AdminClientTui.catchAll(e);
        } finally {
            this.isClosed.set(true);
            this.closedLatch.countDown();
        }
    }

    protected abstract
    void syncRun ();

    public final
    void refresh () {
        if (this.isClosed()) {
            throw new PickerClosedException();
        }
        synchronized (this.processingQueue) {
            if (this.processingQueue.isEmpty()) {
                this.processingQueue.add(true);
            }
        }
    }

    public static
    class PickerClosedException extends IllegalStateException {
    }
}
