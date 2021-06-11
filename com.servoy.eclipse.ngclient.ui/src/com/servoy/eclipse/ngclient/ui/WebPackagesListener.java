/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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

package com.servoy.eclipse.ngclient.ui;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sablo.specification.Package.DirPackageReader;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.Package.ZipPackageReader;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.WebServiceSpecProvider;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.ngpackages.ILoadedNGPackagesListener;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ngclient.ui.utils.ZipUtils;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompager
 * @since 2021.06
 */
public class WebPackagesListener implements ILoadedNGPackagesListener
{
	/**
	 * @author jcompanger
	 * @since 2021.06
	 */
	private static final class PackageCheckerJob extends Job
	{
		/**
		 * @param name
		 */
		private PackageCheckerJob(String name)
		{
			super(name);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor)
		{
			IOConsoleOutputStream console = Activator.getInstance().getConsole().newOutputStream();
			try
			{
				scheduled.incrementAndGet();
				long time = System.currentTimeMillis();
				writeConsole(console, "Starting ngclient source check");
				File projectFolder = Activator.getInstance().getProjectFolder();
				Set<String> packageToInstall = new HashSet<>();
				// service are based just on all service specifications
				Map<WebObjectSpecification, IPackageReader> ng2Services = new TreeMap<>((spec1, spec2) -> spec1.getName().compareTo(spec2.getName()));
				SpecProviderState specProviderState = WebServiceSpecProvider.getSpecProviderState();
				WebObjectSpecification[] allServices = specProviderState.getAllWebComponentSpecifications();
				for (WebObjectSpecification webObjectSpecification : allServices)
				{
					if (!webObjectSpecification.getNG2Config().isNull("packageName"))
					{
						IPackageReader packageReader = specProviderState
							.getPackageReader(webObjectSpecification.getPackageName());
						ng2Services.put(webObjectSpecification, packageReader);
					}
				}

				// modules and css of the components those are based on the Packages itself
				TreeSet<String> cssLibs = new TreeSet<>();
				TreeMap<PackageSpecification<WebObjectSpecification>, IPackageReader> componentSpecToReader = new TreeMap<>(
					(spec1, spec2) -> spec1.getPackageName().compareTo(spec2.getPackageName()));
				SpecProviderState componentsSpecProviderState = WebComponentSpecProvider.getSpecProviderState();
				for (PackageSpecification<WebObjectSpecification> entry : componentsSpecProviderState.getWebObjectSpecifications().values())
				{
					String module = entry.getNg2Module();
					String packageName = entry.getNpmPackageName();
					if (!Utils.stringIsEmpty(module) && !Utils.stringIsEmpty(packageName))
					{
						IPackageReader packageReader = componentsSpecProviderState.getPackageReader(entry.getPackageName());
						componentSpecToReader.put(entry, packageReader);
					}

					List<String> libs = entry.getNg2CssDesignLibrary();
					if (libs != null)
					{
						cssLibs.addAll(libs);
					}
				}

				boolean sourceChanged = false;
				if (ng2Services.size() > 0 || componentSpecToReader.size() > 0)
				{
					try
					{
						File packageJson = new File(projectFolder, "package.json");
						String json = FileUtils.readFileToString(packageJson, "UTF-8");
						JSONObject jsonObject = new JSONObject(json);
						JSONObject dependencies = jsonObject.optJSONObject("dependencies");
						ng2Services.entrySet().forEach(entry -> {
							WebObjectSpecification spec = entry.getKey();
							JSONObject ng2Config = spec.getNG2Config();
							String packageName = ng2Config.optString("packageName");
							IPackageReader packageReader = entry.getValue();
							String entryPoint = ng2Config.optString("entryPoint", null);
							String pck = checkPackage(dependencies, packageName, packageReader, entryPoint);
							if (pck != null)
							{
								writeConsole(console, "need to install package " + pck);
								packageToInstall.add(pck);
							}
						});

						componentSpecToReader.entrySet().forEach(entry -> {
							PackageSpecification<WebObjectSpecification> spec = entry.getKey();
							String packageName = spec.getNpmPackageName();
							IPackageReader packageReader = entry.getValue();
							String entryPoint = spec.getEntryPoint();
							String pck = checkPackage(dependencies, packageName, packageReader, entryPoint);
							if (pck != null)
							{
								packageToInstall.add(pck);
							}
						});
					}
					catch (IOException e)
					{
						ServoyLog.logError(e);
					}
				}

				// adjust the allservices.sevice.ts
				try
				{
					StringBuilder imports = new StringBuilder("// generated imports start\n");
					StringBuilder services = new StringBuilder("// generated services start\n");
					StringBuilder providers = new StringBuilder("// generated providers start\n");
					StringBuilder modules = new StringBuilder("// generated modules start\n");
					ng2Services.keySet().forEach(service -> {
						if (service.getNG2Config().has("serviceName"))
						{
							// import the service
							imports.append("import { ");
							String serviceName = service.getNG2Config().optString("serviceName");
							imports.append(serviceName);
							imports.append(" } from '");
							imports.append(service.getNG2Config().optString("packageName"));
							imports.append("';\n");
							// add it to the service declarations in the constructor
							services.append("private ");
							services.append(service.getName());
							services.append(": ");
							services.append(serviceName);
							services.append(",\n");
							// add it to the providers (if it is not a module)
							if (!"aggridservice".equals(service.getPackageName()))
							{
								providers.append(serviceName);
								providers.append(",\n");
							}
							// add it to the modules
							if (service.getNG2Config().has("moduleName"))
							{
								modules.append(service.getNG2Config().getString("moduleName"));
								modules.append(",\n");
							}
						}
					});
					imports.append("// generated imports end");
					services.append("// generated services end");
					providers.append("// generated providers end");
					modules.append("// generated modules end");

					String content = FileUtils.readFileToString(new File(projectFolder, "src/ngclient/allservices.service.ts"), "UTF-8");
					String old = content;
					content = replace(content, "// generated imports start", "// generated imports end", imports);
					content = replace(content, "// generated services start", "// generated services end", services);
					content = replace(content, "// generated providers start", "// generated providers end", providers);
					content = replace(content, "// generated modules start", "// generated modules end", modules);
					if (!old.equals(content))
					{
						sourceChanged = true;
						writeConsole(console, "services ts file changed");
						FileUtils.writeStringToFile(new File(projectFolder, "src/ngclient/allservices.service.ts"), content, "UTF-8");
					}
				}
				catch (IOException e)
				{
					ServoyLog.logError(e);
				}

				ComponentTemplateGenerator generator = new ComponentTemplateGenerator();
				Pair<StringBuilder, StringBuilder> componentTemplates = generator.generateHTMLTemplate();
				try
				{
					// adjust component templates
					String content = FileUtils.readFileToString(new File(projectFolder, "src/ngclient/form/form_component.component.ts"), "UTF-8");
					String old = content;
					content = replace(content, "<!-- component template generate start -->", "<!-- component template generate end -->",
						componentTemplates.getLeft());
					content = replace(content, "// component viewchild template generate start", "// component viewchild template generate end",
						componentTemplates.getRight());
					if (!old.equals(content))
					{
						sourceChanged = true;
						writeConsole(console, "components ts file changed");
						FileUtils.writeStringToFile(new File(projectFolder, "src/ngclient/form/form_component.component.ts"), content, "UTF-8");
					}

				}
				catch (IOException e1)
				{
					ServoyLog.logError(e1);
				}

				try
				{


					// generate the all components.module.ts
					StringBuilder allComponentsModule = new StringBuilder(256);
					allComponentsModule.append("import { NgModule } from '@angular/core';\n");
					componentSpecToReader.keySet().forEach(spec -> {
						allComponentsModule.append("import { ");
						allComponentsModule.append(spec.getNg2Module());
						allComponentsModule.append(" } from '");
						allComponentsModule.append(spec.getNpmPackageName());
						allComponentsModule.append("';\n");
					});

					// static list for now
					allComponentsModule.append("import { ServoyDefaultComponentsModule } from '../servoydefault/servoydefault.module';\n");
					// end

					allComponentsModule.append("@NgModule({\n imports: [\n");
					componentSpecToReader.keySet().forEach(spec -> {
						allComponentsModule.append(spec.getNg2Module());
						allComponentsModule.append(",\n");
					});

					// static list for now
					allComponentsModule.append("ServoyDefaultComponentsModule,\n");
					// end
					allComponentsModule.append(" ],\n exports: [\n");
					componentSpecToReader.keySet().forEach(spec -> {
						allComponentsModule.append(spec.getNg2Module());
						allComponentsModule.append(",\n");
					});
					// static list for now
					allComponentsModule.append("ServoyDefaultComponentsModule,\n");
					// end
					allComponentsModule.append(" ]\n})\nexport class AllComponentsModule { }\n");
					String current = allComponentsModule.toString();
					String content = FileUtils.readFileToString(new File(projectFolder, "src/ngclient/allcomponents.module.ts"), "UTF-8");

					if (!current.equals(content))
					{
						sourceChanged = true;
						writeConsole(console, "component modules  ts file changed");
						FileUtils.writeStringToFile(new File(projectFolder, "src/ngclient/allcomponents.module.ts"), current, "UTF-8");
					}
				}
				catch (IOException e1)
				{
					ServoyLog.logError(e1);
				}

				try
				{
					/* component/services imports */
					if (cssLibs.size() > -0)
					{
						String content = FileUtils.readFileToString(new File(projectFolder, "src/styles.css"), "UTF-8");
						int index = content.indexOf("/* component/services imports end */");
						StringBuilder sb = new StringBuilder();
						cssLibs.forEach(lib -> sb.append("@import \"").append(lib).append("\";\n"));
						String imports = sb.toString();
						if (!imports.equals(content.substring(0, index)))
						{
							if (index > 0)
							{
								content = content.substring(index);
							}
							content = imports + content;
							sourceChanged = true;
							writeConsole(console, "Styles source changed");
							FileUtils.writeStringToFile(new File(projectFolder, "src/styles.css"), content, "UTF-8");
						}
					}
				}
				catch (IOException e)
				{
					ServoyLog.logError(e);
				}
				if (packageToInstall.size() > 0 || sourceChanged || !new File(projectFolder, "dist").exists() || cleanInstall.get())
				{
					// first exeuted npm install with all the packages.
					// only execute this if a source is changed (should always happens the first time)
					// or if there are really packages to install.
					List<String> command = new ArrayList<>();
					command.add("install");
					packageToInstall.forEach(packageName -> command.add(packageName));
					command.add("--legacy-peer-deps");
					if (SOURCE_DEBUG)
					{
						writeConsole(console, "SOURCE DEBUG, skipping npm install and npm run debug, need to be run by your self");
						writeConsole(console, "npm command not done: " + RunNPMCommand.commandArgsToString(command));
					}
					else
					{
						RunNPMCommand npmCommand = Activator.getInstance().createNPMCommand(command);
						try
						{
							npmCommand.runCommand(monitor);
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
						}
						if (cleanInstall.get())
						{
							cleanInstall.set(false);
							npmCommand = Activator.getInstance().createNPMCommand(Arrays.asList("ci", "--legacy-peer-deps"));
							try
							{
								npmCommand.runCommand(monitor);
							}
							catch (Exception e)
							{
								ServoyLog.logError(e);
							}
						}
						npmCommand = Activator.getInstance().createNPMCommand(Arrays.asList("run", "build_debug_nowatch"));
						try
						{
							npmCommand.runCommand(monitor);
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
						}
					}
				}
				writeConsole(console, "Total time to check/install NG2 target folder: " + projectFolder + " is " +
					Math.round((System.currentTimeMillis() - time) / 1000) + "s\n");
				return Status.OK_STATUS;
			}
			finally
			{
				try
				{
					console.close();
				}
				catch (IOException e)
				{
				}
				// remove the schedules 1 and start + 1 from the scheduled.
				// if that is still higher then 0 we need to start another.
				if (scheduled.addAndGet(-2) > 0)
				{
					// first just set it now to 0 to start over.
					scheduled.set(0);
					checkPackages(false);
				}
			}
		}

		/**
		 * @param console
		 * @param pck
		 */
		private void writeConsole(IOConsoleOutputStream console, String message)
		{
			try
			{
				console.write(message + "\n");
			}
			catch (IOException e2)
			{
			}
		}

		/**
		 * @param packageToInstall
		 * @param dependencies
		 * @param packageName
		 * @param packageReader
		 * @param entryPoint
		 */
		private String checkPackage(JSONObject dependencies, String packageName, IPackageReader packageReader, String entryPoint)
		{
			String packageVersion = packageReader.getVersion();
			if (entryPoint != null)
			{
				// its a file based service (something installed in the workspace as a source project)
				if (packageReader instanceof DirPackageReader)
				{
					try
					{
						IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
						IContainer[] containers = root.findContainersForLocationURI(packageReader.getPackageURL().toURI());
						if (containers != null && containers.length == 1)
						{
							try
							{
								IProject project = containers[0].getProject();
								IFolder file = project.getFolder(entryPoint);
								if (file.exists())
								{

									File projectFolder = Activator.getInstance().getProjectFolder();
									File packagesFolder = new File(projectFolder, "packages");
									File packageFolder = new File(packagesFolder, packageName);

									IFile sourcePath = project.getFile(".sourcepath");
									JSONObject sourcePathJson = null;
									File apiFile = null;
									if (sourcePath.exists())
									{
										String sourcePathContents = FileUtils.readFileToString(new File(sourcePath.getRawLocationURI()), "UTF8");
										sourcePathJson = new JSONObject(sourcePathContents);
										file = project.getFolder(sourcePathJson.getString("srcDir"));
										apiFile = new File(packageFolder, sourcePathJson.getString("apiFile") + ".ts");

									}

									String location = packageFolder.getCanonicalPath();
									// check/copy the dist folder to the target packages location
									if (!WebPackagesListener.watchCreated.containsKey(project.getName()) || !packageFolder.exists() ||
										(apiFile != null && !apiFile.exists()))
									{
										DirectorySync directorySync = WebPackagesListener.watchCreated.get(project.getName());
										if (directorySync != null) directorySync.destroy();
										FileUtils.deleteQuietly(packageFolder);
										File srcDir = new File(file.getRawLocationURI());
										FileUtils.copyDirectory(srcDir, packageFolder);
										WebPackagesListener.watchCreated.put(project.getName(), new DirectorySync(srcDir, packageFolder, null));
									}
									// also add if this is a src thing to the ts config
									if (sourcePathJson != null)
									{
										File tsConfig = new File(projectFolder, "tsconfig.json");
										String tsConfigContents = FileUtils.readFileToString(tsConfig, "UTF8");
										JSONObject json = new JSONObject(tsConfigContents);
										JSONObject paths = json.getJSONObject("compilerOptions").getJSONObject("paths");
										if (!paths.has(packageName))
										{
											JSONArray array = new JSONArray();
											array.put(new File(packageFolder, sourcePathJson.getString("apiFile")).getCanonicalPath());
											paths.put(packageName, array);
											FileUtils.write(tsConfig, json.toString(1), "UTF8", false);
										}
									}

									String installedVersion = dependencies.optString(packageName);
									if (!installedVersion.endsWith("packages/" + packageName))
									{
										return location;
									}
								}
								return null;
							}
							catch (IOException e)
							{
								ServoyLog.logError(e);
							}
						}
					}
					catch (URISyntaxException e)
					{
						ServoyLog.logError(e);
					}
				}
				else if (packageReader instanceof ZipPackageReader)
				{
					// this has an entry point in the zip, extract this package.
					File projectFolder = Activator.getInstance().getProjectFolder();
					File packagesFolder = new File(projectFolder, "packages");
					File packageFolder = new File(packagesFolder, packageName);
					boolean exists = packageFolder.exists();
					if (exists)
					{
						File entry = new File(packageFolder, entryPoint);
						if (packageFolder.lastModified() < packageReader.getResource().lastModified() || !entry.exists())
						{
							FileUtils.deleteQuietly(packageFolder);
							exists = false;
						}

					}

					try
					{
						if (!exists)
						{
							ZipUtils.extractZip(packageReader.getResource().toURI().toURL(), packageFolder);
						}
						File entry = new File(packageFolder, entryPoint);
						if (entry.exists())
						{
							String installedVersion = dependencies.optString(packageName);
							if (!installedVersion.endsWith(entryPoint))
							{
								return entry.getCanonicalPath();
							}
							return null;
						}
					}
					catch (IOException e)
					{
						ServoyLog.logError(e);
					}
				}

				// check for prerelease (npm uses x.y.z-rc1 manifest: x.y.z.rc1)
				String[] split = packageVersion.split("\\.");
				if (split.length == 4)
				{
					packageVersion = split[0] + '.' + split[1] + '.' + split[2] + '-' + split[3];
				}
				String installedVersion = dependencies.optString(packageName);
				if (!installedVersion.contains(packageVersion))
				{
					return packageName + '@' + packageVersion;
				}
			}

			return null;
		}

		private String replace(String content, String start, String end, StringBuilder toInsert)
		{
			int startIndex = content.indexOf(start);
			int endIndex = content.indexOf(end) + end.length();
			return content.substring(0, startIndex) + toInsert + content.substring(endIndex);
		}
	}

	private static boolean SOURCE_DEBUG = false;

	private static final AtomicBoolean ignore = new AtomicBoolean(false);

	private static final AtomicInteger scheduled = new AtomicInteger(0); // 0 == no jobs, 1 == job scheduled, 2  or 3 == job running, 3 == run again.
	private static final AtomicBoolean cleanInstall = new AtomicBoolean(false);

	private static final ConcurrentMap<String, DirectorySync> watchCreated = new ConcurrentHashMap<>();

	public WebPackagesListener()
	{
		if (WebServiceSpecProvider.isLoaded())
			checkPackages(false);
	}

	@Override
	public void ngPackagesChanged(CHANGE_REASON changeReason, boolean loadedPackagesAreTheSameAlthoughReferencingModulesChanged)
	{
		checkPackages(false);
	}

	/**
	 * if true then checkpackage will be blocked until it is called with false again
	 * if false then all call to checkpackage will run a job, this will also trigger a checkPackage itself to make sure that all the blocked calls will now be triggerd once
	 * @param ignore
	 */
	public static void setIgnore(boolean ignore)
	{
		WebPackagesListener.ignore.set(ignore);
		if (!ignore) checkPackages(false);
	}

	public static void checkPackages(boolean ci)
	{
		if (ServoyModelFinder.getServoyModel().getActiveProject() == null || ignore.get())
		{
			return;
		}
		if (ci) cleanInstall.set(ci);
		// only schedule 1 and a bit later to relax first the system
		if (scheduled.compareAndSet(0, 1))
		{
			Job job = new PackageCheckerJob("Checking/Installing NGClient2 Components and Services");
			job.schedule(5000);
		}
		else if (scheduled.get() == 2)
		{
			// there is only 1 job and that one is running (not scheduled) then increase by one to make it run again
			scheduled.incrementAndGet();
		}
	}
}
