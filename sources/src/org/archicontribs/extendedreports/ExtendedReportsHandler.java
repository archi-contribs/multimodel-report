/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.extendedreports;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

import com.archimatetool.editor.ui.components.ExtendedWizardDialog;
import com.archimatetool.model.IArchimateModel;



/**
 * Command Action Handler for Extended Reports
 * 
 * @author Phillip Beauvoir
 * @author Vincent Boulet
 */
public class ExtendedReportsHandler extends AbstractHandler {
    
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchPart part = HandlerUtil.getActivePart(event);
        IArchimateModel model = part != null ? part.getAdapter(IArchimateModel.class) : null;
        
        if(model != null) {
            ExtendedReportsWizard wizard = new ExtendedReportsWizard();
            
            WizardDialog dialog = new ExtendedWizardDialog(HandlerUtil.getActiveShell(event),
                    wizard,
                    "ExtendedReportsWizard"); //$NON-NLS-1$
            
            if(dialog.open() == Window.OK) {
                try {
                    wizard.runWithProgress();
                }
                catch(InvocationTargetException | InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }

        return null;
    }
    
}
