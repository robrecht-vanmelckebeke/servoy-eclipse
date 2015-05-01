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
package com.servoy.eclipse.designer.editor;


import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebComponentSpecification;

import com.servoy.eclipse.designer.editor.commands.RefreshingCommand;
import com.servoy.eclipse.designer.property.SetValueCommand;
import com.servoy.eclipse.dnd.FormElementDragData.PersistDragData;
import com.servoy.eclipse.dnd.FormElementTransfer;
import com.servoy.eclipse.dnd.IDragData;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.eclipse.ui.views.IndexedListViewer;
import com.servoy.eclipse.ui.views.IndexedStructuredSelection;
import com.servoy.j2db.persistence.IFlattenedPersistWrapper;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportDataProviderID;
import com.servoy.j2db.persistence.ISupportTabSeq;
import com.servoy.j2db.persistence.IWebComponent;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.TabSeqComparator;
import com.servoy.j2db.server.ngclient.property.types.NGTabSeqPropertyType;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.Utils;

/**
 * Tab in form editor for managing tab sequences.
 *
 * @author rgansevles
 */

public class VisualFormEditorTabSequencePage extends Composite
{
	public static final ILabelProvider LABELPROVIDER = new LabelProvider();
	public static final IContentProvider CONTENTPROVIDER = new ArrayContentProvider();

	private final BaseVisualFormEditor editor;
	private IndexedListViewer availableListViewer;
	private TableViewer selectedTableViewer;
	private Button upButton;
	private Button removeButton;
	private Button addButton;
	private Button downButton;
	private Button defaultButton;
	private boolean initialised = false;
	private boolean doRefresh;


	public VisualFormEditorTabSequencePage(BaseVisualFormEditor editor, Composite parent, int style)
	{
		super(parent, style);
		this.editor = editor;
		createContents();
		initialised = true;
		doRefresh();
	}

	protected void createContents()
	{
		// Scrolling container
		this.setLayout(new FillLayout());
		ScrolledComposite scrolledComposite = new ScrolledComposite(this, SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);

		Composite container = new Composite(scrolledComposite, SWT.NONE);
		scrolledComposite.setContent(container);


		removeButton = new Button(container, SWT.PUSH);
		removeButton.setText("<<");
		removeButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(final SelectionEvent e)
			{
				handleRemoveElements();
			}
		});

		addButton = new Button(container, SWT.PUSH);
		addButton.setText(">>");
		addButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(final SelectionEvent e)
			{
				handleAddElements();
			}
		});

		upButton = new Button(container, SWT.PUSH);
		upButton.setText("up");
		upButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(final SelectionEvent e)
			{
				handleMoveElements(-1);
			}
		});

		downButton = new Button(container, SWT.PUSH);
		downButton.setText("down");
		downButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(final SelectionEvent e)
			{
				handleMoveElements(1);
			}
		});

		defaultButton = new Button(container, SWT.PUSH);
		defaultButton.setText("Default");
		defaultButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(final SelectionEvent e)
			{
				executeCommand(getSetDefaultCommand());
			}
		});

		availableListViewer = new IndexedListViewer(container, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		availableListViewer.addOpenListener(new IOpenListener()
		{
			public void open(OpenEvent event)
			{
				handleAddElements();
			}
		});
		availableListViewer.addSelectionChangedListener(new ISelectionChangedListener()
		{
			public void selectionChanged(SelectionChangedEvent event)
			{
				configureButtons();
			}
		});
		availableListViewer.setLabelProvider(LABELPROVIDER);
		availableListViewer.setContentProvider(CONTENTPROVIDER);
		org.eclipse.swt.widgets.List availableList = availableListViewer.getList();

		selectedTableViewer = new TableViewer(container, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		selectedTableViewer.addOpenListener(new IOpenListener()
		{
			public void open(OpenEvent event)
			{
				handleRemoveElements();
			}
		});
		selectedTableViewer.addSelectionChangedListener(new ISelectionChangedListener()
		{
			public void selectionChanged(SelectionChangedEvent event)
			{
				configureButtons();
			}
		});
		selectedTableViewer.setLabelProvider(LABELPROVIDER);
		selectedTableViewer.setContentProvider(CONTENTPROVIDER);
		org.eclipse.swt.widgets.Table selectedTable = selectedTableViewer.getTable();

		Transfer[] types = new Transfer[] { FormElementTransfer.getInstance() };
		DragSource source = new DragSource(selectedTable, DND.DROP_MOVE | DND.DROP_COPY);
		source.setTransfer(types);

		source.addDragListener(new DragSourceAdapter()
		{
			@Override
			public void dragSetData(DragSourceEvent event)
			{
				// Get the selected items in the drag source
				DragSource ds = (DragSource)event.widget;
				Table table = (Table)ds.getControl();
				TableItem[] selection = table.getSelection();
				if (selection != null && selection.length > 0) event.data = new Object[] { Platform.getAdapterManager().getAdapter(
					((TabSeqProperty)selection[0].getData()).element, IDragData.class) };
			}
		});

		// Create the drop target
		DropTarget target = new DropTarget(selectedTable, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_DEFAULT);
		target.setTransfer(types);
		target.addDropListener(new DropTargetAdapter()
		{
			@Override
			public void dragEnter(DropTargetEvent event)
			{
				if (event.detail == DND.DROP_DEFAULT)
				{
					event.detail = (event.operations & DND.DROP_COPY) != 0 ? DND.DROP_COPY : DND.DROP_NONE;
				}

				for (TransferData dataType : event.dataTypes)
				{
					if (FormElementTransfer.getInstance().isSupportedType(dataType))
					{
						event.currentDataType = dataType;
					}
				}
			}

			@Override
			public void dragOver(DropTargetEvent event)
			{
				event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL;
			}

			@Override
			public void drop(DropTargetEvent event)
			{
				if (FormElementTransfer.getInstance().isSupportedType(event.currentDataType))
				{
					// Get the dropped data
					DropTarget dropTarget = (DropTarget)event.widget;
					Table table = (Table)dropTarget.getControl();
					Object data = event.data;
					if (data instanceof Object[] && ((Object[])data).length == 1)
					{
						Object dragSource = ((Object[])data)[0];
						if (dragSource instanceof PersistDragData && event.item instanceof TableItem)
						{
							int targetIndex = Arrays.asList(table.getItems()).indexOf(event.item);
							List<TabSeqProperty> input = (List<TabSeqProperty>)selectedTableViewer.getInput();
							int sourceIndex = -1;
							for (TabSeqProperty persistProperty : input)
							{
								if (persistProperty.element.getUUID().equals(((PersistDragData)dragSource).uuid))
								{
									sourceIndex = input.indexOf(persistProperty);
									break;
								}
							}
							if (sourceIndex >= 0 && targetIndex >= 0 && sourceIndex != targetIndex)
							{
								handleMoveElements(targetIndex - sourceIndex);
							}
						}
					}
					table.redraw();
				}
			}
		});
		Label availableElementsLabel;
		availableElementsLabel = new Label(container, SWT.NONE);
		availableElementsLabel.setText("Available elements");

		Label selectedElementsLabel;
		selectedElementsLabel = new Label(container, SWT.NONE);
		selectedElementsLabel.setText("Selected elements");
		final GroupLayout groupLayout = new GroupLayout(container);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(
					groupLayout.createSequentialGroup().add(
						groupLayout.createParallelGroup(GroupLayout.LEADING).add(
							groupLayout.createSequentialGroup().add(availableList, GroupLayout.PREFERRED_SIZE, 175, Short.MAX_VALUE).addPreferredGap(
								LayoutStyle.RELATED).add(
								groupLayout.createParallelGroup(GroupLayout.LEADING).add(addButton, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE).add(
									removeButton, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE))).add(availableElementsLabel)).addPreferredGap(
						LayoutStyle.RELATED).add(
						groupLayout.createParallelGroup(GroupLayout.LEADING).add(selectedTable, GroupLayout.PREFERRED_SIZE, 175, Short.MAX_VALUE).add(
							selectedElementsLabel, GroupLayout.PREFERRED_SIZE, 134, GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
						groupLayout.createParallelGroup(GroupLayout.TRAILING).add(upButton, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE).add(
							downButton, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE))).add(defaultButton, GroupLayout.PREFERRED_SIZE, 81,
					GroupLayout.PREFERRED_SIZE)).addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.TRAILING).add(availableElementsLabel).add(selectedElementsLabel)).addPreferredGap(
				LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(
					groupLayout.createSequentialGroup().add(upButton).addPreferredGap(LayoutStyle.RELATED).add(downButton)).add(
					groupLayout.createParallelGroup(GroupLayout.LEADING).add(selectedTable, GroupLayout.PREFERRED_SIZE, 188, Short.MAX_VALUE).add(
						groupLayout.createSequentialGroup().addPreferredGap(LayoutStyle.RELATED).add(
							groupLayout.createParallelGroup(GroupLayout.LEADING).add(
								groupLayout.createSequentialGroup().add(removeButton).addPreferredGap(LayoutStyle.RELATED).add(addButton)).add(availableList,
								GroupLayout.PREFERRED_SIZE, 188, Short.MAX_VALUE))))).add(10, 10, 10).add(defaultButton).addContainerGap()));
		container.setLayout(groupLayout);

		scrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}


	public void refresh()
	{
		if (!initialised || isDisposed() || editor.getForm() == null) return;

		if (isVisible())
		{
			doRefresh();
		}
		else
		{
			doRefresh = true;
		}
	}

	public void doRefresh()
	{
		// preserve selection when possible
		ISelection availableSelection = availableListViewer.getSelection();
		ISelection selectedSelection = selectedTableViewer.getSelection();

		SortedList<TabSeqProperty> available = new SortedList<TabSeqProperty>(new Comparator<TabSeqProperty>()
		{
			public int compare(TabSeqProperty o1, TabSeqProperty o2)
			{
				String name1 = "";
				String name2 = "";
				IFormElement el1 = o1.element;
				IFormElement el2 = o2.element;
				if (el1.getName() != null)
				{
					name1 += el1.getName();
				}
				if (el2.getName() != null)
				{
					name2 += el2.getName();
				}
				if (el1 instanceof ISupportDataProviderID)
				{
					name1 += ((ISupportDataProviderID)el1).getDataProviderID();
				}
				if (el2 instanceof ISupportDataProviderID)
				{
					name2 += ((ISupportDataProviderID)el2).getDataProviderID();
				}
				return name1.compareTo(name2);
			}
		});
		SortedList<TabSeqProperty> selected = new SortedList<TabSeqProperty>(new Comparator<TabSeqProperty>()
		{
			public int compare(TabSeqProperty o1, TabSeqProperty o2)
			{
				return TabSeqComparator.compareTabSeq(o1.getSeqValue(), o1.element, o2.getSeqValue(), o2.element);
			}
		});
		Iterator<IPersist> iterator = ModelUtils.getEditingFlattenedSolution(editor.getForm()).getFlattenedForm(editor.getForm()).getAllObjects();
		for (int i = 0; iterator.hasNext(); i++)
		{
			IPersist persist = iterator.next();
			if (FormTemplateGenerator.isWebcomponentBean(persist))
			{
				String componentType = FormTemplateGenerator.getComponentTypeName((IWebComponent)persist);
				WebComponentSpecification specification = WebComponentSpecProvider.getInstance().getWebComponentSpecification(componentType);
				if (specification != null)
				{
					Collection<PropertyDescription> properties = specification.getProperties(NGTabSeqPropertyType.NG_INSTANCE);
					if (properties != null && properties.size() > 0)
					{
						JSONObject json = ((IWebComponent)persist).getJson();
						for (PropertyDescription pd : properties)
						{
							int tabseq = json != null ? json.optInt(pd.getName()) : 0;
							if (tabseq >= 0)
							{
								selected.add(new TabSeqProperty((IFormElement)persist, pd.getName()));
							}
							else
							{
								available.add(new TabSeqProperty((IFormElement)persist, pd.getName()));
							}
						}
					}
				}
			}
			else if (persist instanceof ISupportTabSeq)
			{
				if (((ISupportTabSeq)persist).getTabSeq() >= 0)
				{
					selected.add(new TabSeqProperty((IFormElement)persist, null));
				}
				else
				{
					available.add(new TabSeqProperty((IFormElement)persist, null));
				}
			}

		}
		availableListViewer.setInput(available);
		selectedTableViewer.setInput(selected);

		availableListViewer.setSelection(availableSelection);
		selectedTableViewer.setSelection(selectedSelection);

		configureButtons();

		doRefresh = false;
	}

	@Override
	public void setVisible(boolean visible)
	{
		super.setVisible(visible);
		if (visible && doRefresh)
		{
			doRefresh();
		}
	}

	protected void configureButtons()
	{
		if (!initialised) return;

		StructuredSelection selection = (StructuredSelection)selectedTableViewer.getSelection();
		boolean enableUp = false;
		boolean enableDown = false;
		if (selection.size() == 1)
		{
			int selectedIndex = ((List)selectedTableViewer.getInput()).indexOf(selection.getFirstElement());
			int nSelected = ((List)selectedTableViewer.getInput()).size();
			enableUp = selectedIndex > 0;
			enableDown = selectedIndex < nSelected - 1;
		}
		upButton.setEnabled(enableUp);
		downButton.setEnabled(enableDown);

		removeButton.setEnabled(selection.size() > 0);
		addButton.setEnabled(((IStructuredSelection)availableListViewer.getSelection()).size() > 0);
		defaultButton.setEnabled(getSetDefaultCommand() != null);
	}

	/**
	 * Create a command for saving the tab sequences, return null if there is nothing to save.
	 *
	 * @param tabSeqs
	 * @param label
	 * @return
	 */
	protected Command getSaveCommand(int[] tabSeqs, String label)
	{
		CompoundCommand command = null;
		List<TabSeqProperty> available = (List<TabSeqProperty>)availableListViewer.getInput();
		List<TabSeqProperty> selected = (List<TabSeqProperty>)selectedTableViewer.getInput();
		int nAvailable = available.size();
		int nSelected = selected.size();

		for (int i = 0; i < tabSeqs.length; i++)
		{
			TabSeqProperty ts = null;
			if (i < nAvailable)
			{
				ts = available.get(i);
			}
			else if ((i - nAvailable) < nSelected)
			{
				ts = selected.get(i - nAvailable);
			}

			if (ts.getSeqValue() != tabSeqs[i])
			{
				if (command == null)
				{
					command = new CompoundCommand(label);
				}
				command.add(getSetTabSeqCommand(ts, tabSeqs[i]));
			}
		}

		return command;
	}

	protected Command getSetTabSeqCommand(TabSeqProperty tabSeqProperty, int tabSeq)
	{
		IPersist persist = tabSeqProperty.element;
		if (persist instanceof IFlattenedPersistWrapper) persist = ((IFlattenedPersistWrapper< ? >)persist).getWrappedPersist();
		return SetValueCommand.createSetvalueCommand("", PersistPropertySource.createPersistPropertySource(persist, editor.getForm(), false),
			tabSeqProperty.propertyName != null ? tabSeqProperty.propertyName : StaticContentSpecLoader.PROPERTY_TABSEQ.getPropertyName(), new Integer(tabSeq));
	}

	/**
	 * Execute a command on the command stack and refresh the views.
	 *
	 * @param command
	 */
	protected void executeCommand(Command command)
	{
		if (command == null)
		{
			return;
		}
		editor.getCommandStack().execute(command);
	}

	protected void handleAddElements()
	{
		final IndexedStructuredSelection selection = ((IndexedStructuredSelection)availableListViewer.getSelection());

		int[] tabSeqs = getBaseTabIndexes();
		int max = 0;
		for (int element : tabSeqs)
			if (element > max) max = element;
		for (int index : selection.getSelectedIndices())
			tabSeqs[index] = ++max;

		executeCommand(new RefreshingCommand(getSaveCommand(tabSeqs, "add to tab sequence"))
		{
			@Override
			public void refresh(boolean haveExecuted)
			{
				if (haveExecuted)
				{
					selectedTableViewer.setSelection(selection);
				}
				else
				{
					// undo
					availableListViewer.setSelection(selection);
				}
			}
		});
	}

	protected void handleRemoveElements()
	{
		final StructuredSelection selection = ((StructuredSelection)selectedTableViewer.getSelection());
		int nAvailable = ((List)availableListViewer.getInput()).size();

		int[] tabSeqs = getBaseTabIndexes();
		Iterator it = selection.iterator();
		List input = (List)selectedTableViewer.getInput();
		while (it.hasNext())
		{
			int index = input.indexOf(it.next());
			tabSeqs[nAvailable + index] = ISupportTabSeq.SKIP;
		}
		int count = 1;
		for (int i = 0; i < tabSeqs.length; i++)
			if (tabSeqs[i] >= 0) tabSeqs[i] = count++;

		executeCommand(new RefreshingCommand(getSaveCommand(tabSeqs, "remove from tab sequence"))
		{
			@Override
			public void refresh(boolean haveExecuted)
			{
				if (haveExecuted)
				{
					availableListViewer.setSelection(selection);
				}
				else
				{
					// undo
					selectedTableViewer.setSelection(selection);
				}
			}
		});
	}

	protected void handleMoveElements(int moveIndex)
	{
		final StructuredSelection selection = ((StructuredSelection)selectedTableViewer.getSelection());
		if (selection.size() == 1)
		{
			int nAvailable = ((List)availableListViewer.getInput()).size();
			int nSelected = ((List)selectedTableViewer.getInput()).size();

			int selectedIndex = ((List)selectedTableViewer.getInput()).indexOf(selection.getFirstElement());
			int[] tabSeqs = getBaseTabIndexes();

			// flip the selected with its sibling
			if (moveIndex < 0 && (selectedIndex + moveIndex) >= 0)
			{
				for (int j = moveIndex; j < 0; j++)
				{
					tabSeqs[nAvailable + selectedIndex + j] = selectedIndex + j + 2;
				}
				tabSeqs[nAvailable + selectedIndex] = selectedIndex + moveIndex + 1;
			}
			else if (moveIndex > 0 && (selectedIndex + moveIndex <= nSelected - 1))
			{
				for (int j = 1; j <= moveIndex; j++)
				{
					tabSeqs[nAvailable + selectedIndex + j] = selectedIndex + j;
				}
				tabSeqs[nAvailable + selectedIndex] = selectedIndex + moveIndex + 1;
			}

			executeCommand(new RefreshingCommand(getSaveCommand(tabSeqs, "change tab index"))
			{
				@Override
				public void refresh(boolean haveExecuted)
				{
					selectedTableViewer.setSelection(selection);
				}
			});
		}
	}

	private int[] getBaseTabIndexes()
	{
		int nAvailable = ((List)availableListViewer.getInput()).size();
		int nSelected = ((List)selectedTableViewer.getInput()).size();

		int[] tabSeqs = new int[nAvailable + nSelected];

		// first the non-selected
		for (int i = 0; i < nAvailable; i++)
		{
			tabSeqs[i] = ISupportTabSeq.SKIP;
		}

		// then the selected
		for (int i = 0; i < nSelected; i++)
		{
			tabSeqs[nAvailable + i] = i + 1;
		}

		return tabSeqs;
	}

	protected Command getSetDefaultCommand()
	{
		int nFields = ((List)availableListViewer.getInput()).size() + ((List)selectedTableViewer.getInput()).size();
		int[] tabSeqs = new int[nFields];
		for (int i = 0; i < nFields; i++)
		{
			tabSeqs[i] = ISupportTabSeq.DEFAULT;
		}
		return getSaveCommand(tabSeqs, "set default tab sequence");
	}

	private class TabSeqProperty
	{
		IFormElement element;
		String propertyName;

		public TabSeqProperty(IFormElement element, String propertyName)
		{
			this.element = element;
			this.propertyName = propertyName;
		}

		public int getSeqValue()
		{
			if (propertyName != null && element instanceof IWebComponent)
			{
				String componentType = FormTemplateGenerator.getComponentTypeName(element);
				WebComponentSpecification specification = WebComponentSpecProvider.getInstance().getWebComponentSpecification(componentType);
				if (specification != null)
				{
					PropertyDescription property = specification.getProperty(propertyName);
					if (property != null)
					{
						JSONObject json = ((IWebComponent)element).getJson();
						if (json != null)
						{
							return json.optInt(propertyName);
						}
						return 0;
					}
				}
			}
			else if (element instanceof ISupportTabSeq)
			{
				return ((ISupportTabSeq)element).getTabSeq();
			}
			return -1;
		}

		@Override
		public String toString()
		{
			return element.toString() + (propertyName != null ? " [" + propertyName + "]" : "");
		}

		@Override
		public boolean equals(Object obj)
		{
			if (!(obj instanceof TabSeqProperty)) return false;
			return Utils.equalObjects(element, ((TabSeqProperty)obj).element) && Utils.equalObjects(propertyName, ((TabSeqProperty)obj).propertyName);
		}
	}

}
