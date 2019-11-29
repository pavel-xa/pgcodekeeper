package ru.taximaxim.codekeeper.ui.differ;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.ISharedImages;
import org.osgi.framework.Bundle;

import cz.startnet.utils.pgdiff.libraries.PgLibrary;
import cz.startnet.utils.pgdiff.loader.JdbcConnector;
import cz.startnet.utils.pgdiff.schema.PgStatement;
import cz.startnet.utils.pgdiff.xmlstore.DependenciesXmlStore;
import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.IgnoreList;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.TreeElement;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.TreeElement.DiffSide;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.TreeFlattener;
import ru.taximaxim.codekeeper.apgdiff.model.exporter.AbstractModelExporter;
import ru.taximaxim.codekeeper.ui.Activator;
import ru.taximaxim.codekeeper.ui.AggregatingListener;
import ru.taximaxim.codekeeper.ui.Log;
import ru.taximaxim.codekeeper.ui.UIConsts.FILE;
import ru.taximaxim.codekeeper.ui.UIConsts.PG_EDIT_PREF;
import ru.taximaxim.codekeeper.ui.UIConsts.PLUGIN_ID;
import ru.taximaxim.codekeeper.ui.UiSync;
import ru.taximaxim.codekeeper.ui.comparetools.CompareAction;
import ru.taximaxim.codekeeper.ui.comparetools.CompareInput;
import ru.taximaxim.codekeeper.ui.dialogs.FilterDialog;
import ru.taximaxim.codekeeper.ui.differ.filters.AbstractFilter;
import ru.taximaxim.codekeeper.ui.differ.filters.CodeFilter;
import ru.taximaxim.codekeeper.ui.differ.filters.SchemaFilter;
import ru.taximaxim.codekeeper.ui.differ.filters.UserFilter;
import ru.taximaxim.codekeeper.ui.fileutils.GitUserReader;
import ru.taximaxim.codekeeper.ui.localizations.Messages;
import ru.taximaxim.codekeeper.ui.xmlstore.ListXmlStore;

/**
 * Use {@link #setChecked(TreeElement, boolean)} for any kind of checkbox work.
 * It refreshes all the needed states without calling slow full viewer refresh.
 * Always call {@link #viewerChecksUpdated()} after finishing checkbox work.
 */
public class DiffTableViewer extends Composite {

    private static final Pattern REGEX_SPECIAL_CHARS = Pattern.compile("[\\[\\\\\\^$.|?*+()]"); //$NON-NLS-1$
    private static final String GITLABEL_PROP = "GITLABEL_PROP"; //$NON-NLS-1$

    private static final ListXmlStore XML_HISTORY = new ListXmlStore(200, "fhistory.xml", "history", "element"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    private final boolean showGitUser;
    private boolean showDbUser;

    private final Image iSideBoth;
    private final Image iSideLeft;
    private final Image iSideRight;

    private final boolean viewOnly;
    private final Path location;
    private final Map<TreeElement, ElementMetaInfo> elementInfoMap = new HashMap<>();
    private final Set<TreeElement> elements = elementInfoMap.keySet();
    private final DiffContentProvider contentProvider = new DiffContentProvider();
    private final CheckStateProvider checkProvider;
    private final TableViewerComparator comparator = new TableViewerComparator();
    private IStructuredSelection oldSelection;
    private IStructuredSelection newSelection;

    private final LocalResourceManager lrm;
    private final Text txtFilterName;
    private final Button useRegEx;
    private Label lblObjectCount;
    private Label lblCheckedCount;

    private final CheckboxTreeViewer viewer;
    private final TableViewerFilter viewerFilter = new TableViewerFilter();
    private TreeViewerColumn columnCheck;
    private TreeViewerColumn columnType;
    private TreeViewerColumn columnChange;
    private TreeViewerColumn columnName;
    private TreeViewerColumn columnGitUser;
    private TreeViewerColumn columnDbUser;
    private TreeViewerColumn columnLocation;

    private DbSource dbProject;
    private DbSource dbRemote;

    private boolean isApplyToProj = true;

    private final IStatusLineManager lineManager;

    private final List<ICheckStateListener> programmaticCheckListeners = new ArrayList<>();

    private enum Columns {
        CHECK, NAME, TYPE, CHANGE, LOCATION, GIT_USER, DB_USER
    }

    public StructuredViewer getViewer() {
        return viewer;
    }

    public Collection<TreeElement> getElements() {
        return Collections.unmodifiableCollection(elements);
    }

    public DiffTableViewer(Composite parent, boolean viewOnly) {
        this(parent, viewOnly, null, null);
    }

    public DiffTableViewer(Composite parent, boolean viewOnly, IStatusLineManager lineManager, Path location) {
        super(parent, SWT.NONE);
        this.viewOnly = viewOnly;
        this.lineManager = lineManager;
        this.location = location;
        showGitUser = location != null
                && Activator.getDefault().getPreferenceStore().getBoolean(PG_EDIT_PREF.SHOW_GIT_USER)
                && GitUserReader.checkRepo(location);

        PixelConverter pc = new PixelConverter(this);
        lrm = new LocalResourceManager(JFaceResources.getResources(), this);
        Bundle bundle = Activator.getContext().getBundle();

        iSideBoth = lrm.createImage(ImageDescriptor.createFromURL(bundle
                .getResource(FILE.ICONEDIT)));
        iSideRight = Activator.getEclipseImage(ISharedImages.IMG_OBJ_ADD);
        iSideLeft = Activator.getEclipseImage(ISharedImages.IMG_ETOOL_DELETE);

        GridLayout gl = new GridLayout();
        gl.marginHeight = gl.marginWidth = 0;
        setLayout(gl);

        // upper composite
        Composite upperComp = new Composite(this, SWT.NONE);
        upperComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        gl = new GridLayout(viewOnly ? 3 : 4, false);
        gl.marginWidth = gl.marginHeight = 0;
        upperComp.setLayout(gl);

        if (!viewOnly) {
            ToolBarManager mgrTblBtn = new ToolBarManager(SWT.FLAT | SWT.RIGHT);

            mgrTblBtn.add(new Action(Messages.select_all, ImageDescriptor
                    .createFromURL(bundle.getResource(FILE.ICONSELECTALL))) {

                @Override
                public void run() {
                    setElementsChecked(elements, true, true);
                }
            });

            mgrTblBtn.add(new Action(Messages.select_none, ImageDescriptor
                    .createFromURL(bundle.getResource(FILE.ICONSELECTNONE))) {

                @Override
                public void run() {
                    setElementsChecked(elements, false, true);
                }
            });

            mgrTblBtn.add(new Action(Messages.diffTableViewer_invert_selection, ImageDescriptor
                    .createFromURL(bundle.getResource(FILE.ICONINVERTSELECTION))) {

                @Override
                public void run() {
                    setElementsChecked(elements, el -> !el.isSelected(), true);
                }
            });

            mgrTblBtn.add(new Action(Messages.DiffTableViewer_copy_as_regex, ImageDescriptor
                    .createFromImage(Activator.getEclipseImage(ISharedImages.IMG_TOOL_COPY))) {

                @Override
                public void run() {
                    saveCheckedElements2ClipboardAsExpession();
                }
            });

            mgrTblBtn.add(new Action(Messages.DiffTableViewer_show_filters, ImageDescriptor
                    .createFromURL(bundle.getResource(FILE.ICONEMPTYFILTER))) {

                @Override
                public void run() {
                    FilterDialog dialog = new FilterDialog(getShell(),
                            viewerFilter.schemaFilter, viewerFilter.codeFilter,
                            viewerFilter.gitUserFilter, viewerFilter.dbUserFilter,
                            viewerFilter.types, viewerFilter.sides,
                            viewerFilter.isLocalChange, viewerFilter.isHideLibs,
                            isApplyToProj);
                    if (dialog.open() == Dialog.OK) {
                        setImageDescriptor(ImageDescriptor.createFromURL(bundle.getResource(
                                viewerFilter.isAdvancedEmpty() ? FILE.ICONEMPTYFILTER : FILE.ICONFILTER)));
                        viewer.refresh();
                    }
                }
            });

            mgrTblBtn.createControl(upperComp);
        }

        txtFilterName = new Text(upperComp, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
        GridData gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
        gd.widthHint = pc.convertWidthInCharsToPixels(30);
        txtFilterName.setLayoutData(gd);
        txtFilterName.setMessage(Messages.DiffTableViewer_filter_placeholder);

        List<String> history = new ArrayList<>();
        try {
            history = XML_HISTORY.readObjects();
        } catch (IOException ex) {
            Log.log(ex);
        }

        SimpleContentProposalProvider scp = new SimpleContentProposalProvider(history.toArray(new String[history.size()]));
        scp.setFiltering(true);

        ContentProposalAdapter adapter = new ContentProposalAdapter(txtFilterName,
                new TextContentAdapter(), scp, null, null);
        adapter.setPopupSize(new Point(pc.convertWidthInCharsToPixels(40), pc.convertHeightInCharsToPixels(8)));
        adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);

        useRegEx = new Button(upperComp, SWT.CHECK);
        useRegEx.setToolTipText(Messages.diffTableViewer_use_java_regular_expressions_see_more);
        useRegEx.setText(Messages.diffTableViewer_use_regular_expressions);
        useRegEx.setLayoutData(new GridData(SWT.DEFAULT, SWT.CENTER, false, false));
        useRegEx.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                viewerFilter.setUseRegEx(useRegEx.getSelection());
                viewer.refresh();
            }
        });

        Composite container = new Composite(upperComp, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        if (lineManager != null) {
            createRightSide(container);
        } else {
            container.setLayout(new GridLayout(viewOnly ? 2 : 3, false));
            Label l = new Label(container, SWT.NONE);
            l.setEnabled(false);
            l.setText("|"); //$NON-NLS-1$

            if (!viewOnly) {
                lblCheckedCount = new Label(container, SWT.NONE);
            }
            lblObjectCount = new Label(container, SWT.NONE);
        }

        updateObjectsLabels();
        // end upper composite

        int viewerStyle = SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER;
        if (!viewOnly) {
            viewerStyle |= SWT.CHECK;
        }
        viewer = new CheckboxTreeViewer(new Tree(this, viewerStyle));

        ModifyListener listener = new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                String text = ((Text)e.getSource()).getText();
                filterHistory(text);
                viewerFilter.setFilter(text);
                viewer.refresh();
            }

            private void filterHistory(String text) {
                try {
                    if (text != null && !text.isEmpty()) {
                        XML_HISTORY.addHistoryEntry(text);
                    }
                    List<String> history = XML_HISTORY.readObjects();
                    scp.setProposals(history.toArray(new String[history.size()]));
                } catch (IOException e) {
                    Log.log(e);
                }
            }
        };

        AggregatingListener.addModifyListener(txtFilterName, listener);

        viewer.addSelectionChangedListener(event -> {
            oldSelection = newSelection;
            newSelection = (IStructuredSelection)event.getSelection();
        });

        viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        viewer.getTree().setLinesVisible(true);
        viewer.getTree().setHeaderVisible(true);

        viewer.getControl().setMenu(
                getViewerMenu().createContextMenu(viewer.getControl()));

        if (!viewOnly) {
            viewer.addCheckStateListener(new CheckStateListener());
            checkProvider = new CheckStateProvider();
            viewer.setCheckStateProvider(checkProvider);
            viewer.addTreeListener(new RefreshCheckedTreeListener());
        } else {
            checkProvider = null;
        }
        // resolves a bottleneck in findItems() which is very hot
        // and slow when using native API impl
        viewer.setUseHashlookup(true);
        viewer.setComparator(comparator);
        viewer.setFilters(viewerFilter);
        initColumns();
        viewer.setContentProvider(contentProvider);
    }

    public void createRightSide(Composite parent) {
        // will be overridden by subclasses if needed
    }

    private MenuManager getViewerMenu() {
        MenuManager menuMgr = new MenuManager();
        if (!viewOnly) {
            menuMgr.add(new Action(Messages.diffTableViewer_select_child_elements) {

                @Override
                public void run() {
                    setSelectionSubtreesChecked((IStructuredSelection) viewer.getSelection(), true);
                }
            });
            menuMgr.add(new Action(Messages.diffTableViewer_deselect_child_elements) {

                @Override
                public void run() {
                    setSelectionSubtreesChecked((IStructuredSelection) viewer.getSelection(), false);
                }
            });
            menuMgr.add(new Separator());
            menuMgr.add(new Action(Messages.diffTableViewer_mark_selected_elements) {

                @Override
                public void run() {
                    setElementsChecked(((IStructuredSelection) viewer.getSelection()).toList(), true, false);
                }
            });
            menuMgr.add(new Action(Messages.diffTableViewer_unmark_selected_elements) {

                @Override
                public void run() {
                    setElementsChecked(((IStructuredSelection) viewer.getSelection()).toList(), false, false);
                }
            });
            menuMgr.add(new Separator());
        }
        menuMgr.add(new Action(Messages.DiffTableViewer_expand_all) {

            @Override
            public void run() {
                viewer.expandAll();
            }

            @Override
            public boolean isEnabled() {
                return true;
            }
        });
        menuMgr.add(new Action(Messages.DiffTableViewer_collapse_all) {

            @Override
            public void run() {
                viewer.collapseAll();
            }

            @Override
            public boolean isEnabled() {
                return true;
            }
        });
        menuMgr.add(new Separator());
        menuMgr.add(new Action(Messages.diffTableViewer_open_diff_in_new_window) {

            @Override
            public void run() {
                TreeElement el = (TreeElement)
                        ((IStructuredSelection) viewer.getSelection()).getFirstElement();
                PgStatement remote = el.getSide() == DiffSide.LEFT ? null
                        : el.getPgStatement(dbRemote.getDbObject());
                PgStatement project = el.getSide() == DiffSide.RIGHT ? null
                        : el.getPgStatement(dbProject.getDbObject());
                CompareAction.openCompareEditor(new CompareInput(el.getName(),
                        el.getType(), remote, project));
            }
        });

        menuMgr.addMenuListener(manager -> {
            boolean enable = !viewer.getSelection().isEmpty();
            for (IContributionItem it : manager.getItems()) {
                if (it instanceof ActionContributionItem) {
                    ((ActionContributionItem) it).getAction().setEnabled(enable);
                }
            }
        });

        return menuMgr;
    }

    private void initColumns() {
        ColumnViewerToolTipSupport.enableFor(viewer);

        columnCheck = new TreeViewerColumn(viewer, SWT.LEFT);
        columnCheck.getColumn().setResizable(!viewOnly);
        columnCheck.getColumn().setMoveable(!viewOnly);

        columnCheck.getColumn().addSelectionListener(getHeaderSelectionAdapter(Columns.CHECK));

        columnCheck.setLabelProvider(new ColumnLabelProvider() {

            @Override
            public String getText(Object element) {
                return ""; //$NON-NLS-1$
            }

            @Override
            public Image getImage(Object element) {
                ElementMetaInfo meta = elementInfoMap.get(element);
                return meta != null && meta.getLibLocation() != null ?
                        Activator.getRegisteredImage(FILE.ICONLIB) : null;
            }

            @Override
            public String getToolTipText(Object element) {
                ElementMetaInfo meta = elementInfoMap.get(element);
                if (meta != null) {
                    String libLocation = meta.getLibLocation();
                    if (libLocation != null) {
                        return libLocation;
                    }
                }

                return null;
            }
        });

        columnType = new TreeViewerColumn(viewer, SWT.LEFT);
        columnChange = new TreeViewerColumn(viewer, SWT.LEFT);
        columnName = new TreeViewerColumn(viewer, SWT.LEFT);
        columnLocation = new TreeViewerColumn(viewer, SWT.LEFT);
        columnDbUser = new TreeViewerColumn(viewer, SWT.LEFT);
        columnGitUser = new TreeViewerColumn(viewer, SWT.LEFT);

        columnName.getColumn().setResizable(true);
        columnName.getColumn().setMoveable(true);

        columnType.getColumn().setResizable(true);
        columnType.getColumn().setMoveable(true);

        columnChange.getColumn().setResizable(true);
        columnChange.getColumn().setMoveable(true);

        columnLocation.getColumn().setResizable(true);
        columnLocation.getColumn().setMoveable(true);

        columnGitUser.getColumn().setResizable(true);
        columnGitUser.getColumn().setMoveable(true);

        columnDbUser.getColumn().setResizable(true);
        columnDbUser.getColumn().setMoveable(true);

        setColumnHeaders();

        columnCheck.getColumn().setToolTipText(Messages.DiffTableViewer_reset_sorting);
        columnDbUser.getColumn().setToolTipText(Messages.DiffTableViewer_reset_sorting);
        columnGitUser.getColumn().setToolTipText(Messages.DiffTableViewer_reset_sorting);
        columnName.getColumn().setToolTipText(Messages.DiffTableViewer_reset_sorting);
        columnType.getColumn().setToolTipText(Messages.DiffTableViewer_reset_sorting);
        columnChange.getColumn().setToolTipText(Messages.DiffTableViewer_reset_sorting);
        columnLocation.getColumn().setToolTipText(Messages.DiffTableViewer_reset_sorting);

        columnName.getColumn().addSelectionListener(getHeaderSelectionAdapter(Columns.NAME));
        columnDbUser.getColumn().addSelectionListener(getHeaderSelectionAdapter(Columns.DB_USER));
        columnGitUser.getColumn().addSelectionListener(getHeaderSelectionAdapter(Columns.GIT_USER));
        columnType.getColumn().addSelectionListener(getHeaderSelectionAdapter(Columns.TYPE));
        columnChange.getColumn().addSelectionListener(getHeaderSelectionAdapter(Columns.CHANGE));
        columnLocation.getColumn().addSelectionListener(getHeaderSelectionAdapter(Columns.LOCATION));

        updateColumnsWidth();

        columnName.setLabelProvider(new StyledCellLabelProvider(){

            @Override
            public void update(ViewerCell cell) {
                String name = ((TreeElement)cell.getElement()).getName();
                cell.setText(name);

                Region loc = viewerFilter.getMatchingLocation(name, viewerFilter.filterName,
                        viewerFilter.useRegEx ? viewerFilter.regExPattern : null);
                if (loc != null) {
                    StyleRange highlightMatch = new StyleRange(loc.getOffset(),
                            loc.getLength(), null,
                            getDisplay().getSystemColor(SWT.COLOR_YELLOW));
                    cell.setStyleRanges(new StyleRange[] { highlightMatch });
                } else {
                    cell.setStyleRanges(null);
                }
                super.update(cell);
            }
        });

        columnType.setLabelProvider(new ColumnLabelProvider() {

            @Override
            public String getText(Object element) {
                return ((TreeElement) element).getType().toString();
            }

            @Override
            public Image getImage(Object element) {
                return Activator.getDbObjImage(((TreeElement) element).getType());
            }
        });

        columnChange.setLabelProvider(new ColumnLabelProvider() {

            @Override
            public String getText(Object element) {
                switch (((TreeElement) element).getSide()) {
                case BOTH: return isApplyToProj ? "edit" : "ALTER"; //$NON-NLS-1$ //$NON-NLS-2$
                case LEFT: return isApplyToProj ? "delete" : "CREATE"; //$NON-NLS-1$ //$NON-NLS-2$
                case RIGHT: return isApplyToProj ? "add" : "DROP"; //$NON-NLS-1$ //$NON-NLS-2$
                default: return null;
                }
            }

            @Override
            public Image getImage(Object element) {
                switch (((TreeElement) element).getSide()) {
                case BOTH: return iSideBoth;
                case LEFT: return isApplyToProj ? iSideLeft : iSideRight;
                case RIGHT: return isApplyToProj ? iSideRight : iSideLeft;
                default: return null;
                }
            }
        });

        columnGitUser.setLabelProvider(new ColumnLabelProvider() {

            @Override
            public String getText(Object element) {
                ElementMetaInfo meta = elementInfoMap.get(element);
                return meta != null ? meta.getGitUser() : ""; //$NON-NLS-1$
            }

            @Override
            public boolean isLabelProperty(Object element, String property) {
                return property == GITLABEL_PROP;
            }
        });

        columnDbUser.setLabelProvider(new ColumnLabelProvider() {

            @Override
            public String getText(Object element) {
                ElementMetaInfo meta = elementInfoMap.get(element);
                return meta != null ? meta.getDbUser() : ""; //$NON-NLS-1$
            }
        });


        columnLocation.setLabelProvider(new ColumnLabelProvider() {

            @Override
            public String getText(Object element) {
                return ((TreeElement) element).getContainerQName();
            }
        });
    }

    private void setColumnHeaders(){
        columnCheck.getColumn().setText("✓"); //$NON-NLS-1$
        columnName.getColumn().setText(Messages.diffTableViewer_object_name);
        columnType.getColumn().setText(Messages.diffTableViewer_object_type);
        columnChange.getColumn().setText(Messages.diffTableViewer_change_type);
        columnLocation.getColumn().setText(Messages.diffTableViewer_container);
        columnGitUser.getColumn().setText(Messages.diffTableViewer_git_user);
        columnDbUser.getColumn().setText(Messages.diffTableViewer_db_user);
    }

    private void updateColumnsWidth() {
        PixelConverter pc = new PixelConverter(viewer.getControl());
        columnCheck.getColumn().setWidth(viewOnly ? 0 : pc.convertWidthInCharsToPixels(10));
        columnType.getColumn().setWidth(pc.convertWidthInCharsToPixels(25));
        columnChange.getColumn().setWidth(pc.convertWidthInCharsToPixels(35));
        // name column will take half of the space
        int width = (int)(viewer.getControl().getSize().x * 0.4f);
        columnName.getColumn().setWidth(Math.max(width, 200));
        columnLocation.getColumn().setWidth(pc.convertWidthInCharsToPixels(20));
        columnGitUser.getColumn().setWidth(showGitUser && !viewOnly ? pc.convertWidthInCharsToPixels(20) : 0);
        columnDbUser.getColumn().setWidth(showDbUser && !viewOnly ? pc.convertWidthInCharsToPixels(20) : 0);
    }

    private SelectionAdapter getHeaderSelectionAdapter(final Columns index) {
        return new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if ((e.stateMask & SWT.CTRL) != 0){
                    comparator.clearSortList();
                    setColumnHeaders();
                }
                sortViewer(index);
            }
        };
    }

    private void sortViewer(Columns index) {
        comparator.addSort(index);
        updateSortIndexes();
        viewer.refresh();
    }

    private void updateSortIndexes(){
        int i = 0;
        StringBuilder sb = new StringBuilder();
        for (SortingColumn col : comparator.sortOrder) {
            sb.setLength(0);
            sb.append(comparator.sortOrder.size() - i++)
            .append(!col.desc ? '\u25BF' : '\u25B5')
            .append('\t');

            switch (col.col) {
            case CHECK:
                sb.setLength(sb.length() - 1);
                columnCheck.getColumn().setText(sb.append('✓').toString());
                break;
            case TYPE:
                columnType.getColumn().setText(sb.append(Messages.diffTableViewer_object_type).toString());
                break;
            case CHANGE:
                sb.append(Messages.diffTableViewer_change_type);
                sb.append(isApplyToProj ? Messages.diffTableViewer_for_project
                        : Messages.diffTableViewer_for_database);
                columnChange.getColumn().setText(sb.toString());
                break;
            case NAME:
                columnName.getColumn().setText(sb.append(Messages.diffTableViewer_object_name).toString());
                break;
            case LOCATION:
                columnLocation.getColumn().setText(sb.append(Messages.diffTableViewer_container).toString());
                break;
            case GIT_USER:
                columnGitUser.getColumn().setText(sb.append(Messages.diffTableViewer_git_user).toString());
                break;
            case DB_USER:
                columnDbUser.getColumn().setText(sb.append(Messages.diffTableViewer_db_user).toString());
                break;
            default:
                break;
            }
        }
    }

    public void addCheckStateListener(ICheckStateListener listener) {
        programmaticCheckListeners.add(listener);
    }

    private void saveCheckedElements2ClipboardAsExpession(){
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (TreeElement el : elements) {
            if (!el.isSelected()) {
                continue;
            }
            if (!first) {
                sb.append('|');
            } else {
                first = false;
            }
            String name = el.getName();
            if (REGEX_SPECIAL_CHARS.matcher(name).find()) {
                name = Pattern.quote(name);
            }
            sb.append('^').append(name).append('$');
        }

        if (sb.length() != 0) {
            Clipboard clip = new Clipboard(getDisplay());
            try {
                clip.setContents(new String[] { sb.toString() },
                        new TextTransfer[] { TextTransfer.getInstance() });
            } finally {
                clip.dispose();
            }
        }
    }

    public void setAutoExpand(boolean enabled) {
        viewer.setAutoExpandLevel(enabled ? AbstractTreeViewer.ALL_LEVELS : 0);
    }


    public void setInput(DbSource dbProject, DbSource dbRemote, TreeElement diffTree,
            IgnoreList ignoreList) {
        setInputCollection(diffTree == null ? Collections.<TreeElement>emptyList() :
            new TreeFlattener()
            .onlyEdits(dbProject.getDbObject(), dbRemote.getDbObject())
            .useIgnoreList(ignoreList, dbRemote.getDbName())
            .flatten(diffTree), dbProject, dbRemote);
    }

    /**
     * Используется в коммит диалоге для установки элементов
     * @param collection элементы для показа
     * @param dbTime
     */
    public void setInputCollection(Collection<TreeElement> collection,
            DbSource dbProject, DbSource dbRemote) {
        this.dbProject = dbProject;
        this.dbRemote = dbRemote;

        // reset sorting while using empty input
        // no full re-sorts, no full refreshes
        viewer.setInput(null);
        comparator.clearSortList();
        setColumnHeaders();
        sortViewer(Columns.NAME);
        sortViewer(Columns.CHANGE);
        sortViewer(Columns.TYPE);
        sortViewer(Columns.LOCATION);

        elementInfoMap.clear();
        collection.forEach(el -> this.elementInfoMap.put(el, new ElementMetaInfo()));

        if (showGitUser && !elementInfoMap.isEmpty()) {
            readGitUsers();
        }

        if (!elementInfoMap.isEmpty() && location != null) {
            setLibLocations();
        }

        if (dbRemote != null) {
            readDbUsers();
        }

        viewer.setInput(elements);
        updateColumnsWidth();

        updateObjectsLabels();
    }

    private void setLibLocations() {
        Path p = location.resolve(DependenciesXmlStore.FILE_NAME);
        if (!Files.exists(p)) {
            return;
        }

        List<PgLibrary> libs;
        try {
            libs = new DependenciesXmlStore(p).readObjects();
        } catch (IOException e) {
            Log.log(e);
            return;
        }

        elementInfoMap.forEach((k,v) -> {
            if (k.getSide() != DiffSide.RIGHT) {
                PgStatement st = k.getPgStatement(dbProject.getDbObject());
                if (!st.isLib()) {
                    return;
                }

                String name = null;
                String type = null;
                String loc = st.getLocation().getFilePath();
                switch (PgLibrary.getSource(loc)) {
                case JDBC:
                    type = Messages.DiffTableViewer_database;
                    name = JdbcConnector.dbNameFromUrl(loc);
                    break;
                case URL:
                    type = Messages.DiffTableViewer_uri ;
                    name = loc;
                    try {
                        String urlPath = new URI(loc).getPath();
                        if (urlPath != null) {
                            name = urlPath.substring(urlPath.lastIndexOf('/') + 1);
                        }
                    } catch (URISyntaxException e) {
                        // Nothing to do, use default path
                    }
                    break;
                case LOCAL:
                    Path lib = libs.stream().map(PgLibrary::getPath)
                    .filter(loc::startsWith).findFirst().map(Paths::get).get();
                    Path location = Paths.get(loc);
                    name = lib.getFileName().toString();

                    if (!lib.equals(location)) {
                        type = Messages.DiffTableViewer_directory;
                        loc = lib.relativize(location).toString();
                    } else {
                        type = Messages.DiffTableViewer_file;
                    }
                    break;
                }

                v.setLibLocation(Messages.DiffTableViewer_library + name + '\n' + Messages.DiffTableViewer_type + type
                        + (loc == null ? "" : ('\n' + Messages.DiffTableViewer_path + loc))); //$NON-NLS-1$
            }
        });
    }

    private void readDbUsers() {
        elementInfoMap.forEach((k,v) -> {
            if (k.getSide() != DiffSide.LEFT) {
                String author = k.getPgStatement(dbRemote.getDbObject()).getAuthor();
                v.setDbUser(author);
                if (author != null) {
                    showDbUser = true;
                }
            }
        });
    }


    private void readGitUsers() {
        Job job = new Job(Messages.DiffTableViewer_reading_git_history) {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try (GitUserReader reader = new GitUserReader(location)) {
                    Path root = reader.getLocation();
                    Map<String, List<ElementMetaInfo>> metas = new HashMap<>();
                    elementInfoMap.forEach((k,v) -> {
                        if (k.getSide() != DiffSide.RIGHT) {
                            Path fullPath = location.resolve(AbstractModelExporter.getRelativeFilePath(
                                    k.getPgStatement(dbProject.getDbObject())));
                            // git always uses linux paths
                            // since all paths here are relative it's ok to simply
                            // join their elements with forward slashes
                            String location = StreamSupport.stream(
                                    root.relativize(fullPath).spliterator(), false)
                                    .map(Path::toString)
                                    .collect(Collectors.joining("/")); //$NON-NLS-1$

                            List<ElementMetaInfo> meta = metas.get(location);
                            if (meta == null) {
                                meta = new ArrayList<>();
                                metas.put(location, meta);
                            }
                            meta.add(v);
                        }
                    });
                    reader.parseLocalChanges(metas);
                    reader.parseLastChange(metas);
                    return Status.OK_STATUS;
                } catch (IOException e) {
                    return new Status(Status.ERROR, PLUGIN_ID.THIS,
                            Messages.DiffTableViewer_error_reading_git_history, e);
                }
            }
        };

        job.addJobChangeListener(new JobChangeAdapter() {

            @Override
            public void done(IJobChangeEvent event) {
                if (event.getResult().isOK()) {
                    UiSync.exec(getDisplay(), () -> {
                        if (viewerFilter.isLocalChange.get()
                                || !viewerFilter.gitUserFilter.isEmpty()
                                || comparator.sortOrder.stream().anyMatch(c -> c.col == Columns.GIT_USER)) {
                            viewer.refresh();
                        } else {
                            viewer.update(elements.toArray(new TreeElement[0]),
                                    new String[] { GITLABEL_PROP });
                        }
                    });
                }
            }
        });

        job.setUser(true);
        job.schedule();
    }

    private void setChecked(TreeElement el, boolean checked) {
        if (elements.contains(el)) {
            // меняем состояние только элементов в наборе
            el.setSelected(checked);
        }
        if (isContainer(el)) {
            setCheckedGrayed(el, null);
        } else {
            viewer.setChecked(el, checked);
            if (isSubElement(el)) {
                setCheckedGrayed(el.getParent(), null);
            }
        }
    }

    private void setCheckedGrayed(TreeElement el, Boolean providedExpandedState) {
        Entry<Boolean, Boolean> pair = checkProvider.getState(el, providedExpandedState);
        viewer.setChecked(el, pair.getKey());
        viewer.setGrayed(el, pair.getValue());
    }

    private void viewerChecksUpdated() {
        updateObjectsLabels();
        for (ICheckStateListener list : programmaticCheckListeners) {
            list.checkStateChanged(null);
        }
    }

    public void updateObjectsLabels() {
        int count = elementInfoMap.size();
        int checked = getCheckedElementsCount();
        if (lineManager != null) {
            lineManager.setMessage(Activator.getRegisteredImage(FILE.ICONAPPSMALL),
                    MessageFormat.format(Messages.DiffTableViewer_selected_count, checked, count));
        } else {
            lblObjectCount.setText(MessageFormat.format(Messages.diffTableViewer_objects, count));
            if (!viewOnly) {
                lblCheckedCount.setText(MessageFormat.format(Messages.DiffTableViewer_selected,
                        checked));
            }
            lblObjectCount.getParent().layout();
        }
    }

    public int getCheckedElementsCount() {
        int count = 0;
        for (TreeElement el : elements) {
            if (el.isSelected()) {
                ++count;
            }
        }
        return count;
    }

    public boolean checkLibChange() {
        for (TreeElement el : elements) {
            if (el.isSelected() && el.getSide() != DiffSide.RIGHT
                    && el.getPgStatement(dbProject.getDbObject()).isLib()) {
                return true;
            }
        }

        return false;
    }

    private void setElementsChecked(Collection<?> elements, boolean state,
            boolean checkFilterMatch) {
        setElementsChecked(elements, el -> state, checkFilterMatch);
    }

    private void setElementsChecked(Collection<?> elements, Predicate<TreeElement> state,
            boolean checkFilterMatch) {
        Stream<TreeElement> stream = elements.stream().map(o -> (TreeElement) o);
        if (checkFilterMatch) {
            stream = stream.filter(el -> viewerFilter.select(viewer, el.getParent(), el));
        }
        stream.forEach(el -> setChecked(el, state.test(el)));

        viewerChecksUpdated();
    }

    private void setSelectionSubtreesChecked(IStructuredSelection selection, boolean checked) {
        for (Object o : selection.toList()) {
            TreeElement el = (TreeElement) o;
            setSubTreeChecked(el, checked);
        }
        viewerChecksUpdated();
    }

    private void setSubTreeChecked(TreeElement element, boolean selected) {
        setChecked(element, selected);
        for (TreeElement child : element.getChildren()) {
            setSubTreeChecked(child, selected);
        }
    }
    public boolean isApplyToProj() {
        return isApplyToProj;
    }

    public void setApplyToProj(boolean isApplyToProj) {
        this.isApplyToProj = isApplyToProj;
        updateSortIndexes();
    }

    public static boolean isContainer(TreeElement el) {
        return el.getType() == DbObjType.TABLE || el.getType() == DbObjType.VIEW;
    }

    public static boolean isSubElement(TreeElement el) {
        TreeElement parent = el.getParent();
        return parent != null && isContainer(parent);
    }

    private class DiffContentProvider implements ITreeContentProvider {

        @Override
        public Object[] getElements(Object inputElement) {
            Collection<?> input = (Collection<?>) inputElement;
            Set<TreeElement> rootTableEntries = new HashSet<>(input.size());
            for (Object o : input) {
                TreeElement el = (TreeElement) o;
                rootTableEntries.add(isSubElement(el) ? el.getParent() : el);
            }
            return rootTableEntries.toArray();
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            if (!(parentElement instanceof TreeElement)) {
                // process as root input (Collection of elements)
                return getElements(parentElement);
            }
            TreeElement el = (TreeElement) parentElement;
            if (isContainer(el) && el.hasChildren()) {
                List<TreeElement> children = el.getChildren();
                List<TreeElement> childrenInInput = new ArrayList<>(children.size());
                for (TreeElement child : children) {
                    if (elements.contains(child)) {
                        childrenInInput.add(child);
                    }
                }
                return childrenInInput.toArray();
            } else {
                return ApgdiffConsts.EMPTY_ARRAY;
            }
        }

        @Override
        public Object getParent(Object element) {
            TreeElement el = (TreeElement) element;
            return isSubElement(el) ? el.getParent() : null;
        }

        @Override
        public boolean hasChildren(Object element) {
            TreeElement el = (TreeElement) element;
            if (isContainer(el) && el.hasChildren()) {
                for (TreeElement child : el.getChildren()) {
                    if (elements.contains(child)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private class CheckStateListener implements ICheckStateListener {

        @Override
        public void checkStateChanged(CheckStateChangedEvent event) {
            List<?> selection = getSelectionList(oldSelection);
            if (selection.contains(event.getElement())) {
                for (Object element : selection) {
                    setChecked(element, event.getChecked());
                }
                viewer.setSelection(oldSelection);
            } else {
                setChecked(event.getElement(), event.getChecked());
            }
            viewerChecksUpdated();
        }

        private List<?> getSelectionList(IStructuredSelection selection) {
            return (selection != null && selection.size() > 1) ? selection.toList()
                    : Collections.emptyList();
        }

        private void setChecked(Object element, boolean checked) {
            TreeElement el = (TreeElement) element;
            if (isContainer(el)) {
                setSubTreeChecked(el, checked);
            }
            // explicitly check root even when using setSubTreeChecked
            // in case it's not in the viewer's input set
            DiffTableViewer.this.setChecked(el, checked);
        }
    }

    private class CheckStateProvider implements ICheckStateProvider {

        @Override
        public boolean isChecked(Object element) {
            TreeElement el = (TreeElement)element;
            if (el.isSelected()) {
                return true;
            }
            // gray nodes need selection to show gray state
            return contGraySelected(el, null);
        }

        @Override
        public boolean isGrayed(Object element) {
            return contGraySelected((TreeElement) element, null);
        }

        private Entry<Boolean, Boolean> getState(TreeElement el, Boolean providedExpandedState) {
            Boolean grayed = contGraySelected(el, providedExpandedState);
            return new SimpleEntry<>(el.isSelected() || grayed, grayed);
        }

        /**
         * @param providedExpandedState element's expanded state, if null viewer is queried
         */
        private boolean contGraySelected(TreeElement el, Boolean providedExpandedState) {
            if (!isContainer(el) || !el.hasChildren() ||
                    (providedExpandedState != null ? providedExpandedState : viewer.getExpandedState(el))) {
                return false;
            }
            boolean hasChecked = false;
            boolean hasUnchecked = false;
            for (TreeElement child : el.getChildren()) {
                if (elements.contains(child)) {
                    if (child.isSelected()) {
                        hasChecked = true;
                    } else {
                        hasUnchecked = true;
                    }
                }
                if (hasChecked && hasUnchecked) {
                    // has both states, no further checking required
                    // also this state is always grayed so we may return here
                    return true;
                }
            }
            // both false means no subelements shown, no gray state needed
            // otherwise hasChecked means ALL or NONE checked at this point
            // and we can use XOR boolean function to get required gray state
            // ALL  PAR  GRAY
            //  0    0    0
            //  0    1    1
            //  1    0    1
            //  1    1    0
            return (hasChecked || hasUnchecked) &&
                    (hasChecked ^ el.isSelected());
        }
    }

    private class RefreshCheckedTreeListener implements ITreeViewerListener {

        @Override
        public void treeExpanded(TreeExpansionEvent event) {
            setCheckedGrayed((TreeElement) event.getElement(), true);
        }

        @Override
        public void treeCollapsed(TreeExpansionEvent event) {
            setCheckedGrayed((TreeElement) event.getElement(), false);
        }
    }

    private static class SortingColumn {

        private final Columns col;
        private final boolean desc;

        public SortingColumn(Columns col, boolean desc) {
            this.col = col;
            this.desc = desc;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof SortingColumn
                    && ((SortingColumn) obj).col == col;
        }

        @Override
        public int hashCode() {
            return col.hashCode();
        }
    }

    private class TableViewerComparator extends ViewerComparator {

        private final Deque<SortingColumn> sortOrder = new LinkedList<>();

        public void clearSortList() {
            sortOrder.clear();
        }

        public void addSort(Columns column) {
            if (!sortOrder.isEmpty() && column.equals(sortOrder.getLast().col)) {
                SortingColumn oldCol = sortOrder.pollLast();
                sortOrder.addLast(new SortingColumn(column, !oldCol.desc));
            } else {
                SortingColumn c = new SortingColumn(column, false);
                sortOrder.remove(c);
                sortOrder.addLast(c);
            }
        }

        @Override
        public int compare(Viewer v, Object e1, Object e2) {
            TreeElement el1 = (TreeElement) e1;
            TreeElement el2 = (TreeElement) e2;

            Iterator<SortingColumn> it = sortOrder.descendingIterator();
            while (it.hasNext()) {
                SortingColumn c = it.next();
                int res = 0;
                switch (c.col) {
                case CHANGE:
                    res = el1.getSide().toString().compareTo(el2.getSide().toString());
                    break;
                case GIT_USER:
                    res = compareUsers(c.col, el1, el2);
                    break;
                case LOCATION:
                    res = el1.getContainerQName().compareTo(el2.getContainerQName());
                    break;
                case CHECK:
                    res = -Boolean.compare(el1.isSelected(), el2.isSelected());
                    break;
                case NAME:
                    res = el1.getName().compareTo(el2.getName());
                    break;
                case TYPE:
                    res = el1.getType().toString().compareTo(el2.getType().toString());
                    break;
                case DB_USER:
                    res = compareUsers(c.col, el1, el2);
                    break;
                default:
                    break;
                }
                if (res != 0) {
                    if (c.desc) {
                        res = -res;
                    }
                    return res;
                }
            }

            return 0;
        }

        private int compareUsers(Columns col, TreeElement el1, TreeElement el2) {
            Function<ElementMetaInfo, String> getter;
            switch(col) {
            case DB_USER:
                getter = ElementMetaInfo::getDbUser;
                break;
            case GIT_USER:
                getter = ElementMetaInfo::getGitUser;
                break;
            default:
                return 0;
            }

            ElementMetaInfo el1Meta = elementInfoMap.get(el1);
            ElementMetaInfo el2Meta = elementInfoMap.get(el2);
            if (el1Meta == null) {
                if (el2Meta == null) {
                    return 0;
                }
                return -1;
            }
            if (el2Meta == null) {
                return 1;
            }

            return getter.apply(el1Meta).compareTo(getter.apply(el2Meta));
        }
    }

    private class TableViewerFilter extends ViewerFilter {

        private final Collection<DbObjType> types = EnumSet.noneOf(DbObjType.class);
        private final Collection<DiffSide> sides = EnumSet.noneOf(DiffSide.class);

        private final AbstractFilter codeFilter = new CodeFilter();
        private final AbstractFilter schemaFilter = new SchemaFilter();
        private final AbstractFilter gitUserFilter = new UserFilter(ElementMetaInfo::getGitUser);
        private final AbstractFilter dbUserFilter = new UserFilter(ElementMetaInfo::getDbUser);

        private final AtomicBoolean isLocalChange = new AtomicBoolean(false);
        private final AtomicBoolean isHideLibs = new AtomicBoolean(false);

        private String filterName;
        private boolean useRegEx;
        private Pattern regExPattern;

        public void setFilter(String value) {
            if (value == null || value.isEmpty()) {
                filterName = null;
                regExPattern = null;
            } else {
                filterName = value.toLowerCase(Locale.ROOT);
                try {
                    regExPattern = Pattern.compile(value, Pattern.CASE_INSENSITIVE);
                } catch (PatternSyntaxException e) {
                    regExPattern = null;
                }
            }
        }

        public boolean isAdvancedEmpty() {
            return types.isEmpty() && sides.isEmpty()
                    && codeFilter.isEmpty()
                    && dbUserFilter.isEmpty()
                    && gitUserFilter.isEmpty()
                    && schemaFilter.isEmpty()
                    && !isLocalChange.get()
                    && !isHideLibs.get();
        }

        public void setUseRegEx(Boolean useRegEx) {
            this.useRegEx = useRegEx;
        }

        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
            TreeElement el = (TreeElement) element;
            boolean isSubElement = isSubElement(el);

            if (!types.isEmpty() && !types.contains(el.getType())
                    && (!isSubElement || !types.contains(el.getParent().getType()))
                    && (!isContainer(el) || el.getChildren().stream()
                            .noneMatch(e -> types.contains(e.getType())))) {
                return false;
            }

            if (!sides.isEmpty() && !sides.contains(el.getSide())
                    && (!isSubElement || !sides.contains(el.getParent().getSide()))
                    && (!isContainer(el) || el.getChildren().stream()
                            .noneMatch(e -> sides.contains(e.getSide())))) {
                return false;
            }

            if (isLocalChange.get() && !hasLocalChanges(el)) {
                return false;
            }

            if (isHideLibs.get() && isLibElement(el)) {
                return false;
            }

            if (!gitUserFilter.isEmpty() && !gitUserFilter.checkElement(el, elementInfoMap, null, null)) {
                return false;
            }

            if (!dbUserFilter.isEmpty() && !dbUserFilter.checkElement(el, elementInfoMap, null, null)) {
                return false;
            }

            if (!schemaFilter.isEmpty() && !schemaFilter.checkElement(el, null, null, null)) {
                return false;
            }

            if (filterName != null && !findName(el, isSubElement)) {
                return false;
            }

            return (codeFilter.isEmpty() || codeFilter.checkElement(el,
                    elementInfoMap, dbProject.getDbObject(), dbRemote.getDbObject()));
        }

        private boolean findName(TreeElement el, boolean isSubElement) {
            Pattern filterRegex = useRegEx ? regExPattern : null;

            // show all child, if parent have match
            TreeElement parent = el.getParent();
            if (isSubElement && getMatchingLocation(parent.getName(), filterName, filterRegex) != null){
                return true;
            }

            boolean found = getMatchingLocation(el.getName(), filterName, filterRegex) != null;

            // also show containers that have content matching current filter
            if (!found && isContainer(el)) {
                Iterator<TreeElement> it = el.getChildren().iterator();
                while (!found && it.hasNext()) {
                    TreeElement child = it.next();
                    found |= elements.contains(child) &&
                            getMatchingLocation(child.getName(), filterName, filterRegex) != null;
                }
            }
            return found;
        }

        private Region getMatchingLocation(String text, String filter, Pattern regExPattern) {
            if (filter != null && !filter.isEmpty() && text != null) {
                String textLc = text.toLowerCase(Locale.ROOT);
                int offset = -1;
                int length = 0;
                if (regExPattern != null) {
                    Matcher matcher = regExPattern.matcher(textLc);
                    if (matcher.find()) {
                        offset = matcher.start();
                        length = matcher.end() - offset;
                    }
                } else {
                    offset = textLc.indexOf(filter);
                    length = filter.length();
                }
                if (offset >= 0) {
                    return new Region(offset, length);
                }
            }
            return null;
        }

        private boolean hasLocalChanges(TreeElement el) {
            ElementMetaInfo meta = elementInfoMap.get(el);

            if (meta != null) {
                if (meta.isChanged()) {
                    return true;
                }

                if (isSubElement(el)) {
                    ElementMetaInfo parent = elementInfoMap.get(el.getParent());
                    return parent != null && parent.isChanged();
                }

                return isContainer(el) && el.getChildren().stream().filter(elementInfoMap::containsKey)
                        .map(elementInfoMap::get).anyMatch(m -> m != null && m.isChanged());
            }

            return false;
        }

        private boolean isLibElement(TreeElement el) {
            ElementMetaInfo meta = elementInfoMap.get(el);

            if (meta != null) {
                return meta.getLibLocation() != null;
            }

            return false;
        }
    }
}