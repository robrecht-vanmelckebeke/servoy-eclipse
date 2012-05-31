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
package com.servoy.eclipse.debug.script;


import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.swing.Icon;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.javascript.scriptdoc.JavaDoc2HTMLTextReader;
import org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext;
import org.eclipse.dltk.javascript.typeinfo.ITypeNames;
import org.eclipse.dltk.javascript.typeinfo.TypeCache;
import org.eclipse.dltk.javascript.typeinfo.TypeUtil;
import org.eclipse.dltk.javascript.typeinfo.model.JSType;
import org.eclipse.dltk.javascript.typeinfo.model.Member;
import org.eclipse.dltk.javascript.typeinfo.model.Method;
import org.eclipse.dltk.javascript.typeinfo.model.Parameter;
import org.eclipse.dltk.javascript.typeinfo.model.ParameterKind;
import org.eclipse.dltk.javascript.typeinfo.model.Property;
import org.eclipse.dltk.javascript.typeinfo.model.SimpleType;
import org.eclipse.dltk.javascript.typeinfo.model.Type;
import org.eclipse.dltk.javascript.typeinfo.model.TypeInfoModelFactory;
import org.eclipse.dltk.javascript.typeinfo.model.TypeKind;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.mozilla.javascript.JavaMembers;
import org.mozilla.javascript.JavaMembers.BeanProperty;
import org.mozilla.javascript.MemberBox;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.Scriptable;

import com.servoy.eclipse.core.DesignApplication;
import com.servoy.eclipse.core.IPersistChangeListener;
import com.servoy.eclipse.core.JSDeveloperSolutionModel;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.debug.Activator;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.eclipse.ui.util.IconProvider;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerListContentProvider;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormController.JSForm;
import com.servoy.j2db.FormManager.HistoryProvider;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IServoyBeanFactory;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.DataException;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.dataprocessing.JSDatabaseManager;
import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.documentation.IParameter;
import com.servoy.j2db.documentation.ScriptParameter;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.FormEncapsulation;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.LiteralDataprovider;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RelationItem;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.plugins.IBeanClassProvider;
import com.servoy.j2db.plugins.IClientPlugin;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.plugins.IPluginManager;
import com.servoy.j2db.querybuilder.impl.QBAggregate;
import com.servoy.j2db.querybuilder.impl.QBColumn;
import com.servoy.j2db.querybuilder.impl.QBColumns;
import com.servoy.j2db.querybuilder.impl.QBCondition;
import com.servoy.j2db.querybuilder.impl.QBFactory;
import com.servoy.j2db.querybuilder.impl.QBFunction;
import com.servoy.j2db.querybuilder.impl.QBFunctions;
import com.servoy.j2db.querybuilder.impl.QBGroupBy;
import com.servoy.j2db.querybuilder.impl.QBJoin;
import com.servoy.j2db.querybuilder.impl.QBJoins;
import com.servoy.j2db.querybuilder.impl.QBLogicalCondition;
import com.servoy.j2db.querybuilder.impl.QBParameter;
import com.servoy.j2db.querybuilder.impl.QBParameters;
import com.servoy.j2db.querybuilder.impl.QBPart;
import com.servoy.j2db.querybuilder.impl.QBResult;
import com.servoy.j2db.querybuilder.impl.QBSelect;
import com.servoy.j2db.querybuilder.impl.QBSort;
import com.servoy.j2db.querybuilder.impl.QBSorts;
import com.servoy.j2db.querybuilder.impl.QBTableClause;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.scripting.IDeprecated;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.IScriptObject;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.scripting.ITypedScriptObject;
import com.servoy.j2db.scripting.InstanceJavaMembers;
import com.servoy.j2db.scripting.JSApplication;
import com.servoy.j2db.scripting.JSI18N;
import com.servoy.j2db.scripting.JSSecurity;
import com.servoy.j2db.scripting.JSUnitAssertFunctions;
import com.servoy.j2db.scripting.JSUtils;
import com.servoy.j2db.scripting.RuntimeGroup;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.scripting.solutionmodel.JSSolutionModel;
import com.servoy.j2db.ui.IScriptAccordionPanelMethods;
import com.servoy.j2db.ui.IScriptDataLabelMethods;
import com.servoy.j2db.ui.IScriptPortalComponentMethods;
import com.servoy.j2db.ui.IScriptScriptLabelMethods;
import com.servoy.j2db.ui.IScriptTabPanelMethods;
import com.servoy.j2db.ui.runtime.IRuntimeButton;
import com.servoy.j2db.ui.runtime.IRuntimeCalendar;
import com.servoy.j2db.ui.runtime.IRuntimeCheck;
import com.servoy.j2db.ui.runtime.IRuntimeChecks;
import com.servoy.j2db.ui.runtime.IRuntimeCombobox;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;
import com.servoy.j2db.ui.runtime.IRuntimeDataButton;
import com.servoy.j2db.ui.runtime.IRuntimeHtmlArea;
import com.servoy.j2db.ui.runtime.IRuntimeImageMedia;
import com.servoy.j2db.ui.runtime.IRuntimeListBox;
import com.servoy.j2db.ui.runtime.IRuntimePassword;
import com.servoy.j2db.ui.runtime.IRuntimeRadio;
import com.servoy.j2db.ui.runtime.IRuntimeRadios;
import com.servoy.j2db.ui.runtime.IRuntimeRtfArea;
import com.servoy.j2db.ui.runtime.IRuntimeSpinner;
import com.servoy.j2db.ui.runtime.IRuntimeSplitPane;
import com.servoy.j2db.ui.runtime.IRuntimeTextArea;
import com.servoy.j2db.ui.runtime.IRuntimeTextField;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 *
 */
@SuppressWarnings("nls")
public class TypeCreator extends TypeCache
{
	private static final String SCOPE_QBCOLUMNS = "scope:qbcolumns";
	private static final String SCOPE_TABLES = "scope:tables";

	private static final int INSTANCE_METHOD = 1;
	private static final int STATIC_METHOD = 2;
	private static final int INSTANCE_FIELD = 3;
	private static final int STATIC_FIELD = 4;

	public static final String TYPE_PREFIX = "plugins.";

	protected final static ImageDescriptor METHOD = ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(),
		new Path("/icons/function.gif"), null));
	protected final static ImageDescriptor PROPERTY = ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(),
		new Path("/icons/properties_icon.gif"), null));
	protected final static ImageDescriptor CONSTANT = ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(),
		new Path("/icons/constant.gif"), null));

	protected final static ImageDescriptor ELEMENTS = ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(),
		new Path("/icons/elements.gif"), null));

	protected final static ImageDescriptor SPECIAL_PROPERTY = ImageDescriptor.createFromURL(FileLocator.find(
		com.servoy.eclipse.ui.Activator.getDefault().getBundle(), new Path("/icons/special_properties_icon.gif"), null));

	protected final static ImageDescriptor GLOBAL_VAR_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/global_variable.gif"), null));
	protected final static ImageDescriptor GLOBAL_METHOD_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/global_method.gif"), null));

	protected final static ImageDescriptor FORM_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(
		com.servoy.eclipse.ui.Activator.getDefault().getBundle(), new Path("/icons/designer.gif"), null));
	protected final static ImageDescriptor FORM_METHOD_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/form_method.gif"), null));
	protected final static ImageDescriptor FORM_VARIABLE_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/form_variable.gif"), null));

	protected final static ImageDescriptor FOUNDSET_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/foundset.gif"), null));
	protected final static ImageDescriptor RELATION_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/relation.gif"), null));

	protected final static ImageDescriptor COLUMN_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/column.gif"), null));
	protected final static ImageDescriptor COLUMN_AGGR_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/columnaggr.gif"), null));
	protected final static ImageDescriptor COLUMN_CALC_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/columncalc.gif"), null));

	protected final static ImageDescriptor GLOBALS = ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(),
		new Path("/icons/globe.gif"), null));
	protected final static ImageDescriptor SCOPES = ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(),
		new Path("/icons/scopes.gif"), null));
	protected final static ImageDescriptor FORMS = ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(),
		new Path("/icons/forms.gif"), null));

	protected final static ImageDescriptor PLUGINS = ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(),
		new Path("/icons/plugin.gif"), null));

	protected final static ImageDescriptor PLUGIN_DEFAULT = ImageDescriptor.createFromURL(FileLocator.find(
		com.servoy.eclipse.ui.Activator.getDefault().getBundle(), new Path("/icons/plugin_conn.gif"), null));

	public static final String IMAGE_DESCRIPTOR = "servoy.IMAGEDESCRIPTOR";
	public static final String RESOURCE = "servoy.RESOURCE";
	public static final String VALUECOLLECTION = "servoy.VALUECOLLECTION";
	public static final String LAZY_VALUECOLLECTION = "servoy.LAZY_VALUECOLLECTION";

	public final static Set<String> BASE_TYPES = new HashSet<String>(128);

	static
	{
		BASE_TYPES.add("Object");
		BASE_TYPES.add("Number");
		BASE_TYPES.add("Array");
		BASE_TYPES.add("String");
		BASE_TYPES.add("Date");
		BASE_TYPES.add("Function");
		BASE_TYPES.add("Boolean");
		BASE_TYPES.add("RegExp");
		BASE_TYPES.add("Error");
		BASE_TYPES.add("Math");
	}

	private final ConcurrentMap<String, Class< ? >> classTypes = new ConcurrentHashMap<String, Class< ? >>();
	private final ConcurrentMap<String, Class< ? >> anonymousClassTypes = new ConcurrentHashMap<String, Class< ? >>();
	private final ConcurrentMap<String, IScopeTypeCreator> scopeTypes = new ConcurrentHashMap<String, IScopeTypeCreator>();
	protected final ConcurrentMap<Class< ? >, Class< ? >[]> linkedTypes = new ConcurrentHashMap<Class< ? >, Class< ? >[]>();
	protected final ConcurrentMap<Class< ? >, String> prefixedTypes = new ConcurrentHashMap<Class< ? >, String>();
	private volatile boolean initialized;
	protected static final List<String> objectMethods = Arrays.asList(new String[] { "wait", "toString", "hashCode", "equals", "notify", "notifyAll", "getClass" });

	public TypeCreator()
	{
		super("servoy", "javascript");
		addType("JSDataSet", JSDataSet.class);
		addType(IExecutingEnviroment.TOPLEVEL_SERVOY_EXCEPTION, ServoyException.class);
		addType(DataException.class.getSimpleName(), DataException.class);

		addAnonymousClassType("Controller", JSForm.class);
		addAnonymousClassType("JSApplication", JSApplication.class);
		addAnonymousClassType("JSI18N", JSI18N.class);
		addAnonymousClassType("HistoryProvider", HistoryProvider.class);
		addAnonymousClassType("JSUtils", JSUtils.class);
		addAnonymousClassType("JSUnit", JSUnitAssertFunctions.class);
		addAnonymousClassType("JSSolutionModel", JSSolutionModel.class);
		addAnonymousClassType("JSDatabaseManager", JSDatabaseManager.class);
		addAnonymousClassType("JSDeveloperSolutionModel", JSDeveloperSolutionModel.class);
		addAnonymousClassType("JSSecurity", JSSecurity.class);
		ElementResolver.registerConstantType("JSSecurity", "JSSecurity");


		addScopeType(Record.JS_RECORD, new RecordCreator());
		addScopeType(FoundSet.JS_FOUNDSET, new FoundSetCreator());
		addScopeType("JSDataSet", new JSDataSetCreator());
		addScopeType("Form", new FormScopeCreator());
		addScopeType("RuntimeForm", new FormScopeCreator());
		addScopeType("Elements", new ElementsScopeCreator());
		addScopeType("Plugins", new PluginsScopeCreator());
		addScopeType("Forms", new FormsScopeCreator());
		addScopeType("Relations", new RelationsScopeCreator());
		addScopeType("Dataproviders", new DataprovidersScopeCreator());
		addScopeType("InvisibleRelations", new InvisibleRelationsScopeCreator());
		addScopeType("InvisibleDataproviders", new InvisibleDataprovidersScopeCreator());
		addScopeType("Scopes", new ScopesScopeCreator());
		addScopeType("Scope", new ScopeScopeCreator());
		addScopeType(QBAggregate.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBColumn.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBCondition.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBFactory.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBFunction.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBGroupBy.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBJoin.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBJoins.class.getSimpleName(), new QueryBuilderJoinsCreator());
		addScopeType(QBLogicalCondition.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBResult.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBSelect.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBSort.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBSorts.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBTableClause.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBPart.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBParameter.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBParameters.class.getSimpleName(), new QueryBuilderCreator());
		addScopeType(QBColumns.class.getSimpleName(), new QueryBuilderColumnsCreator());
		addScopeType(QBFunctions.class.getSimpleName(), new QueryBuilderCreator());

	}


	private final ConcurrentHashMap<String, Boolean> ignorePackages = new ConcurrentHashMap<String, Boolean>();

	@Override
	protected Type createType(String typeName)
	{
		if (BASE_TYPES.contains(typeName) || typeName.startsWith("Array<")) return null;
		if (!initialized) initalize();

		Type type = null;
		if (typeName.startsWith("Packages.") || typeName.startsWith("java.") || typeName.startsWith("javax."))
		{
			String name = typeName;
			if (name.startsWith("Packages."))
			{
				name = name.substring("Packages.".length());
				type = findType(name);
				if (type != null)
				{
					return type;
				}
			}
			if (ignorePackages.containsKey(name)) return null;
			try
			{
				ClassLoader cl = com.servoy.eclipse.core.Activator.getDefault().getDesignClient().getBeanManager().getClassLoader();
				if (cl == null) cl = Thread.currentThread().getContextClassLoader();
				Class< ? > clz = Class.forName(name, false, cl);
				type = getClassType(clz, name);
			}
			catch (ClassNotFoundException e)
			{
				ignorePackages.put(name, Boolean.FALSE);
			}
		}
		else if (typeName.equals("Continuation"))
		{
			type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(typeName);
			type.setKind(TypeKind.JAVASCRIPT);
			type.setSuperType(getType("Function"));
		}
		else if (typeName.equals("byte"))
		{
			// special support for byte type (mostly used in Array<byte>)
			type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(typeName);
			type.setKind(TypeKind.JAVASCRIPT);
			type.setSuperType(getType("Object"));
		}
		if (type != null)
		{
			return addType(null, type);
		}


		String realTypeName = typeName;
		if (realTypeName.equals("JSFoundset")) realTypeName = FoundSet.JS_FOUNDSET;
		type = createType(realTypeName, realTypeName);
		if (type != null)
		{
			return addType(null, type);
		}
		else
		{
			FlattenedSolution fs = getFlattenedSolution();
			if (fs != null)
			{
				DynamicTypeCache tc = getTypeCache(fs);
				type = tc.findInBucket(fs.getSolution().getName(), realTypeName);
				if (type == null)
				{
					type = findInBucket(SCOPE_TABLES, realTypeName);
					if (type == null)
					{
						type = findInBucket(SCOPE_QBCOLUMNS, realTypeName);
					}
					if (type == null)
					{
						type = createDynamicType(realTypeName, realTypeName);
						if (type != null && realTypeName.indexOf('<') != -1 && type.eResource() == null && !type.isProxy())
						{
							return tc.addType(fs.getSolution().getName(), type);
						}
					}
				}
			}
			else
			{
				type = createDynamicType(realTypeName, realTypeName);
			}
		}
		return type;
	}

	private static final ConcurrentMap<String, DynamicTypeCache> typeCache = new ConcurrentHashMap<String, DynamicTypeCache>();

	/**
	 * @param fs
	 */
	private DynamicTypeCache getTypeCache(FlattenedSolution fs)
	{
		final String name = fs.getSolution().getName();
		DynamicTypeCache tc = typeCache.get(name);
		if (tc == null)
		{
			tc = new DynamicTypeCache("servoy", name, name);
			DynamicTypeCache previous = typeCache.putIfAbsent(name, tc);
			if (previous != null) tc = previous;
		}
		return tc;
	}

	private Type getClassType(Class< ? > clz, String name)
	{
		Type type = TypeInfoModelFactory.eINSTANCE.createType();
		type.setName(name);
		type.setKind(TypeKind.JAVA);
		type.setAttribute(JavaClassRuntimeTypeFactory.JAVA_CLASS, clz);

		java.lang.reflect.Method[] methods = clz.getMethods();
		Field[] fields = clz.getFields();

		EList<Member> members = type.getMembers();

		for (Field field : fields)
		{
			Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
			property.setName(field.getName());
			property.setType(getJSType(field.getType()));
			if (Modifier.isStatic(field.getModifiers()))
			{
				property.setStatic(true);
			}
			members.add(property);
		}
		for (java.lang.reflect.Method method : methods)
		{
			org.eclipse.dltk.javascript.typeinfo.model.Method m = TypeInfoModelFactory.eINSTANCE.createMethod();
			m.setName(method.getName());
			m.setType(getJSType(method.getReturnType()));

			EList<Parameter> parameters = m.getParameters();
			Class< ? >[] parameterTypes = method.getParameterTypes();
			for (int i = 0; i < parameterTypes.length; i++)
			{
				Parameter parameter = TypeInfoModelFactory.eINSTANCE.createParameter();
				parameter.setName("arg" + i);
				parameter.setType(getJSType(parameterTypes[i]));
				parameters.add(parameter);
			}
			if (Modifier.isStatic(method.getModifiers()))
			{
				m.setStatic(true);
			}
			members.add(m);
		}
		return type;
	}

	private JSType getJSType(Class< ? > type)
	{
		if (type != null && type != Void.class && type != void.class)
		{
			if (type == Object.class) return getTypeRef(ITypeNames.OBJECT);
			if (type.isArray())
			{
				Class< ? > componentType = type.getComponentType();
				JSType componentJSType = getJSType(componentType);
				if (componentJSType != null)
				{
					return TypeUtil.arrayOf(componentJSType);
				}
				return getTypeRef(ITypeNames.ARRAY);
			}
			else if (type == Boolean.class || type == boolean.class)
			{
				return getTypeRef(ITypeNames.BOOLEAN);
			}
			else if (type == Byte.class || type == byte.class)
			{
				return getTypeRef("byte");
			}
			else if (Number.class.isAssignableFrom(type) || type.isPrimitive())
			{
				return getTypeRef(ITypeNames.NUMBER);
			}
			else if (type == String.class || type == CharSequence.class)
			{
				return getTypeRef(ITypeNames.STRING);
			}
			else
			{

				return getTypeRef("Packages." + type.getName());
			}
		}
		return null;
	}


	protected void initalize()
	{

		DesignApplication application = com.servoy.eclipse.core.Activator.getDefault().getDesignClient();
		synchronized (this)
		{
			registerConstantsForScriptObject(ScriptObjectRegistry.getScriptObjectForClass(JSApplication.class), null);
			registerConstantsForScriptObject(ScriptObjectRegistry.getScriptObjectForClass(JSSecurity.class), null);
			registerConstantsForScriptObject(ScriptObjectRegistry.getScriptObjectForClass(JSSolutionModel.class), null);
			registerConstantsForScriptObject(ScriptObjectRegistry.getScriptObjectForClass(JSDatabaseManager.class), null);
			registerConstantsForScriptObject(ScriptObjectRegistry.getScriptObjectForClass(ServoyException.class), null);

			List<IClientPlugin> lst = application.getPluginManager().getPlugins(IClientPlugin.class);
			for (IClientPlugin clientPlugin : lst)
			{
				// for now cast to deprecated interface
				try
				{
					java.lang.reflect.Method method = clientPlugin.getClass().getMethod("getScriptObject", (Class[])null);
					if (method != null)
					{
						IScriptable scriptObject = (IScriptable)method.invoke(clientPlugin, (Object[])null);
						if (scriptObject instanceof IReturnedTypesProvider)
						{
							String prefix = "plugins." + clientPlugin.getName() + ".";
							registerConstantsForScriptObject((IReturnedTypesProvider)scriptObject, prefix);
							if (((IReturnedTypesProvider)scriptObject).getAllReturnedTypes() != null &&
								((IReturnedTypesProvider)scriptObject).getAllReturnedTypes().length > 0)
							{
								linkedTypes.put(scriptObject.getClass(), ((IReturnedTypesProvider)scriptObject).getAllReturnedTypes());
							}
						}
					}
				}
				catch (Throwable e)
				{
					Debug.error("error registering constants for client plugin ", e); //$NON-NLS-1$
				}
			}
		}
		IBeanClassProvider beanManager = (IBeanClassProvider)application.getBeanManager();
		Class< ? >[] allBeanClasses = beanManager.getAllBeanClasses();
		for (Class< ? > beanClass : allBeanClasses)
		{
			if (IServoyBeanFactory.class.isAssignableFrom(beanClass))
			{
				try
				{
					IServoyBeanFactory beanFactory = (IServoyBeanFactory)beanClass.newInstance();
					Object beanInstance = beanFactory.getBeanInstance(application.getApplicationType(), (IClientPluginAccess)application.getPluginAccess(),
						new Object[] { "developer", "developer", null });
					addType(beanClass.getSimpleName(), beanInstance.getClass());
				}
				catch (Exception e)
				{
					ServoyLog.logError("error creating bean for in the js type provider", e);
				}
				catch (NoClassDefFoundError e)
				{
					ServoyLog.logError("error creating bean for in the js type provider", e);
				}
			}
			else
			{
				addType(beanClass.getSimpleName(), beanClass);
			}
		}

		IServoyModel servoyModel = ServoyModelFinder.getServoyModel();
		synchronized (this)
		{
			if (!initialized)
			{
				initialized = true;
				if (servoyModel instanceof ServoyModel)
				{
					((ServoyModel)servoyModel).addPersistChangeListener(true, new IPersistChangeListener()
					{
						public void persistChanges(Collection<IPersist> changes)
						{
							flushCache();
						}
					});
				}
			}
		}
	}

	protected final Class< ? > getTypeClass(String name)
	{
		Class< ? > clz = classTypes.get(name);
		if (clz == null)
		{
			clz = anonymousClassTypes.get(name);
		}
		return clz;
	}


	public final Set<String> getTypeNames(String prefix)
	{
		Set<String> names = new HashSet<String>(classTypes.keySet());
		if (prefix != null && !"".equals(prefix.trim()))
		{
			String lowerCasePrefix = prefix.toLowerCase();
			Iterator<String> iterator = names.iterator();
			while (iterator.hasNext())
			{
				String name = iterator.next();
				if (name.startsWith(TYPE_PREFIX)) name = name.substring(name.lastIndexOf(".") + 1);
				if (!name.toLowerCase().startsWith(lowerCasePrefix)) iterator.remove();
			}
		}
		return names;
	}


	protected final void registerConstantsForScriptObject(IReturnedTypesProvider scriptObject, String prefix)
	{
		if (scriptObject == null) return;
		Class< ? >[] allReturnedTypes = scriptObject.getAllReturnedTypes();
		if (allReturnedTypes == null) return;

		for (Class< ? > element : allReturnedTypes)
		{
			boolean constant = false;
			JavaMembers javaMembers = ScriptObjectRegistry.getJavaMembers(element, null);
			if (javaMembers != null)
			{
				Object[] members = javaMembers.getIds(false);
				ArrayList<String> al = new ArrayList<String>(members.length);
				for (Object el : members)
				{
					al.add((String)el);
				}
				if (javaMembers instanceof InstanceJavaMembers)
				{
					al.removeAll(((InstanceJavaMembers)javaMembers).getGettersAndSettersToHide());
				}
				else
				{
					al.removeAll(objectMethods);
				}
				// skip constants only classes
				constant = al.size() == 0;
			}

			boolean add = false;
			if (IPrefixedConstantsObject.class.isAssignableFrom(element))
			{
				add = true;
				try
				{
					IPrefixedConstantsObject constants = (IPrefixedConstantsObject)element.newInstance();
					if (constant)
					{
						addAnonymousClassType(constants.getPrefix(), element);
						ElementResolver.registerConstantType(constants.getPrefix(), constants.getPrefix());
						if (prefix != null)
						{
							addAnonymousClassType(prefix + constants.getPrefix(), element);
							ElementResolver.registerConstantType(prefix + constants.getPrefix(), prefix + constants.getPrefix());
						}
					}
					else
					{
						addType(constants.getPrefix(), element);
						if (prefix != null)
						{
							addType(prefix + constants.getPrefix(), element);
						}
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
			else if (IConstantsObject.class.isAssignableFrom(element) || IJavaScriptType.class.isAssignableFrom(element))
			{
				add = true;
				if (constant)
				{
					addAnonymousClassType(element.getSimpleName(), element);
					ElementResolver.registerConstantType(element.getSimpleName(), element.getSimpleName());
					if (prefix != null)
					{
						addAnonymousClassType(prefix + element.getSimpleName(), element);
						ElementResolver.registerConstantType(prefix + element.getSimpleName(), prefix + element.getSimpleName());
					}
				}
				else
				{
					addType(element.getSimpleName(), element);
					if (prefix != null)
					{
						addType(prefix + element.getSimpleName(), element);
					}
				}

			}
			if (prefix != null && add)
			{
				prefixedTypes.put(element, prefix);
			}
		}
	}

	protected Type createDynamicType(String typeNameClassName, String fullTypeName)
	{
		// is it a 'generified' type
		int index = typeNameClassName.indexOf('<');
		if (index != -1 && (typeNameClassName.indexOf('>', index)) != -1)
		{
			String fullClassName = typeNameClassName;
			String classType = fullClassName.substring(0, index);
			if (classType.equals("JSFoundset"))
			{
				classType = FoundSet.JS_FOUNDSET;
				fullClassName = classType + fullClassName.substring(index);
			}
			Type type = createDynamicType(classType, fullClassName);
			if (type == null) type = createType(classType, fullClassName);
			return type;
		}

		IScopeTypeCreator creator = scopeTypes.get(typeNameClassName);
		if (creator != null)
		{
			return creator.createType(fullTypeName);
		}
		return null;
	}

	/**
	 * @param typeNameClassName
	 * @return
	 */
	protected final Type createType(String typeNameClassName, String fullTypeName)
	{
		Class< ? > cls = getTypeClass(typeNameClassName);
		if (cls != null)
		{
			return createType(fullTypeName, cls);
		}
		return null;
	}

	/**
	 * @param context
	 * @param typeName
	 * @param cls
	 * @return
	 */
	protected final Type createType(String typeName, Class< ? > cls)
	{
		Type type = TypeInfoModelFactory.eINSTANCE.createType();
		type.setName(typeName);
		type.setKind(TypeKind.JAVA);
		EList<Member> members = type.getMembers();

		fill(members, cls, typeName);

		if (cls != ServoyException.class && !IFoundSet.class.isAssignableFrom(cls))
		{
			ImageDescriptor desc = IconProvider.instance().descriptor(cls);
			type.setAttribute(IMAGE_DESCRIPTOR, desc);
		}
		if (IDeprecated.class.isAssignableFrom(cls) || (prefixedTypes.containsKey(cls) && !typeName.equals(prefixedTypes.get(cls) + cls.getSimpleName())))
		{
			type.setDeprecated(true);
			type.setVisible(false);
		}
		if (cls != IRuntimeComponent.class && IRuntimeComponent.class.isAssignableFrom(cls))
		{
			type.setSuperType(getType("RuntimeComponent"));
		}
		Class< ? >[] returnTypes = linkedTypes.get(cls);
		if (returnTypes != null)
		{
			int index = typeName.indexOf('<');
			int index2;
			String config = typeName;
			if (index != -1 && (index2 = typeName.indexOf('>', index)) != -1)
			{
				config = typeName.substring(index + 1, index2);
			}
			for (Class< ? > returnTypeClass : returnTypes)
			{
				String name = returnTypeClass.getSimpleName();
				if (IPrefixedConstantsObject.class.isAssignableFrom(returnTypeClass))
				{
					try
					{
						IPrefixedConstantsObject constants = (IPrefixedConstantsObject)returnTypeClass.newInstance();
						name = constants.getPrefix();
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
					}
				}
				String prefix = TYPE_PREFIX + config + ".";
				members.add(createProperty(name, true, TypeUtil.classType(getType(prefix + name)), null, null));
			}
		}
		return type;
	}

	/**
	 * @param typeName 
	 * @param members
	 * @param class1
	 */
	private final void fill(EList<Member> membersList, Class< ? > scriptObjectClass, String typeName)
	{
		ArrayList<String> al = new ArrayList<String>();
		JavaMembers javaMembers = ScriptObjectRegistry.getJavaMembers(scriptObjectClass, null);
		if (Scriptable.class.isAssignableFrom(scriptObjectClass) && !(javaMembers instanceof InstanceJavaMembers))
		{
			// if the class is a scriptable an the javamembers is not a instance java members, just return nothing.
			return;
		}
		if (javaMembers != null)
		{
			IScriptObject scriptObject = ScriptObjectRegistry.getScriptObjectForClass(scriptObjectClass);
			Object[] members = javaMembers.getIds(false);
			for (Object element : members)
			{
				al.add((String)element);
			}
			if (IConstantsObject.class.isAssignableFrom(scriptObjectClass))
			{
				members = javaMembers.getIds(true);
				for (Object element : members)
				{
					al.add((String)element);
				}
			}
			if (javaMembers instanceof InstanceJavaMembers)
			{
				al.removeAll(((InstanceJavaMembers)javaMembers).getGettersAndSettersToHide());
			}
			else
			{
				al.removeAll(objectMethods);
			}

			for (String name : al)
			{
				int type = 0;
				Object object = javaMembers.getMethod(name, false);
				if (object == null)
				{
					object = javaMembers.getField(name, false);
					if (object == null)
					{
						object = javaMembers.getField(name, true);
						if (object != null)
						{
							type = STATIC_FIELD;
						}
						else
						{
							object = javaMembers.getMethod(name, true);
							type = STATIC_METHOD;
						}
					}
					else type = INSTANCE_FIELD;
				}
				else type = INSTANCE_METHOD;

				if (object != null)
				{
					Class< ? > returnTypeClz = getReturnType(object);
					if (type == INSTANCE_METHOD || type == STATIC_METHOD)
					{
						MemberBox[] memberbox = null;
						if (object instanceof NativeJavaMethod)
						{
							memberbox = ((NativeJavaMethod)object).getMethods();
						}
						int membersSize = memberbox == null ? 1 : memberbox.length;
						for (int i = 0; i < membersSize; i++)
						{
							Method method = TypeInfoModelFactory.eINSTANCE.createMethod();
							method.setName(name);
							Class< ? >[] parameterTypes = memberbox[i].getParameterTypes();

							if (scriptObject instanceof ITypedScriptObject)
							{
								if (((ITypedScriptObject)scriptObject).isDeprecated(name, parameterTypes))
								{
									method.setDeprecated(true);
									method.setVisible(false);
								}
							}
							else if (scriptObject != null && scriptObject.isDeprecated(name))
							{
								method.setDeprecated(true);
								method.setVisible(false);
							}
							method.setDescription(getDoc(name, scriptObjectClass, name, parameterTypes)); // TODO name should be of parent.
							if (returnTypeClz != null)
							{
								method.setType(getMemberTypeName(name, returnTypeClz, typeName));
							}
							method.setAttribute(IMAGE_DESCRIPTOR, METHOD);
							method.setStatic(type == STATIC_METHOD);

							IParameter[] scriptParams = getParameters(name, scriptObjectClass, memberbox[i]);
							if (scriptParams != null && scriptParams.length > 0)
							{
								EList<Parameter> parameters = method.getParameters();
								for (IParameter param : scriptParams)
								{
									Parameter parameter = TypeInfoModelFactory.eINSTANCE.createParameter();
									parameter.setName(param.getName());
									if (param.getType() != null)
									{
										Class< ? > paramType = param.getRealType();
										if (paramType != null && paramType.isArray())
										{
											Class< ? > componentType = paramType.getComponentType();
											if (param.isVarArgs())
											{
												parameter.setType(getMemberTypeName(name, componentType, typeName));
											}
											else if (componentType == Object.class)
											{
												parameter.setType(getTypeRef(ITypeNames.ARRAY));
											}
											else
											{
												parameter.setType(TypeUtil.arrayOf(getMemberTypeName(name, componentType, typeName)));
											}
										}
										else if (paramType != null)
										{
											parameter.setType(getMemberTypeName(name, paramType, typeName));
										}
										else
										{
											parameter.setType(getTypeRef(SolutionExplorerListContentProvider.TYPES.get(param.getType())));
										}
									}
									parameter.setKind(param.isVarArgs() ? ParameterKind.VARARGS : param.isOptional() ? ParameterKind.OPTIONAL
										: ParameterKind.NORMAL);
									parameters.add(parameter);
								}
							}
							else if (parameterTypes != null && parameterTypes.length > 0)
							{
								EList<Parameter> parameters = method.getParameters();
								for (Class< ? > paramClass : parameterTypes)
								{
									Parameter parameter = TypeInfoModelFactory.eINSTANCE.createParameter();
									if (paramClass.isArray())
									{
										Class< ? > componentType = paramClass.getComponentType();
										parameter.setName(SolutionExplorerListContentProvider.TYPES.get(componentType.getName()));
										parameter.setType(TypeUtil.arrayOf(getTypeRef(SolutionExplorerListContentProvider.TYPES.get(componentType.getName()))));
									}
									else
									{
										parameter.setName(SolutionExplorerListContentProvider.TYPES.get(paramClass.getName()));
										parameter.setType(getTypeRef(SolutionExplorerListContentProvider.TYPES.get(paramClass.getName())));
									}
									parameter.setKind(ParameterKind.NORMAL);
									parameters.add(parameter);
								}
							}
							membersList.add(method);
						}
					}
					else
					{
						JSType returnType = null;
						if (returnTypeClz != null)
						{
							returnType = getMemberTypeName(name, returnTypeClz, typeName);
						}
						ImageDescriptor descriptor = IconProvider.instance().descriptor(returnTypeClz);
						if (descriptor == null)
						{
							descriptor = type == STATIC_FIELD ? CONSTANT : PROPERTY;
						}
						Property property = createProperty(name, false, returnType, getDoc(name, scriptObjectClass, name, null), descriptor);
						property.setStatic(type == STATIC_FIELD);
						if (scriptObject != null && scriptObject.isDeprecated(name))
						{
							property.setDeprecated(true);
							property.setVisible(false);
						}
						membersList.add(property);
					}
				}
			}
		}
	}

	protected final JSType getMemberTypeName(String memberName, Class< ? > memberReturnType, String objectTypeName)
	{
		int index = objectTypeName.indexOf('<');
		int index2;
		if (index != -1 && (index2 = objectTypeName.indexOf('>', index)) != -1)
		{
			String config = objectTypeName.substring(index + 1, index2);

			if (memberReturnType == Record.class)
			{
				return getTypeRef(Record.JS_RECORD + '<' + config + '>');
			}
			if (memberReturnType == FoundSet.class)
			{
				if (memberName.equals("unrelated"))
				{
					if (config.indexOf('.') == -1)
					{
						// its really a relation, unrelate it.
						FlattenedSolution fs = TypeCreator.getFlattenedSolution();
						if (fs != null)
						{
							Relation relation = fs.getRelation(config);
							if (relation != null)
							{
								return getTypeRef(FoundSet.JS_FOUNDSET + '<' + relation.getForeignDataSource() + '>');
							}
						}
						return getTypeRef(FoundSet.JS_FOUNDSET);
					}
				}
				return getTypeRef(FoundSet.JS_FOUNDSET + '<' + config + '>');
			}
		}
		if (memberReturnType.isArray())
		{
			Class< ? > returnType = getReturnType(memberReturnType.getComponentType());
			if (returnType != null)
			{
				JSType componentJSType = getMemberTypeName(memberName, returnType, objectTypeName);
				if (componentJSType != null)
				{
					return TypeUtil.arrayOf(componentJSType);
				}
			}
			return getTypeRef(ITypeNames.ARRAY);
		}

		String typeName = null;
		if (prefixedTypes.containsKey(memberReturnType))
		{
			typeName = prefixedTypes.get(memberReturnType) + memberReturnType.getSimpleName();
		}
		else
		{
			typeName = SolutionExplorerListContentProvider.TYPES.get(memberReturnType.getSimpleName());
			addAnonymousClassType(typeName, memberReturnType);
		}
		return getTypeRef(typeName);
	}

	public final void addType(String name, Class< ? > cls)
	{
		classTypes.put(name, cls);
	}

	protected void addAnonymousClassType(String name, Class< ? > cls)
	{
		if (!classTypes.containsKey(name) && !scopeTypes.containsKey(name) && !BASE_TYPES.contains(name))
		{
			anonymousClassTypes.put(name, cls);
		}
	}

	public final void addScopeType(String name, IScopeTypeCreator creator)
	{
		scopeTypes.put(name, creator);
	}

	/**
	 * @param context
	 * @param type
	 * @param provider
	 * @return
	 */
	protected static final Type getDataProviderType(ITypeInfoContext context, IDataProvider provider)
	{
		if (provider instanceof Column)
		{
			ColumnInfo columnInfo = ((Column)provider).getColumnInfo();
			if (columnInfo != null)
			{
				if (columnInfo.hasFlag(Column.UUID_COLUMN))
				{
					return context.getType("UUID");
				}
			}
		}
		ComponentFormat componentFormat = ComponentFormat.getComponentFormat(null, provider, com.servoy.eclipse.core.Activator.getDefault().getDesignClient());
		switch (componentFormat.dpType)
		{
			case IColumnTypes.DATETIME :
				return context.getType("Date");

			case IColumnTypes.INTEGER :
			case IColumnTypes.NUMBER :
				return context.getType("Number");

			case IColumnTypes.TEXT :
				return context.getType("String");

			default :
				// for now don't return a type (so that anything is valid)
				// maybe we should return Array<byte>
				// should be in sync with TypeProvider.DataprovidersScopeCreator
//				return context.getType("Object");
		}
		return null;
	}


	/**
	 * @author jcompagner
	 *
	 */
	private final class DynamicTypeCache extends TypeCache
	{
		private final String name;

		/**
		 * @param scheme
		 * @param authority
		 * @param name
		 */
		private DynamicTypeCache(String scheme, String authority, String name)
		{
			super(scheme, authority);
			this.name = name;
		}

		@Override
		protected Type createType(String typeName)
		{
			CONTEXT.set(ElementResolver.getFlattenedSolution(name));
			try
			{
				return TypeCreator.this.createType(typeName);
			}
			finally
			{
				CONTEXT.remove();
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.dltk.javascript.typeinfo.TypeCache#findInBucket(java.lang.String, java.lang.String)
		 */
		@Override
		public Type findInBucket(String bucket, String typeName)
		{
			return super.findInBucket(bucket, typeName);
		}

		@Override
		public Type addType(String bucket, Type type)
		{
			if (bucket == null || "".equals(bucket))
			{
				return TypeCreator.this.addType(bucket, type);
			}
			return super.addType(bucket, type);
		}
	}

	protected interface IScopeTypeCreator
	{
		Type createType(String fullTypeName);
	}

	public IParameter[] getParameters(String key, Class< ? > scriptObjectClass, MemberBox member)
	{
		if (scriptObjectClass == null) return null;
		IScriptObject scriptObject = ScriptObjectRegistry.getScriptObjectForClass(scriptObjectClass);
		IParameter[] parameters = null;
		String[] parameterNames = null;
		if (scriptObject instanceof ITypedScriptObject)
		{
			parameters = ((ITypedScriptObject)scriptObject).getParameters(key, member.getParameterTypes());
		}
		else if (scriptObject != null)
		{
			parameterNames = scriptObject.getParameterNames(key);
		}

		if (parameterNames != null && parameters == null)
		{
			int memberParamLength = member.getParameterTypes().length;
			if (memberParamLength < parameterNames.length)
			{
				boolean removeOptional = false;
				// if parameterNames bigger then the members parameter types and it is not a vararg, just get the first names.
				if (memberParamLength == 1 && member.getParameterTypes()[0].isArray())
				{
					parameters = new IParameter[parameterNames.length];
				}
				else
				{
					parameters = new IParameter[memberParamLength];
					removeOptional = true;
				}
				for (int i = 0; i < parameters.length; i++)
				{
					String name = parameterNames[i];
					boolean vararg = false;
					boolean optional = name.startsWith("[") && name.endsWith("]");
					if (optional && removeOptional)
					{
						optional = false;
						name = name.substring(1, name.length() - 1);
						if (name.startsWith("..."))
						{
							vararg = true;
							name = name.substring(3);
						}
					}
					else if (name.startsWith("[..."))
					{
						vararg = true;
					}
					else if (optional)
					{
						name = name.substring(1, name.length() - 1);
					}
					String type = null;
					if (removeOptional && i < member.getParameterTypes().length)
					{
						Class< ? > paramClass = member.getParameterTypes()[i];
						String className = (paramClass.isArray()) ? paramClass.getComponentType().getName() : paramClass.getName();
						if (prefixedTypes.containsKey((paramClass.isArray()) ? paramClass.getComponentType() : paramClass))
						{
							className = prefixedTypes.get((paramClass.isArray()) ? paramClass.getComponentType() : paramClass) + className;
						}
						if (paramClass.isArray())
						{
							type = SolutionExplorerListContentProvider.TYPES.get(className) + "[]";
						}
						else
						{
							type = SolutionExplorerListContentProvider.TYPES.get(className);
						}
					}
					parameters[i] = new ScriptParameter(name, type, i < member.getParameterTypes().length ? member.getParameterTypes()[i] : null, optional,
						vararg);
				}
			}
			else if (memberParamLength == parameterNames.length)
			{
				parameters = new IParameter[memberParamLength];
				for (int i = 0; i < memberParamLength; i++)
				{
					Class< ? > paramClass = member.getParameterTypes()[i];
					String name = null;
					String type = null;
					boolean optional = false;
					if (parameterNames != null)
					{
						name = parameterNames[i];
						if (name.startsWith("[") && name.endsWith("]"))
						{
							name = name.substring(1, name.length() - 1);
							optional = true;
						}
						else if (paramClass.isArray())
						{
							type = SolutionExplorerListContentProvider.TYPES.get((prefixedTypes.containsKey(paramClass.getComponentType())
								? prefixedTypes.get(paramClass.getComponentType()) : "") + paramClass.getComponentType().getName()) +
								"[]";
						}
						else
						{
							type = SolutionExplorerListContentProvider.TYPES.get((prefixedTypes.containsKey(paramClass) ? prefixedTypes.get(paramClass) : "") +
								paramClass.getName());
						}

					}
					else if (paramClass.isArray())
					{
						type = SolutionExplorerListContentProvider.TYPES.get((prefixedTypes.containsKey(paramClass.getComponentType())
							? prefixedTypes.get(paramClass.getComponentType()) : "") + paramClass.getComponentType().getName()) +
							"[]";
						name = type;

					}
					else
					{
						type = SolutionExplorerListContentProvider.TYPES.get((prefixedTypes.containsKey(paramClass) ? prefixedTypes.get(paramClass) : "") +
							paramClass.getName());
						name = type;
					}
					parameters[i] = new ScriptParameter(name, type, paramClass, optional, false);
				}
			}
		}
		return parameters;
	}

	/**
	 * 
	 */
	protected void flushCache()
	{
		//if (changes.contains(tables))
		clear(SCOPE_TABLES);
		clear(SCOPE_QBCOLUMNS);

		typeCache.clear();

		relationCache.clear();
	}

	public static ThreadLocal<FlattenedSolution> CONTEXT = new ThreadLocal<FlattenedSolution>();

	public static FlattenedSolution getFlattenedSolution()
	{
		return CONTEXT.get();
	}

	protected Member createMethod(ScriptMethod sm, ImageDescriptor image)
	{
		return createMethod(sm, image, null);
	}

	/**
	 * @param sm
	 * @return
	 */
	protected Member createMethod(ScriptMethod sm, ImageDescriptor image, String fileName)
	{
		Method method = TypeInfoModelFactory.eINSTANCE.createMethod();
		method.setName(sm.getName());

		MethodArgument[] arguments = sm.getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS);
		if (arguments != null && arguments.length > 0)
		{
			EList<Parameter> parameters = method.getParameters();
			for (MethodArgument argument : arguments)
			{
				Parameter parameter = TypeInfoModelFactory.eINSTANCE.createParameter();
				parameter.setKind(ParameterKind.NORMAL);
				parameter.setName(argument.getName());
				parameter.setType(getTypeRef(argument.getType().getName()));
				parameters.add(parameter);
			}
		}


		String type = sm.getSerializableRuntimeProperty(IScriptProvider.TYPE);
		if (type != null)
		{
			method.setType(getTypeRef(type));
		}
		String comment = sm.getRuntimeProperty(IScriptProvider.COMMENT);
		if (comment == null)
		{
			String declaration = sm.getDeclaration();
			int commentStart = declaration.indexOf("/**");
			if (commentStart != -1)
			{
				int commentEnd = declaration.indexOf("*/", commentStart);
				comment = declaration.substring(commentStart, commentEnd);
			}
		}
		if (comment != null)
		{
			if (comment.lastIndexOf("@deprecated") != -1)
			{
				method.setDeprecated(true);
			}

			method.setDescription(getParsedComment(comment));
		}
		if (image != null)
		{
			method.setAttribute(IMAGE_DESCRIPTOR, image);
		}
		if (fileName != null)
		{
			method.setAttribute(RESOURCE, fileName);
		}
		return method;
	}

	public Property createProperty(String name, boolean readonly, String typeName, String description, ImageDescriptor image)
	{
		return createProperty(name, readonly, typeName, description, image, null);
	}

	public Property createProperty(String name, boolean readonly, String typeName, String description, ImageDescriptor image, Object resource)
	{
		SimpleType type = null;
		if (typeName != null)
		{
			type = getTypeRef(typeName);
		}
		return createProperty(name, readonly, type, description, image, resource);
	}


	public Property createProperty(String name, boolean readonly, String typeName, ImageDescriptor image)
	{
		SimpleType type = null;
		if (typeName != null)
		{
			type = getTypeRef(typeName);
		}
		return createProperty(name, readonly, type, null, image);
	}

	public static Property createProperty(String name, boolean readonly, JSType type, String description, ImageDescriptor image)
	{
		return createProperty(name, readonly, type, description, image, null);
	}

	public static Property createProperty(String name, boolean readonly, JSType type, String description, ImageDescriptor image, Object resource)
	{
		Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
		property.setName(name);
		property.setReadOnly(readonly);
		if (description != null)
		{
			property.setDescription(description);
		}
		if (type != null)
		{
			property.setType(type);
		}
		if (image != null)
		{
			property.setAttribute(IMAGE_DESCRIPTOR, image);
		}
		else if (type instanceof SimpleType && ((SimpleType)type).getTarget().getAttribute(IMAGE_DESCRIPTOR) != null)
		{
			property.setAttribute(IMAGE_DESCRIPTOR, ((SimpleType)type).getTarget().getAttribute(IMAGE_DESCRIPTOR));
		}
		if (resource != null)
		{
			property.setAttribute(RESOURCE, resource);
		}
		return property;
	}

	public static String getParsedComment(String comment)
	{
		int currPos = 0;
		int endPos = comment.length();
		boolean newLine = true;
		StringBuilder sb = new StringBuilder(comment.length());
		outer : while (currPos < endPos)
		{
			char ch;
			if (newLine)
			{
				do
				{
					ch = comment.charAt(currPos++);
					if (currPos >= endPos) break outer;
					if (ch == '\n' || ch == '\r') break;
				}
				while (Character.isWhitespace(ch) || ch == '*' || ch == '/');
			}
			else
			{
				ch = comment.charAt(currPos++);
			}
			newLine = ch == '\n' || ch == '\r';

			if (newLine)
			{
				if (sb.length() != 0) sb.append("<br/>\n");
			}
			else
			{
				sb.append(ch);
			}
		}

		JavaDoc2HTMLTextReader reader = new JavaDoc2HTMLTextReader(new StringReader(sb.toString()));
		try
		{
			return reader.getString();
		}
		catch (IOException e)
		{
			return comment;
		}
	}

	private static final ConcurrentMap<MethodSignature, String> docCache = new ConcurrentHashMap<MethodSignature, String>(64, 0.9f, 16);

	/**
	 * @param key
	 * @param scriptObject
	 * @param name
	 * @return
	 */
	public static String getDoc(String key, Class< ? > scriptObjectClass, String name, Class< ? >[] parameterTypes)
	{
		if (scriptObjectClass == null) return null;

		MethodSignature cacheKey = new MethodSignature(scriptObjectClass, name, parameterTypes);
		String doc = docCache.get(cacheKey);
		if (doc == null)
		{
			doc = key;
			IScriptObject scriptObject = ScriptObjectRegistry.getScriptObjectForClass(scriptObjectClass);
			if (scriptObject != null)
			{
				StringBuilder docBuilder = new StringBuilder(200);
				String sample = null;
				boolean deprecated = false;
				String deprecatedText = null;
				IParameter[] parameters = null;
				String returnText = null;
				if (scriptObject instanceof ITypedScriptObject)
				{
					String toolTip = ((ITypedScriptObject)scriptObject).getToolTip(key, parameterTypes);
					if (toolTip != null) docBuilder.append(toolTip);
					sample = ((ITypedScriptObject)scriptObject).getSample(key, parameterTypes);
					deprecated = ((ITypedScriptObject)scriptObject).isDeprecated(key, parameterTypes);
					if (deprecated)
					{
						deprecatedText = ((ITypedScriptObject)scriptObject).getDeprecatedText(key, parameterTypes);
					}

					if (parameterTypes != null)
					{
						parameters = ((ITypedScriptObject)scriptObject).getParameters(key, parameterTypes);
					}

					Class< ? > returnedType = ((ITypedScriptObject)scriptObject).getReturnedType(key, parameterTypes);
					String returnDescription = ((ITypedScriptObject)scriptObject).getReturnDescription(key, parameterTypes);
					if ((returnedType != Void.class && returnedType != void.class && returnedType != null) || returnDescription != null)
					{
						returnText = "<b>@return</b> ";
						if (returnedType != null) returnText += returnedType.getSimpleName() + ' ';
						if (returnDescription != null) returnText += returnDescription;
					}
				}
				else
				{
					String toolTip = scriptObject.getToolTip(name);
					if (toolTip != null) docBuilder.append(toolTip);
					sample = scriptObject.getSample(key);
					deprecated = scriptObject.isDeprecated(key);
				}
				if (sample != null)
				{
					docBuilder.append("<br/><pre>");
					docBuilder.append(HtmlUtils.escapeMarkup(sample));
					docBuilder.append("</pre>");
				}
				if (docBuilder.length() > 0)
				{
					if (parameters != null)
					{
						StringBuilder sb = new StringBuilder(parameters.length * 30);
						for (IParameter parameter : parameters)
						{
							sb.append("<br/><b>@param</b> ");
							if (parameter.getType() != null)
							{
								sb.append("{");
								sb.append(SolutionExplorerListContentProvider.TYPES.get(parameter.getType()));
								sb.append("} ");
							}
							sb.append(parameter.getName());
							if (parameter.getDescription() != null)
							{
								sb.append(" ");
								sb.append(parameter.getDescription());
							}
						}
						docBuilder.append(sb);
					}
					if (returnText != null)
					{
						docBuilder.append("<br/></br>");
						docBuilder.append(returnText);
					}
					if (deprecatedText != null)
					{
						docBuilder.append("<br/><br/><b>@deprecated</b> ");
						docBuilder.append(deprecatedText);
					}
					else if (deprecated) docBuilder.append("<br/><br/><b>@deprecated</b>");
					doc = Utils.stringReplace(docBuilder.toString(), "%%prefix%%", ""); //$NON-NLS-1$ //$NON-NLS-2$
					doc = Utils.stringReplace(doc, "%%elementName%%", "elements.elem"); //$NON-NLS-1$
				}
			}
			docCache.putIfAbsent(cacheKey, doc);
		}
		return doc;
	}

	public static Class< ? > getReturnType(Object object)
	{
		Class< ? > returnType = null;
		if (object instanceof NativeJavaMethod)
		{
			NativeJavaMethod method = (NativeJavaMethod)object;
			MemberBox[] methods = method.getMethods();
			if (methods != null && methods.length > 0)
			{
				returnType = methods[0].getReturnType();
			}
		}
		else if (object instanceof BeanProperty)
		{
			returnType = ((BeanProperty)object).getGetter().getReturnType();
		}
		else if (object instanceof Field)
		{
			returnType = ((Field)object).getType();
		}
		return getReturnType(returnType);
	}

	/**
	 * @param returnType
	 */
	private static Class< ? > getReturnType(Class< ? > returnType)
	{
		if (returnType == Object.class || returnType == null) return null;
		if (returnType.isArray()) return returnType;
		if (!returnType.isAssignableFrom(Void.class) && !returnType.isAssignableFrom(void.class))
		{
			if (returnType.isAssignableFrom(Record.class))
			{
				return Record.class;
			}
			else if (returnType.isAssignableFrom(FoundSet.class))
			{
				return FoundSet.class;
			}
			else if (returnType.isPrimitive() || Number.class.isAssignableFrom(returnType))
			{
				if (returnType.isAssignableFrom(boolean.class)) return Boolean.class;
				if (returnType.isAssignableFrom(byte.class))
				{
					return byte.class;
				}
				return Number.class;
			}
			else if (returnType == Object.class || returnType == String.class || Date.class.isAssignableFrom(returnType))
			{
				return returnType;
			}
			JavaMembers javaMembers = ScriptObjectRegistry.getJavaMembers(returnType, null);
			if (javaMembers != null)
			{
				return returnType;
			}
		}
		return null;
	}

	private final static class MethodSignature
	{
		private final Class< ? > scriptObjectClass;
		private final String name;
		private final Class< ? >[] parameterTypes;

		/**
		 * @param scriptObjectClass
		 * @param name
		 * @param parameterTypes
		 */
		public MethodSignature(Class< ? > scriptObjectClass, String name, Class< ? >[] parameterTypes)
		{
			this.scriptObjectClass = scriptObjectClass;
			this.name = name;
			this.parameterTypes = parameterTypes;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + Arrays.hashCode(parameterTypes);
			result = prime * result + ((scriptObjectClass == null) ? 0 : scriptObjectClass.hashCode());
			return result;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			MethodSignature other = (MethodSignature)obj;
			if (name == null)
			{
				if (other.name != null) return false;
			}
			else if (!name.equals(other.name)) return false;
			if (!Arrays.equals(parameterTypes, other.parameterTypes)) return false;
			if (scriptObjectClass == null)
			{
				if (other.scriptObjectClass != null) return false;
			}
			else if (!scriptObjectClass.equals(other.scriptObjectClass)) return false;
			return true;
		}
	}


	/**
	 * @param recordType
	 * @return
	 */
	private static Type getRecordType(String type)
	{
		String recordType = type;
		if (recordType.startsWith("{") && recordType.endsWith("}"))
		{
			recordType = recordType.substring(1, recordType.length() - 1);
		}
		Type t = TypeInfoModelFactory.eINSTANCE.createType();
		t.setKind(TypeKind.JAVA);

		EList<Member> members = t.getMembers();
		StringTokenizer st = new StringTokenizer(recordType, ",");
		while (st.hasMoreTokens())
		{
			String typeName = "Object";
			String propertyName = st.nextToken();
			int typeSeparator = propertyName.indexOf(':');
			if (typeSeparator != -1)
			{
				typeName = propertyName.substring(typeSeparator + 1);
				propertyName = propertyName.substring(0, typeSeparator);
			}

			Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
			property.setName(propertyName);
			property.setType(TypeUtil.ref(typeName));
			members.add(property);
		}
		return t;
	}


	private class ScopesScopeCreator implements IScopeTypeCreator
	{
		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public Type createType(String fullTypeName)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setKind(TypeKind.JAVA);
			type.setAttribute(IMAGE_DESCRIPTOR, SCOPES);
			type.setName(fullTypeName);

			FlattenedSolution fs;
			if ("Scopes".equals(fullTypeName) || (fs = getFlattenedSolution()) == null)
			{
				// special array lookup property so that scopes[xxx]. does code complete.
				Property arrayProp = createProperty("[]", true, "Scope", PROPERTY);
				arrayProp.setVisible(false);
				type.getMembers().add(arrayProp);
				// quickly add this one to the static types. context.markInvariant(type); 
			}
			else
			{
				type.setSuperType(getType("Scopes"));
				EList<Member> members = type.getMembers();

				for (Pair<String, IRootObject> scope : fs.getScopes())
				{
					Property scopeProperty = createProperty(scope.getLeft(), true, getTypeRef("Scope<" + scope.getRight().getRootObject().getName() + '/' +
						scope.getLeft() + '>'), "Global scope " + scope.getLeft() + " defined in solution " + scope.getRight().getRootObject().getName(),
						SCOPES);
//					scopeProperty.setAttribute(LAZY_VALUECOLLECTION, persist); // currently not needed, solution name from config is used
					members.add(scopeProperty);
				}
			}
			return type;
		}
	}

	private class ScopeScopeCreator implements IScopeTypeCreator
	{
		public Type createType(String typeName)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(typeName);
			type.setKind(TypeKind.JAVA);

			if (typeName.endsWith(">"))
			{
				// Scope<solutionname/scopeName>
				FlattenedSolution fs = getFlattenedSolution();
				String config = typeName.substring(typeName.indexOf('<') + 1, typeName.length() - 1);
				String[] split = config.split("/");
				String solutionName = split[0];
				String scopeName = split[1];

				if (ScriptVariable.GLOBAL_SCOPE.equals(scopeName))
				{
					EList<Member> members = type.getMembers();


					members.add(createProperty("allmethods", true, TypeUtil.arrayOf("String"), "Returns all global method names in an Array", SPECIAL_PROPERTY));
					members.add(createProperty("allvariables", true, TypeUtil.arrayOf("String"), "Returns all global variable names in an Array",
						SPECIAL_PROPERTY));
					members.add(createProperty("allrelations", true, TypeUtil.arrayOf("String"), "Returns all global relation names in an Array",
						SPECIAL_PROPERTY));
				}

				if (fs != null && (fs.getMainSolutionMetaData().getName().equals(solutionName) || fs.hasModule(solutionName)))
				{
					type.setSuperType(getType("Relations<" + config + '>')); // Relations<solutionName/scopeName>
				}
			}

			return type;
		}
	}

	private class FormsScopeCreator implements IScopeTypeCreator
	{
		private final ConcurrentMap<String, String> descriptions = new ConcurrentHashMap<String, String>();

		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public Type createType(String fullTypeName)
		{
			Type type = null;
			if ("Forms".equals(fullTypeName))
			{
				type = TypeInfoModelFactory.eINSTANCE.createType();
				type.setKind(TypeKind.JAVA);
				type.setAttribute(IMAGE_DESCRIPTOR, FORMS);

				EList<Member> members = type.getMembers();
				members.add(createProperty("allnames", true, TypeUtil.arrayOf("String"), "All form names as an array", SPECIAL_PROPERTY));
				members.add(createProperty("length", true, "Number", "Number of forms", PROPERTY));

				// special array lookup property so that forms[xxx]. does code complete.
				Property arrayProp = createProperty("[]", true, "RuntimeForm", PROPERTY);
				arrayProp.setVisible(false);
				members.add(arrayProp);
				type.setName("Forms");
				// quickly add this one to the static types.
				return addType(null, type);
			}
			else
			{
				FlattenedSolution fs = getFlattenedSolution();
				if (fs == null) return getType("Forms");
				type = TypeInfoModelFactory.eINSTANCE.createType();
				type.setSuperType(getType("Forms"));
				type.setName("Forms<" + fs.getMainSolutionMetaData().getName() + '>');
				type.setKind(TypeKind.JAVA);
				type.setAttribute(IMAGE_DESCRIPTOR, FORMS);
				EList<Member> members = type.getMembers();
				Iterator<Form> forms = fs.getForms(false);
				while (forms.hasNext())
				{
					Form form = forms.next();
					Property formProperty = createProperty(form.getName(), true, getTypeRef("RuntimeForm<" + form.getName() + '>'),
						getDescription(form.getDataSource()), FORM_IMAGE);
					formProperty.setAttribute(LAZY_VALUECOLLECTION, form);
					if (FormEncapsulation.isPrivate(form, fs))
					{
						formProperty.setVisible(false);
					}
					members.add(formProperty);
				}
			}
			return type;
		}

		/**
		 * @param dataSource
		 * @return
		 */
		private String getDescription(String ds)
		{
			String datasource = ds;
			if (datasource == null) datasource = "<no datasource>";
			String description = descriptions.get(datasource);
			if (description == null)
			{
				description = "Form based on datasource: " + datasource;
				descriptions.putIfAbsent(datasource, description);
			}
			return description;
		}

	}

	private class FoundSetCreator implements IScopeTypeCreator
	{
		private Type cachedSuperTypeTemplateType = null;

		public Type createType(String fullTypeName)
		{
			Type type;
			if (fullTypeName.equals(FoundSet.JS_FOUNDSET))
			{
				type = createBaseType(fullTypeName);

				// quickly add this one to the static types.
				return addType(null, type);
			}
			else
			{
				FlattenedSolution fs = TypeCreator.getFlattenedSolution();
				String config = fullTypeName.substring(fullTypeName.indexOf('<') + 1, fullTypeName.length() - 1);
				if (cachedSuperTypeTemplateType == null)
				{
					cachedSuperTypeTemplateType = createBaseType(FoundSet.JS_FOUNDSET);
				}
				EList<Member> members = cachedSuperTypeTemplateType.getMembers();
				List<Member> overwrittenMembers = new ArrayList<Member>();
				for (Member member : members)
				{
					JSType memberType = member.getType();
					if (memberType != null)
					{
						if (memberType.getName().equals("Array<" + Record.JS_RECORD + '>'))
						{
							overwrittenMembers.add(TypeCreator.clone(member, TypeUtil.arrayOf(Record.JS_RECORD + '<' + config + '>')));
						}
						else if (memberType.getName().equals(Record.JS_RECORD) || memberType.getName().equals(QBSelect.class.getSimpleName()))
						{
							overwrittenMembers.add(TypeCreator.clone(member, getTypeRef(memberType.getName() + '<' + config + '>')));
						}
						else if (memberType.getName().equals(FoundSet.JS_FOUNDSET))
						{
							if (member.getName().equals("unrelate"))
							{
								// its really a relation, unrelate it.
								if (fs != null)
								{
									Relation relation = fs.getRelation(config);
									if (relation != null)
									{
										overwrittenMembers.add(TypeCreator.clone(member,
											getTypeRef(FoundSet.JS_FOUNDSET + '<' + relation.getForeignDataSource() + '>')));
									}
									else
									{
										overwrittenMembers.add(TypeCreator.clone(member, getTypeRef(FoundSet.JS_FOUNDSET + '<' + config + '>')));
									}
								}
							}
							else
							{
								overwrittenMembers.add(TypeCreator.clone(member, getTypeRef(FoundSet.JS_FOUNDSET + '<' + config + '>')));
							}
						}
					}
				}
				type = getCombinedType(fullTypeName, config, overwrittenMembers, getType(FoundSet.JS_FOUNDSET), FOUNDSET_IMAGE, true);
			}
			return type;
		}

		/**
		 * @param context
		 * @param fullTypeName
		 * @return
		 */
		private Type createBaseType(String fullTypeName)
		{
			Type type;
			type = TypeCreator.this.createType(fullTypeName, FoundSet.class);
			//type.setAttribute(IMAGE_DESCRIPTOR, FOUNDSET_IMAGE);

			Property alldataproviders = TypeInfoModelFactory.eINSTANCE.createProperty();
			alldataproviders.setName("alldataproviders");
			alldataproviders.setDescription("the dataproviders array of this foundset");
			alldataproviders.setAttribute(IMAGE_DESCRIPTOR, SPECIAL_PROPERTY);
			type.getMembers().add(makeDeprected(alldataproviders));

			Property maxRecordIndex = TypeInfoModelFactory.eINSTANCE.createProperty();
			maxRecordIndex.setName("maxRecordIndex");
			type.getMembers().add(makeDeprected(maxRecordIndex));

			Property selectedIndex = TypeInfoModelFactory.eINSTANCE.createProperty();
			selectedIndex.setName("selectedIndex");
			type.getMembers().add(makeDeprected(selectedIndex));
			return type;
		}
	}

	private class JSDataSetCreator implements IScopeTypeCreator
	{

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.eclipse.debug.script.TypeCreator.IScopeTypeCreator#createType(org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext,
		 * java.lang.String)
		 */
		public Type createType(String fullTypeName)
		{
			Type type = getType("JSDataSet");
			int index = fullTypeName.indexOf('<');
			if (index != -1 && fullTypeName.endsWith(">"))
			{
				String recordType = fullTypeName.substring(index + 1, fullTypeName.length() - 1);
				Type t = getRecordType(recordType);
				t.setName(fullTypeName);
				t.setSuperType(type);
				type = t;
			}
			return type;
		}

	}


	private class RecordCreator extends FoundSetCreator
	{
		private Type cachedSuperTypeTemplateType;

		@Override
		public Type createType(String fullTypeName)
		{
			Type type;
			if (fullTypeName.equals(Record.JS_RECORD))
			{
				type = TypeCreator.this.createType(fullTypeName, Record.class);
				ImageDescriptor desc = IconProvider.instance().descriptor(Record.class);
				type.setAttribute(IMAGE_DESCRIPTOR, desc);
				// quickly add this one to the static types.
				return addType(null, type);
			}
			else
			{
				String config = fullTypeName.substring(fullTypeName.indexOf('<') + 1, fullTypeName.length() - 1);
				if (cachedSuperTypeTemplateType == null)
				{
					cachedSuperTypeTemplateType = createType(Record.JS_RECORD);
				}
				EList<Member> members = cachedSuperTypeTemplateType.getMembers();
				List<Member> overwrittenMembers = new ArrayList<Member>();
				for (Member member : members)
				{
					JSType memberType = member.getType();
					if (memberType != null)
					{
						if (memberType.getName().equals(FoundSet.JS_FOUNDSET))
						{
							overwrittenMembers.add(TypeCreator.clone(member, getTypeRef(FoundSet.JS_FOUNDSET + '<' + config + '>')));
						}
					}
				}
				type = getCombinedType(fullTypeName, config, overwrittenMembers, getType(Record.JS_RECORD), IconProvider.instance().descriptor(Record.class),
					true);
			}
			return type;
		}
	}

	private class PluginsScopeCreator implements IScopeTypeCreator
	{
		private final Map<String, Image> images = new ConcurrentHashMap<String, Image>();

		/**
		 * @see com.servoy.eclipse.debug.script.ElementResolver.IDynamicTypeCreator#getDynamicType()
		 */
		public Type createType(String fullTypeName)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(fullTypeName);
			type.setKind(TypeKind.JAVA);
			type.setAttribute(IMAGE_DESCRIPTOR, PLUGINS);

			EList<Member> members = type.getMembers();
			members.add(createProperty("allnames", true, TypeUtil.arrayOf("String"), "All plugin names as an array", SPECIAL_PROPERTY));
			members.add(createProperty("length", true, "Number", "Number of plugins", PROPERTY));

			IPluginManager pluginManager = com.servoy.eclipse.core.Activator.getDefault().getDesignClient().getPluginManager();
			List<IClientPlugin> clientPlugins = pluginManager.getPlugins(IClientPlugin.class);
			for (IClientPlugin clientPlugin : clientPlugins)
			{
				IScriptable scriptObject = null;
				try
				{
					java.lang.reflect.Method method = clientPlugin.getClass().getMethod("getScriptObject", (Class[])null);
					if (method != null)
					{
						scriptObject = (IScriptable)method.invoke(clientPlugin, (Object[])null);
					}
				}
				catch (Throwable t)
				{
					Debug.error("Could not get scriptobject from plugin " + clientPlugin.getName());
				}
				if (scriptObject != null)
				{
					ScriptObjectRegistry.registerScriptObjectForClass(scriptObject.getClass(), scriptObject);
					Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
					property.setName(clientPlugin.getName());
					property.setReadOnly(true);
					addAnonymousClassType("Plugin<" + clientPlugin.getName() + '>', scriptObject.getClass());
					property.setType(getTypeRef("Plugin<" + clientPlugin.getName() + '>'));

					if (clientPlugin.getName().equals("window"))
					{
						Property deprecatedPluginProperty = createProperty("kioskmode", true, getTypeRef("Plugin<" + clientPlugin.getName() + '>'),
							"Window plugin", null);
						deprecatedPluginProperty.setDeprecated(true);
						deprecatedPluginProperty.setVisible(false);
						members.add(deprecatedPluginProperty);
						deprecatedPluginProperty = createProperty("popupmenu", true, getTypeRef("Plugin<" + clientPlugin.getName() + '>'), "Window plugin",
							null);
						deprecatedPluginProperty.setDeprecated(true);
						deprecatedPluginProperty.setVisible(false);
						members.add(deprecatedPluginProperty);
						deprecatedPluginProperty = createProperty("menubar", true, getTypeRef("Plugin<" + clientPlugin.getName() + '>'), "Window plugin", null);
						deprecatedPluginProperty.setDeprecated(true);
						deprecatedPluginProperty.setVisible(false);
						members.add(deprecatedPluginProperty);
						deprecatedPluginProperty = createProperty("it2be_menubar", true, getTypeRef("Plugin<" + clientPlugin.getName() + '>'), "Window plugin",
							null);
						deprecatedPluginProperty.setDeprecated(true);
						deprecatedPluginProperty.setVisible(false);
						members.add(deprecatedPluginProperty);

					}

					Image clientImage = null;
					Icon icon = clientPlugin.getImage();
					if (icon != null)
					{
						clientImage = images.get(clientPlugin.getName());
						if (clientImage == null)
						{
							clientImage = UIUtils.getSWTImageFromSwingIcon(icon, Display.getDefault());
						}
						if (clientImage != null)
						{
							com.servoy.eclipse.debug.Activator.getDefault().registerImage(clientImage);
							images.put(clientPlugin.getName(), clientImage);
						}
					}
					if (clientImage == null)
					{
						property.setAttribute(IMAGE_DESCRIPTOR, PLUGIN_DEFAULT);
					}
					else
					{
						property.setAttribute(IMAGE_DESCRIPTOR, ImageDescriptor.createFromImage(clientImage));
					}

					members.add(property);
				}
			}
			// quickly add this one to the static types.
			return addType(null, type);
		}
	}

	private class FormScopeCreator implements IScopeTypeCreator
	{
		public Type createType(String typeName)
		{
			Type type;
			if (typeName.equals("Form") || typeName.equals("RuntimeForm"))
			{
				type = TypeInfoModelFactory.eINSTANCE.createType();
				type.setName("RuntimeForm");
				type.setKind(TypeKind.JAVA);

				EList<Member> members = type.getMembers();

				members.add(makeDeprected(createProperty("allnames", true, TypeUtil.arrayOf("String"), "Array with all the names in this form scope",
					SPECIAL_PROPERTY)));
				members.add(makeDeprected(createProperty("alldataproviders", true, TypeUtil.arrayOf("String"), "Array with all the dataprovider names",
					SPECIAL_PROPERTY)));
				members.add(makeDeprected(createProperty("allmethods", true, TypeUtil.arrayOf("String"), "Array with all the method names", SPECIAL_PROPERTY)));
				members.add(makeDeprected(createProperty("allrelations", true, TypeUtil.arrayOf("String"), "Array with all the relation names",
					SPECIAL_PROPERTY)));
				members.add(makeDeprected(createProperty("allvariables", true, TypeUtil.arrayOf("String"), "Array with all the variable names",
					SPECIAL_PROPERTY)));

				// controller and foundset and elements
				members.add(createProperty("controller", true, "Controller", IconProvider.instance().descriptor(JSForm.class)));
				members.add(createProperty("foundset", true, FoundSet.JS_FOUNDSET, FOUNDSET_IMAGE));
				members.add(createProperty("elements", true, "Elements", ELEMENTS));

				//type.setAttribute(IMAGE_DESCRIPTOR, FORM_IMAGE);
				// quickly add this one to the static types.
				return addType(null, type);
			}
			else
			{
				FlattenedSolution fs = getFlattenedSolution();
				if (fs == null) return null;
				String config = typeName.substring(typeName.indexOf('<') + 1, typeName.length() - 1);
				Form form = fs.getForm(config);
				if (form == null) return null;
				Form formToUse = fs.getFlattenedForm(form);
				Type superForm = getType("RuntimeForm");
				if (form.getExtendsID() > 0)
				{
					Form extendsForm = fs.getForm(form.getExtendsID());
					if (extendsForm != null) superForm = getType("RuntimeForm<" + extendsForm.getName() + '>');
				}

				String ds = formToUse.getDataSource();
				List<Member> overwrittenMembers = new ArrayList<Member>();

				if (ds != null || FormEncapsulation.hideFoundset(formToUse))
				{
					String foundsetType = FoundSet.JS_FOUNDSET;
					if (ds != null) foundsetType += '<' + ds + '>';
					Member clone = createProperty("foundset", true, foundsetType, FOUNDSET_IMAGE);
					overwrittenMembers.add(clone);
					clone.setVisible(!FormEncapsulation.hideFoundset(formToUse));
				}
				if (FormEncapsulation.hideController(formToUse))
				{
					Member clone = createProperty("controller", true, "Controller", IconProvider.instance().descriptor(JSForm.class));
					overwrittenMembers.add(clone);
					clone.setVisible(false);
				}
				if (FormEncapsulation.hideDataproviders(formToUse))
				{
					Member clone = createProperty("alldataproviders", true, TypeUtil.arrayOf("String"), "Array with all the dataprovider names",
						SPECIAL_PROPERTY);
					overwrittenMembers.add(clone);
					clone.setVisible(false);

					clone = createProperty("allrelations", true, TypeUtil.arrayOf("String"), "Array with all the relation names", SPECIAL_PROPERTY);
					overwrittenMembers.add(clone);
					clone.setVisible(false);
				}

				Member clone = createProperty("elements", true, "Elements<" + config + '>', ELEMENTS);
				overwrittenMembers.add(clone);
				clone.setVisible(!FormEncapsulation.hideElements(formToUse));

				type = getCombinedType(typeName, ds, overwrittenMembers, superForm, FORM_IMAGE, !FormEncapsulation.hideDataproviders(formToUse));
				if (type != null) type.setAttribute(LAZY_VALUECOLLECTION, form);
			}
			return type;
		}
	}

	private class QueryBuilderCreator implements IScopeTypeCreator
	{
		private Type cachedSuperTypeTemplateType = null;

		private final Map<String, Class< ? >> qbClasses = new ConcurrentHashMap<String, Class< ? >>();

		QueryBuilderCreator()
		{
			addClass(QBAggregate.class);
			addClass(QBColumn.class);
			addClass(QBColumns.class);
			addClass(QBCondition.class);
			addClass(QBFactory.class);
			addClass(QBFunction.class);
			addClass(QBGroupBy.class);
			addClass(QBJoin.class);
			addClass(QBJoins.class);
			addClass(QBLogicalCondition.class);
			addClass(QBResult.class);
			addClass(QBSelect.class);
			addClass(QBSort.class);
			addClass(QBSorts.class);
			addClass(QBPart.class);
			addClass(QBTableClause.class);
			addClass(QBParameter.class);
			addClass(QBParameters.class);
			addClass(QBFunctions.class);
		}

		private void addClass(Class< ? > clazz)
		{
			qbClasses.put(clazz.getSimpleName(), clazz);
		}

		public Type createType(String fullTypeName)
		{
			int indexOf = fullTypeName.indexOf('<');
			if (indexOf == -1)
			{
				Type type = createBaseType(fullTypeName);

				// quickly add this one to the static types.
				return addType(null, type);
			}

			String config = fullTypeName.substring(indexOf + 1, fullTypeName.length() - 1);
			if (cachedSuperTypeTemplateType == null)
			{
				cachedSuperTypeTemplateType = createType(fullTypeName.substring(0, indexOf));
			}
			EList<Member> members = cachedSuperTypeTemplateType.getMembers();
			List<Member> overwrittenMembers = new ArrayList<Member>();
			for (Member member : members)
			{
				JSType memberType = member.getType();
				if (memberType != null && qbClasses.containsKey(memberType.getName()))
				{
					overwrittenMembers.add(TypeCreator.clone(member, getTypeRef(memberType.getName() + '<' + config + '>')));
				}
			}

			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.getMembers().addAll(overwrittenMembers);
			type.setName(fullTypeName);
			type.setKind(TypeKind.JAVA);
//			type.setAttribute(IMAGE_DESCRIPTOR, imageDescriptor);
			type.setSuperType(cachedSuperTypeTemplateType);
			return type;
		}

		/**
		 * @param context
		 * @param fullTypeName
		 * @return
		 */
		private Type createBaseType(String fullTypeName)
		{
			Class< ? > cls = qbClasses.get(fullTypeName);
			Type type = TypeCreator.this.createType(fullTypeName, cls);
			String superclass = cls.getSuperclass().getSimpleName();
			if (qbClasses.containsKey(superclass))
			{
				type.setSuperType(getType(superclass));
			}
			return type;

		}
	}

	private class QueryBuilderColumnsCreator implements IScopeTypeCreator
	{

		public Type createType(String typeName)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(typeName);
			type.setKind(TypeKind.JAVA);

			TypeConfig fsAndTable = getFlattenedSolutonAndTable(typeName);
			if (fsAndTable != null && fsAndTable.table != null)
			{
				addDataProviders(fsAndTable.table.getColumns().iterator(), type.getMembers(), fsAndTable.table.getDataSource());
				return addType(SCOPE_QBCOLUMNS, type);
			}

			return type;
		}

		private void addDataProviders(Iterator< ? extends IDataProvider> dataproviders, EList<Member> members, String dataSource)
		{
			while (dataproviders.hasNext())
			{
				IDataProvider provider = dataproviders.next();
				Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
				property.setName(provider.getDataProviderID());
				property.setAttribute(RESOURCE, provider);
				property.setVisible(true);
				property.setType(getTypeRef(QBColumn.class.getSimpleName() + '<' + dataSource + '>'));
				ImageDescriptor image = COLUMN_IMAGE;
				String description = "Column";
				if (provider instanceof AggregateVariable)
				{
					image = COLUMN_AGGR_IMAGE;
					description = "Aggregate (" + ((AggregateVariable)provider).getRootObject().getName() + ")";
				}
				else if (provider instanceof ScriptCalculation)
				{
					image = COLUMN_CALC_IMAGE;
					description = "Calculation (" + ((ScriptCalculation)provider).getRootObject().getName() + ")";
				}
				property.setAttribute(IMAGE_DESCRIPTOR, image);
				property.setDescription(description.intern());
				members.add(property);
			}
		}
	}

	private class QueryBuilderJoinsCreator extends QueryBuilderCreator
	{
		@Override
		public Type createType(String fullTypeName)
		{
			Type type = super.createType(fullTypeName);

			TypeConfig fsAndTable = getFlattenedSolutonAndTable(fullTypeName);
			FlattenedSolution fs = getFlattenedSolution();
			if (fsAndTable != null && fs != null && fsAndTable.table != null)
			{
				try
				{
					Iterator<Relation> relations = fs.getRelations(fsAndTable.table, true, false, false, false, false);
					while (relations.hasNext())
					{
						try
						{
							Relation relation = relations.next();
							if (relation.isValid())
							{
								Property property = createProperty(relation.getName(), true,
									getTypeRef(QBJoin.class.getSimpleName() + '<' + relation.getForeignDataSource() + '>'),
									getRelationDescription(relation, relation.getPrimaryDataProviders(fs), relation.getForeignColumns()), RELATION_IMAGE,
									relation);
								property.setVisible(true);
								type.getMembers().add(property);
							}
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
						}
					}
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}

			return type;
		}
	}

	private class InvisibleRelationsScopeCreator extends RelationsScopeCreator
	{
		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.eclipse.debug.script.TypeProvider.RelationsScopeCreator#isVisible()
		 */
		@Override
		protected boolean isVisible()
		{
			return false;
		}
	}

	private class RelationsScopeCreator implements IScopeTypeCreator
	{
		public Type createType(String typeName)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(typeName);
			type.setKind(TypeKind.JAVA);

			TypeConfig fsAndTable = getFlattenedSolutonAndTable(typeName);
			if (fsAndTable != null && fsAndTable.flattenedSolution != null)
			{
				// do not set Relations<solutionName> as supertype when table is not null, if you do then in a table 
				// context (like forms.x.foundset) global relations show up.
				try
				{
					addRelations(fsAndTable.flattenedSolution, fsAndTable.scopeName, type.getMembers(),
						fsAndTable.flattenedSolution.getRelations(fsAndTable.table, true, false, fsAndTable.table == null, false, false), isVisible());
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}

			return type;
		}

		protected boolean isVisible()
		{
			return true;
		}
	}

	private class InvisibleDataprovidersScopeCreator extends DataprovidersScopeCreator
	{
		@Override
		protected boolean isVisible()
		{
			return false;
		}

	}

	private class DataprovidersScopeCreator implements IScopeTypeCreator
	{
		public Type createType(String typeName)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(typeName);
			type.setKind(TypeKind.JAVA);

			TypeConfig fsAndTable = getFlattenedSolutonAndTable(typeName);
			if (fsAndTable != null && fsAndTable.table != null)
			{
				if (fsAndTable.flattenedSolution != null)
				{
					type.setSuperType(getType(typeName.substring(0, typeName.indexOf('<') + 1) + fsAndTable.table.getDataSource() + '>'));
					try
					{
						Map<String, IDataProvider> allDataProvidersForTable = fsAndTable.flattenedSolution.getAllDataProvidersForTable(fsAndTable.table);
						if (allDataProvidersForTable != null)
						{
							addDataProviders(allDataProvidersForTable.values().iterator(), type.getMembers(), fsAndTable.table.isMarkedAsHiddenInDeveloper(),
								isVisible(), false);
						}
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError(e);
					}
				}
				else
				{
					addDataProviders(fsAndTable.table.getColumns().iterator(), type.getMembers(), false, isVisible(), true);
					return addType(SCOPE_TABLES, type);

				}
			}

			return type;
		}

		private void addDataProviders(Iterator< ? extends IDataProvider> dataproviders, EList<Member> members, boolean hiddenTable, boolean visible,
			boolean columnsOnly)
		{
			while (dataproviders.hasNext())
			{
				IDataProvider provider = dataproviders.next();
				boolean uuid = false;
				if (columnsOnly)
				{
					if (provider instanceof AggregateVariable || provider instanceof ScriptCalculation) continue;
					if (provider instanceof Column)
					{
						ColumnInfo ci = ((Column)provider).getColumnInfo();
						if (ci != null && ci.isExcluded()) continue;
						if (ci != null && ci.hasFlag(Column.UUID_COLUMN))
						{
							uuid = true;
						}
					}
				}
				else if (provider instanceof Column) continue;

				Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
				property.setName(provider.getDataProviderID());
				property.setAttribute(RESOURCE, provider);
				property.setVisible(visible);

				ComponentFormat componentFormat = ComponentFormat.getComponentFormat(null, provider,
					com.servoy.eclipse.core.Activator.getDefault().getDesignClient());
				switch (componentFormat.dpType)
				{
					case IColumnTypes.DATETIME :
						property.setType(getTypeRef("Date"));
						break;

					case IColumnTypes.INTEGER :
					case IColumnTypes.NUMBER :
						property.setType(getTypeRef("Number"));
						break;

					case IColumnTypes.TEXT :
						property.setType(getTypeRef("String"));
						break;

					case IColumnTypes.MEDIA :
						// for now don't return a type (so that anything is valid)
						// mamybe we should return Array<byte> but then we also have to check column converters.
						// should be in sync with TypeCreater.getDataProviderType
//						property.setType(TypeUtil.arrayOf("byte"));
						break;
				}
				if (uuid)
				{
					property.setType(getTypeRef("UUID"));
				}
				ImageDescriptor image = COLUMN_IMAGE;
				String description = "Column";
				if (provider instanceof AggregateVariable)
				{
					image = COLUMN_AGGR_IMAGE;
					description = "Aggregate (" + ((AggregateVariable)provider).getRootObject().getName() + ")".intern();
				}
				else if (provider instanceof ScriptCalculation)
				{
					image = COLUMN_CALC_IMAGE;
					description = "Calculation (" + ((ScriptCalculation)provider).getRootObject().getName() + ")".intern();
				}
				if (provider instanceof Column && ((Column)provider).getColumnInfo() != null)
				{
					String columnDesc = ((Column)provider).getColumnInfo().getDescription();
					if (columnDesc != null)
					{
						description += "<br/>" + columnDesc.replace("\n", "<br/>");
					}
				}
				property.setAttribute(IMAGE_DESCRIPTOR, image);
				property.setDescription(description);
				if (hiddenTable)
				{
					property.setDeprecated(true);
					property.setDescription(property.getDescription() + " <b>of table marked as HIDDEN in developer</b>");
				}
				members.add(property);
			}
		}

		protected boolean isVisible()
		{
			return true;
		}
	}

	/**
	 * Parse the config for a type. Possible combinations: 
	 *
	 * <br>* Typename&lt;solutionName;dataSource&gt;
	 * 
	 * <br>* Typename&lt;dataSource&gt;
	 * 
	 * <br>* Typename&lt;solutionName&gt;
	 * 
	 * <br>* Typename&lt;solutionName/scopeName&gt;
	 */
	private static TypeConfig getFlattenedSolutonAndTable(String typeName)
	{
		if (typeName.endsWith(">"))
		{
			IServoyModel servoyModel = ServoyModelFinder.getServoyModel();
			int index = typeName.indexOf('<');
			if (index > 0)
			{
				String config = typeName.substring(index + 1, typeName.length() - 1);
				int sep = config.indexOf(';');
				if (sep > 0)
				{
					// solutionName;dataSource
					ServoyProject servoyProject = servoyModel.getServoyProject(config.substring(0, sep));
					if (servoyProject != null && servoyModel.isSolutionActive(servoyProject.getProject().getName()) &&
						servoyModel.getFlattenedSolution().getSolution() != null)
					{
						try
						{
							FlattenedSolution fs = servoyProject.getEditingFlattenedSolution();
							String[] dbServernameTablename = DataSourceUtils.getDBServernameTablename(config.substring(sep + 1));
							if (dbServernameTablename != null)
							{
								IServer server = fs.getSolution().getRepository().getServer(dbServernameTablename[0]);
								if (server != null)
								{
									Table table = (Table)server.getTable(dbServernameTablename[1]);
									if (table != null)
									{
										return new TypeConfig(fs, table);
									}
								}
							}
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
						}
					}
				}
				else
				{
					// this is only dataSource or solutionname[/scope]
					if (servoyModel.getFlattenedSolution().getSolution() != null)
					{
						String[] dbServernameTablename = DataSourceUtils.getDBServernameTablename(config);
						if (dbServernameTablename != null)
						{
							try
							{
								IServer server = servoyModel.getFlattenedSolution().getSolution().getRepository().getServer(dbServernameTablename[0]);
								if (server != null)
								{
									Table table = (Table)server.getTable(dbServernameTablename[1]);
									if (table != null)
									{
										return new TypeConfig(table);
									}
								}
							}
							catch (Exception e)
							{
								ServoyLog.logError(e);
							}
						}
						else
						{
							// solutionName[/scopeName]
							String[] split = config.split("/");
							ServoyProject servoyProject = servoyModel.getServoyProject(split[0]);
							if (servoyProject != null && servoyModel.isSolutionActive(servoyProject.getProject().getName()) &&
								servoyModel.getFlattenedSolution().getSolution() != null)
							{
								return new TypeConfig(servoyProject.getEditingFlattenedSolution(), split.length == 1 ? null : split[1]);
							}
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * @param createProperty
	 * @return
	 */
	private static Member makeDeprected(Property property)
	{
		property.setDeprecated(true);
		property.setVisible(false);
		return property;
	}


	public class ElementsScopeCreator implements IScopeTypeCreator
	{
		private final Map<String, String> typeNames = new ConcurrentHashMap<String, String>();

		private ElementsScopeCreator()
		{
			typeNames.put(IRuntimeButton.class.getSimpleName(), "RuntimeButton");
			addType("RuntimeButton", IRuntimeButton.class);
			typeNames.put(IRuntimeDataButton.class.getSimpleName(), "RuntimeDataButton");
			addType("RuntimeDataButton", IRuntimeDataButton.class);
			typeNames.put(IScriptScriptLabelMethods.class.getSimpleName(), "RuntimeLabel");
			addType("RuntimeLabel", IScriptScriptLabelMethods.class);
			typeNames.put(IScriptDataLabelMethods.class.getSimpleName(), "RuntimeDataLabel");
			addType("RuntimeDataLabel", IScriptDataLabelMethods.class);
			typeNames.put(IRuntimePassword.class.getSimpleName(), "RuntimePassword");
			addType("RuntimePassword", IRuntimePassword.class);
			typeNames.put(IRuntimeHtmlArea.class.getSimpleName(), "RuntimeHtmlArea");
			addType("RuntimeHtmlArea", IRuntimeHtmlArea.class);
			typeNames.put(IRuntimeRtfArea.class.getSimpleName(), "RuntimeRtfArea");
			addType("RuntimeRtfArea", IRuntimeRtfArea.class);
			typeNames.put(IRuntimeTextArea.class.getSimpleName(), "RuntimeTextArea");
			addType("RuntimeTextArea", IRuntimeTextArea.class);
			typeNames.put(IRuntimeChecks.class.getSimpleName(), "RuntimeChecks");
			addType("RuntimeChecks", IRuntimeChecks.class);
			typeNames.put(IRuntimeCheck.class.getSimpleName(), "RuntimeCheck");
			addType("RuntimeCheck", IRuntimeCheck.class);
			typeNames.put(IRuntimeRadios.class.getSimpleName(), "RuntimeRadios");
			addType("RuntimeRadios", IRuntimeRadios.class);
			typeNames.put(IRuntimeRadio.class.getSimpleName(), "RuntimeRadio");
			addType("RuntimeRadio", IRuntimeRadio.class);
			typeNames.put(IRuntimeCombobox.class.getSimpleName(), "RuntimeComboBox");
			addType("RuntimeComboBox", IRuntimeCombobox.class);
			typeNames.put(IRuntimeCalendar.class.getSimpleName(), "RuntimeCalendar");
			addType("RuntimeCalendar", IRuntimeCalendar.class);
			typeNames.put(IRuntimeImageMedia.class.getSimpleName(), "RuntimeImageMedia");
			addType("RuntimeImageMedia", IRuntimeImageMedia.class);
			typeNames.put(IRuntimeTextField.class.getSimpleName(), "RuntimeTypeAhead");
			typeNames.put(IRuntimeTextField.class.getSimpleName(), "RuntimeTextField");
			addType("RuntimeTextField", IRuntimeTextField.class);
			typeNames.put(IScriptTabPanelMethods.class.getSimpleName(), "RuntimeTabPanel");
			addType("RuntimeTabPanel", IScriptTabPanelMethods.class);
			typeNames.put(IRuntimeSplitPane.class.getSimpleName(), "RuntimeSplitPane");
			addType("RuntimeSplitPane", IRuntimeSplitPane.class);
			typeNames.put(IScriptPortalComponentMethods.class.getSimpleName(), "RuntimePortal");
			addType("RuntimePortal", IScriptPortalComponentMethods.class);
			typeNames.put(IRuntimeListBox.class.getSimpleName(), "RuntimeListBox");
			addType("RuntimeListBox", IRuntimeListBox.class);
			typeNames.put(IScriptAccordionPanelMethods.class.getSimpleName(), "RuntimeAccordionPanel");
			addType("RuntimeAccordionPanel", IScriptAccordionPanelMethods.class);
			typeNames.put(IRuntimeSpinner.class.getSimpleName(), "RuntimeSpinner");
			addType("RuntimeSpinner", IRuntimeSpinner.class);
			addType("RuntimeComponent", IRuntimeComponent.class);
		}

		public Type createType(String typeName)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(typeName);
			type.setKind(TypeKind.JAVA);
			type.setAttribute(IMAGE_DESCRIPTOR, ELEMENTS);
			if (typeName.equals("Elements"))
			{
				EList<Member> members = type.getMembers();
				members.add(createProperty("allnames", true, TypeUtil.arrayOf("String"), "Array with all the element names", SPECIAL_PROPERTY));
				members.add(createProperty("length", true, "Number", PROPERTY));
				Property arrayProp = createProperty("[]", true, "RuntimeComponent", PROPERTY);
				arrayProp.setVisible(false);
				members.add(arrayProp);
				// quickly add this one to the static types.
				return addType(null, type);
			}
			else
			{
				FlattenedSolution fs = getFlattenedSolution();
				if (fs != null)
				{
					String config = typeName.substring(typeName.indexOf('<') + 1, typeName.length() - 1);
					Form form = fs.getForm(config);
					if (form != null)
					{
						type.setSuperType(getType("Elements"));
						try
						{
							EList<Member> members = type.getMembers();
							Form formToUse = form;
							if (form.getExtendsID() > 0)
							{
								formToUse = fs.getFlattenedForm(form);
							}
							IApplication application = com.servoy.eclipse.core.Activator.getDefault().getDesignClient();
							Iterator<IPersist> formObjects = formToUse.getAllObjects();
							createFormElementProperties(application, members, formObjects);
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
						}
					}
				}
			}
			return type;
		}

		/**
		 * @param application
		 * @param context
		 * @param members
		 * @param formObjects
		 */
		private void createFormElementProperties(IApplication application, EList<Member> members, Iterator<IPersist> formObjects)
		{
			while (formObjects.hasNext())
			{
				IPersist persist = formObjects.next();
				if (persist instanceof IFormElement)
				{
					IFormElement formElement = (IFormElement)persist;
					if (!Utils.stringIsEmpty(formElement.getName()))
					{
						Class< ? > persistClass = ElementUtil.getPersistScriptClass(application, persist);
						if (persistClass != null && formElement instanceof Bean)
						{
							String beanClassName = ((Bean)formElement).getBeanClassName();
							if (beanClassName != null)
							{
								// map the persist class that is registered in the initialize() method under the beanclassname under that same name.
								// So SwingDBTreeView class/name points to "DBTreeView" which points to that class again of the class types 
								typeNames.put(persistClass.getSimpleName(), beanClassName.substring(beanClassName.lastIndexOf('.') + 1));
							}
						}
						members.add(createProperty(formElement.getName(), true, getElementType(persistClass), null, PROPERTY));
					}
					if (formElement.getGroupID() != null)
					{
						String groupName = FormElementGroup.getName(formElement.getGroupID());
						if (groupName != null)
						{
							members.add(createProperty(groupName, true, getElementType(RuntimeGroup.class), null, PROPERTY));
						}
					}
					if (formElement instanceof Portal)
					{
						createFormElementProperties(application, members, ((Portal)formElement).getAllObjects());
					}
				}
			}
		}

		private SimpleType getElementType(Class< ? > cls)
		{
			if (cls == null) return null;
			String name = typeNames.get(cls.getSimpleName());
			if (name == null)
			{
				Debug.log("no element name found for " + cls.getSimpleName()); // TODO make trace, this will always be hit by beans.
				name = cls.getSimpleName();
				addAnonymousClassType(name, cls);
			}
			return getTypeRef(name);
		}

	}

	/**
	 * @param context
	 * @param fs
	 * @param members
	 * @param relations
	 * @param visible 
	 * @throws RepositoryException
	 */
	private void addRelations(FlattenedSolution fs, String scopeName, EList<Member> members, Iterator<Relation> relations, boolean visible)
	{
		while (relations.hasNext())
		{
			try
			{
				Relation relation = relations.next();
				// show only relations that are valid and defined for this scope
				if (relation.isValid() && (scopeName == null || relation.usesScope(scopeName)))
				{
					Property property = createProperty(relation.getName(), true, getTypeRef(FoundSet.JS_FOUNDSET + '<' + relation.getName() + '>'),
						getRelationDescription(relation, relation.getPrimaryDataProviders(fs), relation.getForeignColumns()), RELATION_IMAGE, relation);
					if (visible)
					{
						IServerInternal sp = ((IServerInternal)relation.getPrimaryServer());
						IServerInternal sf = ((IServerInternal)relation.getForeignServer());
						if ((sp != null && sp.isTableMarkedAsHiddenInDeveloper(relation.getPrimaryTableName())) ||
							(sf != null && sf.isTableMarkedAsHiddenInDeveloper(relation.getForeignTableName())))
						{
							property.setDeprecated(true);
							property.setDescription(property.getDescription() + "<br><b>This relation is based on a table marked as HIDDEN in developer</b>.");
						}
					}

					property.setVisible(visible);
					members.add(property);
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	private static final ConcurrentMap<Relation, String> relationCache = new ConcurrentHashMap<Relation, String>(64, 0.9f, 16);

	static String getRelationDescription(Relation relation, IDataProvider[] primaryDataProviders, Column[] foreignColumns)
	{
		String str = relationCache.get(relation);
		if (str != null) return str;
		StringBuilder sb = new StringBuilder(150);
		if (relation.isGlobal())
		{
			sb.append("Global relation defined in solution: "); //$NON-NLS-1$
		}
		else if (primaryDataProviders.length == 0)
		{
			sb.append("Self referencing relation defined in solution:"); //$NON-NLS-1$
		}
		else
		{
			sb.append("Relation defined in solution: "); //$NON-NLS-1$
		}
		sb.append(relation.getRootObject().getName());
		if (relation.isGlobal() || primaryDataProviders.length == 0)
		{
			sb.append("<br/>On table: "); //$NON-NLS-1$
			sb.append(relation.getForeignServerName() + "->" + relation.getForeignTableName()); //$NON-NLS-1$
		}
		else
		{
			sb.append("<br/>From: "); //$NON-NLS-1$
//			sb.append(relation.getPrimaryDataSource());
			sb.append(relation.getPrimaryServerName() + " -> " + relation.getPrimaryTableName()); //$NON-NLS-1$
			sb.append("<br/>To: "); //$NON-NLS-1$
			sb.append(relation.getForeignServerName() + " -> " + relation.getForeignTableName()); //$NON-NLS-1$
		}
		sb.append("<br/>"); //$NON-NLS-1$
		if (primaryDataProviders.length != 0)
		{
			for (int i = 0; i < foreignColumns.length; i++)
			{
				sb.append("&nbsp;&nbsp;"); //$NON-NLS-1$
				if (primaryDataProviders[i] instanceof LiteralDataprovider)
				{
					sb.append(((LiteralDataprovider)primaryDataProviders[i]).getValue());
					sb.append("&nbsp;"); //$NON-NLS-1$
					sb.append(RelationItem.getOperatorAsString(relation.getOperators()[i]));
					sb.append("&nbsp;"); //$NON-NLS-1$
				}
				else
				{
					sb.append((primaryDataProviders[i] != null) ? primaryDataProviders[i].getDataProviderID() : "unresolved");
					sb.append(" -> "); //$NON-NLS-1$
				}
				sb.append((foreignColumns[i] != null) ? foreignColumns[i].getDataProviderID() : "unresolved");
				sb.append("<br/>"); //$NON-NLS-1$
			}
		}
		str = sb.toString();
		relationCache.put(relation, str);
		return str;
	}


	/**
	 * @param member
	 * @param config
	 * @return
	 */
	private static Member clone(Member member, JSType type)
	{
		Member clone = null;
		if (member instanceof Property)
		{
			Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
			property.setReadOnly(((Property)member).isReadOnly());
			clone = property;
		}
		else
		{
			org.eclipse.dltk.javascript.typeinfo.model.Method method = TypeInfoModelFactory.eINSTANCE.createMethod();
			EList<Parameter> cloneParameters = method.getParameters();
			EList<Parameter> parameters = ((org.eclipse.dltk.javascript.typeinfo.model.Method)member).getParameters();
			for (Parameter parameter : parameters)
			{
				cloneParameters.add(clone(parameter));
			}
			clone = method;
		}

		EMap<String, Object> attributes = member.getAttributes();
		for (Entry<String, Object> entry : attributes)
		{
			clone.setAttribute(entry.getKey(), entry.getValue());
		}
		clone.setDeprecated(member.isDeprecated());
		clone.setStatic(member.isStatic());
		clone.setVisible(member.isVisible());
		clone.setDescription(member.getDescription());
		clone.setName(member.getName());
		if (type == null)
		{
			if (member.getDirectType() != null)
			{
				SimpleType typeRef = TypeInfoModelFactory.eINSTANCE.createSimpleType();
				typeRef.setTarget(member.getDirectType());
				clone.setType(typeRef);
			}
		}
		else
		{
			clone.setType(type);
		}

		return clone;
	}

	/**
	 * @param parameter
	 * @return
	 */
	private static Parameter clone(Parameter parameter)
	{
		Parameter clone = TypeInfoModelFactory.eINSTANCE.createParameter();
		clone.setKind(parameter.getKind());
		clone.setName(parameter.getName());
		if (parameter.getDirectType() != null)
		{
			SimpleType typeRef = TypeInfoModelFactory.eINSTANCE.createSimpleType();
			typeRef.setTarget(parameter.getDirectType());
			clone.setType(typeRef);
		}
		return clone;
	}

	private Type getCombinedType(String fullTypeName, String config, List<Member> members, Type superType, ImageDescriptor imageDescriptor, boolean visible)
	{
		if (config == null)
		{
			Type type = TypeInfoModelFactory.eINSTANCE.createType();
			type.setName(fullTypeName);
			type.setKind(TypeKind.JAVA);
			type.setAttribute(IMAGE_DESCRIPTOR, imageDescriptor);
			type.setSuperType(superType);
			type.getMembers().addAll(members);
			return type;
		}

		FlattenedSolution fs = getFlattenedSolution();
		if (fs == null) return null;


		String serverName = null;
		String tableName = null;
		String[] serverAndTableName = DataSourceUtils.getDBServernameTablename(config);
		if (serverAndTableName != null)
		{
			serverName = serverAndTableName[0];
			tableName = serverAndTableName[1];
		}
		else
		{
			int index = config.indexOf('.');
			if (index != -1)
			{
				// table foundset
				serverName = config.substring(0, index);
				tableName = config.substring(index + 1);
			}
		}

		Table table = null;
		if (serverName != null)
		{
			try
			{
				IServer server = fs.getSolution().getRepository().getServer(serverName);
				if (server != null)
				{
					table = (Table)server.getTable(tableName);
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
			if (table == null) return null;
		}
		else
		{
			// relation
			try
			{
				Relation relation = fs.getRelation(config);
				if (relation != null && relation.isValid())
				{
					table = relation.getForeignTable();
					superType = getType(superType.getName() + '<' + table.getDataSource() + '>');
					table = null;
				}
				else return null;
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}

		Type type;
		if (table == null && config.startsWith("{") && config.endsWith("}"))
		{
			type = getRecordType(config);
		}
		else
		{
			type = TypeInfoModelFactory.eINSTANCE.createType();
		}
		type.getMembers().addAll(members);
		type.setAttribute(IMAGE_DESCRIPTOR, imageDescriptor);
		type.setSuperType(superType);
		type.setName(fullTypeName);
		type.setKind(TypeKind.JAVA);

		if (table != null)
		{
			String traitsConfig = fs.getSolution().getName() + ';' + table.getDataSource();
			String relationsType = "Relations<" + traitsConfig + '>';
			String dataproviderType = "Dataproviders<" + traitsConfig + '>';
			if (!visible)
			{
				relationsType = "Invisible" + relationsType;
				dataproviderType = "Invisible" + dataproviderType;
			}

			EList<Type> traits = type.getTraits();
			traits.add(getType(dataproviderType));
			traits.add(getType(relationsType));

			if (table.isMarkedAsHiddenInDeveloper())
			{
				type.setDescription("<b>Based on a table that is marked as HIDDEN in developer</b>");
				type.setDeprecated(true);
			}
		}

		return type;
	}

	public static class TypeConfig
	{
		public final FlattenedSolution flattenedSolution;
		public final Table table;
		public final String scopeName;

		public TypeConfig(FlattenedSolution flattenedSolution, String scopeName, Table table)
		{
			this.flattenedSolution = flattenedSolution;
			this.scopeName = scopeName;
			this.table = table;
		}

		public TypeConfig(FlattenedSolution flattenedSolution, Table table)
		{
			this(flattenedSolution, null, table);
		}

		public TypeConfig(FlattenedSolution flattenedSolution, String scopeName)
		{
			this(flattenedSolution, scopeName, null);
		}

		public TypeConfig(Table table)
		{
			this(null, null, table);
		}
	}

}
