package com.servoy.eclipse.core.repository;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.util.ReturnValueRunnable;
import com.servoy.eclipse.model.extensions.IUnexpectedSituationHandler;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.util.Utils;

/**
 * Handles unexpected situations that occur in model classes. Some require user decisions.
 * @author acostescu
 */
public class UnexpectedSituationHandler implements IUnexpectedSituationHandler
{

	public boolean allowUnexpectedDBIWrite(final ITable t)
	{
		// we will ask the user if he really wants to do this
		// normally difference markers will be solved by quick fixes - not by editing database information from
		// memory and then saving it...
		ReturnValueRunnable asker = new ReturnValueRunnable()
		{
			public void run()
			{
				returnValue = new Boolean(
					MessageDialog.openQuestion(Display.getCurrent().getActiveShell(),
						"Unexpected database information file write", //$NON-NLS-1$
						"The database information file (.dbi) contents for table '" + //$NON-NLS-1$
							t.getName() + "' of server '" + //$NON-NLS-1$
							t.getServerName() +
							"' are about to be written. This table currently has associated error markers for problems that might have prevented the loading of .dbi information in the first place. This means that you could be overwriting the current .dbi file contents with defaults.\nIf you are not sure why this happened, you should choose 'No', check the 'Problems' view for these error markers and try to solve them (see if context menu - Quick Fix is enabled).\n\nDo you wish to continue with the write?")); //$NON-NLS-1$
			}
		};
		if (Display.getCurrent() != null)
		{
			asker.run();
		}
		else
		{
			Display.getDefault().syncExec(asker);
		}
		return ((Boolean)asker.getReturnValue()).booleanValue();
	}

	public void cannotFindRepository()
	{
		MessageDialog.openError(Display.getCurrent().getActiveShell(), "Repository error", "Cannot find Servoy Eclipse repository."); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void cannotWriteI18NFiles(final Exception ex)
	{
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "Cannot write project I18N files.\n" + ex.getMessage()); //$NON-NLS-1$//$NON-NLS-2$
			}
		});
	}

	public void writeOverExistingScriptFile(final IFile scriptFile, final String fileContent)
	{
		Display.getDefault().asyncExec(new Runnable()
		{
			public void run()
			{
				try
				{
					IWorkbenchWindow[] workbenchWindows = PlatformUI.getWorkbench().getWorkbenchWindows();
					for (IWorkbenchWindow workbenchWindow : workbenchWindows)
					{
						if (workbenchWindow.getActivePage() == null) continue;
						IEditorReference[] editorReferences = workbenchWindow.getActivePage().getEditorReferences();
						for (IEditorReference reference : editorReferences)
						{
							IEditorInput editorInput = reference.getEditorInput();
							if (editorInput instanceof IFileEditorInput)
							{
								if (((IFileEditorInput)editorInput).getFile().equals(scriptFile))
								{

									IEditorPart editor = reference.getEditor(false);
									if (editor != null && editor.isDirty())
									{
										if (!MessageDialog.openQuestion(Display.getDefault().getActiveShell(), "Saving script changes with dirty editor", //$NON-NLS-1$
											"Overwrite editor changes? (if not then (property) changes could be ignored)")) //$NON-NLS-1$
										{
											return;
										}
									}
								}
							}
						}
					}
					scriptFile.setContents(Utils.getUTF8EncodedStream(fileContent), IResource.FORCE, null);
				}
				catch (CoreException e)
				{
					ServoyLog.logError(e);
				}
			}
		});
	}

}
