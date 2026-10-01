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
package org.jjazz.mixconsole.actions;

import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.midi.api.InstrumentMix;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.mixconsole.api.MixConsole;
import org.jjazz.mixconsole.api.MixConsoleTopComponent;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;

/**
 * Defines one-click Minus-One practice profiles (Bassist, Pianist/Guitarist, Drummer, Full Band).
 */
public enum MinusOneProfile
{
    FULL_BAND,
    MINUS_BASS,
    MINUS_COMPING,
    MINUS_DRUMS;

    public void apply()
    {
        MixConsoleTopComponent tc = MixConsoleTopComponent.getInstance();
        if (tc == null)
        {
            return;
        }
        MixConsole mixConsole = tc.getEditor();
        if (mixConsole == null)
        {
            return;
        }
        MidiMix songMidiMix = mixConsole.getMidiMix();
        if (songMidiMix == null || songMidiMix != ActiveSongManager.getDefault().getActiveMidiMix())
        {
            return;
        }

        Rhythm visibleRhythm = mixConsole.getVisibleRhythm();
        for (Integer channel : songMidiMix.getUsedChannels(visibleRhythm))
        {
            InstrumentMix insMix = songMidiMix.getInstrumentMix(channel);
            if (insMix == null)
            {
                continue;
            }
            RhythmVoice rv = songMidiMix.getRhythmVoice(channel);
            boolean mute = false;
            if (rv != null)
            {
                RhythmVoice.Type type = rv.getType();
                switch (this)
                {
                    case FULL_BAND -> mute = false;
                    case MINUS_BASS -> mute = (type == RhythmVoice.Type.BASS);
                    case MINUS_COMPING -> mute = (type == RhythmVoice.Type.CHORD1
                            || type == RhythmVoice.Type.CHORD2
                            || type == RhythmVoice.Type.PAD);
                    case MINUS_DRUMS -> mute = (type != null && type.isDrums());
                    default -> mute = false;
                }
            }
            insMix.setMute(mute);
        }
    }
}

