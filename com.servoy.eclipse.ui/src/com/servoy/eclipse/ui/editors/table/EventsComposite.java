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
package com.servoy.eclipse.ui.editors.table;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.MethodDialog;
import com.servoy.eclipse.ui.editors.TableEditor;
import com.servoy.eclipse.ui.property.MethodWithArguments;
import com.servoy.eclipse.ui.property.StringTokenizerConverter;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.util.UUID;

public class EventsComposite extends Composite
{
	private final TreeViewer treeViewer;
	private final Composite treeContainer;

	/**
	 * Create the composite
	 * 
	 * @param parent
	 * @param style
	 */
	public EventsComposite(final TableEditor te, Composite parent, int style)
	{
		super(parent, style);

		this.setLayout(new FillLayout());
		ScrolledComposite myScrolledComposite = new ScrolledComposite(this, SWT.H_SCROLL | SWT.V_SCROLL);
		myScrolledComposite.setExpandHorizontal(true);
		myScrolledComposite.setExpandVertical(true);

		Composite container = new Composite(myScrolledComposite, SWT.NONE);

		myScrolledComposite.setContent(container);

		final Table t = te.getTable();
		treeContainer = new Composite(container, SWT.NONE);

		treeViewer = new TreeViewer(treeContainer, SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);

		Tree tree = treeViewer.getTree();
		tree.setLinesVisible(true);
		tree.setHeaderVisible(true);

		final GroupLayout groupLayout = new GroupLayout(container);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			GroupLayout.TRAILING,
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.TRAILING).add(GroupLayout.LEADING, treeContainer, GroupLayout.PREFERRED_SIZE, 482, Short.MAX_VALUE))));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(GroupLayout.TRAILING,
			groupLayout.createSequentialGroup().addContainerGap().add(treeContainer, GroupLayout.PREFERRED_SIZE, 323, Short.MAX_VALUE)));
		container.setLayout(groupLayout);
		//
		initDataBindings(t, te);
		myScrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	@Override
	protected void checkSubclass()
	{
		// Disable the check that prevents subclassing of SWT components
	}

	public static final int CI_NAME = 0;
	public static final int CI_METHOD = 1;

	protected void initDataBindings(Table t, final TableEditor te)
	{
		TreeColumn nameColumn = new TreeColumn(treeViewer.getTree(), SWT.LEFT, CI_NAME);
		nameColumn.setText("Name");

		TreeColumn methodColumn = new TreeColumn(treeViewer.getTree(), SWT.LEFT, CI_METHOD);
		methodColumn.setText("Method");
		TreeViewerColumn methodViewerColumn = new TreeViewerColumn(treeViewer, methodColumn);
		EventsMethodEditingSupport methodEditing = new EventsMethodEditingSupport(treeViewer, t);
		methodViewerColumn.setEditingSupport(methodEditing);
		methodEditing.addChangeListener(new IChangeListener()
		{
			public void handleChange(ChangeEvent event)
			{
				te.flagModified();
			}
		});

		TreeColumnLayout layout = new TreeColumnLayout();
		treeContainer.setLayout(layout);
		layout.setColumnData(nameColumn, new ColumnWeightData(10, 50, true));
		layout.setColumnData(methodColumn, new ColumnWeightData(10, 50, true));

		treeViewer.setLabelProvider(new EventsLabelProvider());

		treeViewer.setContentProvider(EventsContentProvider.INSTANCE);
		List<EventNode> rows = new ArrayList<EventNode>();
		try
		{
			ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			ServoyProject servoyProject = servoyModel.getActiveProject();
			if (servoyProject != null)
			{
				Solution solution = (Solution)servoyProject.getEditingPersist(servoyProject.getSolution().getUUID());
				Set<UUID> solutions = new HashSet<UUID>();
				rows.add(new EventNode(solution, t));
				solutions.add(solution.getUUID());
				for (int i = 0; i < rows.size(); i++)
				{
					Solution sol = rows.get(i).getSolution();
					String[] modulesNames = new StringTokenizerConverter(",", true).convertProperty("modulesNames", sol.getModulesNames());
					for (String modulename : modulesNames)
					{
						ServoyProject moduleProject = servoyModel.getServoyProject(modulename);
						if (moduleProject != null && solutions.add(moduleProject.getSolution().getUUID()))
						{
							rows.add(new EventNode((Solution)moduleProject.getEditingPersist(moduleProject.getSolution().getUUID()), t));
						}
					}
				}
			}
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}

		treeViewer.setInput(rows);
		List<EventNode> expandedRows = new ArrayList<EventNode>();
		for (EventNode node : rows)
		{
			for (EventNode methodNode : node.getChildren())
			{
				if (methodNode.getMethodWithArguments() != null && methodNode.getMethodWithArguments().methodId > 0)
				{
					expandedRows.add(node);
					break;
				}
			}
		}
		treeViewer.setExpandedElements(expandedRows.toArray());
	}

	public static class EventNode
	{
		public static enum EventNodeType
		{
			insertMethod, updateMethod, deleteMethod, afterInsertMethod, afterUpdateMethod, afterDeleteMethod;
			@Override
			public String toString()
			{
				switch (this)
				{
					case insertMethod :
						return "onRecordInsert";
					case updateMethod :
						return "onRecordUpdate";
					case deleteMethod :
						return "onRecordDelete";
					case afterInsertMethod :
						return "afterRecordInsert";
					case afterUpdateMethod :
						return "afterRecordUpdate";
					case afterDeleteMethod :
						return "afterRecordDelete";
				}
				return super.toString();
			}

			protected String getProperty()
			{
				switch (this)
				{
					case insertMethod :
						return "onInsertMethodID";
					case updateMethod :
						return "onUpdateMethodID";
					case deleteMethod :
						return "onDeleteMethodID";
					case afterInsertMethod :
						return "onAfterInsertMethodID";
					case afterUpdateMethod :
						return "onAfterUpdateMethodID";
					case afterDeleteMethod :
						return "onAfterDeleteMethodID";
				}
				return null;
			}
		}

		private final Solution solution;
		private final boolean isSolution;
		private List<EventNode> children;
		private EventNodeType type;
		private MethodWithArguments mwa;

		public EventNode(EventNodeType type, MethodWithArguments mwa, Solution solution)
		{
			this.type = type;
			this.mwa = mwa;
			this.solution = solution;
			isSolution = false;
		}

		public EventNode(Solution solution, Table t)
		{
			this.solution = solution;
			isSolution = true;
			children = new ArrayList<EventNode>();
			TableNode tableNode = null;
			try
			{
				Iterator<TableNode> it = solution.getTableNodes(t);
				if (it.hasNext())
				{
					tableNode = it.next();
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
			children.add(new EventNode(EventNodeType.insertMethod, tableNode == null ? MethodDialog.METHOD_DEFAULT : new MethodWithArguments(
				tableNode.getOnInsertMethodID()), solution));
			children.add(new EventNode(EventNodeType.updateMethod, tableNode == null ? MethodDialog.METHOD_DEFAULT : new MethodWithArguments(
				tableNode.getOnUpdateMethodID()), solution));
			children.add(new EventNode(EventNodeType.deleteMethod, tableNode == null ? MethodDialog.METHOD_DEFAULT : new MethodWithArguments(
				tableNode.getOnDeleteMethodID()), solution));
			children.add(new EventNode(EventNodeType.afterInsertMethod, tableNode == null ? MethodDialog.METHOD_DEFAULT : new MethodWithArguments(
				tableNode.getOnAfterInsertMethodID()), solution));
			children.add(new EventNode(EventNodeType.afterUpdateMethod, tableNode == null ? MethodDialog.METHOD_DEFAULT : new MethodWithArguments(
				tableNode.getOnAfterUpdateMethodID()), solution));
			children.add(new EventNode(EventNodeType.afterDeleteMethod, tableNode == null ? MethodDialog.METHOD_DEFAULT : new MethodWithArguments(
				tableNode.getOnAfterDeleteMethodID()), solution));
		}

		public MethodWithArguments getMethodWithArguments()
		{
			return mwa;
		}

		public void setMethodWithArguments(MethodWithArguments mwa)
		{
			this.mwa = mwa;
		}

		public String getName()
		{
			return type.toString();
		}

		public Solution getSolution()
		{
			return solution;
		}

		public EventNodeType getType()
		{
			return type;
		}

		public boolean isSolution()
		{
			return isSolution;
		}

		public List<EventNode> getChildren()
		{
			return children;
		}
	}
}
