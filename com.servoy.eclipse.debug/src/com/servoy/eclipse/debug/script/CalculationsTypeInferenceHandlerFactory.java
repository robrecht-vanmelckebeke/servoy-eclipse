/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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
package com.servoy.eclipse.debug.script;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.dltk.javascript.typeinfo.ITypeInferenceHandler;
import org.eclipse.dltk.javascript.typeinfo.ITypeInferenceHandlerFactory;
import org.eclipse.dltk.javascript.typeinfo.ITypeInferencerVisitor;
import org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext;

import com.servoy.eclipse.model.repository.SolutionSerializer;

public class CalculationsTypeInferenceHandlerFactory implements ITypeInferenceHandlerFactory
{
	public ITypeInferenceHandler create(ITypeInfoContext context, ITypeInferencerVisitor visitor)
	{
		IResource resource = context.getModelElement().getResource();
		if (resource != null)
		{
			IPath path = resource.getProjectRelativePath();
			if (path != null && path.segmentCount() == 3 && path.segment(0).equals(SolutionSerializer.DATASOURCES_DIR_NAME) &&
				path.segment(2).endsWith(SolutionSerializer.CALCULATIONS_POSTFIX_WITH_EXT))
			{
				return new CalculationsTypeInferenceHandler(visitor);
			}
		}
		return null;
	}

}
