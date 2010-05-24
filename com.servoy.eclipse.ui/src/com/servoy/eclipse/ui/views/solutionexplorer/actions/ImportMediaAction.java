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
package com.servoy.eclipse.ui.views.solutionexplorer.actions;


import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.repository.EclipseRepository;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.Utils;

/**
 * Action to import media from file.
 * 
 * @author rob
 */
public class ImportMediaAction extends Action implements ISelectionChangedListener
{
	private final SolutionExplorerView viewer;
	private Solution solution;

	/**
	 * Creates a new "create new method" action for the given solution view.
	 * 
	 * @param viewer the solution view to use.
	 */
	public ImportMediaAction(SolutionExplorerView viewer)
	{
		this.viewer = viewer;

		setImageDescriptor(Activator.loadImageDescriptorFromOldLocations("import.gif"));
		setText("Import media");
		setToolTipText(getText());
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		solution = null;
		if (sel.size() == 1 && (((SimpleUserNode)sel.getFirstElement()).getType() == UserNodeType.MEDIA))
		{
			SimpleUserNode node = ((SimpleUserNode)sel.getFirstElement());
			SimpleUserNode solutionNode = node.getAncestorOfType(Solution.class);
			if (solutionNode != null)
			{
				// make sure you have the in-memory version of the solution
				solution = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(((Solution)solutionNode.getRealObject()).getName()).getEditingSolution();
			}
		}
		setEnabled(solution != null);
	}

	@Override
	public void run()
	{
		if (solution == null) return;

		FileDialog fd = new FileDialog(viewer.getSite().getShell(), SWT.OPEN | SWT.MULTI);
		fd.open();
		String[] fileNames = fd.getFileNames();
		String filterPath = fd.getFilterPath();
		if (fileNames == null || fileNames.length == 0)
		{
			return;
		}
		try
		{
			addFiles(solution, filterPath, fileNames);
		}
		catch (RepositoryException e)
		{
			MessageDialog.openError(viewer.getSite().getShell(), "Error", "Could not import media files: " + e.getMessage());
			ServoyLog.logError("Could not import media files", e);
		}
		catch (Exception e)
		{
			ServoyLog.logError("Could not import media files", e);
		}
	}

	protected static void addFiles(Solution solution, String directory, String[] fileNames) throws IOException, RepositoryException
	{
		List<IPersist> nodesToSave = new ArrayList<IPersist>(fileNames.length + 1);
		EclipseRepository repository = (EclipseRepository)solution.getRepository();
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		for (String fileName : fileNames)
		{
			File file = new File(directory, fileName);
			if (file.exists())
			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream((int)file.length());
				FileInputStream fis = new FileInputStream(file);
				BufferedInputStream bis = new BufferedInputStream(fis);
				Utils.streamCopy(bis, baos);
				byte[] media_data = baos.toByteArray();

				String mime = ImageLoader.getContentType(media_data);
				if (mime == null)
				{
					repository.getContentType(fileName);
				}
				String name = Utils.stringReplace(fileName, " ", "_");
				Media media = solution.getMedia(name);
				if (media == null)
				{
					media = solution.createNewMedia(servoyModel.getNameValidator(), name);
				}
				// Save the media in the repository.
				media.setMimeType(mime);
				media.setPermMediaData(media_data);
				media.flagChanged();
				repository.copyPersistIntoSolution(media, solution, true);
				nodesToSave.add(media);

			}
		}
		nodesToSave.add(solution);
		servoyModel.getServoyProject(solution.getName()).saveEditingSolutionNodes(nodesToSave.toArray(new IPersist[nodesToSave.size()]), false);
	}
}
