/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebServiceSpecProvider;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyNGPackageProject;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ResourcesUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.FlatTreeContentProvider;
import com.servoy.eclipse.ui.dialogs.LeafnodesSelectionFilter;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.PlatformSimpleUserNode;
import com.servoy.j2db.persistence.Solution;

/**
 * @author gganea@servoy.com
 *
 */
public class AddPackageProjectAction extends Action implements ISelectionChangedListener
{

	private PlatformSimpleUserNode selection;
	private final Shell shell;

	public AddPackageProjectAction(Shell shell)
	{
		this.shell = shell;
		setText("Add Servoy package project reference");
		setToolTipText("Add a Servoy Package Project reference to the solution");
	}

	@Override
	public void run()
	{
		ILabelProvider labelProvider = new LabelProvider()
		{
			@Override
			public String getText(Object element)
			{
				if (element instanceof IProject) return ((IProject)element).getName();
				return super.getText(element);
			}
		};
		Object realObject = selection.getRealObject();

		if (realObject instanceof Solution)
		{
			try
			{
				List<IProject> selectedProjectsList = new ArrayList<IProject>();
				IProject solutionProject = ServoyModel.getWorkspace().getRoot().getProject(((Solution)realObject).getName());
				ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(solutionProject.getName());

				ArrayList<IProject> selectablePackages = new ArrayList<IProject>();
				IProject[] allProjects = ServoyModel.getWorkspace().getRoot().getProjects();

				List<IPackageReader> allReaders = new ArrayList<IPackageReader>();
				allReaders.addAll(Arrays.asList(WebComponentSpecProvider.getSpecProviderState().getAllPackageReaders()));
				allReaders.addAll(Arrays.asList(WebServiceSpecProvider.getSpecProviderState().getAllPackageReaders()));

				for (IProject iProject : allProjects)
				{
					if (iProject.isAccessible() && iProject.hasNature(ServoyNGPackageProject.NATURE_ID) &&
						!isZipPackgeWithSameName(servoyProject.getSolutionAndModuleReferencedProjects(), iProject.getName(), allReaders))
					{
						selectablePackages.add(iProject);
					}
				}

				IProject[] referencedProjects = solutionProject.getReferencedProjects();
				for (IProject iProject : referencedProjects)
				{
					if (iProject.isAccessible() && iProject.hasNature(ServoyNGPackageProject.NATURE_ID))
					{
						selectedProjectsList.add(iProject);
					}
				}
				StructuredSelection theSelection = new StructuredSelection(selectedProjectsList);
				ITreeContentProvider contentProvider = FlatTreeContentProvider.INSTANCE;
				IFilter selectionFilter = new LeafnodesSelectionFilter(contentProvider);

				int treeStyle = SWT.MULTI | SWT.CHECK;

				TreeSelectDialog dialog = new TreeSelectDialog(shell, false, false, TreePatternFilter.FILTER_LEAFS, contentProvider, labelProvider, null,
					selectionFilter, treeStyle, "Select Servoy Packages", selectablePackages.toArray(), theSelection, true, "Select Servoy Packages", null);

				dialog.open();

				if (dialog.getReturnCode() == Window.CANCEL) return;

				ArrayList<IProject> list = new ArrayList<IProject>();
				Iterator iterator = ((IStructuredSelection)dialog.getSelection()).iterator();
				IProjectDescription solutionProjectDescription = solutionProject.getDescription();
				while (iterator.hasNext())
				{
					Object next = iterator.next();
					AddAsWebPackageAction.addReferencedProjectToDescription((IProject)next, solutionProjectDescription);
				}
				solutionProject.setDescription(solutionProjectDescription, new NullProgressMonitor());
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		}


	}


	private boolean isZipPackgeWithSameName(List<IProject> list, String name, List<IPackageReader> packages)
	{
		IWorkspaceRoot root = ServoyModel.getWorkspace().getRoot();
		for (IPackageReader reader : packages)
		{
			File resource = reader.getResource();

			if (resource != null && resource.isFile())
			{
				IFile file = ResourcesUtils.findFileWithShortestPathForLocationURI(resource.toURI());
				if (file != null && list.contains(file.getProject()))
				{
					String packName = resource.getName().toLowerCase().split("\\.")[0];
					if (packName.equals(name.toLowerCase())) return true;
				}
			}
		}
		return false;
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		selection = null;
		if (event.getSelection() instanceof IStructuredSelection)
		{
			if (((IStructuredSelection)event.getSelection()).size() == 1)
			{
				Object firstElement = ((IStructuredSelection)event.getSelection()).getFirstElement();
				if (firstElement instanceof PlatformSimpleUserNode)
				{
					if (((PlatformSimpleUserNode)firstElement).getType() == UserNodeType.SOLUTION_CONTAINED_AND_REFERENCED_WEB_PACKAGES)
					{
						selection = (PlatformSimpleUserNode)firstElement;
					}
				}
			}
		}
	}

	@Override
	public boolean isEnabled()
	{
		return selection != null;
	}

}
