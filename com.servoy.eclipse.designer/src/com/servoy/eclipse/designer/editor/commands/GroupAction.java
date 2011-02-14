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
package com.servoy.eclipse.designer.editor.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.ui.IWorkbenchPart;

import com.servoy.eclipse.designer.actions.SetPropertyRequest;
import com.servoy.eclipse.designer.editor.GroupGraphicalEditPart;
import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.util.UUID;

/**
 * An action to group selected objects.
 */
public class GroupAction extends DesignerSelectionAction
{
	public GroupAction(IWorkbenchPart part)
	{
		super(part, null);
	}

	/**
	 * Initializes this action's text and images.
	 */
	@Override
	protected void init()
	{
		super.init();
		setText(DesignerActionFactory.GROUP_TEXT);
		setToolTipText(DesignerActionFactory.GROUP_TOOLTIP);
		setId(DesignerActionFactory.GROUP.getId());
		setImageDescriptor(DesignerActionFactory.GROUP_IMAGE);
	}

	@Override
	protected Map<EditPart, Request> createRequests(List<EditPart> selected)
	{
		return createGroupingRequests(selected);
	}

	public static Map<EditPart, Request> createGroupingRequests(List<EditPart> selected)
	{
		// check existing groups
		String groupID = null;
		int ngroups = 0;
		List<EditPart> affectedEditparts = new ArrayList<EditPart>();
		for (EditPart editPart : selected)
		{
			if (editPart instanceof GroupGraphicalEditPart)
			{
				ngroups++;
				groupID = ((GroupGraphicalEditPart)editPart).getGroup().getGroupID();
				affectedEditparts.addAll(editPart.getChildren());
			}
			else
			{
				affectedEditparts.add(editPart);
			}
		}

		if (affectedEditparts.size() == 0)
		{
			return null;
		}

		if (ngroups != 1)
		{
			// reuse the group if only 1 was part of the selection
			groupID = UUID.randomUUID().toString();
		}

		Map<EditPart, Request> requests = new HashMap<EditPart, Request>(affectedEditparts.size());
		for (EditPart editPart : affectedEditparts)
		{
			requests.put(editPart, new SetPropertyRequest(VisualFormEditor.REQ_SET_PROPERTY, StaticContentSpecLoader.PROPERTY_GROUPID.getPropertyName(),
				groupID, "group"));
		}

		return requests;
	}

	@Override
	protected Iterable<EditPart> getToRefresh(Iterable<EditPart> affected)
	{
		return DesignerUtil.getFormEditparts(affected);
	}
}
