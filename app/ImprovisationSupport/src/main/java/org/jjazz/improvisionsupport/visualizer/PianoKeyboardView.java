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
package org.jjazz.improvisionsupport.visualizer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JComponent;

/**
 * Interactive piano keyboard displaying chord tones, scale notes, and live MIDI notes.
 */
public class PianoKeyboardView extends JComponent
{
    private static final int START_PITCH = 48; // C3
    private static final int NUM_KEYS = 37;    // C3 to C6 (3 octaves + 1 note)

    private final Set<Integer> activePitches = new HashSet<>();
    private final Set<Integer> chordPitches = new HashSet<>();
    private final Set<Integer> scalePitches = new HashSet<>();
    private int rootPitch = -1;

    private boolean showChordTones = true;
    private boolean showScaleNotes = true;
    private boolean showLiveNotes = true;

    private static final boolean[] IS_BLACK = new boolean[]{
        false, true, false, true, false, false, true, false, true, false, true, false
    };

    private static final String[] NOTE_NAMES = new String[]{
        "C", "Db", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B"
    };

    public PianoKeyboardView()
    {
        setPreferredSize(new Dimension(650, 110));
        setMinimumSize(new Dimension(300, 80));
    }

    public void setRootPitch(int rootPitch)
    {
        this.rootPitch = (rootPitch >= 0) ? (rootPitch % 12) : -1;
        repaint();
    }

    public void setChordPitches(Set<Integer> pitches)
    {
        this.chordPitches.clear();
        if (pitches != null)
        {
            for (int p : pitches)
            {
                this.chordPitches.add(p % 12);
            }
        }
        repaint();
    }

    public void setScalePitches(Set<Integer> pitches)
    {
        this.scalePitches.clear();
        if (pitches != null)
        {
            for (int p : pitches)
            {
                this.scalePitches.add(p % 12);
            }
        }
        repaint();
    }

    public void noteOn(int pitch)
    {
        activePitches.add(pitch);
        repaint();
    }

    public void noteOff(int pitch)
    {
        activePitches.remove(pitch);
        repaint();
    }

    public void setActivePitches(Set<Integer> pitches)
    {
        activePitches.clear();
        if (pitches != null)
        {
            activePitches.addAll(pitches);
        }
        repaint();
    }

    public void clearActiveNotes()
    {
        activePitches.clear();
        repaint();
    }

    public void setDisplayOptions(boolean chordTones, boolean scaleNotes, boolean liveNotes)
    {
        this.showChordTones = chordTones;
        this.showScaleNotes = scaleNotes;
        this.showLiveNotes = liveNotes;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Background
        g2.setColor(new Color(30, 30, 35));
        g2.fillRect(0, 0, width, height);

        // Count white keys
        int numWhiteKeys = 0;
        for (int i = 0; i < NUM_KEYS; i++)
        {
            if (!IS_BLACK[(START_PITCH + i) % 12])
            {
                numWhiteKeys++;
            }
        }

        double whiteKeyWidth = (double) (width - 4) / numWhiteKeys;
        int whiteKeyHeight = height - 6;
        int blackKeyHeight = (int) (whiteKeyHeight * 0.62);
        double blackKeyWidth = whiteKeyWidth * 0.65;

        // Draw white keys
        double xPos = 2;
        int whiteKeyIndex = 0;
        for (int i = 0; i < NUM_KEYS; i++)
        {
            int pitch = START_PITCH + i;
            int rel = pitch % 12;
            if (!IS_BLACK[rel])
            {
                int kx = (int) Math.round(xPos + whiteKeyIndex * whiteKeyWidth);
                int kw = (int) Math.round(xPos + (whiteKeyIndex + 1) * whiteKeyWidth) - kx - 1;

                boolean isPressed = showLiveNotes && (activePitches.contains(pitch)
                        || activePitches.contains(pitch - 12)
                        || activePitches.contains(pitch + 12));
                boolean isRoot = showChordTones && (rel == rootPitch);
                boolean isChord = showChordTones && chordPitches.contains(rel);
                boolean isScale = showScaleNotes && scalePitches.contains(rel);

                Color keyColor;
                if (isPressed)
                {
                    keyColor = new Color(255, 160, 40); // Vivid orange
                }
                else if (isRoot)
                {
                    keyColor = new Color(255, 205, 210); // Soft red
                }
                else if (isChord)
                {
                    keyColor = new Color(200, 230, 201); // Soft green
                }
                else if (isScale)
                {
                    keyColor = new Color(227, 242, 253); // Soft blue
                }
                else
                {
                    keyColor = Color.WHITE;
                }

                g2.setColor(keyColor);
                g2.fillRoundRect(kx, 2, kw, whiteKeyHeight, 4, 4);
                g2.setColor(new Color(120, 120, 130));
                g2.drawRoundRect(kx, 2, kw, whiteKeyHeight, 4, 4);

                // Note label
                if (rel == 0 || isRoot || isChord)
                {
                    g2.setFont(new Font("SansSerif", isRoot ? Font.BOLD : Font.PLAIN, 10));
                    g2.setColor(isRoot ? new Color(180, 20, 20) : Color.DARK_GRAY);
                    String label = NOTE_NAMES[rel];
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = kx + (kw - fm.stringWidth(label)) / 2;
                    g2.drawString(label, tx, whiteKeyHeight - 6);
                }

                whiteKeyIndex++;
            }
        }

        // Draw black keys on top
        whiteKeyIndex = 0;
        for (int i = 0; i < NUM_KEYS; i++)
        {
            int pitch = START_PITCH + i;
            int rel = pitch % 12;
            if (IS_BLACK[rel])
            {
                // Position above the border of previous white key
                double center = xPos + whiteKeyIndex * whiteKeyWidth;
                int kx = (int) Math.round(center - blackKeyWidth / 2.0);
                int kw = (int) Math.round(blackKeyWidth);

                boolean isPressed = showLiveNotes && (activePitches.contains(pitch)
                        || activePitches.contains(pitch - 12)
                        || activePitches.contains(pitch + 12));
                boolean isRoot = showChordTones && (rel == rootPitch);
                boolean isChord = showChordTones && chordPitches.contains(rel);
                boolean isScale = showScaleNotes && scalePitches.contains(rel);

                Color keyColor;
                if (isPressed)
                {
                    keyColor = new Color(255, 140, 0);
                }
                else if (isRoot)
                {
                    keyColor = new Color(190, 40, 40);
                }
                else if (isChord)
                {
                    keyColor = new Color(46, 125, 50);
                }
                else if (isScale)
                {
                    keyColor = new Color(30, 90, 160);
                }
                else
                {
                    keyColor = new Color(25, 25, 28);
                }

                g2.setColor(keyColor);
                g2.fillRoundRect(kx, 2, kw, blackKeyHeight, 3, 3);
                g2.setColor(new Color(60, 60, 70));
                g2.drawRoundRect(kx, 2, kw, blackKeyHeight, 3, 3);

                if (isRoot || isChord)
                {
                    g2.setFont(new Font("SansSerif", Font.BOLD, 9));
                    g2.setColor(Color.WHITE);
                    String label = NOTE_NAMES[rel];
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = kx + (kw - fm.stringWidth(label)) / 2;
                    g2.drawString(label, tx, blackKeyHeight - 6);
                }
            }
            else
            {
                whiteKeyIndex++;
            }
        }

        g2.dispose();
    }
}

