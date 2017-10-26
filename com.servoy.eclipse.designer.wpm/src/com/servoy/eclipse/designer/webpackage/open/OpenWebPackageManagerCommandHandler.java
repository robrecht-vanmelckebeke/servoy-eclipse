package com.servoy.eclipse.designer.webpackage.open;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.intro.impl.model.url.IntroURL;

import com.servoy.eclipse.core.IStartPageAction;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.util.EditorUtil;

public class OpenWebPackageManagerCommandHandler extends AbstractHandler implements IHandler, IStartPageAction
{

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		ISelection selection = HandlerUtil.getActiveMenuSelection(event);
		String solutionName = null;
		if (selection instanceof StructuredSelection && ((StructuredSelection)selection).getFirstElement() instanceof SimpleUserNode)
		{
			SimpleUserNode project = ((SimpleUserNode)((StructuredSelection)selection).getFirstElement()).getAncestorOfType(ServoyProject.class);
			if (project != null)
			{
				solutionName = ((ServoyProject)project.getRealObject()).getSolution().getName();
			}
		}
		EditorUtil.openWebPackageManager(solutionName);
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.commands.AbstractHandler#isEnabled()
	 */
	@Override
	public boolean isEnabled()
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.core.IStartPageAction#runAction(com.servoy.eclipse.core.IntroURL)
	 */
	@Override
	public void runAction(IntroURL introUrl)
	{
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		ServoyProject activeProject = servoyModel.getActiveProject();

		if (activeProject != null)
		{
			EditorUtil.openWebPackageManager(activeProject.getSolution().getName());
		}
		else
		{
			new MessageDialog(Display.getDefault().getActiveShell(), "Web Package Manager", null,
				"Web Package Manager does not work when there is no active solution set.", MessageDialog.INFORMATION,
				new String[] { IDialogConstants.OK_LABEL }, 0).open();
		}
	}

}
