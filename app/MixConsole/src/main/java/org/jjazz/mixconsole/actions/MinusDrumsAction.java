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

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import org.jjazz.utilities.api.ResUtil;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(category = "MixConsole", id = "org.jjazz.mixconsole.actions.minusdrums")
@ActionRegistration(displayName = "#CTL_MinusDrums", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/MixConsole/MenuBar/Profiles", position = 300)
        })
public class MinusDrumsAction extends AbstractAction
{
    public MinusDrumsAction()
    {
        putValue(NAME, ResUtil.getString(getClass(), "CTL_MinusDrums"));
        putValue(SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_MinusDrumsTooltip"));
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        MinusOneProfile.MINUS_DRUMS.apply();
    }
}

