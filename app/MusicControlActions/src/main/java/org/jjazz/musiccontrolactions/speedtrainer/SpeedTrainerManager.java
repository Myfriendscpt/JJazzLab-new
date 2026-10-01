/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2026 JJazzLab. All rights reserved.
 *
 *  This file is part of the JJazzLab software.
 *   
 *  JJazzLab is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLab is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 */
package org.jjazz.musiccontrolactions.speedtrainer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.harmony.api.Position;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.PlaybackListener;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.rhythm.api.TempoRange;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongPart;
import org.openide.awt.StatusDisplayer;

/**
 * Manages the intelligent speed trainer: automatically increases tempo when a loop repeats.
 */
public class SpeedTrainerManager implements PlaybackListener, PropertyChangeListener
{
    public static final String PROP_ENABLED = "PropSpeedTrainerEnabled";
    public static final String PROP_START_TEMPO = "PropStartTempo";
    public static final String PROP_TARGET_TEMPO = "PropTargetTempo";
    public static final String PROP_BPM_INCREMENT = "PropBpmIncrement";
    public static final String PROP_LOOP_INTERVAL = "PropLoopInterval";
    public static final String PROP_COMPLETED_LOOPS = "PropCompletedLoops";

    private static SpeedTrainerManager INSTANCE;
    private static final Logger LOGGER = Logger.getLogger(SpeedTrainerManager.class.getSimpleName());

    private boolean enabled = false;
    private int startTempo = 100;
    private int targetTempo = 180;
    private int bpmIncrement = 4;
    private int loopInterval = 1;
    private int completedLoops = 0;

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public static synchronized SpeedTrainerManager getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new SpeedTrainerManager();
        }
        return INSTANCE;
    }

    private SpeedTrainerManager()
    {
        MusicController.getInstance().addPlaybackListener(this);
        MusicController.getInstance().addPropertyChangeListener(this);
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        if (this.enabled != enabled)
        {
            boolean old = this.enabled;
            this.enabled = enabled;
            this.completedLoops = 0;
            pcs.firePropertyChange(PROP_ENABLED, old, enabled);
        }
    }

    public int getStartTempo()
    {
        return startTempo;
    }

    public void setStartTempo(int startTempo)
    {
        this.startTempo = Math.max(TempoRange.MIN_TEMPO, Math.min(TempoRange.MAX_TEMPO, startTempo));
        pcs.firePropertyChange(PROP_START_TEMPO, null, this.startTempo);
    }

    public int getTargetTempo()
    {
        return targetTempo;
    }

    public void setTargetTempo(int targetTempo)
    {
        this.targetTempo = Math.max(TempoRange.MIN_TEMPO, Math.min(TempoRange.MAX_TEMPO, targetTempo));
        pcs.firePropertyChange(PROP_TARGET_TEMPO, null, this.targetTempo);
    }

    public int getBpmIncrement()
    {
        return bpmIncrement;
    }

    public void setBpmIncrement(int bpmIncrement)
    {
        this.bpmIncrement = Math.max(1, bpmIncrement);
        pcs.firePropertyChange(PROP_BPM_INCREMENT, null, this.bpmIncrement);
    }

    public int getLoopInterval()
    {
        return loopInterval;
    }

    public void setLoopInterval(int loopInterval)
    {
        this.loopInterval = Math.max(1, loopInterval);
        pcs.firePropertyChange(PROP_LOOP_INTERVAL, null, this.loopInterval);
    }

    public int getCompletedLoops()
    {
        return completedLoops;
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    // =========================================================================
    // PlaybackListener implementation
    // =========================================================================

    @Override
    public boolean isAccepted(PlaybackSession session)
    {
        return session != null && session.getContext() == PlaybackSession.Context.SONG;
    }

    @Override
    public void enabledChanged(boolean b)
    {
    }

    @Override
    public void beatChanged(Position oldPos, Position newPos, float newPosInBeats)
    {
        if (!enabled)
        {
            return;
        }

        // Detect loop wrap-around: position bar drops back, or beat drops back within same bar (1-bar loop)
        boolean loopRewound = (oldPos != null && newPos != null)
                && ((newPos.getBar() < oldPos.getBar())
                || (newPos.getBar() == oldPos.getBar() && newPos.getBeat() < oldPos.getBeat()));

        if (loopRewound)
        {
            SwingUtilities.invokeLater(this::onLoopCycleCompleted);
        }
    }

    @Override
    public void chordSymbolChanged(CLI_ChordSymbol chordSymbol)
    {
    }

    @Override
    public void songPartChanged(SongPart spt)
    {
    }

    @Override
    public void midiActivity(long tick, int channel)
    {
    }

    // =========================================================================
    // PropertyChangeListener implementation
    // =========================================================================

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == MusicController.getInstance()
                && MusicController.PROP_STATE.equals(evt.getPropertyName()))
        {
            MusicController.State newState = (MusicController.State) evt.getNewValue();
            if (newState == MusicController.State.STOPPED)
            {
                completedLoops = 0;
                pcs.firePropertyChange(PROP_COMPLETED_LOOPS, null, completedLoops);
            }
        }
    }

    private void onLoopCycleCompleted()
    {
        completedLoops++;
        pcs.firePropertyChange(PROP_COMPLETED_LOOPS, null, completedLoops);

        if (completedLoops % loopInterval == 0)
        {
            Song song = ActiveSongManager.getDefault().getActiveSong();
            if (song != null)
            {
                int currentTempo = song.getTempo();
                if (currentTempo < targetTempo)
                {
                    int nextTempo = Math.min(targetTempo, currentTempo + bpmIncrement);
                    song.setTempo(nextTempo);
                    StatusDisplayer.getDefault().setStatusText(
                            String.format("Speed Trainer: Loop #%d -> Tempo: %d BPM (Target: %d BPM)",
                                    completedLoops, nextTempo, targetTempo));
                }
                else
                {
                    StatusDisplayer.getDefault().setStatusText(
                            String.format("Speed Trainer: Target tempo %d BPM reached! (%d loops completed)",
                                    targetTempo, completedLoops));
                }
            }
        }
    }
}

