
package ru.taximaxim.codekeeper.ui.parts;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import ru.taximaxim.codekeeper.ui.UIConsts.PREF;


public class TestPart {
    
    @Inject
    @Preference(PREF.PGDUMP_CUSTOM_PARAMS)
    private String prefPgdumpPath;
    
    @Inject
    @Named(IServiceConstants.ACTIVE_SHELL)
    private Shell shell_;
    
    private Text txt;
    
    @PostConstruct
    private void createUI(final Composite parent) {
        final Shell shell = parent.getShell();
        
        parent.setLayout(new FormLayout());
        Button btn = new Button(parent, SWT.PUSH);
        btn.setText("qwe"); //$NON-NLS-1$
        
        FormData data = new FormData();
        data.left = data.top = new FormAttachment(5);
        btn.setLayoutData(data);
        
        btn.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                
                txt.setText(txt.getText() + "\n" + //$NON-NLS-1$
                        System.getenv("SWT_GTK3")); //$NON-NLS-1$
                
                
            }
        });
        
        txt = new Text(parent, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
        data = new FormData();
        data.left = new FormAttachment(5);
        data.right = new FormAttachment(95);
        data.bottom = new FormAttachment(95);
        data.top = new FormAttachment(btn, 5);
        txt.setLayoutData(data);
    }
}
