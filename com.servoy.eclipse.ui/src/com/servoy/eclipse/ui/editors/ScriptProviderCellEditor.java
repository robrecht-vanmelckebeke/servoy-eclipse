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
package com.servoy.eclipse.ui.editors;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.dialogs.LeafnodesSelectionFilter;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.editors.ScriptProviderCellEditor.ScriptDialog.ScriptDialogLabelProvider;
import com.servoy.eclipse.ui.editors.ScriptProviderCellEditor.ScriptDialog.ScriptProviderValueEditor;
import com.servoy.eclipse.ui.labelproviders.IPersistLabelProvider;
import com.servoy.eclipse.ui.labelproviders.MethodLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.property.MethodWithArguments;
import com.servoy.eclipse.ui.property.ScriptProviderPropertyController;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.util.IControlFactory;
import com.servoy.eclipse.ui.util.IKeywordChecker;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Table;

/**
 * A cell editor that manages a script field (calculation & global methods only)
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
public class ScriptProviderCellEditor extends DialogCellEditor
{
	private final Table table;
	private final IPersist persist;

	private final IPersist context;
	private final String methodKey;

	public ScriptProviderCellEditor(Composite parent, Table table, IPersist persist, IPersist context, String methodKey, boolean readOnly)
	{
		super(parent, new SolutionContextDelegateLabelProvider(new ScriptDialogLabelProvider(persist, context, table, true), persist),
			new ScriptProviderValueEditor(persist, table), readOnly, SWT.NONE);
		this.table = table;
		this.persist = persist;
		this.context = context;
		this.methodKey = methodKey;
	}

	@Override
	public Object openDialogBox(Control cellEditorWindow)
	{

		final ScriptDialog dialog = new ScriptDialog(cellEditorWindow.getShell(), persist, context, table, getSelection(), SWT.NONE, "Select script");

		dialog.setOptionsAreaFactory(new IControlFactory()
		{
			public Control createControl(Composite composite)
			{
				AddScriptProviderButtonsComposite buttons = new AddScriptProviderButtonsComposite(composite, methodKey, SWT.NONE);
				buttons.setTable(table);
				buttons.setPersist(persist);
				buttons.setDialog(dialog);
				return buttons;
			}
		});

		dialog.open();

		if (dialog.getReturnCode() != Window.CANCEL)
		{
			return ((IStructuredSelection)dialog.getSelection()).getFirstElement(); // single select
		}

		return null;
	}

	public static class ScriptDialog extends TreeSelectDialog
	{
		private static final String CALCULATIONS = "calculations";
		private static final String GLOBAL_METHODS = "global methods";

		ScriptDialog(Shell shell, IPersist persist, IPersist context, Table table, ISelection selection, int treeStyle, String title)
		{
			super(shell, true, false, TreePatternFilter.FILTER_LEAFS,
			// content provider
				new ScriptTreeContentProvider(table, persist),
				// label provider
				new SolutionContextDelegateLabelProvider(new ScriptDialogLabelProvider(persist, context, table, false), persist),
				// ViewerComparator
				null,
				// selection filter
				new LeafnodesSelectionFilter(new ScriptTreeContentProvider(table, persist)),
				// tree style
				treeStyle,
				// title
				title,
				// input
				new Object(),
				// selection
				selection, false, TreeSelectDialog.SCRIPT_DIALOG, null);
		}

		public void expandCalculationNode()
		{
			getTreeViewer().getViewer().expandToLevel(CALCULATIONS, 1);
		}

		public void expandGlobalsNode()
		{
			getTreeViewer().getViewer().expandToLevel(GLOBAL_METHODS, 1);
		}

		@Override
		public ISelection getSelection()
		{
			IStructuredSelection selection = (IStructuredSelection)super.getSelection();
			List<MethodWithArguments> lst = new ArrayList<MethodWithArguments>();
			for (Object o : selection.toArray())
			{
				if (o instanceof MethodWithArguments)
				{
					lst.add((MethodWithArguments)o);
				}
			}
			return new StructuredSelection(lst.toArray(new MethodWithArguments[lst.size()]));
		}

		public static class ScriptTreeContentProvider extends ArrayContentProvider implements ITreeContentProvider, IKeywordChecker
		{
			private final Table table;
			private final IPersist persist;

			public ScriptTreeContentProvider(Table table, IPersist persist)
			{
				this.table = table;
				this.persist = persist;
			}

			@Override
			public Object[] getElements(Object inputElement)
			{
				return new Object[] { ScriptProviderPropertyController.NONE, ScriptDialog.CALCULATIONS, ScriptDialog.GLOBAL_METHODS };
			}

			public Object[] getChildren(Object parentElement)
			{
				List<Object> children = new ArrayList<Object>();
				try
				{
					if (ScriptDialog.CALCULATIONS == parentElement && table != null)
					{
						Iterator<ScriptCalculation> calcs = ModelUtils.getEditingFlattenedSolution(persist).getScriptCalculations(table, true);
						while (calcs.hasNext())
						{
							children.add(new MethodWithArguments(calcs.next().getID()));
						}
					}

					if (ScriptDialog.GLOBAL_METHODS == parentElement)
					{
						Iterator<ScriptMethod> scriptMethodsIte = ModelUtils.getEditingFlattenedSolution(persist).getScriptMethods(true);
						while (scriptMethodsIte.hasNext())
						{
							children.add(new MethodWithArguments(scriptMethodsIte.next().getID()));
						}
					}
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}

				return children.toArray();
			}

			public Object getParent(Object value)
			{
				if (value instanceof MethodWithArguments)
				{
					IScriptProvider scriptProvider = ModelUtils.getScriptMethod(persist, null, table, ((MethodWithArguments)value).methodId);
					if (scriptProvider instanceof ScriptCalculation)
					{
						return ScriptDialog.CALCULATIONS;
					}
					if (scriptProvider instanceof ScriptMethod)
					{
						return ScriptDialog.GLOBAL_METHODS;
					}
				}
				return null;
			}

			public boolean hasChildren(Object element)
			{
				return ScriptDialog.CALCULATIONS == element || ScriptDialog.GLOBAL_METHODS == element;
			}

			public boolean isKeyword(Object element)
			{
				return ScriptDialog.CALCULATIONS == element || ScriptDialog.GLOBAL_METHODS == element;
			}
		}

		public static class ScriptProviderValueEditor implements IValueEditor<Object>
		{
			private final IPersist persist;
			private final Table table;

			public ScriptProviderValueEditor(IPersist persist, Table table)
			{
				this.persist = persist;
				this.table = table;
			}

			public boolean canEdit(Object value)
			{
				return value instanceof MethodWithArguments && ModelUtils.getScriptMethod(persist, null, table, ((MethodWithArguments)value).methodId) != null;
			}

			public void openEditor(Object value)
			{
				IScriptProvider scriptprovider = ModelUtils.getScriptMethod(persist, null, table, ((MethodWithArguments)value).methodId);
				if (scriptprovider instanceof IDataProvider) // it is a calculation
				{
					EditorUtil.openDataProviderEditor((IDataProvider)scriptprovider);
				}
				EditorUtil.openScriptEditor(scriptprovider, true);
			}
		}


		public static class ScriptDialogLabelProvider implements IPersistLabelProvider, IFontProvider
		{
			private static final Image globalMethodsImage = Activator.getDefault().loadImageFromBundle("global_method.gif"); //$NON-NLS-1$
			private final boolean showPrefix;
			private final IPersist persist;
			private final IPersist context;
			private final Table table;

			public ScriptDialogLabelProvider(IPersist persist, IPersist context, Table table, boolean showPrefix)
			{
				this.persist = persist;
				this.context = context;
				this.table = table;
				this.showPrefix = showPrefix;
			}

			public String getText(Object value)
			{
				if (value == null || ScriptProviderPropertyController.NONE == value) return Messages.LabelNone;
				if (value instanceof IScriptProvider)
				{
					String txt = ((IScriptProvider)value).getDisplayName();

					if (showPrefix && value instanceof ScriptMethod && ((ScriptMethod)value).getParent() instanceof IRootObject)
					{
						return ScriptVariable.GLOBAL_DOT_PREFIX + txt;
					}

					return txt;
				}
				if (value instanceof MethodWithArguments)
				{
					return MethodLabelProvider.getMethodText((MethodWithArguments)value, persist, context, table, showPrefix, false);
				}

				return value.toString();
			}

			public Font getFont(Object value)
			{
				if (value == null || ScriptProviderPropertyController.NONE == value)
				{
					return FontResource.getDefaultFont(SWT.BOLD, -1);
				}

				if (ScriptDialog.CALCULATIONS == value || ScriptDialog.GLOBAL_METHODS == value)
				{
					return FontResource.getDefaultFont(SWT.ITALIC, 1);
				}

				return FontResource.getDefaultFont(SWT.NONE, 0);
			}

			/**
			 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
			 */
			public Image getImage(Object element)
			{
				if (ScriptDialog.GLOBAL_METHODS == element) return globalMethodsImage;
				return null;
			}

			public void addListener(ILabelProviderListener listener)
			{
			}

			public void dispose()
			{
			}

			public boolean isLabelProperty(Object element, String property)
			{
				return false;
			}

			public void removeListener(ILabelProviderListener listener)
			{
			}

			public IPersist getPersist(Object value)
			{
				if (value instanceof MethodWithArguments)
				{
					return ModelUtils.getScriptMethod(persist, context, table, ((MethodWithArguments)value).methodId);
				}
				return null;
			}
		}
	}
}
