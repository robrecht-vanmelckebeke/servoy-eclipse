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
package com.servoy.eclipse.ui.wizards;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyResourcesProject;
import com.servoy.eclipse.core.builder.ChangeResourcesProjectQuickFix.IValidator;
import com.servoy.eclipse.core.builder.ChangeResourcesProjectQuickFix.ResourcesProjectChooserComposite;
import com.servoy.eclipse.core.repository.EclipseImportUserChannel;
import com.servoy.eclipse.core.repository.EclipseRepository;
import com.servoy.eclipse.core.repository.XMLEclipseWorkspaceImportHandlerVersions11AndHigher;
import com.servoy.eclipse.ui.util.MessageDialog;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.server.ApplicationServerSingleton;
import com.servoy.j2db.server.IApplicationServerSingleton;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.xmlxport.IXMLImportEngine;
import com.servoy.j2db.util.xmlxport.IXMLImportHandlerVersions11AndHigher;

public class ImportSolutionWizard extends Wizard implements IImportWizard
{
	private ImportSolutionWizardPage page;

	@Override
	public boolean performFinish()
	{
		final File file = new File(page.getPath());
		if (!file.exists() || !file.isFile())
		{
			org.eclipse.jface.dialogs.MessageDialog.openError(getShell(), "Import failed", "Invalid file.");
			return false;
		}
		if (!file.canRead())
		{
			org.eclipse.jface.dialogs.MessageDialog.openError(getShell(), "Import failed", "File cannot be read.");
			return false;
		}
		if (page.getErrorMessage() != null)
		{
			org.eclipse.jface.dialogs.MessageDialog.openError(getShell(), "Import failed", page.getErrorMessage());
			return false;
		}
		final String resourcesProjectName = page.getNewName();
		final ServoyResourcesProject existingProject = page.getResourcesProject();
		final boolean cleanImport = page.isCleanImport();
		final boolean allowDataModelChanges = page.getAllowDataModelChange();
		final boolean displayDataModelChanges = page.getDisplayDataModelChange();
		IRunnableWithProgress runnable = new IRunnableWithProgress()
		{
			public void run(IProgressMonitor monitor)
			{
				final EclipseImportUserChannel userChannel = new EclipseImportUserChannel(allowDataModelChanges, displayDataModelChanges, getShell());
				IApplicationServerSingleton as = ApplicationServerSingleton.get();
				try
				{
					IXMLImportEngine importEngine = as.createXMLImportEngine(file, (EclipseRepository)ServoyModel.getDeveloperRepository(), as.getDataServer(),
						as.getClientId(), userChannel);

					IXMLImportHandlerVersions11AndHigher x11handler = as.createXMLInMemoryImportHandler(importEngine.getVersionInfo(), as.getDataServer(),
						as.getClientId(), userChannel, (EclipseRepository)ServoyModel.getDeveloperRepository());

					IRootObject[] rootObjects = XMLEclipseWorkspaceImportHandlerVersions11AndHigher.importFromJarFile(importEngine, x11handler, userChannel,
						(EclipseRepository)ServoyModel.getDeveloperRepository(), resourcesProjectName, existingProject, monitor, cleanImport);
					if (rootObjects != null)
					{
						final String detail = userChannel.getAllImportantMSGes() + "\nSolution '" + rootObjects[0].getName() + "' imported and activated.";
						Display.getDefault().asyncExec(new Runnable()
						{
							public void run()
							{
								MessageDialog dialog = new MessageDialog(getShell(), "Solution imported", null, null,
									org.eclipse.jface.dialogs.MessageDialog.NONE, new String[] { "OK" }, 0, detail);
								dialog.open();
							}
						});
					}
				}
				catch (final RepositoryException ex)
				{
					// Don't show an error message if the import was canceled.
					if (!ex.hasErrorCode(ServoyException.InternalCodes.OPERATION_CANCELLED))
					{
						// Don't show an stack trace for CRC related messages.
						if (!ex.hasErrorCode(ServoyException.InternalCodes.CHECKSUM_FAILURE))
						{
							ServoyLog.logError(ex);
						}
						Display.getDefault().asyncExec(new Runnable()
						{
							public void run()
							{
								org.eclipse.jface.dialogs.MessageDialog.openError(getShell(), "Could not import solution", ex.getMessage());
							}
						});
					}
				}
				catch (final Exception ex)
				{
					ServoyLog.logError(ex);
					String msg = "An unexpected error occured";
					if (ex.getMessage() != null) msg += ex.getMessage();
					else msg += ". Check the log for more details.";
					final String mymsg = msg;
					Display.getDefault().asyncExec(new Runnable()
					{
						public void run()
						{
							org.eclipse.jface.dialogs.MessageDialog.openError(getShell(), "Could not import solution", mymsg);
						}
					});

				}
			}
		};
		try
		{
			getContainer().run(true, false, runnable);
		}
		catch (InvocationTargetException e)
		{
			ServoyLog.logError(e);
		}
		catch (InterruptedException e)
		{
			ServoyLog.logError(e);
		}
		return true;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		setNeedsProgressMonitor(true);
		page = new ImportSolutionWizardPage("Import solution");
	}

	@Override
	public void addPages()
	{
		addPage(page);
	}

	public static class ImportSolutionWizardPage extends WizardPage implements IValidator
	{
		private ResourcesProjectChooserComposite resourceProjectComposite;
		private Text filePath;
		private Button browseButton;
		private Button cleanImport;
		private Button allowDataModelChange;
		private Button displayDataModelChanges;

		protected ImportSolutionWizardPage(String pageName)
		{
			super(pageName);
			setTitle("Import a solution");
			setDescription("A solution (with or without modules) will be imported into the workspace from a .servoy file.");
		}

		public void createControl(Composite parent)
		{
			Composite topLevel = new Composite(parent, SWT.NONE);
			topLevel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
			setControl(topLevel);

			filePath = new Text(topLevel, SWT.BORDER);
			filePath.addModifyListener(new ModifyListener()
			{
				public void modifyText(ModifyEvent e)
				{
					validate();
				}
			});
			browseButton = new Button(topLevel, SWT.NONE);
			browseButton.setText("Browse");
			browseButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{

					FileDialog dlg = new FileDialog(getShell(), SWT.NONE);
					dlg.setFilterExtensions(new String[] { "*.servoy" });
					if (dlg.open() != null)
					{
						filePath.setText(dlg.getFilterPath() + File.separator + dlg.getFileName());
					}
				}
			});
			cleanImport = new Button(topLevel, SWT.CHECK);
			cleanImport.setText("Clean Import");

			allowDataModelChange = new Button(topLevel, SWT.CHECK);
			allowDataModelChange.setText("Allow data model (database) changes");
			allowDataModelChange.setSelection(true);

			displayDataModelChanges = new Button(topLevel, SWT.CHECK);
			displayDataModelChanges.setText("Display data model (database) changes");

			resourceProjectComposite = new ResourcesProjectChooserComposite(topLevel, SWT.NONE, this,
				"Please choose the resources project the solution will reference (for styles, column/sequence info, security)",
				ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject());

			// layout of the page
			FormLayout formLayout = new FormLayout();
			formLayout.spacing = 5;
			formLayout.marginWidth = formLayout.marginHeight = 20;
			topLevel.setLayout(formLayout);

			FormData formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.top = new FormAttachment(browseButton, 0, SWT.CENTER);
			formData.right = new FormAttachment(100, -100);
			filePath.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(filePath, 0);
			formData.top = new FormAttachment(0, 0);
			formData.right = new FormAttachment(100, 0);
			browseButton.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(filePath, 0, SWT.LEFT);
			formData.top = new FormAttachment(filePath, 0, SWT.BOTTOM);
			formData.right = new FormAttachment(100, 0);
			cleanImport.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(cleanImport, 0, SWT.LEFT);
			formData.top = new FormAttachment(cleanImport, 0, SWT.BOTTOM);
			formData.right = new FormAttachment(100, 0);
			allowDataModelChange.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(allowDataModelChange, 10, SWT.LEFT);
			formData.top = new FormAttachment(allowDataModelChange, 0, SWT.BOTTOM);
			formData.right = new FormAttachment(100, 0);
			displayDataModelChanges.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.right = new FormAttachment(100, 0);
			formData.top = new FormAttachment(displayDataModelChanges, 10);
			formData.bottom = new FormAttachment(100, 0);
			resourceProjectComposite.setLayoutData(formData);
		}

		public String validate()
		{
			String error = null;
			if (filePath.getText().trim().length() == 0)
			{
				error = "Please select servoy file to import.";
			}
			else if (resourceProjectComposite != null) error = resourceProjectComposite.validate();
			setErrorMessage(error);
			return error;
		}

		public String getPath()
		{
			return filePath.getText();
		}

		public ServoyResourcesProject getResourcesProject()
		{
			return resourceProjectComposite.getExistingResourceProject();
		}

		public String getNewName()
		{
			return resourceProjectComposite.getNewResourceProjectName();
		}

		public boolean isCleanImport()
		{
			return cleanImport.getSelection();
		}

		public boolean getAllowDataModelChange()
		{
			return allowDataModelChange.getSelection();
		}

		public boolean getDisplayDataModelChange()
		{
			return displayDataModelChanges.getSelection();
		}
	}

}
