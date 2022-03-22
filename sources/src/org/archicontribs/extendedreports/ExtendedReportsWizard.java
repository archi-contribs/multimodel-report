/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.extendedreports;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import com.archimatetool.editor.Logger;
import org.archicontribs.extendedreports.ExtendedReportsExporter.CancelledException;
import com.archimatetool.model.IArchimateModel;



/**
 * Reports Wizard
 * 
 * @author Phillip Beauvoir
 * @author Vincent Boulet
 */
public class ExtendedReportsWizard extends Wizard {
    
    private List<IArchimateModel> fModels;
    
    private ExtendedReportsWizardPage1 fPage1;
    
    private File exportFolder;
    private String reportTitle;

    public ExtendedReportsWizard() {
        setWindowTitle(Messages.ExtendedReportsWizard_0);
    }
    
    @Override
    public void addPages() {
        fPage1 = new ExtendedReportsWizardPage1();
        addPage(fPage1);
    }

    @Override
    public boolean performFinish() {
        fPage1.storePreferences();
        
        exportFolder = fPage1.getExportFolder();
        reportTitle = fPage1.getReportTitle();
        fModels  = fPage1.getExportModels();

        // Check valid dir        
        if(exportFolder.exists()) {
            String[] children = exportFolder.list();
            if(children != null && children.length > 0) {
                boolean result = MessageDialog.openQuestion(Display.getCurrent().getActiveShell(),
                        Messages.ExtendedReportsExporter_2,
                        NLS.bind(Messages.ExtendedReportsWizard_6, exportFolder));
                if(!result) {
                    return false;
                }
            }
        }
        else {
            try {
                exportFolder.mkdirs();
              
            }
            catch(Exception ex) {
                MessageDialog.openError(getShell(), Messages.ExtendedReportsWizard_3, Messages.ExtendedReportsWizard_4);
                return false;
            }
        }

        return true;
    }

    // Since this can take a while, show the progress monitor
    public void runWithProgress() throws InvocationTargetException, InterruptedException {
        IRunnableWithProgress runnable = monitor -> {
            try {
                ExtendedReportsExporter exporter = new ExtendedReportsExporter(fModels);
                File file = exporter.createReport(exportFolder, reportTitle, monitor);
                
                // Open it in external Browser
                IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
                IWebBrowser browser = support.getExternalBrowser();
                // This method supports network URLs
                browser.openURL(new URL("file", null, file.getAbsolutePath().replace(" ", "%20"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

            }
            catch(Throwable ex) { // Catch SWT and OOM exceptions
                // Async this to close progress monitor
                Display.getCurrent().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        if(ex instanceof CancelledException) {
                            MessageDialog.openInformation(getShell(), Messages.ExtendedReportsWizard_0, ex.getMessage());
                        }
                        else {
                            Logger.log(IStatus.ERROR, "Error saving Multi-models Report", ex); //$NON-NLS-1$
                            MessageDialog.openError(getShell(), Messages.ExtendedReportsWizard_5, ex.getMessage());
                        }
                    }
                });
            }
            finally {
                monitor.done();
            }
        };

        ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
        dialog.run(false, true, runnable);
    }
}
