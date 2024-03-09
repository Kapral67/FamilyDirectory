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
 */
package org.familydirectory.sdk.adminclient.utility.lanterna;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Container;
import com.googlecode.lanterna.gui2.Label;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * This is a special label that contains not just a single text to display but a number of frames that are cycled
 * through. The class will manage a timer on its own and ensure the label is updated and redrawn. There is a static
 * helper method available to create the classic "spinning bar": {@code createClassicSpinningLine()}
 */
public
class AnimatedLabel2 extends Label {
    private final ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
    private final List<String[]> frames;
    private volatile ScheduledFuture<?> execution = null;
    private TerminalSize combinedMaximumPreferredSize;
    private int currentFrame;

    /**
     * Creates a new animated label, initially set to one frame. You will need to add more frames and call
     * {@code startAnimation()} for this to start moving.
     *
     * @param firstFrameText The content of the label at the first frame
     */
    public
    AnimatedLabel2 (final String firstFrameText) {
        super(firstFrameText);
        this.frames = new ArrayList<>();
        this.currentFrame = 0;
        this.combinedMaximumPreferredSize = TerminalSize.ZERO;

        final String[] lines = this.splitIntoMultipleLines(firstFrameText);
        this.frames.add(lines);
        this.ensurePreferredSize(lines);
    }

    private
    void ensurePreferredSize (final String[] lines) {
        this.combinedMaximumPreferredSize = this.combinedMaximumPreferredSize.max(this.getBounds(lines, this.combinedMaximumPreferredSize));
    }

    /**
     * Creates a classic spinning bar which can be used to signal to the user that an operation in is process.
     *
     * @return {@code AnimatedLabel} instance which is setup to show a spinning bar
     */
    public static
    AnimatedLabel2 createClassicSpinningLine () {
        return createClassicSpinningLine(150);
    }

    /**
     * Creates a classic spinning bar which can be used to signal to the user that an operation in is process.
     *
     * @param speed Delay in between each frame
     *
     * @return {@code AnimatedLabel} instance which is setup to show a spinning bar
     */
    public static
    AnimatedLabel2 createClassicSpinningLine (final int speed) {
        final AnimatedLabel2 animatedLabel = new AnimatedLabel2("-");
        animatedLabel.addFrame("\\");
        animatedLabel.addFrame("|");
        animatedLabel.addFrame("/");
        animatedLabel.startAnimation(speed);
        return animatedLabel;
    }

    /**
     * Adds one more frame at the end of the list of frames
     *
     * @param text Text to use for the label at this frame
     *
     * @return Itself
     */
    public synchronized
    AnimatedLabel2 addFrame (final String text) {
        final String[] lines = this.splitIntoMultipleLines(text);
        this.frames.add(lines);
        this.ensurePreferredSize(lines);
        return this;
    }

    /**
     * Starts the animation thread which will periodically call {@code nextFrame()} at the interval specified by the
     * {@code millisecondsPerFrame} parameter. After all frames have been cycled through, it will start over from the
     * first frame again.
     *
     * @param millisecondsPerFrame The interval in between every frame
     *
     * @return Itself
     */
    public synchronized
    AnimatedLabel2 startAnimation (final long millisecondsPerFrame) {
        if (this.execution != null) {
            throw new IllegalStateException("Animation Previously Started");
        }
        this.execution = this.timer.scheduleAtFixedRate(this::animationTask, millisecondsPerFrame, millisecondsPerFrame, TimeUnit.MILLISECONDS);
        return this;
    }

    protected
    void animationTask () {
        if (!Thread.currentThread()
                   .isInterrupted())
        {
            this.nextFrame();
        }
    }

    /**
     * Advances the animated label to the next frame. You normally don't need to call this manually as it will be done
     * by the animation thread.
     */
    public synchronized
    void nextFrame () {
        this.currentFrame++;
        if (this.currentFrame >= this.frames.size()) {
            this.currentFrame = 0;
        }
        super.setLines(this.frames.get(this.currentFrame));
        this.invalidate();
    }

    @Override
    protected synchronized
    TerminalSize calculatePreferredSize () {
        return super.calculatePreferredSize()
                    .max(this.combinedMaximumPreferredSize);
    }

    @Override
    public synchronized
    void onRemoved (final Container container) {
        if (!(this.execution == null || this.execution.isCancelled())) {
            this.stopAnimation();
        }
        this.timer.shutdownNow();
    }

    /**
     * Halts the animation thread and the label will stop at whatever was the current frame at the time when this was
     * called
     *
     * @return Itself
     */
    public synchronized
    AnimatedLabel2 stopAnimation () {
        if (this.execution == null) {
            throw new IllegalStateException("Animation Never Started");
        }
        this.execution.cancel(true);
        return this;
    }
}

