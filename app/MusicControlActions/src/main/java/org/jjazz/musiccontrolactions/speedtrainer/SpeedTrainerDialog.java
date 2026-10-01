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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.sound.midi.Sequencer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.song.api.Song;
import org.openide.windows.WindowManager;

/**
 * Configuration dialog for Intelligent Speed Trainer.
 */
public class SpeedTrainerDialog extends JDialog implements PropertyChangeListener
{
    private static SpeedTrainerDialog INSTANCE;

    private final JCheckBox chkEnable;
    private final JSpinner spnStartTempo;
    private final JSpinner spnTargetTempo;
    private final JSpinner spnIncrement;
    private final JSpinner spnInterval;
    private final JLabel lblStatus;

    public static synchronized SpeedTrainerDialog getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new SpeedTrainerDialog();
        }
        return INSTANCE;
    }

    private SpeedTrainerDialog()
    {
        super(WindowManager.getDefault().getMainWindow(), "Intelligent Speed Trainer & Looper", false);
        setResizable(false);

        SpeedTrainerManager stm = SpeedTrainerManager.getInstance();

        // Components
        chkEnable = new JCheckBox("Enable Speed Trainer during looped playback", stm.isEnabled());
        spnStartTempo = new JSpinner(new SpinnerNumberModel(stm.getStartTempo(), 40, 320, 1));
        spnTargetTempo = new JSpinner(new SpinnerNumberModel(stm.getTargetTempo(), 40, 320, 1));
        spnIncrement = new JSpinner(new SpinnerNumberModel(stm.getBpmIncrement(), 1, 20, 1));
        spnInterval = new JSpinner(new SpinnerNumberModel(stm.getLoopInterval(), 1, 16, 1));
        lblStatus = new JLabel("Completed loops: " + stm.getCompletedLoops());

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Form panel
        JPanel formPanel = new JPanel(new GridLayout(5, 2, 8, 8));
        formPanel.setBorder(BorderFactory.createTitledBorder("Speed Ramp Settings"));

        formPanel.add(new JLabel("Start Tempo (BPM):"));
        formPanel.add(spnStartTempo);
        formPanel.add(new JLabel("Target Tempo (BPM):"));
        formPanel.add(spnTargetTempo);
        formPanel.add(new JLabel("Increment (+BPM):"));
        formPanel.add(spnIncrement);
        formPanel.add(new JLabel("Increase every N loops:"));
        formPanel.add(spnInterval);
        formPanel.add(new JLabel("Current Progress:"));
        formPanel.add(lblStatus);

        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.add(chkEnable, BorderLayout.NORTH);
        centerPanel.add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnStartNow = new JButton("Start Practice Loop");
        JButton btnClose = new JButton("Apply & Close");

        btnStartNow.addActionListener(e -> {
            applySettings();
            stm.setEnabled(true);
            chkEnable.setSelected(true);
            PlaybackSettings.getInstance().setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            Song activeSong = ActiveSongManager.getDefault().getActiveSong();
            if (activeSong != null)
            {
                activeSong.setTempo((Integer) spnStartTempo.getValue());
            }
            if (!MusicController.getInstance().isPlaying())
            {
                javax.swing.Action playAction = org.openide.awt.Actions.forID("MusicControls", "org.jjazz.musiccontrolactions.play");
                if (playAction != null)
                {
                    playAction.actionPerformed(e);
                }
            }
            setVisible(false);
        });

        btnClose.addActionListener(e -> {
            applySettings();
            setVisible(false);
        });

        buttonPanel.add(btnStartNow);
        buttonPanel.add(btnClose);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(getParent());

        stm.addPropertyChangeListener(this);
    }

    private void applySettings()
    {
        SpeedTrainerManager stm = SpeedTrainerManager.getInstance();
        stm.setEnabled(chkEnable.isSelected());
        stm.setStartTempo((Integer) spnStartTempo.getValue());
        stm.setTargetTempo((Integer) spnTargetTempo.getValue());
        stm.setBpmIncrement((Integer) spnIncrement.getValue());
        stm.setLoopInterval((Integer) spnInterval.getValue());
    }

    @Override
    public void setVisible(boolean b)
    {
        if (b)
        {
            SpeedTrainerManager stm = SpeedTrainerManager.getInstance();
            chkEnable.setSelected(stm.isEnabled());
            Song song = ActiveSongManager.getDefault().getActiveSong();
            if (song != null)
            {
                spnStartTempo.setValue(song.getTempo());
            }
            spnTargetTempo.setValue(stm.getTargetTempo());
            spnIncrement.setValue(stm.getBpmIncrement());
            spnInterval.setValue(stm.getLoopInterval());
            lblStatus.setText("Completed loops: " + stm.getCompletedLoops());
        }
        super.setVisible(b);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (SpeedTrainerManager.PROP_COMPLETED_LOOPS.equals(evt.getPropertyName()))
        {
            lblStatus.setText("Completed loops: " + evt.getNewValue());
        }
        else if (SpeedTrainerManager.PROP_ENABLED.equals(evt.getPropertyName()))
        {
            chkEnable.setSelected((Boolean) evt.getNewValue());
        }
    }
}

