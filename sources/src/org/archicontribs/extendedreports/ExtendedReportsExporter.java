/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.extendedreports;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.osgi.framework.Bundle;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;
import org.stringtemplate.v4.StringRenderer;

import com.archimatetool.editor.diagram.util.DiagramUtils;
import com.archimatetool.editor.diagram.util.ModelReferencedImage;
import com.archimatetool.editor.ui.ImageFactory;
import com.archimatetool.editor.utils.FileUtils;
import com.archimatetool.editor.utils.StringUtils;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IDiagramModelContainer;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IDiagramModelReference;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;


/**
 * Export multi-models to HTML report
 * Based on com.archimatetool.reports.HTMLReportExporter
 * 
 * @author Jean-Baptiste Sarrodie
 * @author Quentin Varquet
 * @author Phillip Beauvoir
 * @author Vincent Boulet
 */
public class ExtendedReportsExporter {
    
    private List<IArchimateModel> fModels;
    
    /**
     * Map of new bounds for each digram for bounds offset
     */
    private Map<IDiagramModel, Rectangle> diagramBoundsMap = new HashMap<IDiagramModel, Rectangle>();
    
    /**
     * Map of new bounds for child objects in images for hit areas
     */
    private Map<String, BoundsWithAbsolutePosition> childBoundsMap = new HashMap<String, BoundsWithAbsolutePosition>();
    
    private IProgressMonitor progressMonitor;
    
    static class CancelledException extends IOException {
        public CancelledException(String message) {
            super(message);
        }
    }
    
    /**
     * Clean up preview files
     * @throws IOException

    public static void cleanPreviewFiles() throws IOException {
        FileUtils.deleteFolder(PREVIEW_FOLDER);
    }
    
    */    
    
    public ExtendedReportsExporter(List<IArchimateModel> models) {
        fModels = models;
    }
    
    public void export(File targetFolder, String reportTitle) throws Exception {
        Exception[] exception = new Exception[1];
        
        // Since this can take a while, show the busy dialog
        IRunnableWithProgress runnable = monitor -> {
            try {
                File file = createReport(targetFolder, reportTitle, monitor); //$NON-NLS-1$
                
                // Open it in external Browser
                IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
                IWebBrowser browser = support.getExternalBrowser();
                // This method supports network URLs
                browser.openURL(new URL("file", null, file.getAbsolutePath().replace(" ", "%20"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            catch(Exception ex) { // Catch OOM and SWT exceptions
                exception[0] = ex;
            }
        };
        
        try {
            ProgressMonitorDialog dialog = new ProgressMonitorDialog(Display.getCurrent().getActiveShell());
            dialog.run(false, true, runnable);
        }
        catch(Exception ex) {
            exception[0] = ex;
        }

        if(exception[0] instanceof CancelledException) {
            MessageDialog.openInformation(Display.getCurrent().getActiveShell(), Messages.ExtendedReportsExporter_2, exception[0].getMessage());
        }
        else if(exception[0] != null) {
            throw exception[0];
        }
    }
    
    
    public File createReport(File targetFolder, String reportTitle) throws IOException {
        return createReport(targetFolder, reportTitle, null);
    }
    
    public File createReport(File targetFolder, String reportTitle, IProgressMonitor monitor) throws IOException {
        String indexFileName = "index.html";
        progressMonitor = monitor;
        
        if(progressMonitor != null) {
            progressMonitor.beginTask(Messages.ExtendedReportsExporter_6, -1);
        }
        
        // Copy HTML skeleton to target
        copyHTMLSkeleton(targetFolder);
        
        // Copy hints files from the help plug-in
        copyHintsFiles(targetFolder);
                
        // Instantiate templates files
        File mainFile = new File(ExtendedReportsPlugin.INSTANCE.getTemplatesFolder(), "st/mainst.stg"); //$NON-NLS-1$
        STGroupFile groupFile = new STGroupFile(mainFile.getAbsolutePath(), '^', '^');
 	    groupFile.registerRenderer(String.class, new StringRenderer());
        ST stReport = groupFile.getInstanceOf("report"); //$NON-NLS-1$
    	ST stFrame = groupFile.getInstanceOf("frame"); //$NON-NLS-1$
    	ST stModel = groupFile.getInstanceOf("model");
    	List<Map<String, Object>> modelsList = new ArrayList<>();
    	
        for(IArchimateModel fModel : fModels) {
        	
	        setProgressSubTask(NLS.bind(Messages.ExtendedReportsExporter_12, fModel.getName()), true);
        	        	
	        // Create sub-folders
	        File elementsFolder = new File(targetFolder, fModel.getId() + "/elements"); //$NON-NLS-1$
	        elementsFolder.mkdirs(); // Make dir
	        
	        File viewsFolder = new File(targetFolder, fModel.getId() + "/views"); //$NON-NLS-1$
	        viewsFolder.mkdirs(); // Make dir
	        
	        File imagesFolder = new File(targetFolder, fModel.getId() + "/images"); //$NON-NLS-1$
	        imagesFolder.mkdirs(); // Make dir
	             
	        File objectsFolder = new File(targetFolder, fModel.getId() + "/objects"); //$NON-NLS-1$
	        objectsFolder.mkdirs(); // Make dir        

            // ST stFrame = templates.getInstanceOf("framepage"); //$NON-NLS-1$
	        
	        // Write model gallery, purpose and properties in views folder
	        writeModel(new File(elementsFolder, fModel.getId()+".html"), stModel, fModel); //$NON-NLS-1$
	        
	        // Write all folders
	        writeFolders(elementsFolder, stFrame, fModel.getFolders());
	        
	        // Write other graphical objects
	        writeGraphicalObjects(fModel, objectsFolder, stFrame);
	        
	        // Write Diagrams and images
	        writeDiagrams(fModel, imagesFolder, viewsFolder, stFrame);
	        
	        // Create HashMap for passing model name, model id, model content folders containing elements 
	        // and views folder to modelreport.stg in order to create models tree
	        Map<String, Object> modelMap = new HashMap<>();

	        modelMap.put("modelName", fModel.getName());
	        modelMap.put("modelId", fModel.getId());

	        Map<String, Object> contentFolders = new HashMap<>();
	        // For each folder of the model, testing if it's not empty
	        // and if it's not the views folder (adding in a specific key in the modelMap)
	        for (IFolder folder : fModel.getFolders()) {	        		        	
	        	if (!(folder.getElements().isEmpty() && folder.getFolders().isEmpty()) && (folder.getType() != FolderType.DIAGRAMS)) {
	        		contentFolders.put(folder.getType().getName(), folder);
	        	}
	        }
	        
	        // Adding the filtered model content folders to the modelMap
	        modelMap.put("contentFolders", contentFolders);
	        modelMap.put("viewsFolder", fModel.getFolder(FolderType.DIAGRAMS));	        
	        
	        modelsList.add(modelMap);
        
	        setProgressSubTask(Messages.ExtendedReportsExporter_11, true);
        
        }

        stReport.add("reportTitle", reportTitle);
        stReport.add("modelsList", modelsList);
        
        setProgressSubTask(Messages.ExtendedReportsExporter_13, true);
                
        File indexFile = new File(targetFolder, indexFileName);
        try(OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(indexFile), "UTF8")) { //$NON-NLS-1$
            writer.write(stReport.render());
        }
        
        return indexFile;
    }
    
    /**
     * Copy source HTML files to target folder
     * @throws IOException 
     */
    private void copyHTMLSkeleton(File targetFolder) throws IOException {
        setProgressSubTask(Messages.ExtendedReportsExporter_9, true);
        
        File srcDir = new File(ExtendedReportsPlugin.INSTANCE.getTemplatesFolder(), "html"); //$NON-NLS-1$
        FileUtils.copyFolder(srcDir, targetFolder);
    }
    
    /**
     * Copy hints files to target folder from the help plug-in
     * @throws IOException 
     */
    private void copyHintsFiles(File targetFolder) throws IOException {
        setProgressSubTask(Messages.ExtendedReportsExporter_10, true);
        
        // Main hints
        Bundle bundle = Platform.getBundle("com.archimatetool.help"); //$NON-NLS-1$
        URL url = FileLocator.resolve(bundle.getEntry("hints")); //$NON-NLS-1$
        FileUtils.copyFolder(new File(url.getPath()), new File(targetFolder, "hints")); //$NON-NLS-1$
        
        // Canvas hints
        bundle = Platform.getBundle("com.archimatetool.canvas"); //$NON-NLS-1$
        url = FileLocator.resolve(bundle.getEntry("help/hints")); //$NON-NLS-1$
        FileUtils.copyFolder(new File(url.getPath()), new File(targetFolder, "hints")); //$NON-NLS-1$
    }

    /**
     * Write all folders
     */
    private void writeFolders(File elementsFolder, ST stFrame, List<IFolder> folders) throws IOException {
    	for(IFolder folder : folders) {
    		writeFolder(elementsFolder, stFrame, folder);
    	}
    }
    
    /**
     * Write a single folder
     */
    private void writeFolder(File elementsFolder, ST stFrame, IFolder folder) throws IOException {
    	writeElements(elementsFolder, stFrame, folder.getElements());
    	writeFolders(elementsFolder, stFrame, folder.getFolders());
    }
    
    /**
     * Write all elements
     */
    private void writeElements(File elementsFolder, ST stFrame, List<EObject> list) throws IOException {
        for(EObject object : list) {
            if(object instanceof IArchimateConcept) {
                writeElement(new File(elementsFolder, ((IIdentifier) object).getId() + ".html"), stFrame, object); //$NON-NLS-1$
            }
        }
    }
    
    /**
     * Write a single element
     */
    private void writeElement(File elementFile, ST stFrame, EObject component) throws IOException {
        stFrame.remove("element"); //$NON-NLS-1$
        //frame.remove("children");
        stFrame.add("element", component); //$NON-NLS-1$
        
        try(OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(elementFile), "UTF8")) { //$NON-NLS-1$
            writer.write(stFrame.render());
        }

        updateProgress();
    }
    
    /**
     * Write model gallery, purpose and properties
     */
    private void writeModel(File elementFile, ST stModel, IArchimateModel fModel) throws IOException {
    	
        stModel.remove("element"); //$NON-NLS-1$
        //frame.remove("children");
        stModel.add("element", fModel); //$NON-NLS-1$
        
        try(OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(elementFile), "UTF8")) { //$NON-NLS-1$
            writer.write(stModel.render());
        }

        updateProgress();
        
    }
    
    
    
    /**
     * Write graphical objects
     */
    private void writeGraphicalObjects(IArchimateModel fModel, File objectsFolder, ST stFrame) throws IOException {
        for(IDiagramModel dm : fModel.getDiagramModels()) {
            for(Iterator<EObject> iter =  dm.eAllContents(); iter.hasNext();) {
                EObject eObject = iter.next();
                if(eObject instanceof IDiagramModelObject && !(eObject instanceof IDiagramModelArchimateObject) 
                        && !(eObject instanceof IDiagramModelReference)) {
                    writeElement(new File(objectsFolder, ((IIdentifier) eObject).getId() + ".html"), stFrame, eObject); //$NON-NLS-1$
                }
            }
        }
    }

    /**
     * Write diagrams
     */
    private void writeDiagrams(IArchimateModel fModel, File imagesFolder, File viewsFolder, ST stFrame) throws IOException {
        List<IDiagramModel> diagramModels = fModel.getDiagramModels();
        
        if(diagramModels.isEmpty()) {
            return;
        }
        
        // Save images
        saveImages(imagesFolder, diagramModels);
        
        setProgressSubTask(Messages.ExtendedReportsExporter_11, true);

        // Create html files
        for(IDiagramModel dm : diagramModels) {
            // Add the necessary bounds in order to get correct absolute coordinates for the elements in the generated image
            Rectangle bounds = diagramBoundsMap.get(dm);
            
            // process the children
            for(IDiagramModelObject dmo: dm.getChildren() ) {
                addNewBounds(dmo, bounds.x * -1, bounds.y * -1);
            }

            stFrame.remove("element");
            stFrame.add("element", dm); //$NON-NLS-1$
            stFrame.remove("map");
            stFrame.add("map", childBoundsMap); //$NON-NLS-1$
            
            File viewFile = new File(viewsFolder, dm.getId() + ".html"); //$NON-NLS-1$
            try(OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(viewFile), "UTF8")) { //$NON-NLS-1$
                writer.write(stFrame.render());
            }
            
            updateProgress();
        }
    }
    
    /**
     * Save diagram images
     * @param diagramModels 
     * @throws IOException 
     */
    private void saveImages(File imagesFolder, List<IDiagramModel> diagramModels) throws IOException {
        // Use this to generate unique name for image file
        Hashtable<IDiagramModel, String> nameTable = new Hashtable<IDiagramModel, String>();
        
        int nameCount = 1;
        int total = diagramModels.size();
        int i = 1;
        
        for(IDiagramModel dm : diagramModels) {
            setProgressSubTask(NLS.bind(Messages.HTMLReportExporter_4, i++, total), true);

            Image image = null;
            
            try {
                ModelReferencedImage geoImage = DiagramUtils.createModelReferencedImage(dm, 1, 10);
                image = geoImage.getImage();

                // Generate file name
                String diagramName = dm.getId();
                if(StringUtils.isSet(diagramName)) {
                    // removed this because ids can have hyphens in them (when imported from TOG format)
                    // Let's hope that ids are filename friendly...
                    //diagramName = FileUtils.getValidFileName(diagramName);

                    int j = 2;
                    String s = diagramName + ".png";  //$NON-NLS-1$
                    while(nameTable.containsValue(s)) {
                        s = diagramName + "_" + j++ + ".png"; //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    diagramName = s;
                }
                else {
                    diagramName = Messages.HTMLReportExporter_1 + " " + nameCount++ + ".png";  //$NON-NLS-1$//$NON-NLS-2$
                }

                nameTable.put(dm, diagramName);

                // Get and store the bounds of the top-left element in the figure to act as overall x,y offset
                Rectangle bounds = geoImage.getBounds();
                bounds.performScale(ImageFactory.getImageDeviceZoom() / 100); // Account for device zoom level
                diagramBoundsMap.put(dm, bounds);

                ImageLoader loader = new ImageLoader();
                loader.data = new ImageData[] { image.getImageData(ImageFactory.getImageDeviceZoom()) };
                File file = new File(imagesFolder, diagramName);
                loader.save(file.getAbsolutePath(), SWT.IMAGE_PNG);
            }
            catch(Throwable t) {
                throw new IOException("Error saving image for: " + dm.getName() + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
                        (t.getMessage() == null ? t.toString() : t.getMessage()), t);
            }
            finally {
                if(image != null) {
                    image.dispose();
                }
            }
        }
    }
    
    private void updateProgress() throws CancelledException {
        if(progressMonitor != null && PlatformUI.isWorkbenchRunning() && Display.getCurrent() != null) {
            while(Display.getCurrent().readAndDispatch());
            
            if(progressMonitor.isCanceled()) {
                throw new CancelledException(Messages.HTMLReportExporter_14);
            }
        }
    }
    
    private void setProgressSubTask(String task, boolean doUpdate) throws CancelledException {
        if(progressMonitor != null) {
            progressMonitor.subTask(task);
            if(doUpdate) {
                updateProgress();
            }
        }
    }

    /**
     * Add new bounds for each diagram object in relation to its parent offset x,y
     */
    private void addNewBounds(IDiagramModelObject dmo, int offsetX, int offsetY) {
        // Add new bounds called to device zoom
        BoundsWithAbsolutePosition newBounds = new BoundsWithAbsolutePosition(dmo.getBounds(), ImageFactory.getImageDeviceZoom() / 100);
        newBounds.setOffset(offsetX, offsetY); // Add offset
        childBoundsMap.put(dmo.getId(), newBounds);
        
        // Children
        if(dmo instanceof IDiagramModelContainer) {
            for(IDiagramModelObject child: ((IDiagramModelContainer)dmo).getChildren() ) {
                addNewBounds(child, newBounds.getX1(), newBounds.getY1());
            }
        }
    }

}
