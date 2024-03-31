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
package org.familydirectory.sdk.adminclient.utility.lanterna;

import com.googlecode.lanterna.gui2.AnimatedLabel;
import com.googlecode.lanterna.gui2.Panels;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.DialogWindow;
import java.util.concurrent.CompletableFuture;
import org.familydirectory.sdk.adminclient.AdminClientTui;
import org.jetbrains.annotations.Contract;
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
    private static final long MILLIS_IN_SEC = 1000L;
    @NotNull
    private static final String FORMAT_TEXT = "Please Wait for %d Seconds";
    private final long seconds;
    @NotNull
    private final AnimatedLabel countDownLabel;
    @NotNull
    private final AnimatedLabel spinningLine;

    public
    EnhancedWaitingDialog (final @NotNull String title, final long seconds) {
        super(requireNonNull(title));
        this.setHints(AdminClientTui.EXTRA_WINDOW_HINTS);
        if (seconds <= 0L) {
            throw new IllegalArgumentException("seconds must be 1 or greater");
        }
        this.seconds = seconds;
        this.countDownLabel = this.getCountDownLabel();
        this.spinningLine = AnimatedLabel.createClassicSpinningLine();
        this.setComponent(Panels.horizontal(this.countDownLabel, this.spinningLine));
    }

    @NotNull
    private
    AnimatedLabel getCountDownLabel () {
        final AnimatedLabel countDownLabel = new AnimatedLabel(FORMAT_TEXT.formatted(this.seconds));
        for (long sec = this.seconds - 1L; sec >= 0L; --sec) {
            countDownLabel.addFrame(FORMAT_TEXT.formatted(sec));
        }
        countDownLabel.startAnimation(MILLIS_IN_SEC);
        return countDownLabel;
    }

    @Override
    @Contract("_ -> null")
    @Nullable
    public
    Object showDialog (final @NotNull WindowBasedTextGUI textGUI) {
        textGUI.addWindow(this);
        final CompletableFuture<?> future = CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(this.seconds * MILLIS_IN_SEC);
            } catch (final InterruptedException e) {
                Thread.currentThread()
                      .interrupt();
                throw new RuntimeException(e);
            } finally {
                this.close();
            }
        });
        this.waitUntilClosed();
        try {
            future.get();
        } catch (final Throwable e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * TODO: Delete this once <a href="https://github.com/mabe02/lanterna/issues/595">lantera issue #595</a> is resolved
     */
    @Override
    public
    void close () {
        super.close();
        this.countDownLabel.stopAnimation();
        this.spinningLine.stopAnimation();
    }
}
