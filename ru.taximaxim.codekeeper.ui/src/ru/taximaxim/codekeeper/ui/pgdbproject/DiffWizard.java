package ru.taximaxim.codekeeper.ui.pgdbproject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;

import cz.startnet.utils.pgdiff.schema.PgDatabase;
import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts;
import ru.taximaxim.codekeeper.ui.Activator;
import ru.taximaxim.codekeeper.ui.UIConsts;
import ru.taximaxim.codekeeper.ui.UIConsts.FILE;
import ru.taximaxim.codekeeper.ui.dbstore.DbInfo;
import ru.taximaxim.codekeeper.ui.dialogs.ExceptionNotifier;
import ru.taximaxim.codekeeper.ui.differ.DbSource;
import ru.taximaxim.codekeeper.ui.differ.DiffTableViewer;
import ru.taximaxim.codekeeper.ui.differ.Differ;
import ru.taximaxim.codekeeper.ui.differ.TreeDiffer;
import ru.taximaxim.codekeeper.ui.fileutils.FileUtilsUi;
import ru.taximaxim.codekeeper.ui.localizations.Messages;

public class DiffWizard extends Wizard implements IPageChangingListener {

    private PageDiff pageDiff;
    private PagePartial pagePartial;

    private final PgDbProject proj;
    private final IPreferenceStore mainPrefs;

    public DiffWizard(PgDbProject proj, IPreferenceStore mainPrefs) {
        this.proj = proj;
        this.mainPrefs = mainPrefs;

        setWindowTitle(Messages.diffWizard_Diff);
        setDefaultPageImageDescriptor(ImageDescriptor.createFromURL(
                Activator.getContext().getBundle().getResource(FILE.ICONAPPWIZ)));
        setNeedsProgressMonitor(true);
    }

    @Override
    public void addPages() {
        pageDiff = new PageDiff(Messages.diffWizard_diff_parameters, mainPrefs, proj);
        pagePartial = new PagePartial(Messages.diffWizard_diff_tree);

        addPage(pageDiff);
        addPage(pagePartial);
    }

    @Override
    public void createPageControls(Composite pageContainer) {
        super.createPageControls(pageContainer);
        ((WizardDialog) getContainer()).addPageChangingListener(this);
    }

    @Override
    public void handlePageChanging(PageChangingEvent e) {
        if (e.getCurrentPage() == pageDiff && e.getTargetPage() == pagePartial) {
            DbSource dbSource = pageDiff.getDbSource();
            DbSource dbTarget = pageDiff.getDbTarget();
            TreeDiffer treediffer = new TreeDiffer(dbSource, dbTarget);

            try {
                getContainer().run(true, true, treediffer);
            }  catch (InvocationTargetException ex) {
                e.doit = false;
                ExceptionNotifier.notifyDefault(Messages.error_in_differ_thread, ex);
                return;
            } catch (InterruptedException ex) {
                // cancelled
                e.doit = false;
                return;
            }

            pagePartial.setData(dbSource.getOrigin(), dbTarget.getOrigin(), treediffer);
            getShell().layout(true, true);
        }
    }

    @Override
    public boolean canFinish() {
        if (getContainer().getCurrentPage() != pagePartial) {
            return false;
        }
        return super.canFinish();
    }

    @Override
    public boolean performFinish() {
        try {
            TreeDiffer treediffer = pagePartial.getTreeDiffer();
            PgDatabase source = treediffer.getDbSource().getDbObject();

            Differ differ = new Differ(source, treediffer.getDbTarget().getDbObject(),
                    treediffer.getDiffTree(), false, pageDiff.getTimezone(),
                    source.getArguments().isMsSql(), null);
            getContainer().run(true, true, differ);

            FileUtilsUi.saveOpenTmpSqlEditor(differ.getDiffDirect(),
                    "diff_wizard_result", source.getArguments().isMsSql());
            return true;
        } catch (InvocationTargetException ex) {
            ExceptionNotifier.notifyDefault(Messages.error_in_differ_thread, ex);
        } catch (InterruptedException ex) {
            // cancelled
        } catch (PartInitException ex) {
            ExceptionNotifier.notifyDefault(ex.getLocalizedMessage(), ex);
        } catch (IOException | CoreException ex) {
            ExceptionNotifier.notifyDefault(Messages.ProjectEditorDiffer_error_creating_file, ex);
        }
        return false;
    }
}

class PageDiff extends WizardPage implements Listener {

    private final IPreferenceStore mainPrefs;
    private final PgDbProject proj;

    private DbSourcePicker dbSource;
    private DbSourcePicker dbTarget;
    private Button btnMsSql;
    private ComboViewer cmbTimezone;
    private CLabel lblWarnPosix;

    public PageDiff(String pageName, IPreferenceStore mainPrefs, PgDbProject proj) {
        super(pageName, pageName, null);

        this.mainPrefs = mainPrefs;
        this.proj = proj;
        setDescription(Messages.diffwizard_diffpage_select);
    }

    public DbSource getDbSource() {
        return dbSource.getDbSource(btnMsSql.getSelection());
    }

    public DbSource getDbTarget() {
        return dbTarget.getDbSource(btnMsSql.getSelection());
    }

    public String getTimezone() {
        return cmbTimezone.getCombo().getText();
    }

    public void setTimezone(String timezone) {
        cmbTimezone.getCombo().setText(timezone);
    }

    public boolean isMsSql() {
        return btnMsSql.getSelection();
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, true));

        dbSource = new DbSourcePicker(container, Messages.DiffWizard_source, mainPrefs, this);
        dbSource.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        dbTarget = new DbSourcePicker(container, Messages.DiffWizard_target, mainPrefs, this);
        dbTarget.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Composite compTz = new Composite(container, SWT.NONE);
        GridLayout gl = new GridLayout(2, false);
        gl.marginWidth = gl.marginHeight = 0;
        compTz.setLayout(gl);

        new Label(compTz, SWT.NONE).setText(Messages.DiffWizard_db_tz);

        cmbTimezone = new ComboViewer(compTz, SWT.DROP_DOWN | SWT.BORDER);
        cmbTimezone.setContentProvider(ArrayContentProvider.getInstance());
        cmbTimezone.setLabelProvider(new LabelProvider());
        cmbTimezone.setInput(UIConsts.TIME_ZONES);
        cmbTimezone.getCombo().setText(ApgdiffConsts.UTC);
        cmbTimezone.getCombo().addModifyListener(e -> timeZoneWarn());

        btnMsSql = new Button(container, SWT.CHECK);
        btnMsSql.setText(Messages.DiffWizard_ms_sql_dump);
        btnMsSql.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                getWizard().getContainer().updateButtons();
                getWizard().getContainer().updateMessage();
            }
        });

        lblWarnPosix = new CLabel(container, SWT.NONE);
        lblWarnPosix.setImage(Activator.getEclipseImage(ISharedImages.IMG_OBJS_WARN_TSK));
        lblWarnPosix.setText(Messages.ProjectProperties_posix_is_used_warn);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
        gd.exclude = true;
        lblWarnPosix.setLayoutData(gd);

        if (proj != null) {
            dbTarget.setDbStore(new StructuredSelection(proj.getProject().getLocation().toFile()));
        }

        setControl(container);
    }

    private void timeZoneWarn() {
        String tz =  cmbTimezone.getCombo().getText();
        GridData data = (GridData) lblWarnPosix.getLayoutData();
        if ((!ApgdiffConsts.UTC.equals(tz)
                && tz.startsWith(ApgdiffConsts.UTC)) == data.exclude)  {
            lblWarnPosix.setVisible(data.exclude);
            data.exclude = !data.exclude;
            lblWarnPosix.getParent().layout();
        }
    }

    private boolean isMsSqlDb(DbSourcePicker sourcePicer) {
        DbInfo dbInfo = sourcePicer.getSelectedDbInfo();
        if (dbInfo != null) {
            return dbInfo.isMsSql();
        }

        return isMsSql();
    }

    @Override
    public boolean isPageComplete() {
        String err = null;

        if (getDbSource() == null) {
            err = Messages.diffwizard_diffpage_source_warning;
        } else if (getDbTarget() == null) {
            err = Messages.diffwizard_diffpage_target_warning;
        } else if (getTimezone().isEmpty()) {
            err = Messages.DiffWizard_select_db_tz;
        } else if (isMsSqlDb(dbSource) != isMsSqlDb(dbTarget)) {
            err = Messages.DiffWizard_different_types;
        }

        setErrorMessage(err);
        return err == null;
    }

    @Override
    public void handleEvent(Event event) {
        getWizard().getContainer().updateButtons();
        getWizard().getContainer().updateMessage();
    }
}

class PagePartial extends WizardPage {

    private TreeDiffer treeDiffer;
    private Label lblSource;
    private Label lblTarget;
    private DiffTableViewer diffTable;

    public void setData(String source, String target, TreeDiffer treeDiffer) {
        this.treeDiffer = treeDiffer;
        lblSource.setText(source);
        lblTarget.setText(target);
        diffTable.setInput(treeDiffer.getDbSource(), treeDiffer.getDbTarget(), treeDiffer.getDiffTree(), null);
    }

    public TreeDiffer getTreeDiffer() {
        return treeDiffer;
    }

    public PagePartial(String pageName) {
        super(pageName, pageName, null);
        setDescription(Messages.diffwizard_pagepartial_description);
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, true));

        new Label(container, SWT.NONE).setText(Messages.DiffWizard_source + ':');
        new Label(container, SWT.NONE).setText(Messages.DiffWizard_target + ':');
        lblSource = new Label(container, SWT.WRAP);
        lblTarget = new Label(container, SWT.WRAP);

        diffTable = new DiffTableViewer(container, false);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
        gd.widthHint = 480;
        gd.heightHint = 360;
        diffTable.setLayoutData(gd);

        setControl(container);
    }
}
