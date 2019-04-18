package ru.taximaxim.codekeeper.ui.pgdbproject;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts;
import ru.taximaxim.codekeeper.ui.Log;
import ru.taximaxim.codekeeper.ui.UIConsts;
import ru.taximaxim.codekeeper.ui.UIConsts.PROJ_PREF;
import ru.taximaxim.codekeeper.ui.dbstore.DbInfo;
import ru.taximaxim.codekeeper.ui.dbstore.DbStorePicker;
import ru.taximaxim.codekeeper.ui.differ.DbSource;
import ru.taximaxim.codekeeper.ui.localizations.Messages;

class DbSourcePicker extends Composite {

    private final PageDiff pageDiff;
    private final DbStorePicker storePicker;
    private final ComboViewer cmbEncoding;

    public DbSourcePicker(Composite parent, String groupTitle, IPreferenceStore mainPrefs,
            final PageDiff pageDiff) {
        super(parent, SWT.NONE);

        this.pageDiff = pageDiff;

        FillLayout fl = new FillLayout();
        fl.marginHeight = fl.marginWidth = 0;
        setLayout(fl);

        Group sourceComp = new Group(this, SWT.NONE);
        sourceComp.setLayout(new GridLayout(2, false));
        sourceComp.setText(groupTitle);

        storePicker = new DbStorePicker(sourceComp, mainPrefs, true, true, false);
        storePicker.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false, 2, 1));

        new Label(sourceComp, SWT.NONE).setText(Messages.diffWizard_target_encoding);

        cmbEncoding = new ComboViewer(sourceComp, SWT.BORDER | SWT.DROP_DOWN);
        cmbEncoding.setContentProvider(ArrayContentProvider.getInstance());
        cmbEncoding.setLabelProvider(new LabelProvider());
        cmbEncoding.setInput(UIConsts.ENCODINGS);
        cmbEncoding.getCombo().setText(ApgdiffConsts.UTF_8);

        storePicker.addListenerToCombo(event -> {
            pageDiff.getWizard().getContainer().updateButtons();
            File dir = storePicker.getPathOfDir();
            PgDbProject project = null;
            boolean isProject = dir != null && (project = getProjectFromDir(dir)) != null;
            if (isProject) {
                try {
                    cmbEncoding.getCombo().setText(project.getProjectCharset());
                    pageDiff.setTimezone(project.getPrefs().get(PROJ_PREF.TIMEZONE, pageDiff.getTimezone()));
                } catch (CoreException ex) {
                    Log.log(ex);
                }
            }
            cmbEncoding.getControl().setEnabled(!isProject);
        });

    }

    public void setDbStore(IStructuredSelection selection) {
        storePicker.setSelection(selection);
    }

    public DbInfo getSelectedDbInfo() {
        return storePicker.getDbInfo();
    }

    public String getEncoding() {
        return cmbEncoding.getCombo().getText();
    }

    public DbSource getDbSource(boolean isMsSql) {
        final boolean forceUnixNewlines = true; // true by default, check project if path is given
        DbInfo dbInfo;
        File file;
        File dir;
        if ((dbInfo = storePicker.getDbInfo()) != null) {
            return DbSource.fromDbInfo(dbInfo, forceUnixNewlines, getEncoding(), pageDiff.getTimezone());
        } else if ((file = storePicker.getPathOfFile()) != null) {
            return DbSource.fromFile(forceUnixNewlines, file, getEncoding(), isMsSql);
        } else if ((dir = storePicker.getPathOfDir()) != null) {
            PgDbProject project = getProjectFromDir(dir);
            if (project != null) {
                return DbSource.fromProject(project);
            } else {
                return DbSource.fromDirTree(forceUnixNewlines, dir.getAbsolutePath(),
                        getEncoding(), isMsSql);
            }
        }
        return null;
    }

    private PgDbProject getProjectFromDir(File dir) {
        IContainer[] conts = ResourcesPlugin.getWorkspace().getRoot().findContainersForLocationURI(dir.toURI());
        IProject project = null;
        for (IContainer cont : conts) {
            if (cont instanceof IProject && ((IProject) cont).isOpen()) {
                if (project == null) {
                    project = (IProject) cont;
                } else {
                    // ambiguous project: work as if with a plain directory
                    project = null;
                    break;
                }
            }
        }
        return project == null ? null : new PgDbProject(project);
    }
}
