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
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.json.JSONException;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.core.repository.EclipseRepository;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.IVariable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.StringResource;
import com.servoy.j2db.persistence.Template;
import com.servoy.j2db.util.IdentDocumentValidator;

/**
 * An action to save some group of elements as template.
 */
public class SaveAsTemplateAction extends SelectionAction
{
	public SaveAsTemplateAction(IWorkbenchPart part)
	{
		super(part);
	}

	/**
	 * Initializes this action's text and images.
	 */
	@Override
	protected void init()
	{
		super.init();
		setText(DesignerActionFactory.SAVE_AS_TEMPLATE_TEXT);
		setToolTipText(DesignerActionFactory.SAVE_AS_TEMPLATE_TOOLTIP);
		setId(DesignerActionFactory.SAVE_AS_TEMPLATE.getId());
		setImageDescriptor(DesignerActionFactory.SAVE_AS_TEMPLATE_IMAGE);
	}

	@Override
	protected boolean calculateEnabled()
	{
		return getSelection() != null && !getSelection().isEmpty();
	}

	private static String askForTemplateName(Shell shell)
	{
		InputDialog dialog = new InputDialog(shell, "New template", "Specify a template name", null, new IInputValidator()
		{
			public String isValid(String newText)
			{
				if (newText.length() == 0) return "";
				return validateMethodName(newText);
			}
		});
		dialog.setBlockOnOpen(true);
		dialog.open();

		return (dialog.getReturnCode() == Window.CANCEL) ? null : dialog.getValue();
	}


	protected static String validateMethodName(String templateName)
	{
		// see if style name is OK
		if (!IdentDocumentValidator.isJavaIdentifier(templateName))
		{
			return "Template name has unsupported characters";
		}
		if (templateName.length() > IRepository.MAX_ROOT_OBJECT_NAME_LENGTH)
		{
			return "Name is too long";
		}
		else
		{
			IStatus validationResult = ServoyModel.getWorkspace().validateName(templateName, IResource.FILE);
			if (!validationResult.isOK())
			{
				return "The name of the template to be created is not valid: " + validationResult.getMessage();
			}
		}
		return null;
	}


	@Override
	public void run()
	{
		String templateName = askForTemplateName(getWorkbenchPart().getSite().getShell());
		if (templateName == null)
		{
			// cancelled
			return;
		}

		// check existing template
		StringResource existingTemplate = (Template)ServoyModelManager.getServoyModelManager().getServoyModel().getActiveRootObject(templateName,
			IRepository.TEMPLATES);
		if (existingTemplate != null && !MessageDialog.openConfirm(getWorkbenchPart().getSite().getShell(), "Template exists", //$NON-NLS-1$
			"A template with name '" + templateName + "' already exists, do you want to overwrite?")) //$NON-NLS-1$
		{
			return;
		}

		Form form = null;
		List<IPersist> persists = new ArrayList<IPersist>();
		for (Object selected : getSelectedObjects())
		{
			if (selected instanceof EditPart)
			{
				Object model = ((EditPart)selected).getModel();
				if (model instanceof Form)
				{
					form = (Form)model;
				}
				else if (model instanceof FormElementGroup)
				{
					Iterator<IFormElement> elements = ((FormElementGroup)model).getElements();
					while (elements.hasNext())
					{
						IFormElement next = elements.next();
						if (next instanceof IPersist)
						{
							persists.add((IPersist)next);
						}
					}
				}
				else if (model instanceof IPersist)
				{
					persists.add((IPersist)model);
				}
			}
		}
		if (form == null && persists.size() > 0)
		{
			form = (Form)persists.get(0).getAncestor(IRepository.FORMS);
		}
		else if (form != null && persists.size() == 0)
		{
			// just the form, add all non-script children
			Iterator<IPersist> allObjects = form.getAllObjects();
			while (allObjects.hasNext())
			{
				IPersist persist = allObjects.next();
				if (!(persist instanceof IVariable || persist instanceof IScriptProvider))
				{
					persists.add(persist);
				}
			}
		}

		if (form == null)
		{
			// should not happen
			ServoyLog.logError("Save template: no form (selection empty?)", null);
			return;
		}

		persists = completeHierarchy(form, persists);

		ServoyModelManager.getServoyModelManager().getServoyModel();
		EclipseRepository repository = (EclipseRepository)ServoyModel.getDeveloperRepository();
		try
		{
			StringResource template;
			if (existingTemplate == null)
			{
				// new template
				template = (StringResource)repository.createNewRootObject(templateName, IRepository.TEMPLATES);
			}
			else
			{
				// overwrite
				template = existingTemplate;
			}
			template.setResourceType(StringResource.FORM_TEMPLATE);
			template.setContent(ElementFactory.createTemplateContent(repository, form, persists));
			repository.updateRootObject(template);
		}
		catch (JSONException e)
		{
			ServoyLog.logError("Could not generate template content", e);
			MessageDialog.openError(getWorkbenchPart().getSite().getShell(), "Cannot create new template", "Reason: " + e.getMessage());
			return;
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError("Cannot save template", e);
			MessageDialog.openError(getWorkbenchPart().getSite().getShell(), "Cannot save template", "Reason: " + e.getMessage());
		}
	}

	/**
	 * Replace selected subelements with their parent nodes, for instance replace a tab with its tabpanel
	 * 
	 * @param form
	 * @param persists
	 * @return
	 */
	private List<IPersist> completeHierarchy(Form form, List<IPersist> persists)
	{
		List<IPersist> retval = new ArrayList<IPersist>();

		//add all nodes between the form and the selected items (for instance a tabpanel container of a selected tab)
		for (IPersist persist : persists)
		{
			if (persist != form)
			{
				while (persist.getParent() != form)
				{
					persist = persist.getParent();
				}
				if (!retval.contains(persist))
				{
					retval.add(persist);
				}
			}
		}
		return retval;
	}

}
