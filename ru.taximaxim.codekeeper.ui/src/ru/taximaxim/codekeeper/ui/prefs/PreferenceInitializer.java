package ru.taximaxim.codekeeper.ui.prefs;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.RGB;

import ru.taximaxim.codekeeper.ui.Activator;
import ru.taximaxim.codekeeper.ui.UIConsts;
import ru.taximaxim.codekeeper.ui.UIConsts.DB_UPDATE_PREF;
import ru.taximaxim.codekeeper.ui.UIConsts.PG_EDIT_PREF;
import ru.taximaxim.codekeeper.ui.UIConsts.PREF;
import ru.taximaxim.codekeeper.ui.UIConsts.SQL_EDITOR_PREF;
import ru.taximaxim.codekeeper.ui.UIConsts.USAGE_REPORT_PREF;
import ru.taximaxim.codekeeper.ui.sqledit.SQLEditorStatementTypes;
import ru.taximaxim.codekeeper.ui.sqledit.SQLEditorSyntaxModel;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();

        store.setDefault(PREF.FORCE_SHOW_CONSOLE, true);
        store.setDefault(PREF.PARSER_CACHE_CLEANING_INTERVAL, 30);

        store.setDefault(PG_EDIT_PREF.EDITOR_UPDATE_ACTION, PG_EDIT_PREF.NO_ACTION);
        store.setDefault(PG_EDIT_PREF.SHOW_GIT_USER, true);

        store.setDefault(DB_UPDATE_PREF.ALTER_COLUMN_STATEMENT, true);
        store.setDefault(DB_UPDATE_PREF.DROP_COLUMN_STATEMENT, true);
        store.setDefault(DB_UPDATE_PREF.DROP_TABLE_STATEMENT, true);
        store.setDefault(DB_UPDATE_PREF.RESTART_WITH_STATEMENT, true);
        store.setDefault(DB_UPDATE_PREF.UPDATE_STATEMENT, true);
        store.setDefault(DB_UPDATE_PREF.USING_ON_OFF, true);
        store.setDefault(DB_UPDATE_PREF.DELETE_SCRIPT_AFTER_CLOSE, MessageDialogWithToggle.PROMPT);
        store.setDefault(DB_UPDATE_PREF.CREATE_SCRIPT_IN_PROJECT, MessageDialogWithToggle.ALWAYS);
        store.setDefault(DB_UPDATE_PREF.MIGRATION_COMMAND, UIConsts.DDL_DEFAULT_CMD);

        store.setDefault(PG_EDIT_PREF.PERSPECTIVE_CHANGING_STATUS, MessageDialogWithToggle.PROMPT);

        store.setDefault(USAGE_REPORT_PREF.USAGEREPORT_ENABLED_ID, true);
        store.setDefault(USAGE_REPORT_PREF.ASK_USER_USAGEREPORT_ID, true);

        store.setDefault(SQL_EDITOR_PREF.MATCHING_BRACKETS, true);
        store.setDefault(SQL_EDITOR_PREF.MATCHING_BRACKETS_COLOR, "127, 0, 85"); //$NON-NLS-1$
        store.setDefault(SQL_EDITOR_PREF.HIGHLIGHT_BRACKET_AT_CARET_LOCATION, true);
        store.setDefault(SQL_EDITOR_PREF.ENCLOSING_BRACKETS, true);

        setSQLSyntaxColorDefaults(store);
    }

    private void setSQLSyntaxColorDefaults(IPreferenceStore store) {
        for (SQLEditorStatementTypes type : SQLEditorStatementTypes.values()) {
            SQLEditorSyntaxModel syntax = new SQLEditorSyntaxModel(type, store);
            switch (type) {
            case RESERVED_WORDS:
            case UN_RESERVED_WORDS:
                syntax.setBold(true);
                syntax.setColor(new RGB(127, 0, 85));
                syntax.setItalic(false);
                syntax.setStrikethrough(false);
                syntax.setUnderline(false);
                break;
            case SINGLE_LINE_COMMENTS:
                syntax.setBold(false);
                syntax.setColor(new RGB(64, 128, 128));
                syntax.setItalic(false);
                syntax.setStrikethrough(false);
                syntax.setUnderline(false);
                break;
            case MULTI_LINE_COMMENTS:
                syntax.setBold(false);
                syntax.setColor(new RGB(0, 0, 200));
                syntax.setItalic(true);
                syntax.setStrikethrough(false);
                syntax.setUnderline(false);
                break;
            case TYPES:
                syntax.setBold(true);
                syntax.setColor(new RGB(64, 0, 200));
                syntax.setItalic(false);
                syntax.setStrikethrough(false);
                syntax.setUnderline(false);
                break;
            case CHARACTER_STRING_LITERAL:
                syntax.setBold(false);
                syntax.setColor(new RGB(0, 0, 200));
                syntax.setItalic(false);
                syntax.setStrikethrough(false);
                syntax.setUnderline(false);
                break;
            case QUOTED_IDENTIFIER:
            case FUNCTIONS:
                syntax.setBold(false);
                syntax.setColor(new RGB(0, 0, 128));
                syntax.setItalic(false);
                syntax.setStrikethrough(false);
                syntax.setUnderline(false);
                break;
            default:
                break;
            }
            syntax.setDefault();
        }
    }
}
