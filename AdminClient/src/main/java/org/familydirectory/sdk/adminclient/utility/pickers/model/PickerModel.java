package org.familydirectory.sdk.adminclient.utility.pickers.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import org.familydirectory.assets.ddb.models.member.MemberRecord;
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
    private final
    CyclicBarrier processingBarrier;

    protected
    PickerModel () {
        this.entriesList = new ArrayList<>();
        this.processingBarrier = new CyclicBarrier(2);
    }

    public synchronized final
    boolean isEmpty () throws InterruptedException {
        this.blockUntilReady();
        return this.is_empty();
    }

    protected
    boolean is_empty () {
        return this.entriesList.isEmpty();
    }

    protected final
    void blockUntilReady () {
        if (!this.isAlive()) {
            throw new PickerClosedException();
        }
        while () {
            this.safeWait();
        }
    }

    protected synchronized final
    void safeWait () {
        try {
            Thread.currentThread().wait();
        } catch (final InterruptedException x) {
            this.close();
        }
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
        if (!this.isAlive()) {
            throw new PickerClosedException();
        }
        try {
            do {
                if (this.isInterrupted()) {
                    return;
                }
                synchronized (this) {
                    this.syncRun();
                    if (this.isInterrupted()) {
                        return;
                    }
                    this.notifyAll();
                }
                if (this.isInterrupted()) {
                    return;
                }
                this.processingBarrier.await();
            } while (true);
        } catch (final InterruptedException ignored) {
            this.interrupt();
        } catch (final BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract
    void syncRun ();

    public final
    void refresh () throws BrokenBarrierException {
        if (!this.isAlive()) {
            throw new PickerClosedException();
        }
        try {
            this.processingBarrier.await();
        } catch (final InterruptedException e) {
            this.close();
        }
    }

    @Override
    public final
    void close () {
        if (this.isAlive()) {
            this.interrupt();
        }
    }

    public static
    class PickerClosedException extends IllegalStateException {
    }
}
