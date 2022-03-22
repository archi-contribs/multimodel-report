/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.extendedreports;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.Preferences;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.editor.ui.IArchiImages;
import com.archimatetool.editor.ui.UIUtils;
import com.archimatetool.model.IArchimateModel;



/**
 * Export Model to Jasper Reports Wizard Page 1
 * 
 * @author Phillip Beauvoir
 * @author Vincent Boulet
 */
public class ExtendedReportsWizardPage1 extends WizardPage {

    private static String HELP_ID = "org.archicontribs.help.ExtendedReportsWizardPage1"; //$NON-NLS-1$
    
    static final String PREFS_LAST_FOLDER = "MR_Last_Folder"; //$NON-NLS-1$
    static final String PREFS_LAST_TITLE = "MR_Last_Title"; //$NON-NLS-1$

    private List<IArchimateModel> fModels;
    private List<Button> modelsButton;

    private Text fTextOutputFolder;
    private Text fTextReportTitle;
    
    

    public ExtendedReportsWizardPage1() {
        super("ExtendedReportsWizardPage1"); //$NON-NLS-1$
        
        setTitle(Messages.ExtendedReportsWizardPage1_0);
        setDescription(Messages.ExtendedReportsWizardPage1_1);
        setImageDescriptor(IArchiImages.ImageFactory.getImageDescriptor(IArchiImages.ECLIPSE_IMAGE_EXPORT_DIR_WIZARD));
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        container.setLayout(new GridLayout());
        setControl(container);
        setPageComplete(false);
        
        PlatformUI.getWorkbench().getHelpSystem().setHelp(container, HELP_ID);
        
        IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(ExtendedReportsPlugin.PLUGIN_ID);
        Preferences sub = preferences.node("node");
        String lastFolder = sub.get(PREFS_LAST_FOLDER, new File(System.getProperty("user.home"), "exported").getPath());
        String lastTitle = sub.get(PREFS_LAST_TITLE, "Multi-models Report");
        
        Composite fieldContainer = new Composite(container, SWT.NULL);
        fieldContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fieldContainer.setLayout(new GridLayout(3, false));
        
        Label label = new Label(fieldContainer, SWT.NONE);
        label.setText(Messages.ExtendedReportsWizardPage1_2);
        
        fTextOutputFolder = UIUtils.createSingleTextControl(fieldContainer, SWT.BORDER, false);
        fTextOutputFolder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fTextOutputFolder.setText(lastFolder);
        fTextOutputFolder.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                validateFields();
            }
        });
        
        Button button = new Button(fieldContainer, SWT.PUSH);
        button.setText(Messages.ExtendedReportsWizardPage1_3);
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleBrowse();
            }
        });

        label = new Label(fieldContainer, SWT.NONE);
        label.setText(Messages.ExtendedReportsWizardPage1_4);
        fTextReportTitle = UIUtils.createSingleTextControl(fieldContainer, SWT.BORDER, false);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        fTextReportTitle.setLayoutData(gd);
        fTextReportTitle.setText(lastTitle);
        fTextReportTitle.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                validateFields();
            }
        });
        
        Group group = new Group(container, SWT.NONE);
        group.setText(Messages.ExtendedReportsWizardPage1_5);
        group.setLayout(new GridLayout(1, false));
        GridData gdata = new GridData(SWT.FILL, SWT.FILL, true, false); 
        gdata.heightHint = 200;
        group.setLayoutData(gdata);
      
        ScrolledComposite scomposite = new ScrolledComposite(group, SWT.V_SCROLL);
        scomposite.setLayout(new GridLayout(1, false));
        scomposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        Composite composite = new Composite(scomposite, SWT.NONE);
        composite.setLayout(new GridLayout(2, true));
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
              
        modelsButton = new ArrayList<Button>();
        
        Button modelButton;
        
        for(IArchimateModel model : IEditorModelManager.INSTANCE.getModels()) {	
	        modelButton = new Button(composite, SWT.CHECK);
	        modelButton.setText(model.getName());
	        modelButton.setData("id", model.getId());
	        modelButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	        modelButton.addSelectionListener(new SelectionAdapter() {
	            @Override
	            public void widgetSelected(SelectionEvent e) {
	                validateFields();
	            }
	        });
	        
	        modelsButton.add(modelButton);    
	        
        }
        scomposite.setContent(composite);
        scomposite.setExpandHorizontal(true);
        scomposite.setExpandVertical(true);
        scomposite.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        
    }
    
    /**
     * @return The Folder for the Reports
     */
    public File getExportFolder() {
        return new File(fTextOutputFolder.getText());
    }
    
    public String getReportTitle() {
        return fTextReportTitle.getText();
    }
    
    public List<IArchimateModel> getExportModels() {
        return fModels;
    } 
    
    
    private void validateFields() {
    	
    	Boolean isOK = false;
    	
    	fModels = new ArrayList<IArchimateModel>();
    	
    	for(Button button : modelsButton) {
    		if(button.getSelection()) {
    			isOK = true;
    			String modelId = (String) button.getData("id");
    			for(IArchimateModel model : IEditorModelManager.INSTANCE.getModels()) {	
    				if(model.getId() == modelId) {
    					fModels.add(model);
    				}
    			}
    		}
    	}
        
        if(!isOK) {
            updateStatus(Messages.ExtendedReportsWizardPage1_10);
            return;
        }
    	
        String s = fTextOutputFolder.getText();
        if("".equals(s.trim())) { //$NON-NLS-1$
            updateStatus(Messages.ExtendedReportsWizardPage1_6);
            return;
        }
        
        s = fTextReportTitle.getText();
        if("".equals(s.trim())) { //$NON-NLS-1$
            updateStatus(Messages.ExtendedReportsWizardPage1_7);
            return;
        }
        
        // OK
        updateStatus(null);
    }

    /**
     * Update the page status
     */
    private void updateStatus(String message) {
        setErrorMessage(message);
        setPageComplete(message == null);
    }
    
    private void handleBrowse() {
        DirectoryDialog dialog = new DirectoryDialog(getShell());
        dialog.setText(Messages.ExtendedReportsWizardPage1_8);
        dialog.setMessage(Messages.ExtendedReportsWizardPage1_9);
        dialog.setFilterPath(fTextOutputFolder.getText());
        
        String path = dialog.open();
        if(path != null) {
            fTextOutputFolder.setText(path);
        }
    }

    void storePreferences() {
        IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(ExtendedReportsPlugin.PLUGIN_ID);
        Preferences sub = preferences.node("node");
        sub.put(PREFS_LAST_FOLDER, getExportFolder().getAbsolutePath());
        sub.put(PREFS_LAST_TITLE, getReportTitle());

    }
}
