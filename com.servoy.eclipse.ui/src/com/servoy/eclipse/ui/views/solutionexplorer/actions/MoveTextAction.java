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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.internal.ui.editor.ScriptEditor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorPart;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.core.repository.SolutionSerializer;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.views.solutionexplorer.ActiveEditorListener;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.ITagResolver;
import com.servoy.j2db.util.Text;

/**
 * Action for inserting code/sample code into editors. It is enabled/disabled based on the currently active editor (JS, NON JS, calculation JS), and the
 * selected node in the detail list.
 */
public class MoveTextAction extends Action implements ISelectionChangedListener, ActiveEditorListener
{
	private final SolutionExplorerView viewer;
	private final boolean moveSampleText;
	private boolean editorAvailable = false; // true if any a JS editor is the active editor
	private boolean moveTextSourceAvailable = false; // true if the node in the detail list is able to produce code in normal mode

	public MoveTextAction(SolutionExplorerView sev, boolean moveSampleText)
	{
		viewer = sev;
		this.moveSampleText = moveSampleText;

		if (moveSampleText)
		{
			setImageDescriptor(Activator.loadImageDescriptorFromBundle("copycont_r_co.gif")); //$NON-NLS-1$
			setText(Messages.MoveTextAction_moveSample);
			setToolTipText(Messages.MoveTextAction_moveSample);
		}
		else
		{
			setImageDescriptor(Activator.loadImageDescriptorFromBundle("goto_obj.gif")); //$NON-NLS-1$
			setText(Messages.MoveTextAction_moveCode);
			setToolTipText(Messages.MoveTextAction_moveCode);
		}
	}

	@Override
	public void run()
	{
		SimpleUserNode un = viewer.getSelectedListNode();
		if (un != null)
		{
			// find the code to move and it's source form
			String textToMove = null;
			if (moveSampleText)
			{
				textToMove = un.getSampleCode();
			}
			else
			{
				textToMove = un.getCode();
			}

			SimpleUserNode formNode = un.parent;
			while (formNode != null && !(formNode.getRealObject() instanceof Form))
			{
				formNode = formNode.parent;
			}

			Form form = null;
			if (formNode != null)
			{
				form = (Form)formNode.getRealObject();
			}

			if (textToMove != null)
			{
				insertText(viewer.getSite().getPage().getActiveEditor(), textToMove, form, moveSampleText);
			}
		}
	}

	/**
	 * Inserts the specified code at the caret position inside the open javaScript editor. It will also modify prefixes in the code based on the source form and
	 * the edited script's form.
	 * 
	 * @param ed the editor into which the text must be inserted.
	 * @param codeText the code to insert.
	 * @param sourceForm the source form for the code to insert (or null if no source form).
	 * @param replacePrefix if true, then the prefix will be changed based on source form and open javaScript editor's form.
	 */
	public static void insertText(IEditorPart ed, String codeText, Form sourceForm, boolean replacePrefix)
	{
		if (!(ed instanceof ScriptEditor)) return;

		ScriptEditor editor = (ScriptEditor)ed;
		ISourceViewer sv = editor.getScriptSourceViewer();
		if (sv == null) return;

		IDocument document = sv.getDocument();
		if (document == null) return;


		StyledText st = sv.getTextWidget();
		if (st == null || st.isDisposed()) return;

		Form currentMethodForm = getFormForEditor(ed);

		codeText = modifyCodeAccordingToUsedForms(currentMethodForm, sourceForm, codeText, replacePrefix);

		Point textSelection = st.getSelectionRange();
		int caretOffset = st.getCaretOffset();
		if (textSelection.y <= 0)
		{
			textSelection.x = caretOffset;
			textSelection.y = 0;
		}
		if (caretOffset > 0 && codeText.startsWith(".") && st.getText(caretOffset - 1, caretOffset - 1).equals(".")) //$NON-NLS-1$ //$NON-NLS-2$
		{
			codeText = codeText.substring(1);
		}
		st.replaceTextRange(textSelection.x, textSelection.y, codeText);
		st.setCaretOffset(textSelection.x + codeText.length());
		st.showSelection();
		st.forceFocus();
	}

	/**
	 * Gets the Form object that the current editor affects, if that is the case. Will return null if the current editor does not affect a Servoy Form.<BR>
	 * <BR>
	 * <B>Note:</B> Currently it only supports javaScript editors but it may be useful to add support for other editors as well (visual editors for example).
	 * 
	 * @param editor the editor to be used.
	 * @return the form that the given editor affects, or null if it does not affect any form.
	 */
	public static Form getFormForEditor(IEditorPart editor)
	{
		Form ret = null;

		if (editor instanceof ScriptEditor)
		{
			ScriptEditor scriptEditor = (ScriptEditor)editor;
			try
			{
				IProject project = scriptEditor.getInputModelElement().getUnderlyingResource().getProject();
				IPath projectRelativePath = scriptEditor.getInputModelElement().getUnderlyingResource().getProjectRelativePath();
				String[] projectRelativePathSegments = projectRelativePath.segments();
				ServoyProject[] servoyProjects = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProjects();
				ServoyProject servoyProject = null;

				// see if we can find the associated ServoyProject
				for (ServoyProject spIterator : servoyProjects)
				{
					if (spIterator.getProject() == project)
					{
						servoyProject = spIterator;
						break;
					}
				}

				if (projectRelativePathSegments.length > 1 && servoyProject != null)
				{
					if (SolutionSerializer.FORMS_DIR.equals(projectRelativePathSegments[0]))
					{
						// this means the form name in the second element in the array; try to get the form based
						// on the project and form name
						Solution solution = servoyProject.getSolution();

						if (solution != null)
						{
							String formName = projectRelativePathSegments[1];
							if (formName.endsWith(SolutionSerializer.JS_FILE_EXTENSION))
							{
								formName = formName.substring(0, formName.length() - SolutionSerializer.JS_FILE_EXTENSION.length());
							}
							ret = solution.getForm(formName);
						}
					}
				}
			}
			catch (ModelException e)
			{
				ServoyLog.logWarning("Cannot get the form object from the script editor...", e);
			}
		}
		return ret;
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		moveTextSourceAvailable = (sel.size() == 1);
		if (moveTextSourceAvailable)
		{
			String textToMove = null;
			SimpleUserNode un = (SimpleUserNode)sel.getFirstElement();
			if (un != null)
			{
				// find the code to move and it's source form
				if (moveSampleText)
				{
					textToMove = un.getSampleCode();
				}
				else
				{
					textToMove = un.getCode();
				}
			}
			moveTextSourceAvailable = (textToMove != null && textToMove.length() > 0);
		}
		enableAsNecessary();
	}

	public void activeEditorChanged(IEditorPart newActiveEditor)
	{
		editorAvailable = (newActiveEditor instanceof ScriptEditor);
		if (editorAvailable)
		{
			try
			{
				// see if it is a calculation JS file
				ScriptEditor scriptEditor = (ScriptEditor)newActiveEditor;
				IResource resource = null;
				if (scriptEditor.getInputModelElement() != null && scriptEditor.getInputModelElement().getResource() != null &&
					scriptEditor.getInputModelElement().getResource().exists())
				{
					resource = scriptEditor.getInputModelElement().getUnderlyingResource();
				}
				if (resource == null || !resource.getName().toLowerCase().endsWith(SolutionSerializer.JS_FILE_EXTENSION))
				{
					editorAvailable = false;
				}
			}
			catch (ModelException e)
			{
				editorAvailable = false;
				ServoyLog.logError("Editor exception while trying to determine if editor is for a .js file", e);
			}
		}
		enableAsNecessary();
	}

	private void enableAsNecessary()
	{
		setEnabled(moveTextSourceAvailable && editorAvailable);
	}

	public static String modifyCodeAccordingToUsedForms(Form currentMethodForm, Form appliedForm, String code, boolean replacePrefix)
	{
		if (!replacePrefix && code.indexOf("%%prefix%%") != -1) replacePrefix = true;
		if (currentMethodForm != null && appliedForm != null)
		{
			if (!currentMethodForm.equals(appliedForm))
			{
				if (appliedForm.getName().equals(code))
				{
					code = "forms." + appliedForm.getName(); //$NON-NLS-1$
				}
				else
				{
					if (replacePrefix)
					{
						SampleTagResolver resolver = new SampleTagResolver("forms." + appliedForm.getName() + "."); //$NON-NLS-1$ //$NON-NLS-2$
						code = Text.processTags(code, resolver);
					}
					else
					{
						code = "forms." + appliedForm.getName() + "." + code; //$NON-NLS-1$ //$NON-NLS-2$
					}
				}

			}
			else if (replacePrefix)
			{
				SampleTagResolver resolver = new SampleTagResolver(""); //$NON-NLS-1$
				code = Text.processTags(code, resolver);
			}
		}
		else if (appliedForm != null)
		{
			if (appliedForm.getName().equals(code))
			{
				code = "forms." + appliedForm.getName(); //$NON-NLS-1$
			}
			else
			{
				if (replacePrefix)
				{
					SampleTagResolver resolver = new SampleTagResolver("forms." + appliedForm.getName() + "."); //$NON-NLS-1$ //$NON-NLS-2$
					code = Text.processTags(code, resolver);
				}
				else
				{
					code = "forms." + appliedForm.getName() + "." + code; //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		else if (replacePrefix)
		{
			SampleTagResolver resolver = new SampleTagResolver(""); //$NON-NLS-1$
			code = Text.processTags(code, resolver);
		}
		return code;
	}

	static class SampleTagResolver implements ITagResolver
	{
		private final String prefix;

		SampleTagResolver(String prefix)
		{
			this.prefix = prefix;
		}

		/**
		 * @see com.servoy.j2db.util.Text.ITagResolver#getValue(java.lang.String)
		 */
		public String getStringValue(String tagname)
		{
			if (tagname.equals("prefix")) //$NON-NLS-1$
			{
				return prefix;
			}
			return null;
		}
	}

}