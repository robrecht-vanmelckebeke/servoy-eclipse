/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.designer.editor.mobile.editparts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.SnapToHelper;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.IPersistChangeListener;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.BaseFormGraphicalEditPart;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.mobile.MobileFormEditPolicy;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.property.MobileListModel;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.debug.layout.MobileFormLayout;
import com.servoy.j2db.persistence.FlattenedForm;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.util.Utils;

/**
 * Edit part form form in mobile form editor.
 * 
 * @author rgansevles
 *
 */
public class MobileFormGraphicalEditPart extends BaseFormGraphicalEditPart implements IPersistChangeListener
{
	/**
	 * @param application
	 * @param editorPart
	 */
	public MobileFormGraphicalEditPart(IApplication application, BaseVisualFormEditor editorPart)
	{
		super(application, editorPart);
	}

	@Override
	public List<Object> getModelChildren()
	{
		FlattenedSolution editingFlattenedSolution = ModelUtils.getEditingFlattenedSolution(getPersist());
		Form flattenedForm = editingFlattenedSolution.getFlattenedForm(getPersist());
		List<Object> list = new ArrayList<Object>();

		for (Part part : Utils.iterate(flattenedForm.getParts()))
		{
			if (part.getPartType() == Part.HEADER)
			{
				list.add(part);
			}
		}

		if (flattenedForm.getView() == FormController.LOCKED_TABLE_VIEW)
		{
			// ignore all other elements, just the list items.
			list.add(MobileListModel.create(getPersist(), getPersist()));
		}
		else
		{
			for (ISupportBounds element : MobileFormLayout.getBodyElementsForRecordView(editingFlattenedSolution, flattenedForm))
			{
				if (element instanceof Portal && ((Portal)element).isMobileInsetList())
				{
					// inset list
					list.add(MobileListModel.create(FlattenedForm.getWrappedForm(flattenedForm), ((Portal)element)));
				}
				else
				{
					list.add(element);
				}
			}
		}

		for (Part part : Utils.iterate(flattenedForm.getParts()))
		{
			if (part.getPartType() == Part.FOOTER)
			{
				list.add(part);
			}
		}

		return list;
	}


	@Override
	public void activate()
	{
		// listen to changes to the elements
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		servoyModel.addPersistChangeListener(false, this);

		super.activate();
	}

	@Override
	public void deactivate()
	{
		// stop listening to changes to the elements
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		servoyModel.removePersistChangeListener(false, this);

		super.deactivate();
	}

	// If the form changed width we have to refresh
	public void persistChanges(Collection<IPersist> changes)
	{
		for (IPersist persist : changes)
		{
			if (getEditorPart().getForm().equals(persist.getAncestor(IRepository.FORMS)))
			{
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						if (getParent() != null) refresh();
					}
				});
				return;
			}
		}
	}

	@Override
	protected IFigure createFigure()
	{
		FreeformLayer formLayer = new FreeformLayer();
		// has to be a XYLayout, see XYLayoutEditPolicy.getXYLayout()
		formLayer.setLayoutManager(new SetHeightToBodyPartXYLayoutManager(new MobileFormLayoutManager(), getPersist()));
		return formLayer;
	}

	@Override
	public Object getAdapter(Class key)
	{
		if (key == SnapToHelper.class)
		{
			return new MobileSnapToHelper(this);
		}
		return super.getAdapter(key);
	}

	@Override
	protected void createEditPolicies()
	{
		installEditPolicy(EditPolicy.LAYOUT_ROLE, new MobileFormXYLayoutEditPolicy());
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new MobileFormEditPolicy(getApplication()));
	}

	@Override
	protected EditPart createChild(Object child)
	{
		return createChild(getApplication(), getEditorPart(), getPersist(), child);
	}

	public static EditPart createChild(IApplication application, BaseVisualFormEditor editorPart, Form form, Object child)
	{
		if (child instanceof Part && ((Part)child).getPartType() == Part.HEADER)
		{
			return new MobileHeaderGraphicalEditPart(application, editorPart, (Part)child);
		}
		if (child instanceof Part && ((Part)child).getPartType() == Part.FOOTER)
		{
			return new MobileFooterGraphicalEditPart(application, editorPart, (Part)child);
		}
		if (child instanceof FormElementGroup)
		{
			return new MobileGroupGraphicalEditPart(application, editorPart, form, (FormElementGroup)child);
		}
		if (child instanceof MobileListModel)
		{
			return new MobileListGraphicalEditPart(application, (MobileListModel)child, editorPart);
		}
		return new MobilePersistGraphicalEditPart(application, (IPersist)child, form, ModelUtils.isInheritedFormElement(child, form),
			new MobilePersistGraphicalEditPartFigureFactory(application, editorPart.getForm()));
	}
}
