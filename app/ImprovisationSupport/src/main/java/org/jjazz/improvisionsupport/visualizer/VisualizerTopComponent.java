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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.harmony.api.ChordSymbol;
import org.jjazz.harmony.api.DefaultScaleManager;
import org.jjazz.harmony.api.Degree;
import org.jjazz.harmony.api.Position;
import org.jjazz.harmony.api.StandardScaleInstance;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.NoteListener;
import org.jjazz.musiccontrol.api.PlaybackListener;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.utilities.api.CoalescingTaskScheduler;
import org.jjazz.utilities.api.ResUtil;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * TopComponent displaying real-time piano keyboard and guitar fretboard visualizers.
 */
@ConvertAsProperties(
        dtd = "-//org.jjazz.improvisionsupport.visualizer//Visualizer//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "VisualizerTopComponent",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "output", openAtStartup = false, position = 100)
@ActionID(category = "Window", id = "org.jjazz.improvisionsupport.visualizer.VisualizerTopComponent")
@ActionReference(path = "Menu/Tools", position = 28)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_VisualizerAction",
        preferredID = "VisualizerTopComponent"
)
public final class VisualizerTopComponent extends TopComponent implements PlaybackListener, NoteListener, PropertyChangeListener
{
    private static final Logger LOGGER = Logger.getLogger(VisualizerTopComponent.class.getSimpleName());

    private final PianoKeyboardView pianoView;
    private final GuitarFretboardView fretboardView;
    private final JLabel lblCurrentChord;
    private final JComboBox<StandardScaleInstance> cmbMatchingScales;
    private final JCheckBox chkChordTones;
    private final JCheckBox chkScaleNotes;
    private final JCheckBox chkLiveNotes;

    // Throttled MIDI note visualizer updates (approx. 40 FPS / 25ms window) to avoid flooding EDT
    private final CoalescingTaskScheduler noteUpdateScheduler = new CoalescingTaskScheduler(25, false, null);
    private final Set<Integer> activeMidiNotes = ConcurrentHashMap.newKeySet();

    private ChordSymbol currentChordSymbol;

    public VisualizerTopComponent()
    {
        try
        {
            setName(ResUtil.getString(getClass(), "CTL_VisualizerTopComponent"));
            setToolTipText(ResUtil.getString(getClass(), "HINT_VisualizerTopComponent"));
        }
        catch (Exception ex)
        {
            setName("Instrument Visualizer (Piano & Fretboard)");
            setToolTipText("Interactive piano keyboard and guitar fretboard visualizer");
        }

        setLayout(new BorderLayout(5, 5));

        // Views
        pianoView = new PianoKeyboardView();
        fretboardView = new GuitarFretboardView();

        // Control Panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        lblCurrentChord = new JLabel("Chord: --");
        lblCurrentChord.setFont(new Font("SansSerif", Font.BOLD, 14));

        cmbMatchingScales = new JComboBox<>();
        cmbMatchingScales.setToolTipText("Select scale guide to display over the fretboard and keyboard");
        cmbMatchingScales.addActionListener(e -> updateScaleDisplay());

        chkChordTones = new JCheckBox("Chord Tones", true);
        chkScaleNotes = new JCheckBox("Scale Notes", true);
        chkLiveNotes = new JCheckBox("Live MIDI Hits", true);

        chkChordTones.addActionListener(e -> updateDisplayOptions());
        chkScaleNotes.addActionListener(e -> updateDisplayOptions());
        chkLiveNotes.addActionListener(e -> updateDisplayOptions());

        controlPanel.add(lblCurrentChord);
        controlPanel.add(new JLabel("Scale Guide:"));
        controlPanel.add(cmbMatchingScales);
        controlPanel.add(chkChordTones);
        controlPanel.add(chkScaleNotes);
        controlPanel.add(chkLiveNotes);

        add(controlPanel, BorderLayout.NORTH);

        // Tabbed Pane for Keyboard & Fretboard
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Piano Keyboard", pianoView);
        tabbedPane.addTab("Guitar / Bass Fretboard", fretboardView);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void updateDisplayOptions()
    {
        boolean ct = chkChordTones.isSelected();
        boolean sn = chkScaleNotes.isSelected();
        boolean ln = chkLiveNotes.isSelected();
        pianoView.setDisplayOptions(ct, sn, ln);
        fretboardView.setDisplayOptions(ct, sn, ln);
    }

    private void updateScaleDisplay()
    {
        StandardScaleInstance ssi = (StandardScaleInstance) cmbMatchingScales.getSelectedItem();
        if (ssi != null)
        {
            Set<Integer> scalePitches = new HashSet<>(ssi.getRelativePitches());
            pianoView.setScalePitches(scalePitches);
            fretboardView.setScalePitches(scalePitches);
        }
        else
        {
            pianoView.setScalePitches(null);
            fretboardView.setScalePitches(null);
        }
    }

    @Override
    public void componentOpened()
    {
        MusicController mc = MusicController.getInstance();
        mc.addPlaybackListener(this);
        mc.addNoteListener(this);
        mc.addPropertyChangeListener(this);
    }

    @Override
    public void componentClosed()
    {
        MusicController mc = MusicController.getInstance();
        mc.removePlaybackListener(this);
        mc.removeNoteListener(this);
        mc.removePropertyChangeListener(this);
        noteUpdateScheduler.cancel();
        activeMidiNotes.clear();
        pianoView.clearActiveNotes();
        fretboardView.clearActiveNotes();
    }

    // =========================================================================
    // PlaybackListener
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
    }

    @Override
    public void chordSymbolChanged(CLI_ChordSymbol cliCs)
    {
        if (cliCs == null)
        {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            currentChordSymbol = cliCs.getChordSymbol();
            if (currentChordSymbol != null)
            {
                lblCurrentChord.setText("Chord: " + currentChordSymbol.getOriginalName());
                int root = currentChordSymbol.getRoot().getRelativePitch();
                pianoView.setRootPitch(root);
                fretboardView.setRootPitch(root);

                // Chord degrees
                Set<Integer> chordPitches = new HashSet<>();
                for (Degree d : currentChordSymbol.getChordType().getDegrees())
                {
                    chordPitches.add((root + d.getPitch()) % 12);
                }
                pianoView.setChordPitches(chordPitches);
                fretboardView.setChordPitches(chordPitches);

                // Scales
                List<StandardScaleInstance> matchingScales = DefaultScaleManager.getInstance()
                        .getMatchingScales(currentChordSymbol);

                DefaultComboBoxModel<StandardScaleInstance> model = new DefaultComboBoxModel<>();
                for (StandardScaleInstance ssi : matchingScales)
                {
                    model.addElement(ssi);
                }
                cmbMatchingScales.setModel(model);
                updateScaleDisplay();
            }
        });
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
    // NoteListener
    // =========================================================================

    @Override
    public void noteOn(long tick, int channel, int pitch, int velocity)
    {
        activeMidiNotes.add(pitch);
        noteUpdateScheduler.requestOnEdt(this::syncActiveNotesToViews);
    }

    @Override
    public void noteOff(long tick, int channel, int pitch)
    {
        activeMidiNotes.remove(pitch);
        noteUpdateScheduler.requestOnEdt(this::syncActiveNotesToViews);
    }

    private void syncActiveNotesToViews()
    {
        Set<Integer> snapshot = new HashSet<>(activeMidiNotes);
        pianoView.setActivePitches(snapshot);
        fretboardView.setActivePitches(snapshot);
    }

    // =========================================================================
    // PropertyChangeListener
    // =========================================================================

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == MusicController.getInstance()
                && MusicController.PROP_STATE.equals(evt.getPropertyName()))
        {
            MusicController.State state = (MusicController.State) evt.getNewValue();
            if (state == MusicController.State.STOPPED || state == MusicController.State.PAUSED)
            {
                noteUpdateScheduler.cancel();
                activeMidiNotes.clear();
                SwingUtilities.invokeLater(() -> {
                    pianoView.clearActiveNotes();
                    fretboardView.clearActiveNotes();
                });
            }
            if (state == MusicController.State.STOPPED)
            {
                SwingUtilities.invokeLater(() -> {
                    lblCurrentChord.setText("Chord: --");
                    pianoView.setRootPitch(-1);
                    fretboardView.setRootPitch(-1);
                    pianoView.setChordPitches(null);
                    fretboardView.setChordPitches(null);
                    pianoView.setScalePitches(null);
                    fretboardView.setScalePitches(null);
                });
            }
        }
    }

    void writeProperties(java.util.Properties p)
    {
        p.setProperty("version", "1.0");
    }

    void readProperties(java.util.Properties p)
    {
    }

    public static VisualizerTopComponent getInstance()
    {
        return (VisualizerTopComponent) WindowManager.getDefault().findTopComponent("VisualizerTopComponent");
    }
}

