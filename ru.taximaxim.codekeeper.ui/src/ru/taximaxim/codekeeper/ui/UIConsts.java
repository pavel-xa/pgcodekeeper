package ru.taximaxim.codekeeper.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface UIConsts {

    String _NL = System.lineSeparator();

    interface PLUGIN_ID {
        String THIS = "ru.taximaxim.codekeeper.ui"; //$NON-NLS-1$
    }

    interface PERSPECTIVE {
        String MAIN = PLUGIN_ID.THIS + ".mainperspective"; //$NON-NLS-1$
    }

    interface CONTEXT {
        String MAIN = PLUGIN_ID.THIS + ".pgCodeKeeper"; //$NON-NLS-1$
        String EDITOR = PLUGIN_ID.THIS + ".pgCodeKeeperEditorScope"; //$NON-NLS-1$
    }

    interface EDITOR {
        String PROJECT = PLUGIN_ID.THIS + ".projectEditorDiffer"; //$NON-NLS-1$
        String SQL = PLUGIN_ID.THIS + ".SQLEditor"; //$NON-NLS-1$
    }

    interface MARKER {
        String ERROR = PLUGIN_ID.THIS + ".sql.errormarker"; //$NON-NLS-1$
        String DANGER_ANNOTATION = PLUGIN_ID.THIS + ".sql.dangerannotation"; //$NON-NLS-1$
    }

    interface DECORATOR {
        String DECORATOR = PLUGIN_ID.THIS + ".decorator"; //$NON-NLS-1$
    }

    interface VIEW {
        String OVERRIDE_VIEW = PLUGIN_ID.THIS + ".pgoverrideview"; //$NON-NLS-1$
        String RESULT_SET_VIEW = PLUGIN_ID.THIS + ".resultsetview"; //$NON-NLS-1$
    }

    interface WIZARD {
        String NEW_PROJECT_WIZARD = PLUGIN_ID.THIS + ".newprojwizard"; //$NON-NLS-1$
    }

    interface COMMAND {
        /* EGit commit command id
        (value of org.eclipse.egit.ui.internal.actions.ActionCommands.COMMIT_ACTION) */
        String COMMIT_COMMAND_ID = "org.eclipse.egit.ui.team.Commit"; //$NON-NLS-1$

        String ADD_DEPCY = PLUGIN_ID.THIS + ".command.AddDepcy"; //$NON-NLS-1$
    }

    interface PREF_PAGE {
        String DB_STORE = PLUGIN_ID.THIS + ".dbstore"; //$NON-NLS-1$
    }

    interface PREF {
        String FORCE_SHOW_CONSOLE = "prefForceShowConsole"; //$NON-NLS-1$
        String DB_STORE_FILES = "prefDbStoreHistory"; //$NON-NLS-1$
        String NO_PRIVILEGES = "prefNoPrivileges"; //$NON-NLS-1$
        String SIMPLIFY_VIEW = "prefSimplifyView"; //$NON-NLS-1$
        String ENABLE_BODY_DEPENDENCIES = "prefEnableBodyDependencies"; //$NON-NLS-1$
        String LAST_OPENED_LOCATION = "prefLastOpenedLocation"; //$NON-NLS-1$
        String CALL_COMMIT_COMMAND_AFTER_UPDATE = "callCommitCommandAfterUpdate"; //$NON-NLS-1$
        String LAST_CREATED_OBJECT_TYPE = "prefLastCreatedObjectType"; //$NON-NLS-1$
        String EXPLICIT_TYPE_CAST = "explicitTypeCast"; //$NON-NLS-1$
        String REUSE_OPEN_COMPARE_EDITOR = "reuseOpenCompareEditors"; //$NON-NLS-1$
        String IGNORE_CONCURRENT_MODIFICATION = "ignoreConcurrentModification"; //$NON-NLS-1$
        String PARSER_CACHE_CLEANING_INTERVAL = "parserCacheCleaningInterval"; //$NON-NLS-1$
    }

    interface DB_UPDATE_PREF {
        String CREATE_SCRIPT_IN_PROJECT = "prefAddScriptToProject"; //$NON-NLS-1$
        String DELETE_SCRIPT_AFTER_CLOSE = "prefDeleteScriptAfterClose"; //$NON-NLS-1$
        String DROP_TABLE_STATEMENT = "prefDropTableStatement"; //$NON-NLS-1$
        String ALTER_COLUMN_STATEMENT = "prefAlterColumnStatement"; //$NON-NLS-1$
        String DROP_COLUMN_STATEMENT = "prefDropColumnStatement"; //$NON-NLS-1$
        String RESTART_WITH_STATEMENT = "prefRestartWithStatement"; //$NON-NLS-1$
        String UPDATE_STATEMENT = "prefUpdateStatement"; //$NON-NLS-1$
        String SCRIPT_IN_TRANSACTION = "prefScriptInTransaction"; //$NON-NLS-1$
        String CHECK_FUNCTION_BODIES = "prefCheckFunctionBodies"; //$NON-NLS-1$
        String USING_ON_OFF = "prefUsingOnOff"; //$NON-NLS-1$;
        String COMMAND_LINE_DDL_UPDATE = "prefCommandLineDdlUpdate"; //$NON-NLS-1$;
        String MIGRATION_COMMAND = "prefMigrationCommand"; //$NON-NLS-1$;
        String PRINT_INDEX_WITH_CONCURRENTLY = "prefPrintIndexWithConcurrently"; //$NON-NLS-1$;
    }

    interface PG_EDIT_PREF {
        String PERSPECTIVE_CHANGING_STATUS = "perspectiveChangingStatus"; //$NON-NLS-1$
        String EDITOR_UPDATE_ACTION = "editorUpdateAction"; //$NON-NLS-1$
        String SHOW_GIT_USER = "showGitUser"; //$NON-NLS-1$
        String UPDATE = "UPDATE"; //$NON-NLS-1$
        String RESET = "RESET"; //$NON-NLS-1$
        String NO_ACTION = "NO_ACTION"; //$NON-NLS-1$
    }

    interface SQL_EDITOR_PREF {
        String MATCHING_BRACKETS = "matchingBrackets"; //$NON-NLS-1$
        String MATCHING_BRACKETS_COLOR = "matchingBracketsColor"; //$NON-NLS-1$
        String HIGHLIGHT_BRACKET_AT_CARET_LOCATION = "highlightBracketAtCaretLocation"; //$NON-NLS-1$
        String ENCLOSING_BRACKETS = "enclosingBrackets"; //$NON-NLS-1$
    }

    interface USAGE_REPORT_PREF {
        String USAGEREPORT_ENABLED_ID = "allow_usage_report_preference"; //$NON-NLS-1$
        String ASK_USER_USAGEREPORT_ID = "ask_user_for_usage_report_preference"; //$NON-NLS-1$
        String ECLIPSE_INSTANCE_ID = "eclipse_instance_id"; //$NON-NLS-1$
        String FIRST_VISIT = "first_visit"; //$NON-NLS-1$
        String LAST_VISIT = "last_visit"; //$NON-NLS-1$
        String VISIT_COUNT = "visit_count"; //$NON-NLS-1$
    }

    interface LANGUAGE {
        String POSTGRESQL = "PostgreSQL"; //$NON-NLS-1$
        String MS_SQL = "MS SQL"; //$NON-NLS-1$
    }

    interface PROJ_PREF {
        String TIMEZONE = "prefGeneralTimezone"; //$NON-NLS-1$
        String FORCE_UNIX_NEWLINES = "prefForceUnixNewlines"; //$NON-NLS-1$
        String LAST_DB_STORE = "prefLastDbStore"; //$NON-NLS-1$
        String LAST_DB_STORE_EDITOR = "prefLastDbStoreEditor"; //$NON-NLS-1$
        String DISABLE_PARSER_IN_EXTERNAL_FILES = "disableParserInExternalFiles"; //$NON-NLS-1$
        String LIB_SAFE_MODE = "libSafeMode"; //$NON-NLS-1$
        String STORAGE_LIST = "storageList"; //$NON-NLS-1$
        String NAME_OF_BOUND_DB = "nameOfBoundDatabase"; //$NON-NLS-1$
        String ENABLE_PROJ_PREF_ROOT = "prefEnableProjPrefRoot"; //$NON-NLS-1$
        String ENABLE_PROJ_PREF_DB_UPDATE = "prefEnableProjPrefDbUpdate"; //$NON-NLS-1$
        String USE_GLOBAL_IGNORE_LIST = "prefUseGlobalIgnoreList"; //$NON-NLS-1$
    }

    interface PROJ_PATH {
        String MIGRATION_DIR = "MIGRATION"; //$NON-NLS-1$
    }

    interface NATURE {
        String ID = PLUGIN_ID.THIS + ".nature"; //$NON-NLS-1$
        String MS = PLUGIN_ID.THIS + ".msnature"; //$NON-NLS-1$
    }

    interface BUILDER {
        String ID = PLUGIN_ID.THIS + ".builder"; //$NON-NLS-1$
    }

    interface FILE {
        String IGNORED_OBJECTS = ".pgcodekeeperignore"; //$NON-NLS-1$
        String IGNORE_LISTS_STORE = PLUGIN_ID.THIS + ".ignoreliststore"; //$NON-NLS-1$

        // external icons
        String ICONAPPSMALL = "/icons/app_icon16.png"; //$NON-NLS-1$
        String ICONAPPWIZ = "/icons/app_icon_wiz.png"; //$NON-NLS-1$
        String ICONAPPBIG = "/icons/app_icon128.png"; //$NON-NLS-1$
        String ICONBALLBLUE = "/icons/ball_blue.png"; //$NON-NLS-1$
        String ICONBALLGREEN = "/icons/ball_green.png"; //$NON-NLS-1$
        String ICONADDDEP = "/icons/add_dep.png"; //$NON-NLS-1$
        String PGPASS = "/icons/pg_pass.png"; //$NON-NLS-1$

        // pgadmin icons
        String ICONPGADMIN = "/icons/pgadmin/"; //$NON-NLS-1$
        String ICONDATABASE = ICONPGADMIN + "database.png"; //$NON-NLS-1$

        // copies of inaccessible Eclipse icons
        String ICONUP = "/icons/search_prev.gif"; //$NON-NLS-1$
        String ICONDOWN = "/icons/search_next.gif"; //$NON-NLS-1$
        String ICONEDIT = "/icons/editor_area.png"; //$NON-NLS-1$
        String ICONSELECTALL = "/icons/check_all.gif"; //$NON-NLS-1$
        String ICONSELECTNONE = "/icons/uncheck_all.gif"; //$NON-NLS-1$
        String ICONINVERTSELECTION = "/icons/loop_obj.png"; //$NON-NLS-1$
        String ICONREFRESH = "/icons/refresh.png"; //$NON-NLS-1$
        String ICONWRITEOUTCONSOLE = "/icons/writeout_co.png"; //$NON-NLS-1$
        String ICONCHECK = "/icons/header_complete.gif"; //$NON-NLS-1$
        String ICONEMPTYFILTER = "/icons/empty_filter.png"; //$NON-NLS-1$
        String ICONFILTER = "/icons/filter_tsk.png"; //$NON-NLS-1$
        String ICONALERT = "/icons/alert_obj.gif"; //$NON-NLS-1$
        String ICONSORT = "/icons/alpha_mode.gif"; //$NON-NLS-1$
        String ICONLIB = "/icons/lib.gif"; //$NON-NLS-1$
        String ICONCLOUD = "/icons/cloud.png"; //$NON-NLS-1$
    }

    interface WORKING_SET {
        String RESOURCE_WORKING_SET = "org.eclipse.ui.resourceWorkingSetPage"; //$NON-NLS-1$
    }

    interface CMD_VARS {
        String SCRIPT_PLACEHOLDER = "%script"; //$NON-NLS-1$
        String DB_HOST_PLACEHOLDER = "%host"; //$NON-NLS-1$
        String DB_PORT_PLACEHOLDER = "%port"; //$NON-NLS-1$
        String DB_NAME_PLACEHOLDER = "%db"; //$NON-NLS-1$
        String DB_USER_PLACEHOLDER = "%user"; //$NON-NLS-1$
        String DB_PASS_PLACEHOLDER = "%pass"; //$NON-NLS-1$
    }

    String DDL_DEFAULT_CMD = "psql -e -1 -w --set ON_ERROR_STOP=1 -X -h %host -p %port -U %user -f %script %db"; //$NON-NLS-1$

    List<String> TIME_ZONES = Collections.unmodifiableList(Arrays.asList(
            "UTC-12:00", //$NON-NLS-1$
            "UTC-11:00", //$NON-NLS-1$
            "UTC-10:00", //$NON-NLS-1$
            "UTC-09:00", //$NON-NLS-1$
            "UTC-08:00", //$NON-NLS-1$
            "UTC-07:00", //$NON-NLS-1$
            "UTC-06:00", //$NON-NLS-1$
            "UTC-05:00", //$NON-NLS-1$
            "UTC-04:00", //$NON-NLS-1$
            "UTC-03:00", //$NON-NLS-1$
            "UTC-02:00", //$NON-NLS-1$
            "UTC-01:00", //$NON-NLS-1$
            "UTC", //$NON-NLS-1$
            "UTC+01:00", //$NON-NLS-1$
            "UTC+02:00", //$NON-NLS-1$
            "UTC+03:00", //$NON-NLS-1$
            "UTC+04:00", //$NON-NLS-1$
            "UTC+05:00", //$NON-NLS-1$
            "UTC+06:00", //$NON-NLS-1$
            "UTC+07:00", //$NON-NLS-1$
            "UTC+08:00", //$NON-NLS-1$
            "UTC+09:00", //$NON-NLS-1$
            "UTC+10:00", //$NON-NLS-1$
            "UTC+11:00", //$NON-NLS-1$
            "UTC+12:00" //$NON-NLS-1$
            ));
    List<String> ENCODINGS = Collections.unmodifiableList(Arrays.asList(
            "UTF-8", //$NON-NLS-1$
            "UTF-16", //$NON-NLS-1$
            "UTF-16BE", //$NON-NLS-1$
            "UTF-16LE", //$NON-NLS-1$
            "US-ASCII", //$NON-NLS-1$
            "KOI8-R", //$NON-NLS-1$
            "windows-1251", //$NON-NLS-1$
            "windows-1252" //$NON-NLS-1$
            ));
}
