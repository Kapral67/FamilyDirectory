package org.familydirectory.sdk.adminclient.utility.pickers.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
import org.familydirectory.sdk.adminclient.AdminClientTui;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

public abstract
class PickerModel extends Thread implements AutoCloseable {
    protected static final Comparator<MemberRecord> LAST_NAME_COMPARATOR = Comparator.comparing(memberRecord -> memberRecord.member()
                                                                                                                            .getLastName());

    @NotNull
    protected final List<MemberRecord> entriesList;
    @NotNull
    private final BlockingQueue<Boolean> processingQueue;
    @NotNull
    private final CountDownLatch closedLatch;
    private volatile boolean isClosed;

    protected
    PickerModel () {
        this.isClosed = false;
        this.entriesList = new ArrayList<>();
        this.processingQueue = new ArrayBlockingQueue<>(1);
        this.closedLatch = new CountDownLatch(1);
    }

    public synchronized final
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

    protected synchronized final
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
        if (!this.isAlive() && !this.isClosed) {
            this.isClosed = true;
        }
        return this.isClosed;
    }

    public synchronized final
    void removeEntry (final @NotNull MemberRecord memberRecord) {
        this.blockUntilReady();
        this.remove_entry(memberRecord);
    }

    protected abstract
    void remove_entry (final @NotNull MemberRecord memberRecord);

    public synchronized final
    void addEntry (final @NotNull MemberRecord memberRecord) {
        this.blockUntilReady();
        this.add_entry(memberRecord);
    }

    protected abstract
    void add_entry (final @NotNull MemberRecord memberRecord);

    @Contract(pure = true)
    @NotNull
    @UnmodifiableView
    public synchronized final
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
                        this.syncRun();
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
            this.isClosed = true;
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
