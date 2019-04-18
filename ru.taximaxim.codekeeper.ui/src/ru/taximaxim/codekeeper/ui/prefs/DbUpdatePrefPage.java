package ru.taximaxim.codekeeper.ui.prefs;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ru.taximaxim.codekeeper.ui.Activator;
import ru.taximaxim.codekeeper.ui.UIConsts.CMD_VARS;
import ru.taximaxim.codekeeper.ui.UIConsts.DB_UPDATE_PREF;
import ru.taximaxim.codekeeper.ui.localizations.Messages;

public class DbUpdatePrefPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public DbUpdatePrefPage() {
        super(GRID);
    }

    @Override
    public void init(IWorkbench workbench) {
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
    }

    @Override
    protected void createFieldEditors() {

        addField( new ComboFieldEditor(DB_UPDATE_PREF.CREATE_SCRIPT_IN_PROJECT,
                Messages.dbUpdatePrefPage_create_script_project_directory, new String[][] {
            {Messages.dbUpdatePrefPage_status_alway_create, MessageDialogWithToggle.ALWAYS},
            {Messages.dbUpdatePrefPage_status_never_create, MessageDialogWithToggle.NEVER},
            {Messages.dbUpdatePrefPage_status_ask_create, MessageDialogWithToggle.PROMPT}},
                getFieldEditorParent()));

        addField( new ComboFieldEditor(DB_UPDATE_PREF.DELETE_SCRIPT_AFTER_CLOSE,
                Messages.dbUpdatePrefPage_script_deleting_status, new String[][] {
            {Messages.dbUpdatePrefPage_status_always_delete, MessageDialogWithToggle.ALWAYS},
            {Messages.dbUpdatePrefPage_status_never_delete, MessageDialogWithToggle.NEVER},
            {Messages.dbUpdatePrefPage_status_ask_delete, MessageDialogWithToggle.PROMPT}},
                getFieldEditorParent()));

        Map<String, String> pref = new HashMap<>();
        pref.put(DB_UPDATE_PREF.DROP_TABLE_STATEMENT, Messages.dBUpdatePrefPage_drop_table);
        pref.put(DB_UPDATE_PREF.ALTER_COLUMN_STATEMENT, Messages.dBUpdatePrefPage_alter_column_statement);
        pref.put(DB_UPDATE_PREF.DROP_COLUMN_STATEMENT, Messages.dBUpdatePrefPage_drop_column_statement);
        pref.put(DB_UPDATE_PREF.RESTART_WITH_STATEMENT, Messages.DbUpdatePrefPage_alter_seq_restart_statement);
        pref.put(DB_UPDATE_PREF.UPDATE_STATEMENT, Messages.DbUpdatePrefPage_update_statement);
        GroupFieldsEditor gfe = new GroupFieldsEditor(pref,
                Messages.dBUpdatePrefPage_set_warning_when_next_statements_present,
                getFieldEditorParent());
        addField(gfe);

        addField(new BooleanFieldEditor(DB_UPDATE_PREF.SCRIPT_IN_TRANSACTION,
                Messages.dbUpdatePrefPage_script_add_transaction,
                getFieldEditorParent()));

        BooleanFieldEditor functionBodies = new BooleanFieldEditor(
                DB_UPDATE_PREF.CHECK_FUNCTION_BODIES,
                Messages.dbUpdatePrefPage_check_function_bodies,
                getFieldEditorParent());
        addField(functionBodies);

        BooleanFieldEditor usingOnOff = new BooleanFieldEditor(DB_UPDATE_PREF.USING_ON_OFF,
                Messages.dbUpdatePrefPage_switch_on_off_using, getFieldEditorParent());
        addField(usingOnOff);

        addField(new BooleanFieldEditor(DB_UPDATE_PREF.PRINT_INDEX_WITH_CONCURRENTLY,
                Messages.DbUpdatePrefPage_print_index_with_concurrently, getFieldEditorParent()));

        BooleanFieldEditor commandLineDdlUpdate = new BooleanFieldEditor(DB_UPDATE_PREF.COMMAND_LINE_DDL_UPDATE,
                Messages.dbUpdatePrefPage_use_command_for_ddl_update, getFieldEditorParent());
        addField(commandLineDdlUpdate);

        StringFieldEditor cmdUpdate = new StringFieldEditor(DB_UPDATE_PREF.MIGRATION_COMMAND,
                Messages.dbUpdatePrefPage_Enter_cmd_to_update_ddl_with_sql_script
                + CMD_VARS.SCRIPT_PLACEHOLDER + ' '
                + CMD_VARS.DB_NAME_PLACEHOLDER + ' '
                + CMD_VARS.DB_HOST_PLACEHOLDER + ' ' + CMD_VARS.DB_PORT_PLACEHOLDER + ' '
                + CMD_VARS.DB_USER_PLACEHOLDER + ' ' + CMD_VARS.DB_PASS_PLACEHOLDER + ')' + ':',
                30, getFieldEditorParent());
        addField(cmdUpdate);
    }
}