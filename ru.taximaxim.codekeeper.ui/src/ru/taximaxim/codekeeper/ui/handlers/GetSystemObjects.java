package ru.taximaxim.codekeeper.ui.handlers;

import java.io.IOException;
import java.io.Serializable;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import cz.startnet.utils.pgdiff.loader.JdbcConnector;
import cz.startnet.utils.pgdiff.loader.JdbcSystemLoader;
import cz.startnet.utils.pgdiff.schema.meta.MetaStorage;
import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts;
import ru.taximaxim.codekeeper.apgdiff.ApgdiffUtils;
import ru.taximaxim.codekeeper.ui.dbstore.DbInfo;
import ru.taximaxim.codekeeper.ui.dialogs.ExceptionNotifier;
import ru.taximaxim.codekeeper.ui.editors.ProjectEditorDiffer;
import ru.taximaxim.codekeeper.ui.localizations.Messages;

public class GetSystemObjects extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) {
        IWorkbenchPart part = HandlerUtil.getActiveEditor(event);
        if (part instanceof ProjectEditorDiffer){
            ProjectEditorDiffer differ = (ProjectEditorDiffer) part;
            Object db = differ.getCurrentDb();
            if (db instanceof DbInfo && !((DbInfo) db).isMsSql()) {
                DbInfo info = ((DbInfo) db);
                FileDialog fd = new FileDialog(HandlerUtil.getActiveShell(event), SWT.SAVE);
                fd.setText(Messages.GetSystemObjects_save_dialog_title);
                fd.setFileName(MetaStorage.FILE_NAME + info.getDbName() + ".ser"); //$NON-NLS-1$
                String select = fd.open();
                if (select != null) {
                    JdbcConnector jdbcConnector = new JdbcConnector(info.getDbHost(),
                            info.getDbPort(), info.getDbUser(), info.getDbPass(),
                            info.getDbName(), info.getProperties(), info.isReadOnly(),
                            ApgdiffConsts.UTC);
                    try {
                        Serializable storage = new JdbcSystemLoader(jdbcConnector,
                                SubMonitor.convert(new NullProgressMonitor())).getStorageFromJdbc();

                        ApgdiffUtils.serialize(select, storage);

                        MessageBox mb = new MessageBox(HandlerUtil.getActiveShell(event), SWT.ICON_INFORMATION);
                        mb.setText(Messages.GetSystemObjects_save_success_title);
                        mb.setMessage(Messages.GetSystemObjects_save_success_message);
                        mb.open();
                    } catch (IOException | InterruptedException e) {
                        ExceptionNotifier.notifyDefault(e.getLocalizedMessage(), e);
                    }
                }
            }
        }

        return null;
    }

    @Override
    public boolean isEnabled() {
        IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        return editor instanceof ProjectEditorDiffer;
    }
}