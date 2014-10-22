package ru.taximaxim.codekeeper.ui.differ;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import ru.taximaxim.codekeeper.apgdiff.model.difftree.TreeElement;
import ru.taximaxim.codekeeper.ui.Activator;
import ru.taximaxim.codekeeper.ui.Log;
import ru.taximaxim.codekeeper.ui.PgCodekeeperUIException;
import ru.taximaxim.codekeeper.ui.UIConsts.PLUGIN_ID;
import ru.taximaxim.codekeeper.ui.UIConsts.PREF;
import ru.taximaxim.codekeeper.ui.UIConsts.PROJ_PREF;
import ru.taximaxim.codekeeper.ui.dbstore.DbPicker;
import ru.taximaxim.codekeeper.ui.dialogs.ExceptionNotifier;
import ru.taximaxim.codekeeper.ui.localizations.Messages;
import ru.taximaxim.codekeeper.ui.pgdbproject.PgDbProject;
import ru.taximaxim.codekeeper.ui.prefs.PreferenceInitializer;

public abstract class DiffPresentationPane extends Composite {

    // should be true for commit, false for diff script
    private final boolean isProjSrc;

    private final String exePgdump;
    private final String pgdumpCustom;

    protected final DiffTableViewer diffTable;
    private final Composite containerSrc;
    private final Composite containerDb;
    protected final Button btnDump, btnPgDump, btnJdbc;
    private final Button btnGetChanges;
    private final Button btnFlipDbPicker;
    protected final DbPicker dbSrc;
    private final DiffPaneViewer diffPane;
    private final Label lblSourceInfo;
    
    protected DbSource dbSource;
    protected DbSource dbTarget;
    protected TreeDiffer treeDiffer;

    private void setDbSource(DbSource dbSource) {
        this.dbSource = dbSource;
        setDiffPaneDb(isProjSrc, dbSource);
    }

    private void setDbTarget(DbSource dbTarget) {
        this.dbTarget = dbTarget;
        setDiffPaneDb(!isProjSrc, dbTarget);
    }

    private void setDiffPaneDb(boolean isDbSrc, DbSource db) {
        if (diffPane != null) {
            if (isDbSrc)
                diffPane.setDbSource(db);
            else 
                diffPane.setDbTarget(db);    
        }
    }
    
    public DiffPresentationPane(Composite parent, boolean projIsSrc,
            final IPreferenceStore mainPrefs, final PgDbProject proj) {
        super(parent, SWT.NONE);
        setLayout(new GridLayout());

        this.isProjSrc = projIsSrc;
        exePgdump = mainPrefs.getString(PREF.PGDUMP_EXE_PATH);
        pgdumpCustom = mainPrefs.getString(PREF.PGDUMP_CUSTOM_PARAMS);
        final IEclipsePreferences projProps = proj.getPrefs();

        // upper container
        final Composite containerUpper = new Composite(this, SWT.NONE);
        containerUpper.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        // initialize default layout for customizable container
        GridLayout gl = new GridLayout(3, false);
        gl.marginHeight = gl.marginWidth = 0;
        containerUpper.setLayout(gl);
        
        // upper left part
        Composite contUpperLeft = new Composite(containerUpper, SWT.NONE);
        contUpperLeft.setLayoutData(new GridData(GridData.FILL_BOTH));
        gl = new GridLayout();
        gl.marginHeight = gl.marginWidth = 0;
        contUpperLeft.setLayout(gl);
        createUpperContainer(contUpperLeft, gl);
        
        // upper middle part
        lblSourceInfo = new Label(containerUpper, SWT.NONE);
        lblSourceInfo.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, true, 1, 1));

        // upper right part
        btnGetChanges = new Button(containerUpper, SWT.PUSH);
        btnGetChanges.setText(Messages.get_changes);
        btnGetChanges.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    loadChanges(proj, projProps, mainPrefs);
                } catch (PgCodekeeperUIException e1) {
                    ExceptionNotifier.showErrorDialog(
                            Messages.DiffPresentationPane_error_loading_changes, e1);
                }
            }
        });

        GridData gd = new GridData(SWT.RIGHT, SWT.FILL, false, true, 1, 1);
        gd.widthHint = btnGetChanges.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
        gd.minimumWidth = btnGetChanges.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
        gd.horizontalIndent = 20;
        btnGetChanges.setLayoutData(gd);
        // end upper container

        SashForm sashOuter = new SashForm(this, SWT.VERTICAL | SWT.SMOOTH);
        sashOuter.setLayoutData(new GridData(GridData.FILL_BOTH));

        // middle container
        containerDb = new Composite(sashOuter, SWT.NONE);
        gl = new GridLayout(3, false);
        gl.marginHeight = gl.marginWidth = 0;
        gl.horizontalSpacing = gl.verticalSpacing = 2;
        containerDb.setLayout(gl);

        diffTable = new DiffTableViewer(containerDb, SWT.NONE, mainPrefs, false);
        diffTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        diffTable.viewer.addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                StructuredSelection selection = ((StructuredSelection) event
                        .getSelection());

                if (selection.size() != 1) {
                    diffPane.setInput(null);
                } else {
                    TreeElement el = (TreeElement) selection.getFirstElement();
                    diffPane.setInput(el);
                }
            }
        });

        // flip button set up
        btnFlipDbPicker = new Button(containerDb, SWT.PUSH | SWT.FLAT);
        btnFlipDbPicker.setText("\u25B8"); //$NON-NLS-1$
        gd = new GridData(GridData.FILL_VERTICAL);
        gd.widthHint = 20;
        btnFlipDbPicker.setLayoutData(gd);
        btnFlipDbPicker.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                flipDbPicker(containerSrc.getVisible());
            }
        });

        // middle right container
        containerSrc = new Composite(containerDb, SWT.NONE);
        gl = new GridLayout(2, false);
        gl.marginHeight = gl.marginWidth = 0;
        containerSrc.setLayout(gl);

        gd = new GridData(SWT.FILL, SWT.FILL, false, true);
        gd.minimumWidth = 300;
        gd.widthHint = 300;
        containerSrc.setLayoutData(gd);

        Group grpSrc = new Group(containerSrc, SWT.NONE);
        grpSrc.setText(isProjSrc ? Messages.commitPartDescr_get_changes_from
                : Messages.diffPartDescr_get_changes_for);
        grpSrc.setLayout(new GridLayout(3, false));

        btnDump = new Button(grpSrc, SWT.RADIO);
        btnDump.setText(Messages.dump);
        btnDump.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                showDbPicker(false);
                lblSourceInfo.setText(getSourceInfoText());
                containerUpper.layout();
            }
        });

        btnPgDump = new Button(grpSrc, SWT.RADIO);
        btnPgDump.setText(Messages.db);
        btnPgDump.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                showDbPicker(true);
                lblSourceInfo.setText(getSourceInfoText());
                containerUpper.layout();
            }
        });

        btnJdbc = new Button(grpSrc, SWT.RADIO);
        btnJdbc.setText(Messages.jdbc);
        btnJdbc.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                showDbPicker(true);
                lblSourceInfo.setText(getSourceInfoText());
                containerUpper.layout();
            }
        });
        
        dbSrc = new DbPicker(containerSrc, SWT.NONE, mainPrefs, false);
        dbSrc.setText(Messages.db_source);
        dbSrc.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false, 2, 1));
        dbSrc.addListener(SWT.Modify, new Listener() {
            
            @Override
            public void handleEvent(Event event) {
                lblSourceInfo.setText(getSourceInfoText());
            }
        });
        
        boolean useDbPicker = false;
        String src = projProps.get(PROJ_PREF.SOURCE, ""); //$NON-NLS-1$
        if (src.equals(PROJ_PREF.SOURCE_TYPE_DUMP)) {
            btnDump.setSelection(true);
        } else {
            btnPgDump.setSelection(true);
            useDbPicker = true;
        }
        showDbPicker(useDbPicker);

        if (useDbPicker) {
            dbSrc.txtDbName.setText(projProps.get(PROJ_PREF.DB_NAME, "")); //$NON-NLS-1$
            dbSrc.txtDbUser.setText(projProps.get(PROJ_PREF.DB_USER, "")); //$NON-NLS-1$
            dbSrc.txtDbPass.setText(projProps.get(PROJ_PREF.DB_PASS, "")); //$NON-NLS-1$
            dbSrc.txtDbHost.setText(projProps.get(PROJ_PREF.DB_HOST, "")); //$NON-NLS-1$
            dbSrc.txtDbPort
                    .setText(String.valueOf(projProps.getInt(PROJ_PREF.DB_PORT, 0)));
        }
        // end middle right container
        
        // read flip position from preferences
        flipDbPicker(Activator.getDefault().getPreferenceStore().getBoolean(PREF.IS_FLIPPED_DB_SOURCE));       
        // end middle container
        
        lblSourceInfo.setText(getSourceInfoText());

        diffPane = new DiffPaneViewer(sashOuter, SWT.NONE, isProjSrc ? dbSource
                : dbTarget, isProjSrc ? dbTarget : dbSource, !isProjSrc);
    }

    private void flipDbPicker(boolean open){
        containerSrc.setVisible(!open);
        ((GridData) containerSrc.getLayoutData()).exclude = open;
        containerDb.layout();

        btnFlipDbPicker.setText(open ? "\u25C2" // ◂ //$NON-NLS-1$
                : "\u25B8"); // ▸ //$NON-NLS-1$
        PreferenceInitializer.savePreference(Activator.getDefault().getPreferenceStore(), 
                PREF.IS_FLIPPED_DB_SOURCE, String.valueOf(open));
    }
    
    private String getSourceInfoText(){
        StringBuilder value = new StringBuilder().append(Messages.source);
        if (btnDump.getSelection()){
            return value.append(Messages.dump_file).toString();
        }else if (btnPgDump.getSelection()){
            value.append(Messages.pg_dump);
        }else if (btnJdbc.getSelection()){
            value.append(Messages.jdbc);
        }
        String preset = dbSrc.getSelectedDbPresetName();
        if (preset.isEmpty()){
            value.append("     " + Messages.connection_details);
            value.append(dbSrc.txtDbUser.getText().isEmpty() ? "" : dbSrc.txtDbUser.getText() + "@");
            value.append(dbSrc.txtDbHost.getText().isEmpty() ? Messages.unknown_host : dbSrc.txtDbHost.getText());
            value.append(dbSrc.txtDbPort.getText().isEmpty() ? "" : ":" + dbSrc.txtDbPort.getText());
            value.append("/");
            value.append(dbSrc.txtDbName.getText().isEmpty() ? Messages.unknown_db : dbSrc.txtDbName.getText());
        }else{
            value.append("     " + Messages.commitPartDescr_used_connection_template + "[" + preset + "]");
        }
        return value.toString();
    }
    
    private void showDbPicker(boolean show) {
        ((GridData) dbSrc.getLayoutData()).exclude = !show;
        dbSrc.setVisible(show);

        dbSrc.getParent().layout();
    }

    private void loadChanges(PgDbProject proj, IEclipsePreferences projProps,
            IPreferenceStore mainPrefs) throws PgCodekeeperUIException {
        DbSource dbsProj, dbsRemote;
        dbsProj = DbSource.fromProject(proj);
        if (btnDump.getSelection()) {
            FileDialog dialog = new FileDialog(getShell());
            dialog.setText(Messages.choose_dump_file_with_changes);
            String dumpfile = dialog.open();
            if (dumpfile == null) {
                return;
            }
            dbsRemote = DbSource
                    .fromFile(dumpfile, projProps.get(PROJ_PREF.ENCODING, "")); //$NON-NLS-1$
        } else if (btnPgDump.getSelection()) {
            String sPort = dbSrc.txtDbPort.getText();
            int port = sPort.isEmpty() ? 0 : Integer.parseInt(sPort);

            dbsRemote = DbSource.fromDb(exePgdump, pgdumpCustom,
                    dbSrc.txtDbHost.getText(), port, dbSrc.txtDbUser.getText(),
                    dbSrc.txtDbPass.getText(), dbSrc.txtDbName.getText(),
                    projProps.get(PROJ_PREF.ENCODING, "")); //$NON-NLS-1$
        } else if (btnJdbc.getSelection()){
            String sPort = dbSrc.txtDbPort.getText();
            int port = sPort.isEmpty() ? 0 : Integer.parseInt(sPort);

            dbsRemote = DbSource.fromJdbc(dbSrc.txtDbHost.getText(), port, dbSrc.txtDbUser.getText(),
                    dbSrc.txtDbPass.getText(), dbSrc.txtDbName.getText(),
                    projProps.get(PROJ_PREF.ENCODING, "")); //$NON-NLS-1$
        }else {
            throw new PgCodekeeperUIException(Messages.undefined_source_for_db_changes);
        }

        setDbSource(isProjSrc ? dbsProj : dbsRemote);
        setDbTarget(isProjSrc ? dbsRemote : dbsProj);

        Log.log(Log.LOG_INFO, "Getting changes for diff"); //$NON-NLS-1$
        treeDiffer = new TreeDiffer(dbSource, dbTarget);
        
        Job job = new Job(Messages.diffPresentationPane_getting_changes_for_diff) {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    treeDiffer.run(monitor);
                } catch (InvocationTargetException e) {
                    return new Status(Status.ERROR, PLUGIN_ID.THIS, 
                            Messages.error_in_differ_thread, e);
                }
                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }
                return Status.OK_STATUS;
            }
        };
        job.addJobChangeListener(new JobChangeAdapter() {
            
            @Override
            public void done(IJobChangeEvent event) {
                if (event.getResult().isOK()) {
                    Display.getDefault().asyncExec(new Runnable() {
                        
                        @Override
                        public void run() {
                            if (DiffPresentationPane.this.isDisposed()) {
                                return;
                            }
                            diffTable.setInput(treeDiffer, !isProjSrc);
                            diffPane.setInput(null);
                            diffLoaded();
                        }
                    });
                }
            }
        });
        job.setUser(true);
        job.schedule();
    }

    /**
     * @param container
     *            has {@link GridLayout} with 0 margins set by default
     * @param gl
     *            pre-made {@link GridLayout} of the container
     */
    protected abstract void createUpperContainer(Composite container, GridLayout gl);
    
    /**
     * Allows clients to make actions after a diff has been loaded.
     */
    protected void diffLoaded() {
    };
    
    public void reset() {
        diffTable.setInput(null, !isProjSrc);
        diffPane.setInput(null);
    };
}
