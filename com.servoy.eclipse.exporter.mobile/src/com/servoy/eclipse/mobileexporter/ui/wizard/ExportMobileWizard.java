/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.mobileexporter.ui.wizard;

import java.net.URL;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.osgi.service.prefs.BackingStoreException;

import com.servoy.eclipse.mobileexporter.export.MobileExporter;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.wizards.FinishPage;

public class ExportMobileWizard extends Wizard implements IExportWizard
{
	private static final String PROPERTY_IS_OPEN_URL = "isOpenURL"; //$NON-NLS-1$

	private final MobileExporter mobileExporter = new MobileExporter();

	private final CustomizedFinishPage finishPage = new CustomizedFinishPage("lastPage");

	private final PhoneGapApplicationPage pgAppPage = new PhoneGapApplicationPage("PhoneGap Application", finishPage);

	private final WarExportPage warExportPage = new WarExportPage("outputPage", "Choose output", null, finishPage, pgAppPage, mobileExporter);

	private final LicensePage licensePage = new LicensePage("licensePage", warExportPage, mobileExporter);

	private final ExportOptionsPage optionsPage = new ExportOptionsPage("optionsPage", licensePage, mobileExporter);

	public ExportMobileWizard()
	{
		IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("MobileExportWizard");//$NON-NLS-1$
		if (section == null)
		{
			section = workbenchSettings.addNewSection("MobileExportWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
		finishPage.setTitle("Export finished");
		setWindowTitle("Mobile Export");
	}

	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
	}

	@Override
	public boolean performFinish()
	{
		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(com.servoy.eclipse.mobileexporter.Activator.PLUGIN_ID);
		preferences.putBoolean(PROPERTY_IS_OPEN_URL, finishPage.isOpenUrl());
		try
		{
			preferences.flush();
		}
		catch (BackingStoreException e)
		{
			ServoyLog.logError(e);
		}

		if (finishPage.getOpenUrl() != null)
		{
			try
			{
				IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
				IWebBrowser browser = support.getExternalBrowser();
				browser.openURL(new URL(finishPage.getOpenUrl()));
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}
		return true;
	}

	@Override
	public void addPages()
	{
		addPage(optionsPage);
		addPage(warExportPage);
		addPage(finishPage);
		addPage(pgAppPage);
		addPage(licensePage);
	}

	public class CustomizedFinishPage extends FinishPage
	{
		private String url = null;
		private String urlDescription;
		private boolean urlSelected;
		private Button openURL = null;

		public CustomizedFinishPage(String pageName)
		{
			super(pageName);
		}

		@Override
		public boolean isPageComplete()
		{
			return super.isCurrentPage();
		}

		@Override
		public boolean canFlipToNextPage()
		{
			return false;
		}

		@Override
		public void createControl(Composite parent)
		{
			if (url != null)
			{
				Composite container = new Composite(parent, SWT.NONE);
				GridLayout layout = new GridLayout();
				container.setLayout(layout);
				layout.numColumns = 1;

				message = new Text(container, SWT.WRAP | SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
				GridData gridData = new GridData();
				gridData.horizontalAlignment = GridData.FILL;
				gridData.verticalAlignment = GridData.FILL;
				gridData.grabExcessHorizontalSpace = true;
				gridData.grabExcessVerticalSpace = true;
				gridData.horizontalSpan = 1;
				message.setLayoutData(gridData);
				message.setEditable(false);

				openURL = new Button(container, SWT.CHECK);
				openURL.setSelection(urlSelected);
				openURL.setText(urlDescription);
				gridData = new GridData();
				gridData.grabExcessHorizontalSpace = true;
				gridData.grabExcessVerticalSpace = false;
				gridData.horizontalAlignment = GridData.FILL;
				openURL.setLayoutData(gridData);


				setControl(container);
				setPageComplete(true);
			}
			else
			{
				super.createControl(parent);
			}
		}

		public void setApplicationURL(String url, String urlDescription, boolean defaultSelected)
		{
			this.url = url;
			this.urlDescription = urlDescription;
			IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(com.servoy.eclipse.mobileexporter.Activator.PLUGIN_ID);
			this.urlSelected = preferences.getBoolean(PROPERTY_IS_OPEN_URL, defaultSelected);
		}

		public String getOpenUrl()
		{
			if (isOpenUrl())
			{
				return url;
			}
			return null;
		}

		public boolean isOpenUrl()
		{
			return openURL != null && openURL.getSelection();
		}
	}
}
