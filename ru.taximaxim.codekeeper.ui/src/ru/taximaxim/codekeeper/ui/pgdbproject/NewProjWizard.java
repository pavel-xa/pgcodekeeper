package ru.taximaxim.codekeeper.ui.pgdbproject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.osgi.service.prefs.BackingStoreException;

import cz.startnet.utils.pgdiff.loader.JdbcConnector;
import cz.startnet.utils.pgdiff.loader.JdbcRunner;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts;
import ru.taximaxim.codekeeper.ui.Activator;
import ru.taximaxim.codekeeper.ui.Log;
import ru.taximaxim.codekeeper.ui.UIConsts;
import ru.taximaxim.codekeeper.ui.UIConsts.FILE;
import ru.taximaxim.codekeeper.ui.UIConsts.PROJ_PREF;
import ru.taximaxim.codekeeper.ui.UIConsts.WIZARD;
import ru.taximaxim.codekeeper.ui.UIConsts.WORKING_SET;
import ru.taximaxim.codekeeper.ui.dbstore.DbInfo;
import ru.taximaxim.codekeeper.ui.dbstore.DbStorePicker;
import ru.taximaxim.codekeeper.ui.dialogs.ExceptionNotifier;
import ru.taximaxim.codekeeper.ui.differ.DbSource;
import ru.taximaxim.codekeeper.ui.handlers.OpenEditor;
import ru.taximaxim.codekeeper.ui.localizations.Messages;

public class NewProjWizard extends Wizard
implements IExecutableExtension, INewWizard {

    private PageRepo pageRepo;
    private PageDb pageDb;

    private final IPreferenceStore mainPrefStore;
    private boolean isPostgres;
    private IConfigurationElement config;
    private IWorkbench workbench;
    private IStructuredSelection selection;

    public NewProjWizard() {
        this.mainPrefStore = Activator.getDefault().getPreferenceStore();

        setWindowTitle(Messages.newProjWizard_new_pg_db_project);
        setDefaultPageImageDescriptor(ImageDescriptor.createFromURL(
                Activator.getDefault().getBundle().getResource(FILE.ICONAPPWIZ)));
        setNeedsProgressMonitor(true);
    }

    @Override
    public void addPages() {
        // page names shouldn't be localized, use page titles instead
        pageRepo = new PageRepo("main", selection); //$NON-NLS-1$
        addPage(pageRepo);
        pageDb = new PageDb(isPostgres, "schema", mainPrefStore); //$NON-NLS-1$
        addPage(pageDb);
    }

    @Override
    public boolean performFinish() {
        PgDbProject props = null;
        boolean initSuccess = false;
        try {
            props = PgDbProject.createPgDbProject(pageRepo.getProjectHandle(),
                    pageRepo.useDefaults() ? null : pageRepo.getLocationURI(), !isPostgres);
            props.getProject().open(null);
            props.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);

            String charset = pageDb.getCharset();
            if (!charset.isEmpty() && !ResourcesPlugin.getWorkspace().getRoot()
                    .getDefaultCharset().equals(charset)) {
                props.setProjectCharset(charset);
            }

            boolean applyProps = false;
            if (isPostgres) {
                String timezone = pageDb.getTimeZone();
                if (!timezone.isEmpty() && !ApgdiffConsts.UTC.equals(timezone)) {
                    props.getPrefs().put(PROJ_PREF.TIMEZONE, timezone);
                    applyProps = true;
                }
            }

            if (pageDb.isBound()) {
                DbInfo dbInfo = pageDb.getDbInfo();
                if (dbInfo != null) {
                    props.getPrefs().put(PROJ_PREF.NAME_OF_BOUND_DB, dbInfo.getName());
                    applyProps = true;
                }
            }

            if (applyProps) {
                try {
                    props.getPrefs().flush();
                } catch (BackingStoreException e) {
                    Log.log(Log.LOG_WARNING, "Error while flushing project properties!", e); //$NON-NLS-1$
                }
            }

            getContainer().run(true, true, new InitProjectFromSource(
                    props, getDbSource(props)));
            initSuccess = true;

            props.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);

            IWorkingSet[] workingSets = pageRepo.getSelectedWorkingSets();
            workbench.getWorkingSetManager().addToWorkingSets(props.getProject(), workingSets);

            BasicNewProjectResourceWizard.updatePerspective(config);
            BasicNewResourceWizard.selectAndReveal(props.getProject(),
                    workbench.getActiveWorkbenchWindow());
            OpenEditor.openEditor(workbench.getActiveWorkbenchWindow().getActivePage(),
                    props.getProject());
        } catch (CoreException e) {
            ExceptionNotifier.notifyDefault(Messages.NewProjWizard_error_creating_project, e);
        } catch (InvocationTargetException ex) {
            ExceptionNotifier.notifyDefault(
                    Messages.newProjWizard_error_in_initializing_repo_from_source, ex);
        } catch (InterruptedException ex) {
            // cancelled
        } finally {
            if (!initSuccess && props != null) {
                try {
                    props.deleteFromWorkspace();
                } catch (CoreException e) {
                    ExceptionNotifier.notifyDefault(Messages.NewProjWizard_cannot_delete_project, e);
                }
            }
        }
        return initSuccess;
    }

    private DbSource getDbSource(PgDbProject props) throws CoreException {
        DbSource src;
        DbInfo dbinfo = pageDb.getDbInfo();
        File dump;

        boolean forceUnixNewlines = props.getPrefs().getBoolean(PROJ_PREF.FORCE_UNIX_NEWLINES, true);
        String charset = props.getProjectCharset();
        String timezone = props.getPrefs().get(PROJ_PREF.TIMEZONE, ApgdiffConsts.UTC);

        if (!pageDb.isInit()) {
            src = DbSource.fromDbObject(new PgDatabase(), "Empty DB"); //$NON-NLS-1$
        } else if (dbinfo != null) {
            src = DbSource.fromDbInfo(dbinfo, forceUnixNewlines, charset, timezone, props.getProject());
        } else if ((dump = pageDb.getDumpPath()) != null) {
            src = DbSource.fromFile(forceUnixNewlines, dump, charset, !isPostgres, props.getProject());
        } else {
            // should be prevented by page completion state
            throw new IllegalStateException(Messages.initProjectFromSource_init_request_but_no_schema_source);
        }

        return src;
    }

    @Override
    public void setInitializationData(IConfigurationElement config,
            String propertyName, Object data) throws CoreException {
        this.config = config;
        this.isPostgres = WIZARD.NEW_PROJECT_WIZARD.equals(config.getAttribute("id")); //$NON-NLS-1$
        if (!isPostgres) {
            setWindowTitle(Messages.NewProjWizard_new_ms_project);
        }
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.workbench = workbench;
        this.selection = selection;
    }
}

class PageRepo extends WizardNewProjectCreationPage {

    private final IStructuredSelection selection;

    PageRepo(String pageName, IStructuredSelection selection) {
        super(pageName);
        this.selection = selection;

        setTitle(Messages.newProjWizard_new_pg_db_project);
        setDescription(Messages.NewProjWizard_create_project);
    }

    @Override
    public void createControl(final Composite parent) {
        super.createControl(parent);
        createWorkingSetGroup((Composite) getControl(), selection,
                new String[] { WORKING_SET.RESOURCE_WORKING_SET });
    }

    @Override
    public boolean isPageComplete() {
        if (!super.isPageComplete()) {
            return false;
        }

        String err = null;
        Path path = Paths.get(getLocationURI());
        if (Files.exists(path) && path.toFile().list().length > 0) {
            err = Messages.NewProjWizard_not_empty_dir;
        }

        setErrorMessage(err);
        return err == null;
    }
}

class PageDb extends WizardPage {

    private static final String QUERY_TZ = "SELECT name, setting FROM pg_catalog.pg_file_settings" //$NON-NLS-1$
            + " WHERE pg_catalog.lower(name) = 'timezone' AND applied AND error IS NULL"; //$NON-NLS-1$

    private final IPreferenceStore mainPrefs;
    private final boolean isPostgres;

    private Button btnInit;
    private Button btnBind;
    private Button btnGetTz;
    private DbStorePicker storePicker;
    private ComboViewer timezoneCombo;
    private ComboViewer charsetCombo;
    private CLabel lblWarnPosix;


    public DbInfo getDbInfo() {
        return storePicker.getDbInfo();
    }

    public File getDumpPath() {
        return storePicker.getPathOfFile();
    }

    public String getCharset(){
        return charsetCombo.getCombo().getText();
    }

    public boolean isInit() {
        return btnInit.getSelection();
    }

    public boolean isBound() {
        return btnBind.getSelection();
    }

    public String getTimeZone(){
        return timezoneCombo.getCombo().getText();
    }

    PageDb(boolean isPostgres, String pageName, IPreferenceStore mainPrefs) {
        super(pageName, pageName, null);
        this.mainPrefs = mainPrefs;
        this.isPostgres = isPostgres;

        setTitle(Messages.NewProjWizard_proj_init);
        setDescription(Messages.NewProjWizard_proj_init_src);
    }

    @Override
    public void createControl(final Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(3, false));

        //init db block
        Group group = new Group(container, SWT.NONE);
        group.setText(Messages.NewProjWizard_initializing_title);
        group.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false, 3, 1));
        group.setLayout(new GridLayout(2, true));

        btnInit = new Button(group, SWT.CHECK);
        btnInit.setText(Messages.NewProjWizard_initializing_check);
        btnInit.setSelection(true);
        btnInit.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                modifyButtons();
            }
        });

        btnBind = new Button(group, SWT.CHECK);
        btnBind.setText(Messages.ProjectProperties_binding_to_db_connection);
        btnBind.setSelection(false);
        btnBind.setEnabled(false);

        storePicker = new DbStorePicker(group, mainPrefs, true, false, false);
        storePicker.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false, 2, 1));
        storePicker.filter(!isPostgres);
        storePicker.addListenerToCombo(e -> modifyButtons());

        //char sets
        new Label(container, SWT.NONE).setText(Messages.NewProjWizard_select_charset);

        charsetCombo = new ComboViewer(container, SWT.DROP_DOWN);
        charsetCombo.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false, 2, 1));
        charsetCombo.setContentProvider(ArrayContentProvider.getInstance());
        charsetCombo.setInput(UIConsts.ENCODINGS);
        charsetCombo.setSelection(new StructuredSelection(ApgdiffConsts.UTF_8));

        //time zones
        if (isPostgres) {
            new Label(container, SWT.NONE).setText(Messages.NewProjWizard_select_time_zone);

            timezoneCombo = new ComboViewer(container, SWT.DROP_DOWN);
            timezoneCombo.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
            timezoneCombo.setContentProvider(ArrayContentProvider.getInstance());
            timezoneCombo.setInput(UIConsts.TIME_ZONES);
            timezoneCombo.setSelection(new StructuredSelection(ApgdiffConsts.UTC));
            timezoneCombo.getCombo().addModifyListener(e -> timeZoneWarn());

            btnGetTz = new Button(container, SWT.PUSH);
            btnGetTz.setText(Messages.NewProjWizard_get_from_db);
            btnGetTz.setEnabled(storePicker.getDbInfo() != null);
            btnGetTz.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    DbInfo dbinfo = getDbInfo();
                    if (dbinfo == null) {
                        return;
                    }
                    try {
                        TimeZoneProgress progress = new TimeZoneProgress(dbinfo);
                        getContainer().run(true, true, progress);
                        String timezone = progress.timezone;
                        timezoneCombo.getCombo().setText(timezone == null ? ApgdiffConsts.UTC : timezone);
                    } catch (InterruptedException ex) {
                        // cancelled
                    } catch (InvocationTargetException ex) {
                        ExceptionNotifier.notifyDefault(Messages.NewProjWizard_error_tz_query, ex);
                    }
                }
            });

            lblWarnPosix = new CLabel(container, SWT.NONE);
            lblWarnPosix.setImage(Activator.getEclipseImage(ISharedImages.IMG_OBJS_WARN_TSK));
            lblWarnPosix.setText(Messages.ProjectProperties_posix_is_used_warn);
            GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, false, false, 3, 1);
            gd.exclude = true;
            lblWarnPosix.setLayoutData(gd);
        }

        setControl(container);
    }

    private void modifyButtons() {
        boolean init = btnInit.getSelection();
        if (isPostgres) {
            btnGetTz.setEnabled(init && storePicker.getDbInfo() != null);
        }
        storePicker.setComboEnabled(init);

        if (getDbInfo() == null) {
            btnBind.setSelection(false);
            btnBind.setEnabled(false);
        } else {
            btnBind.setEnabled(true);
        }

        getWizard().getContainer().updateButtons();
        getWizard().getContainer().updateMessage();
    }

    private void timeZoneWarn() {
        String tz =  timezoneCombo.getCombo().getText();
        GridData data = (GridData) lblWarnPosix.getLayoutData();
        if ((!ApgdiffConsts.UTC.equals(tz)
                && tz.startsWith(ApgdiffConsts.UTC)) == data.exclude)  {
            lblWarnPosix.setVisible(data.exclude);
            data.exclude = !data.exclude;
            lblWarnPosix.getParent().layout();
        }
    }

    @Override
    public boolean isPageComplete() {
        return !btnInit.getSelection() || storePicker.getDbInfo() != null || storePicker.getPathOfFile() != null;
    }

    private static class TimeZoneProgress implements IRunnableWithProgress {

        private final DbInfo dbinfo;
        private String timezone;

        public TimeZoneProgress(DbInfo dbinfo) {
            this.dbinfo = dbinfo;
        }

        @Override
        public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {

            JdbcConnector connector = new JdbcConnector(dbinfo.getDbHost(), dbinfo.getDbPort(),
                    dbinfo.getDbUser(), dbinfo.getDbPass(), dbinfo.getDbName(), dbinfo.getProperties(),
                    dbinfo.isReadOnly(), ApgdiffConsts.UTC);

            try (Connection connection = connector.getConnection();
                    Statement st = connection.createStatement();
                    ResultSet rs = new JdbcRunner(monitor).runScript(st, QUERY_TZ)) {
                timezone = rs.next() ? rs.getString("setting") : null; //$NON-NLS-1$
            } catch (SQLException | IOException e) {
                throw new InvocationTargetException(e, e.getLocalizedMessage());
            }
        }
    }
}