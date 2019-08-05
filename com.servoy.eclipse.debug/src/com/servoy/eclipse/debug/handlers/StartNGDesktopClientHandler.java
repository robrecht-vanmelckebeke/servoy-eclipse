/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

package com.servoy.eclipse.debug.handlers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.json.JSONObject;

import com.servoy.base.util.ITagResolver;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.ZipUtils;
import com.servoy.eclipse.debug.Activator;
import com.servoy.eclipse.debug.actions.IDebuggerStartListener;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Utils;

/**
 * @author costinchiulan
 * @since 2019.06
 */
public class StartNGDesktopClientHandler extends StartDebugHandler implements IRunnableWithProgress, IDebuggerStartListener
{

	static final String NGDESKTOP_MAJOR_VERSION = "2019";
	static final String NGDESKTOP_MINOR_VERSION = "09";

	static private int BUFFER_SIZE = 1024;
	static final String MAC_EXTENSION = ".app";
	static final String WINDOWS_EXTENSION = ".exe";
	//Linux doesn't have any extension

	public static final String WINDOWS_BUILD_PLATFORM = "win";
	public static final String MAC_BUILD_PLATFORM = "mac";
	public static final String LINUX_BUILD_PLATFORM = "linux";

	public static final String NG_DESKTOP_APP_NAME = "servoyngdesktop";
	public static String DOWNLOAD_URL = System.getProperty("ngdesktop.download.url", "http://download.servoy.com/ngdesktop/");
	public static final String NGDESKTOP_VERSION = NGDESKTOP_MAJOR_VERSION + "." + NGDESKTOP_MINOR_VERSION;

	static
	{
		if (!DOWNLOAD_URL.endsWith("/")) DOWNLOAD_URL += "/";
	}

	public static ITagResolver noReplacementResolver = new ITagResolver()
	{
		public String getStringValue(String name)
		{
			return "%%" + name + "%%";
		}
	};

	public static ITagResolver simpleReplacementResolver = new ITagResolver()
	{
		public String getStringValue(String name)
		{
			if ("prefix".equals(name)) return "forms.customer.";
			else if ("elementName".equals(name)) return ".elements.customer_id";
			else return "%%" + name + "%%";
		}
	};

	@Override
	public Object execute(ExecutionEvent event)
	{

		Job job = new Job("NGDesktop client launch")
		{
			@Override
			protected IStatus run(IProgressMonitor monitor)
			{
				try
				{
					StartNGDesktopClientHandler.this.run(monitor);
				}
				catch (InvocationTargetException e)
				{
					ServoyLog.logError(e);
				}
				catch (InterruptedException e)
				{
					ServoyLog.logError(e);
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
		return null;
	}

	public String getStartTitle()
	{
		return "NGDesktop client launch";
	}


	/**
	 * Method for writing into servoy.json details about electron app.
	 * Here can be also changed icon, url, or used modules.
	 */

	private void writeElectronJsonFile(Solution solution, File stateLocation, String fileExtension, IProgressMonitor monitor)
	{

		String solutionUrl = "http://localhost:" + ApplicationServerRegistry.get().getWebServerPort() + "/solutions/" + solution.getName() + "/index.html";

		String osxContent = Utils.isAppleMacOS() ? File.separator + "Contents" : "";

		//Mac folder structure is different, we should adapt url to that.
		String fileUrl = osxContent + File.separator + (Utils.isAppleMacOS() ? "Resources" : "resources") + File.separator + "app.asar.unpacked" +
			File.separator + "config" + File.separator + "servoy.json";


		File f = new File(stateLocation.getAbsolutePath() + File.separator + fileUrl);

		//Store servoy.json file as a JSONObject
		String jsonFile = Utils.getTXTFileContent(f, Charset.forName("UTF-8"));
		JSONObject configFile = new JSONObject(jsonFile);

		JSONObject options = (JSONObject)configFile.get("options");
		//put url and other options in servoy.json(we can put image also here, check servoy.json to see available options.
		options.put("url", solutionUrl);
		options.put("showMenu", true);
		configFile.put("options", options);

		try (FileWriter file = new FileWriter(f))
		{
			BufferedWriter out = new BufferedWriter(file);
			out.write(configFile.toString());
			out.close();
		}
		catch (IOException e1)
		{
			ServoyLog.logError("Error writing  in servoy.json file " + fileUrl, e1);
		}

		//Now try opening servoyNGDesktop app.
		try
		{
			String[] command;
			if (Utils.isWindowsOS() || Utils.isLinuxOS())
				command = new String[] { stateLocation.getAbsolutePath() + File.separator + NG_DESKTOP_APP_NAME + fileExtension };
			else
			{
				command = new String[] { "/usr/bin/open", stateLocation.getAbsolutePath() +
					(Utils.isAppleMacOS() ? "" : File.separator + NG_DESKTOP_APP_NAME + fileExtension) };
			}
			monitor.beginTask("Open NGDesktop", 3);
			Runtime.getRuntime().exec(command);
			monitor.worked(2);
		}
		catch (IOException e)
		{
			ServoyLog.logError("Cannot find servoy NGDesktop executable", e);
		}

	}

	private byte[] getBytes(InputStream in) throws IOException
	{
		try
		{
			byte versionBuffer[] = new byte[BUFFER_SIZE]; //default initialize to '0'
			int bytesRead = in.read(versionBuffer, 0, BUFFER_SIZE);
			return bytesRead != -1 ? Arrays.copyOf(versionBuffer, bytesRead) : null;
		}
		finally
		{
			in.close();
		}
	}

	/*
	 * Compare the remote NGDesktop version with current version and delete current if it's the case. Deleting current version will enforce remote version
	 * download
	 */
	private void checkForHigherVersion(File location)
	{
		try
		{
			File parentFile = location.getParentFile();
			URL fileUrl = new URL(
				DOWNLOAD_URL + "version" + StartNGDesktopClientHandler.NGDESKTOP_MAJOR_VERSION + StartNGDesktopClientHandler.NGDESKTOP_MINOR_VERSION + ".txt");
			File currentVersionFile = new File(parentFile.getAbsolutePath() + File.separator + "version" + StartNGDesktopClientHandler.NGDESKTOP_MAJOR_VERSION +
				StartNGDesktopClientHandler.NGDESKTOP_MINOR_VERSION + ".txt");

			byte[] remoteBuf = getBytes(fileUrl.openStream());
			byte[] currentBuf = currentVersionFile.exists() ? getBytes(new FileInputStream(currentVersionFile)) : null;
			if (!Arrays.equals(remoteBuf, currentBuf))
			{
				//TODO: notify user. if (user decide to download higher version) {
				if (location.exists())
				{
					Files.walk(location.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
				}
				OutputStream versionStream = new FileOutputStream(currentVersionFile);
				versionStream.write(remoteBuf); //this will overwrite the old content
				versionStream.close();
				//} TODO: end
			}
		}
		catch (IOException e)
		{
			ServoyLog.logError("Exception while checking for higher version: ", e);
		}
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
	{
		StartClientHandler.setLastCommand(StartClientHandler.START_NG_DESKTOP_CLIENT);
		monitor.beginTask(getStartTitle(), 5);
		monitor.worked(1);

		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		ServoyProject activeProject = servoyModel.getActiveProject();

		if (activeProject != null && activeProject.getSolution() != null)
		{
			final Solution solution = activeProject.getSolution();

			if (solution.getSolutionType() == SolutionMetaData.SMART_CLIENT_ONLY)
			{
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						org.eclipse.jface.dialogs.MessageDialog.openError(Display.getDefault().getActiveShell(), "Solution type problem",
							"Cant open this solution type in this client");
					}
				});
				return;
			}
			monitor.worked(2);

			if (testAndStartDebugger())
			{

				String extension = Utils.isAppleMacOS() ? MAC_EXTENSION : (Utils.isWindowsOS() ? WINDOWS_EXTENSION : "");

				String folderName = NG_DESKTOP_APP_NAME + "-" + NGDESKTOP_VERSION + "-" +
					((Utils.isAppleMacOS() ? MAC_BUILD_PLATFORM + extension : (Utils.isWindowsOS()) ? WINDOWS_BUILD_PLATFORM : LINUX_BUILD_PLATFORM));

				File stateLocation = Activator.getDefault().getStateLocation().append(folderName).toFile();
				String pathToExecutable = (Utils.isAppleMacOS() ? stateLocation.getAbsolutePath()
					: stateLocation.getAbsolutePath() + File.separator + NG_DESKTOP_APP_NAME + extension);

				File executable = new File(pathToExecutable);
				checkForHigherVersion(executable.getParentFile());

				if (executable.exists())
				{
					writeElectronJsonFile(solution, stateLocation, extension, monitor);
				}
				else
				{
					Display.getDefault().asyncExec(new Runnable()
					{
						public void run()
						{
							ProgressMonitorDialog dialog = new ProgressMonitorDialog(Display.getDefault().getActiveShell());
							try
							{
								dialog.run(true, false, new DownloadElectron());
								if (executable.exists()) writeElectronJsonFile(solution, stateLocation, extension, monitor);
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}

						}
					});
				}

			}
		}
		monitor.done();
	}

	@Override
	protected IDebuggerStartListener getDebuggerAboutToStartListener()
	{
		return this;
	}

	public void aboutToStartDebugClient()
	{

	}

}


class DownloadElectron implements IRunnableWithProgress
{
	@Override
	public void run(IProgressMonitor monitor)
	{
		File f = null;
		try
		{
			monitor.beginTask("Downloading NGDesktop executable", 3);

			f = new File(Activator.getDefault().getStateLocation().toOSString());
			f.mkdirs();

			URL fileUrl = new URL(StartNGDesktopClientHandler.DOWNLOAD_URL + StartNGDesktopClientHandler.NG_DESKTOP_APP_NAME + "-" +
				StartNGDesktopClientHandler.NGDESKTOP_VERSION + "-" +
				(Utils.isAppleMacOS() ? StartNGDesktopClientHandler.MAC_BUILD_PLATFORM
					: (Utils.isWindowsOS() ? StartNGDesktopClientHandler.WINDOWS_BUILD_PLATFORM : StartNGDesktopClientHandler.LINUX_BUILD_PLATFORM)) +
				".tar.gz");

			ZipUtils.extractTarGZ(fileUrl, f);
			monitor.worked(2);
		}
		catch (Exception e)
		{
			//on download error delete current version file, this will enforce a new download on the next attempt to run the solution
			File currentVersionFile = new File(f.getAbsolutePath() + File.separator + "version" + StartNGDesktopClientHandler.NGDESKTOP_MAJOR_VERSION +
				StartNGDesktopClientHandler.NGDESKTOP_MINOR_VERSION + ".txt");
			if (currentVersionFile.exists()) currentVersionFile.delete();

			ServoyLog.logError("Cannot find Electron in download center", e);
		}
	}
}