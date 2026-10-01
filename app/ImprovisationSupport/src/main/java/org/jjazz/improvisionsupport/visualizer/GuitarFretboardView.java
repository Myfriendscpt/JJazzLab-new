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
 * Interactive guitar and bass fretboard displaying chord tones, scale notes, and live MIDI notes.
 */
public class GuitarFretboardView extends JComponent
{
    private static final int NUM_FRETS = 15;
    // Standard Guitar Tuning: E4 (64), B3 (59), G3 (55), D3 (50), A2 (45), E2 (40)
    private static final int[] STRING_TUNING = new int[]{64, 59, 55, 50, 45, 40};
    private static final String[] STRING_NAMES = new String[]{"e", "B", "G", "D", "A", "E"};

    private static final String[] NOTE_NAMES = new String[]{
        "C", "Db", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B"
    };

    private final Set<Integer> activePitches = new HashSet<>();
    private final Set<Integer> chordPitches = new HashSet<>();
    private final Set<Integer> scalePitches = new HashSet<>();
    private int rootPitch = -1;

    private boolean showChordTones = true;
    private boolean showScaleNotes = true;
    private boolean showLiveNotes = true;

    public GuitarFretboardView()
    {
        setPreferredSize(new Dimension(650, 140));
        setMinimumSize(new Dimension(300, 100));
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

        // Fretboard wood background
        g2.setColor(new Color(45, 36, 30));
        g2.fillRect(0, 0, width, height);

        int leftMargin = 30;
        int rightMargin = 10;
        int topMargin = 18;
        int bottomMargin = 18;

        int boardWidth = width - leftMargin - rightMargin;
        int boardHeight = height - topMargin - bottomMargin;

        double fretWidth = (double) boardWidth / NUM_FRETS;
        double stringSpacing = (double) boardHeight / (STRING_TUNING.length - 1);

        // Draw Nut (fret 0)
        g2.setColor(new Color(230, 225, 215));
        g2.fillRect(leftMargin - 5, topMargin - 4, 6, boardHeight + 8);

        // Draw Frets & Fret numbers
        g2.setColor(new Color(175, 175, 185));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
        FontMetrics fm = g2.getFontMetrics();

        for (int f = 1; f <= NUM_FRETS; f++)
        {
            int fx = (int) Math.round(leftMargin + f * fretWidth);
            g2.drawLine(fx, topMargin, fx, topMargin + boardHeight);

            // Fret markers (dots on 3, 5, 7, 9, 12, 15)
            if (f == 3 || f == 5 || f == 7 || f == 9 || f == 15)
            {
                int dotX = (int) Math.round(leftMargin + (f - 0.5) * fretWidth);
                int dotY = topMargin + boardHeight / 2;
                g2.setColor(new Color(160, 155, 145, 130));
                g2.fillOval(dotX - 4, dotY - 4, 8, 8);
                g2.setColor(new Color(175, 175, 185));
            }
            else if (f == 12)
            {
                int dotX = (int) Math.round(leftMargin + (f - 0.5) * fretWidth);
                int dotY1 = (int) (topMargin + boardHeight * 0.3);
                int dotY2 = (int) (topMargin + boardHeight * 0.7);
                g2.setColor(new Color(160, 155, 145, 130));
                g2.fillOval(dotX - 4, dotY1 - 4, 8, 8);
                g2.fillOval(dotX - 4, dotY2 - 4, 8, 8);
                g2.setColor(new Color(175, 175, 185));
            }

            // Fret number at bottom
            String fStr = String.valueOf(f);
            int tx = (int) Math.round(leftMargin + (f - 0.5) * fretWidth - fm.stringWidth(fStr) / 2.0);
            g2.drawString(fStr, tx, height - 4);
        }

        // Draw Strings
        for (int s = 0; s < STRING_TUNING.length; s++)
        {
            int sy = (int) Math.round(topMargin + s * stringSpacing);
            int stringThickness = Math.max(1, s + 1);

            g2.setColor(new Color(200, 200, 210));
            g2.fillRect(leftMargin, sy - stringThickness / 2, boardWidth, stringThickness);

            // String name label on headstock/left
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.setColor(new Color(220, 220, 220));
            g2.drawString(STRING_NAMES[s], 10, sy + 4);
        }

        // Draw Notes / Markers on Frets
        int markerRadius = 9;
        for (int s = 0; s < STRING_TUNING.length; s++)
        {
            int basePitch = STRING_TUNING[s];
            int sy = (int) Math.round(topMargin + s * stringSpacing);

            for (int f = 0; f <= NUM_FRETS; f++)
            {
                int pitch = basePitch + f;
                int rel = pitch % 12;

                boolean isPressed = showLiveNotes && (activePitches.contains(pitch)
                        || activePitches.contains(pitch - 12)
                        || activePitches.contains(pitch + 12));
                boolean isRoot = showChordTones && (rel == rootPitch);
                boolean isChord = showChordTones && chordPitches.contains(rel);
                boolean isScale = showScaleNotes && scalePitches.contains(rel);

                if (!isPressed && !isRoot && !isChord && !isScale)
                {
                    continue;
                }

                int cx = (f == 0)
                        ? leftMargin - 14
                        : (int) Math.round(leftMargin + (f - 0.5) * fretWidth);

                Color dotColor;
                Color textColor = Color.WHITE;
                if (isPressed)
                {
                    dotColor = new Color(255, 140, 0); // Orange
                }
                else if (isRoot)
                {
                    dotColor = new Color(220, 40, 40); // Red
                }
                else if (isChord)
                {
                    dotColor = new Color(46, 125, 50); // Green
                }
                else
                {
                    dotColor = new Color(33, 150, 243); // Blue
                }

                g2.setColor(dotColor);
                g2.fillOval(cx - markerRadius, sy - markerRadius, markerRadius * 2, markerRadius * 2);
                g2.setColor(Color.WHITE);
                g2.drawOval(cx - markerRadius, sy - markerRadius, markerRadius * 2, markerRadius * 2);

                // Note Name inside dot
                g2.setFont(new Font("SansSerif", Font.BOLD, 9));
                String noteStr = NOTE_NAMES[rel];
                FontMetrics dfm = g2.getFontMetrics();
                int nx = cx - dfm.stringWidth(noteStr) / 2;
                int ny = sy + dfm.getAscent() / 2 - 1;
                g2.setColor(textColor);
                g2.drawString(noteStr, nx, ny);
            }
        }

        g2.dispose();
    }
}

