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
package org.jjazz.cl_editorimpl.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.chordleadsheet.spi.item.CLI_Factory;
import org.jjazz.harmony.api.Note;
import org.jjazz.harmony.api.Position;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.song.api.Song;
import org.jjazz.song.spi.SongFactory;
import org.jjazz.songeditormanager.spi.SongEditorManager;
import org.jjazz.songstructure.api.SongPart;
import org.openide.awt.StatusDisplayer;

/**
 * Builds modulation workouts (e.g. Circle of Fifths across all 12 keys).
 */
public class ModulationTrainerWorkoutBuilder
{
    private static final Logger LOGGER = Logger.getLogger(ModulationTrainerWorkoutBuilder.class.getSimpleName());

    public enum CycleType
    {
        CIRCLE_OF_FIFTHS("Circle of 4ths / 5ths (Jazz Cycle: C, F, Bb, Eb...)",
                new int[]{0, 5, 10, 3, 8, 1, 6, 11, 4, 9, 2, 7},
                new String[]{"I", "IV", "bVII", "bIII", "bVI", "bII", "bV", "VII", "III", "VI", "II", "V"}),
        CHROMATIC_ASCENDING("Chromatic Ascending (Half-steps: C, Db, D, Eb...)",
                new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11},
                new String[]{"+0", "+1", "+2", "+3", "+4", "+5", "+6", "+7", "+8", "+9", "+10", "+11"}),
        WHOLE_TONE_ASCENDING("Whole Tone Ascending (Whole-steps: C, D, E...)",
                new int[]{0, 2, 4, 6, 8, 10},
                new String[]{"+0", "+2", "+4", "+6", "+8", "+10"});

        private final String displayName;
        private final int[] semitoneOffsets;
        private final String[] labels;

        CycleType(String displayName, int[] semitoneOffsets, String[] labels)
        {
            this.displayName = displayName;
            this.semitoneOffsets = semitoneOffsets;
            this.labels = labels;
        }

        public int[] getSemitoneOffsets()
        {
            return semitoneOffsets;
        }

        public String[] getLabels()
        {
            return labels;
        }

        @Override
        public String toString()
        {
            return displayName;
        }
    }

    /**
     * Generate a new workout Song transposing the source section across the specified cycle.
     */
    public static void generateWorkoutSong(Song sourceSong, CLI_Section sourceSection,
                                           CycleType cycleType, int repetitionsPerKey,
                                           Note.Accidental accidental, boolean createNewSong)
    {
        if (sourceSong == null || sourceSection == null)
        {
            return;
        }

        ChordLeadSheet srcCls = sourceSong.getChordLeadSheet();
        int sectionStartBar = sourceSection.getPosition().getBar();
        int sectionNbBars = sourceSection.getNbBars();
        var ts = sourceSection.getTimeSignature();

        // Collect chords in source section
        List<CLI_ChordSymbol> srcChords = new ArrayList<>();
        for (CLI_ChordSymbol cs : srcCls.getItems(CLI_ChordSymbol.class))
        {
            int b = cs.getPosition().getBar();
            if (b >= sectionStartBar && b < sectionStartBar + sectionNbBars)
            {
                srcChords.add(cs);
            }
        }

        // Rhythm from source song part
        Rhythm rhythm = null;
        for (SongPart spt : sourceSong.getSongStructure().getSongParts())
        {
            if (spt.getParentSection().equals(sourceSection))
            {
                rhythm = spt.getRhythm();
                break;
            }
        }

        int[] offsets = cycleType.getSemitoneOffsets();
        String[] labels = cycleType.getLabels();
        int totalSections = offsets.length * repetitionsPerKey;
        int totalBars = totalSections * sectionNbBars;

        String workoutTitle = sourceSong.getName() + " - " + cycleType.name() + " Workout";

        try
        {
            SongFactory sf = SongFactory.getDefault();
            CLI_Factory clf = CLI_Factory.getDefault();

            // Create target leadsheet
            ChordLeadSheet targetCls = sf.createEmptyChordLeadSheet("A_" + labels[0], ts, totalBars, null);

            int currentBar = 0;
            for (int k = 0; k < offsets.length; k++)
            {
                int semitones = offsets[k];
                String label = labels[k];

                for (int rep = 0; rep < repetitionsPerKey; rep++)
                {
                    String secName = (sourceSection.getName() + "_" + label + (repetitionsPerKey > 1 ? ("_r" + (rep + 1)) : "")).trim();
                    if (currentBar == 0)
                    {
                        // Update bar 0 section
                        targetCls.setSectionName(targetCls.getSection(0), secName);
                    }
                    else
                    {
                        CLI_Section sec = clf.createSection(secName, ts, currentBar, targetCls);
                        targetCls.addSection(sec);
                    }

                    // Add transposed chords
                    for (CLI_ChordSymbol srcCs : srcChords)
                    {
                        int relBar = srcCs.getPosition().getBar() - sectionStartBar;
                        float beat = srcCs.getPosition().getBeat();
                        int targetBar = currentBar + relBar;

                        ExtChordSymbol origEcs = srcCs.getData();
                        ExtChordSymbol transEcs = origEcs.getTransposedChordSymbol(semitones, accidental);

                        CLI_ChordSymbol newCs = clf.createChordSymbol(transEcs, new Position(targetBar, beat));
                        targetCls.addItem(newCs);
                    }

                    currentBar += sectionNbBars;
                }
            }

            // Create the song from leadsheet
            Song workoutSong = sf.createSong(workoutTitle, targetCls);
            workoutSong.setTempo(sourceSong.getTempo());

            // Set rhythms if available
            if (rhythm != null)
            {
                try
                {
                    var spts = workoutSong.getSongStructure().getSongParts();
                    workoutSong.getSongStructure().setSongPartsRhythm(spts, rhythm, null);
                }
                catch (Exception ex)
                {
                    LOGGER.log(Level.WARNING, "Could not set rhythm for workout: " + ex.getMessage(), ex);
                }
            }

            // Open the song in JJazzLab!
            SongEditorManager.getDefault().showSong(workoutSong, true, true);
            StatusDisplayer.getDefault().setStatusText("Workout song created: " + workoutTitle);
        }
        catch (UnsupportedEditException ex)
        {
            LOGGER.log(Level.SEVERE, "Error creating workout: " + ex.getMessage(), ex);
            StatusDisplayer.getDefault().setStatusText("Error creating workout: " + ex.getMessage());
        }
    }
}

