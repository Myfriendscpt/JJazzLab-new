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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.harmony.api.Note;
import org.jjazz.song.api.Song;
import org.openide.windows.WindowManager;

/**
 * Dialog to configure and generate a 12-key Modulation Workout.
 */
public class ModulationTrainerDialog extends JDialog
{
    private static ModulationTrainerDialog INSTANCE;

    private final JComboBox<CLI_Section> cmbSection;
    private final JComboBox<ModulationTrainerWorkoutBuilder.CycleType> cmbCycle;
    private final JSpinner spnReps;
    private final JComboBox<String> cmbAccidental;
    private final JButton btnGenerate;

    public static synchronized ModulationTrainerDialog getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new ModulationTrainerDialog();
        }
        return INSTANCE;
    }

    private ModulationTrainerDialog()
    {
        super(WindowManager.getDefault().getMainWindow(), "Modulation Trainer (12-Key Workout)", false);
        setResizable(false);

        cmbSection = new JComboBox<>();
        cmbSection.setRenderer(new javax.swing.DefaultListCellRenderer()
        {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
            {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof CLI_Section s)
                {
                    setText(s.getName() + " (bars " + (s.getPosition().getBar() + 1) + "-" + (s.getPosition().getBar() + s.getNbBars()) + ")");
                }
                return this;
            }
        });
        cmbCycle = new JComboBox<>(ModulationTrainerWorkoutBuilder.CycleType.values());
        spnReps = new JSpinner(new SpinnerNumberModel(1, 1, 8, 1));
        cmbAccidental = new JComboBox<>(new String[]{"Flats (b)", "Sharps (#)"});

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 8, 8));
        formPanel.setBorder(BorderFactory.createTitledBorder("Workout Configuration"));

        formPanel.add(new JLabel("Source Section to Practice:"));
        formPanel.add(cmbSection);
        formPanel.add(new JLabel("Modulation Cycle:"));
        formPanel.add(cmbCycle);
        formPanel.add(new JLabel("Repetitions per Key:"));
        formPanel.add(spnReps);
        formPanel.add(new JLabel("Accidental Preference:"));
        formPanel.add(cmbAccidental);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnGenerate = new JButton("Generate Workout Song");
        JButton btnCancel = new JButton("Cancel");

        btnGenerate.addActionListener(e -> {
            Song song = ActiveSongManager.getDefault().getActiveSong();
            CLI_Section sec = (CLI_Section) cmbSection.getSelectedItem();
            if (song != null && sec != null)
            {
                ModulationTrainerWorkoutBuilder.CycleType cycle = (ModulationTrainerWorkoutBuilder.CycleType) cmbCycle.getSelectedItem();
                int reps = (Integer) spnReps.getValue();
                Note.Accidental acc = cmbAccidental.getSelectedIndex() == 0 ? Note.Accidental.FLAT : Note.Accidental.SHARP;
                ModulationTrainerWorkoutBuilder.generateWorkoutSong(song, sec, cycle, reps, acc, true);
                setVisible(false);
            }
        });

        btnCancel.addActionListener(e -> setVisible(false));

        buttonPanel.add(btnGenerate);
        buttonPanel.add(btnCancel);

        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(getParent());
    }

    @Override
    public void setVisible(boolean b)
    {
        if (b)
        {
            Song song = ActiveSongManager.getDefault().getActiveSong();
            boolean hasSections = false;
            if (song != null)
            {
                List<CLI_Section> sections = song.getChordLeadSheet().getSections();
                DefaultComboBoxModel<CLI_Section> model = new DefaultComboBoxModel<>();
                for (CLI_Section s : sections)
                {
                    model.addElement(s);
                }
                cmbSection.setModel(model);
                hasSections = !sections.isEmpty();
            }
            btnGenerate.setEnabled(hasSections);
        }
        super.setVisible(b);
    }
}

