/*
 * This file is part of lanterna (https://github.com/mabe02/lanterna).
 *
 * lanterna is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2010-2020 Martin Berglund
 *
 * Copyright (C) 2024 Maxwell Kapral
 */
package org.familydirectory.sdk.adminclient.utility.dialogs;

import com.googlecode.lanterna.gui2.AnimatedLabel;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panels;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.DialogWindow;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.familydirectory.sdk.adminclient.AdminClientTui;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.util.Objects.requireNonNull;

/**
 * {@link com.googlecode.lanterna.gui2.dialogs.WaitingDialog}
 *
 * @author Martin
 * @author Maxwell Kapral
 */
public final
class EnhancedWaitingDialog extends DialogWindow {
    private static final String FORMAT_TEXT = "Please Wait for %d Seconds";
    private final @NotNull Label label;
    private final AtomicLong seconds;

    public
    EnhancedWaitingDialog (final @NotNull String title, final long seconds) {
        super(requireNonNull(title));
        this.setHints(AdminClientTui.EXTRA_WINDOW_HINTS);
        if (seconds <= 0L) {
            throw new IllegalArgumentException("seconds must be 1 or greater");
        }
        this.seconds = new AtomicLong(seconds);
        this.label = new Label(FORMAT_TEXT.formatted(this.seconds.get()));
        this.setComponent(Panels.horizontal(this.label, AnimatedLabel.createClassicSpinningLine()));
    }

    @Override
    @Nullable
    public
    Object showDialog (final @NotNull WindowBasedTextGUI textGUI) {
        textGUI.addWindow(this);

        final CountDownLatch latch = new CountDownLatch(1);
        try (final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor()) {
            executorService.scheduleAtFixedRate(() -> {
                if (this.seconds.get() > 0L) {
                    this.seconds.set(this.seconds.get() - 1L);
                    this.label.setText(FORMAT_TEXT.formatted(this.seconds.get()));
                } else {
                    latch.countDown();
                }
            }, 0L, 1L, TimeUnit.SECONDS);
            latch.await();
        } catch (final InterruptedException e) {
            Thread.currentThread()
                  .interrupt();
            throw new RuntimeException(e);
        } finally {
            this.close();
        }

        return null;
    }
}
