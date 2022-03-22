package org.archicontribs.extendedreports;
/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */


import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.ui.plugin.AbstractUIPlugin;


/**
 * Activator
 * 
 * @author Phillip Beauvoir
 * @author Vincent Boulet
 */
public class ExtendedReportsPlugin extends AbstractUIPlugin {
    
    public static final String PLUGIN_ID = "org.archicontribs.extendedreports"; //$NON-NLS-1$

    /**
     * The shared instance
     */
    public static ExtendedReportsPlugin INSTANCE;

    /**
     * The File location of this plugin folder
     */
    private static File fPluginFolder;

    public ExtendedReportsPlugin() {
        INSTANCE = this;
    }
    
    /**
     * @return The templates folder
     */
    public File getTemplatesFolder() {
        return new File(getPluginFolder(), "templates"); //$NON-NLS-1$
    }
        
    /**
     * @return The File Location of this plugin
     */
    public File getPluginFolder() {
        if(fPluginFolder == null) {
            URL url = getBundle().getEntry("/"); //$NON-NLS-1$
            try {
                url = FileLocator.resolve(url);
            }
            catch(IOException ex) {
                ex.printStackTrace();
            }
            fPluginFolder = new File(url.getPath());
        }
        
        return fPluginFolder;
    }
}
