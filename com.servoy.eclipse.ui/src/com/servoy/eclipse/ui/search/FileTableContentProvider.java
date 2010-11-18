package com.servoy.eclipse.ui.search;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.search.ui.text.AbstractTextSearchResult;

/**
 * The table content provider for the file based search results.
 * 
 * @author jcompagner
 * @since 6.0
 */
public class FileTableContentProvider implements IStructuredContentProvider, IFileSearchContentProvider
{

	private final Object[] EMPTY_ARR = new Object[0];

	private final FileSearchPage fPage;
	private AbstractTextSearchResult fResult;

	public FileTableContentProvider(FileSearchPage page)
	{
		fPage = page;
	}

	public void dispose()
	{
		// nothing to do
	}

	public Object[] getElements(Object inputElement)
	{
		if (inputElement instanceof FileSearchResult)
		{
			int elementLimit = getElementLimit();
			Object[] elements = ((FileSearchResult)inputElement).getElements();
			if (elementLimit != -1 && elements.length > elementLimit)
			{
				Object[] shownElements = new Object[elementLimit];
				System.arraycopy(elements, 0, shownElements, 0, elementLimit);
				return shownElements;
			}
			return elements;
		}
		return EMPTY_ARR;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
	{
		if (newInput instanceof FileSearchResult)
		{
			fResult = (FileSearchResult)newInput;
		}
	}

	public void elementsChanged(Object[] updatedElements)
	{
		TableViewer viewer = getViewer();
		int elementLimit = getElementLimit();
		boolean tableLimited = elementLimit != -1;
		for (Object updatedElement : updatedElements)
		{
			if (fResult.getMatchCount(updatedElement) > 0)
			{
				if (viewer.testFindItem(updatedElement) != null) viewer.update(updatedElement, null);
				else
				{
					if (!tableLimited || viewer.getTable().getItemCount() < elementLimit) viewer.add(updatedElement);
				}
			}
			else viewer.remove(updatedElement);
		}
	}

	private int getElementLimit()
	{
		return fPage.getElementLimit().intValue();
	}

	private TableViewer getViewer()
	{
		return (TableViewer)fPage.getViewer();
	}

	public void clear()
	{
		getViewer().refresh();
	}
}
