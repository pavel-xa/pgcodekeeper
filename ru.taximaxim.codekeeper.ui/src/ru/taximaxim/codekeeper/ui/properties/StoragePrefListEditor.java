package ru.taximaxim.codekeeper.ui.properties;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;

import ru.taximaxim.codekeeper.ui.localizations.Messages;
import ru.taximaxim.codekeeper.ui.prefs.PrefListEditor;

public class StoragePrefListEditor extends PrefListEditor<String, ListViewer> {

    public StoragePrefListEditor(Composite parent) {
        super(parent);
    }

    @Override
    protected ListViewer createViewer(Composite parent) {
        ListViewer viewerObjs = new ListViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        GridData gd =  new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2);
        viewerObjs.getControl().setLayoutData(gd);

        viewerObjs.setContentProvider(ArrayContentProvider.getInstance());
        viewerObjs.setLabelProvider(new LabelProvider());
        return viewerObjs;
    }

    @Override
    protected String getNewObject(String oldObject) {
        FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
        fd.setText(Messages.StoragePrefListEditor_open_snapshot);
        String[] filterExt = {"*.ser"}; //$NON-NLS-1$
        fd.setFilterExtensions(filterExt);
        return fd.open();
    }

    @Override
    protected String errorAlreadyExists(String obj) {
        return Messages.StoragePrefListEditor_file_already_added;
    }
}
