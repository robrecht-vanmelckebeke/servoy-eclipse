/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */

package com.servoy.eclipse.designer.editor.palette;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.ui.palette.PaletteCustomizer;
import org.eclipse.gef.ui.palette.customize.PaletteEntryFactory;
import org.eclipse.gef.ui.palette.customize.PaletteSeparatorFactory;
import org.eclipse.gef.ui.palette.customize.PaletteStackFactory;

/**
 * Customizer for palette with VisualFormEditor.
 * 
 * @author rgansevles
 *
 */
public class VisualFormEditorPaletteCustomizer extends PaletteCustomizer
{

	@Override
	public List<PaletteEntryFactory> getNewEntryFactories()
	{
		List<PaletteEntryFactory> list = new ArrayList<PaletteEntryFactory>(3);
		list.add(new PaletteSeparatorFactory());
		list.add(new PaletteStackFactory());
		list.add(new PaletteTemplateElementsFactory());
		return list;
	}


	@Override
	public void revertToSaved()
	{
		// TODO  

	}

	@Override
	public void save()
	{
		// TODO  
	}
}
