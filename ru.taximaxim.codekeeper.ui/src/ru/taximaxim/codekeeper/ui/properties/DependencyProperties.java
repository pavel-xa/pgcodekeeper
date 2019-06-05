package ru.taximaxim.codekeeper.ui.properties;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.navigator.CommonNavigator;
import org.osgi.service.prefs.BackingStoreException;

import cz.startnet.utils.pgdiff.libraries.PgLibrary;
import cz.startnet.utils.pgdiff.xmlstore.DependenciesXmlStore;
import ru.taximaxim.codekeeper.ui.Activator;
import ru.taximaxim.codekeeper.ui.CommonEditingSupport;
import ru.taximaxim.codekeeper.ui.Log;
import ru.taximaxim.codekeeper.ui.UIConsts;
import ru.taximaxim.codekeeper.ui.UIConsts.FILE;
import ru.taximaxim.codekeeper.ui.UIConsts.PREF_PAGE;
import ru.taximaxim.codekeeper.ui.UIConsts.PROJ_PREF;
import ru.taximaxim.codekeeper.ui.localizations.Messages;
import ru.taximaxim.codekeeper.ui.prefs.AbstractTxtEditingSupport;
import ru.taximaxim.codekeeper.ui.prefs.PrefListEditor;

public class DependencyProperties extends PropertyPage {

    private final String defaultPath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
    private DependenciesXmlStore store;
    private IEclipsePreferences prefs;

    private DependenciesListEditor editor;
    private Button btnSafeMode;
    private IProject proj;

    @Override
    public void setElement(IAdaptable element) {
        super.setElement(element);
        proj = element.getAdapter(IProject.class);
        store = new DependenciesXmlStore(Paths.get(proj.getLocation()
                .append(DependenciesXmlStore.FILE_NAME).toString()));
        prefs = new ProjectScope(proj).getNode(UIConsts.PLUGIN_ID.THIS);
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite area = new Composite(parent, SWT.NONE);
        area.setLayout(new GridLayout());

        editor = new DependenciesListEditor(area);
        editor.setLayoutData(new GridData(GridData.FILL_BOTH));

        List<PgLibrary> input;
        try {
            input = store.readObjects();
        } catch (IOException e) {
            Log.log(e);
            input = new ArrayList<>();
        }
        editor.setInputList(input);

        btnSafeMode = new Button(area, SWT.CHECK);
        btnSafeMode.setText(Messages.DependencyProperties_safe_mode);
        btnSafeMode.setToolTipText(Messages.DependencyProperties_safe_mode_desc);
        btnSafeMode.setSelection(prefs.getBoolean(PROJ_PREF.LIB_SAFE_MODE, true));

        return area;
    }

    @Override
    public boolean performOk() {
        try {
            prefs.putBoolean(PROJ_PREF.LIB_SAFE_MODE, btnSafeMode.getSelection());
            prefs.flush();
            store.writeObjects(editor.getList());
            refreshProject();
            setValid(true);
            setErrorMessage(null);
        } catch (IOException | BackingStoreException e) {
            setErrorMessage(MessageFormat.format(
                    Messages.projectProperties_error_occurs_while_saving_properties,
                    e.getLocalizedMessage()));
            setValid(false);
            return false;
        }
        return true;
    }

    private void refreshProject() {
        CommonNavigator view = (CommonNavigator)PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getActivePage().findView(IPageLayout.ID_PROJECT_EXPLORER);
        if (view != null) {
            view.getCommonViewer().refresh(proj);
        }
    }

    private class DependenciesListEditor extends PrefListEditor<PgLibrary, TableViewer> {

        public DependenciesListEditor(Composite parent) {
            super(parent);
        }

        @Override
        public boolean checkDuplicate(PgLibrary o1, PgLibrary o2) {
            return o1.getPath().equals(o2.getPath());
        }

        @Override
        protected PgLibrary getNewObject(PgLibrary oldObject) {
            DirectoryDialog dialog = new DirectoryDialog(getShell());
            dialog.setText(Messages.DependencyProperties_select_directory);
            dialog.setFilterPath(defaultPath);
            String path = dialog.open();
            return path != null ? new PgLibrary(path) : null;
        }

        @Override
        protected String errorAlreadyExists(PgLibrary obj) {
            return MessageFormat.format(Messages.DbStorePrefPage_already_present, obj.getPath());
        }

        @Override
        protected TableViewer createViewer(Composite parent) {
            TableViewer viewer = new TableViewer(
                    parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
            viewer.setContentProvider(ArrayContentProvider.getInstance());

            addColumns(viewer);

            GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 7);
            gd.widthHint = PREF_PAGE.WIDTH_HINT_PX;
            viewer.getTable().setLayoutData(gd);
            viewer.getTable().setLinesVisible(true);
            viewer.getTable().setHeaderVisible(true);

            return viewer;
        }

        private void addColumns(TableViewer viewer) {
            TableViewerColumn path = new TableViewerColumn(viewer, SWT.LEFT);
            path.getColumn().setText(Messages.DependencyProperties_path);
            path.getColumn().setResizable(true);
            path.getColumn().setMoveable(true);
            path.setLabelProvider(new ColumnLabelProvider() {

                @Override
                public String getText(Object element) {
                    PgLibrary obj = (PgLibrary) element;
                    return obj.getPath();
                }
            });
            path.setEditingSupport(new TxtLibPathEditingSupport(viewer, this));

            TableViewerColumn owner = new TableViewerColumn(viewer, SWT.CENTER);
            owner.getColumn().setText(Messages.DependencyProperties_owner);
            owner.getColumn().setResizable(true);
            owner.getColumn().setMoveable(true);
            owner.setLabelProvider(new ColumnLabelProvider() {

                @Override
                public String getText(Object element) {
                    return ((PgLibrary) element).getOwner();
                }
            });

            owner.setEditingSupport(new OwnerEditingSupport(viewer));

            TableViewerColumn ignorePriv = new TableViewerColumn(viewer, SWT.CENTER);
            ignorePriv.getColumn().setText(Messages.DependencyProperties_ignore_privileges);
            ignorePriv.getColumn().setResizable(true);
            ignorePriv.getColumn().setMoveable(true);
            ignorePriv.setLabelProvider(new ColumnLabelProvider() {

                @Override
                public String getText(Object element) {
                    return (((PgLibrary)element).isIgnorePriv()) ? "\u2611" : "\u2610"; //$NON-NLS-1$ //$NON-NLS-2$
                }
            });

            ignorePriv.setEditingSupport(new IgnorePrivCheckEditingSupport(viewer));

            viewer.getTable().addListener(SWT.Resize, event -> {
                Table table = (Table)event.widget;
                int width = (int)(table.getClientArea().width * 0.1f);
                table.getColumns()[0].setWidth(width * 5);
                table.getColumns()[1].setWidth(width * 3);
                table.getColumns()[2].setWidth(width * 2);
            });
        }

        @Override
        protected void createButtonsForSideBar(Composite parent) {
            createButton(parent, ADD_ID, Messages.DependencyProperties_add_directory,
                    Activator.getEclipseImage(ISharedImages.IMG_OBJ_FOLDER));

            Button btnAddDump = createButton(parent, CLIENT_ID, Messages.DependencyProperties_add_file,
                    Activator.getEclipseImage(ISharedImages.IMG_OBJ_FILE));
            btnAddDump.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    FileDialog dialog = new FileDialog(getShell());
                    dialog.setText(Messages.choose_dump_file_with_changes);
                    dialog.setFilterExtensions(new String[] {"*.sql", "*.zip", "*"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    dialog.setFilterNames(new String[] {
                            Messages.DiffPresentationPane_sql_file_filter,
                            Messages.DiffPresentationPane_zip_file_filter,
                            Messages.DiffPresentationPane_any_file_filter});
                    dialog.setFilterPath(defaultPath);
                    String value = dialog.open();
                    if (value != null) {
                        getList().add(new PgLibrary(value));
                        getViewer().refresh();
                    }
                }
            });

            Button btnAddDb = createButton(parent, CLIENT_ID,
                    Messages.DependencyProperties_add_database, FILE.ICONDATABASE);
            btnAddDb.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    InputDialog dialog = new InputDialog(getShell(),
                            Messages.DependencyProperties_add_database,
                            Messages.DependencyProperties_enter_connection_string, "jdbc:",  //$NON-NLS-1$
                            newText -> newText.startsWith("jdbc:") ? //$NON-NLS-1$
                                    null : Messages.DependencyProperties_connection_start);

                    if (dialog.open() == Window.OK) {
                        getList().add(new PgLibrary(dialog.getValue()));
                        getViewer().refresh();
                    }
                }
            });

            Button btnAddURI = createButton(parent, CLIENT_ID,
                    Messages.DependencyProperties_add_uri, FILE.ICONCLOUD);
            btnAddURI.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    InputDialog dialog = new InputDialog(getShell(),
                            Messages.DependencyProperties_add_uri,
                            Messages.DependencyProperties_enter_uri, "",   //$NON-NLS-1$
                            DependencyProperties.this::validateUrl);

                    if (dialog.open() == Window.OK) {
                        getList().add(new PgLibrary(dialog.getValue()));
                        getViewer().refresh();
                    }
                }
            });

            createButton(parent, DELETE_ID, Messages.delete, Activator.getEclipseImage(ISharedImages.IMG_ETOOL_DELETE));
            createButton(parent, UP_ID, null, FILE.ICONUP);
            createButton(parent, DOWN_ID, null, FILE.ICONDOWN);
        }
    }

    private String validateUrl(String newText) {
        try {
            if (new URI(newText).getScheme() == null) {
                return Messages.DependencyProperties_empty_scheme;
            }
            return null;
        } catch (URISyntaxException ex) {
            return ex.getLocalizedMessage();
        }
    }

    private static class IgnorePrivCheckEditingSupport extends CommonEditingSupport<CheckboxCellEditor> {

        public IgnorePrivCheckEditingSupport(TableViewer tableViewer) {
            super(tableViewer, new CheckboxCellEditor(tableViewer.getTable()));
        }

        @Override
        protected Object getValue(Object element) {
            return ((PgLibrary) element).isIgnorePriv();
        }

        @Override
        protected void setValue(Object element, Object value) {
            boolean val = (boolean) value;
            ((PgLibrary) element).setIgnorePriv(val);
            getViewer().update(element, null);
            if (val) {
                MessageBox mb = new MessageBox(getViewer().getControl().getShell(), SWT.ICON_INFORMATION);
                mb.setText(Messages.DependencyProperties_attention);
                mb.setMessage(Messages.DependencyProperties_ignore_priv_warn);
                mb.open();
            }
        }
    }

    private static class OwnerEditingSupport extends CommonEditingSupport<TextCellEditor> {

        public OwnerEditingSupport(TableViewer viewer) {
            super(viewer, new TextCellEditor(viewer.getTable()));
        }

        @Override
        protected Object getValue(Object element) {
            return ((PgLibrary) element).getOwner();
        }

        @Override
        protected void setValue(Object element, Object value) {
            ((PgLibrary) element).setOwner((String) value);
            getViewer().update(element, null);
        }
    }

    private static class TxtLibPathEditingSupport extends
    AbstractTxtEditingSupport<PgLibrary, DependenciesListEditor> {

        public TxtLibPathEditingSupport(ColumnViewer viewer,
                DependenciesListEditor dependenciesListEditor) {
            super(viewer, dependenciesListEditor, PgLibrary.class);
        }

        @Override
        protected String getText(PgLibrary obj) {
            return obj.getPath();
        }

        @Override
        protected PgLibrary getCopyWithNewTxt(PgLibrary obj, String newText) {
            return new PgLibrary(newText, obj.isIgnorePriv(), obj.getOwner());
        }
    }
}
