package ru.taximaxim.codekeeper.ui.differ;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.IPreferenceStore;

import cz.startnet.utils.pgdiff.PgDiffArguments;
import cz.startnet.utils.pgdiff.loader.JdbcConnector;
import cz.startnet.utils.pgdiff.loader.JdbcLoader;
import cz.startnet.utils.pgdiff.loader.JdbcMsConnector;
import cz.startnet.utils.pgdiff.loader.JdbcMsLoader;
import cz.startnet.utils.pgdiff.loader.PgDumpLoader;
import cz.startnet.utils.pgdiff.loader.ProjectLoader;
import cz.startnet.utils.pgdiff.parsers.antlr.AntlrError;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts;
import ru.taximaxim.codekeeper.apgdiff.fileutils.TempFile;
import ru.taximaxim.codekeeper.ui.Activator;
import ru.taximaxim.codekeeper.ui.Log;
import ru.taximaxim.codekeeper.ui.UIConsts.DB_UPDATE_PREF;
import ru.taximaxim.codekeeper.ui.UIConsts.PREF;
import ru.taximaxim.codekeeper.ui.UIConsts.PROJ_PREF;
import ru.taximaxim.codekeeper.ui.consoles.UiProgressReporter;
import ru.taximaxim.codekeeper.ui.dbstore.DbInfo;
import ru.taximaxim.codekeeper.ui.externalcalls.PgDumper;
import ru.taximaxim.codekeeper.ui.handlers.OpenProjectUtils;
import ru.taximaxim.codekeeper.ui.localizations.Messages;
import ru.taximaxim.codekeeper.ui.pgdbproject.PgDbProject;
import ru.taximaxim.codekeeper.ui.pgdbproject.parser.UIProjectLoader;

public abstract class DbSource {

    private final String origin;
    private PgDatabase dbObject;
    protected List<? extends Object> errors = Collections.emptyList();

    public String getOrigin() {
        return origin;
    }

    /**
     * @return DB name this source uses or null if not applicable
     */
    public String getDbName() {
        return null;
    }

    public PgDatabase getDbObject() {
        if (dbObject == null) {
            throw new IllegalStateException(
                    Messages.dbSource_db_is_not_loaded_yet_object_is_null);
        }
        return dbObject;
    }

    public PgDatabase get(SubMonitor monitor)
            throws IOException, InterruptedException, CoreException {
        Log.log(Log.LOG_INFO, "Loading DB from " + origin); //$NON-NLS-1$

        dbObject = this.loadInternal(monitor);
        return dbObject;
    }

    public boolean isLoaded(){
        return dbObject != null;
    }

    public List<Object> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    protected DbSource(String origin) {
        this.origin = origin;
    }

    protected abstract PgDatabase loadInternal(SubMonitor monitor)
            throws IOException, InterruptedException, CoreException;

    static PgDiffArguments getPgDiffArgs(String charset, boolean forceUnixNewlines, boolean msSql) {
        return getPgDiffArgs(charset, ApgdiffConsts.UTC, forceUnixNewlines, msSql);
    }

    static PgDiffArguments getPgDiffArgs(String charset, String timeZone,
            boolean forceUnixNewlines, boolean msSql) {
        PgDiffArguments args = new PgDiffArguments();
        IPreferenceStore mainPS = Activator.getDefault().getPreferenceStore();
        args.setInCharsetName(charset);
        args.setAddTransaction(mainPS.getBoolean(DB_UPDATE_PREF.SCRIPT_IN_TRANSACTION));
        args.setDisableCheckFunctionBodies(!mainPS.getBoolean(DB_UPDATE_PREF.CHECK_FUNCTION_BODIES));
        args.setIgnoreConcurrentModification(mainPS.getBoolean(PREF.IGNORE_CONCURRENT_MODIFICATION));
        args.setUsingTypeCastOff(!mainPS.getBoolean(DB_UPDATE_PREF.USING_ON_OFF));
        args.setIgnorePrivileges(mainPS.getBoolean(PREF.NO_PRIVILEGES));
        args.setTimeZone(timeZone);
        args.setKeepNewlines(!forceUnixNewlines);
        args.setMsSql(msSql);
        return args;
    }

    public static DbSource fromDirTree(boolean forceUnixNewlines,String dirTreePath,
            String encoding, boolean isMsSql) {
        return new DbSourceDirTree(forceUnixNewlines, dirTreePath, encoding, isMsSql);
    }

    public static DbSource fromProject(PgDbProject proj) {
        return new DbSourceProject(proj);
    }

    public static DbSource fromFile(boolean forceUnixNewlines, File filename,
            String encoding, boolean isMsSql) {
        return new DbSourceFile(forceUnixNewlines, filename, encoding, isMsSql);
    }

    public static DbSource fromDbInfo(DbInfo dbinfo, IPreferenceStore prefs,
            boolean forceUnixNewlines, String charset, String timezone) {
        if (!dbinfo.isMsSql() && prefs.getBoolean(PREF.PGDUMP_SWITCH)) {
            return DbSource.fromDb(forceUnixNewlines,
                    prefs.getString(PREF.PGDUMP_EXE_PATH),
                    prefs.getString(PREF.PGDUMP_CUSTOM_PARAMS),
                    dbinfo.getDbHost(), dbinfo.getDbPort(),
                    dbinfo.getDbUser(), dbinfo.getDbPass(), dbinfo.getDbName(),
                    charset, timezone);
        } else {
            return DbSource.fromJdbc(dbinfo.getDbHost(), dbinfo.getDbPort(),
                    dbinfo.getDbUser(), dbinfo.getDbPass(), dbinfo.getDbName(),
                    dbinfo.getProperties(), dbinfo.isReadOnly(), timezone,
                    forceUnixNewlines, dbinfo.isMsSql(), dbinfo.isWinAuth());
        }
    }

    public static DbSource fromDb(boolean forceUnixNewlines,
            String exePgdump, String customParams,
            String host, int port, String user, String pass, String dbname,
            String encoding, String timezone) {
        return new DbSourceDb(forceUnixNewlines, exePgdump, customParams,
                host, port, user, pass, dbname, encoding, timezone);
    }

    public static DbSource fromJdbc(String host, int port, String user, String pass, String dbname,
            Map<String, String> properties, boolean readOnly, String timezone,
            boolean forceUnixNewlines, boolean isMsSql, boolean winAuth) {
        return new DbSourceJdbc(host, port, user, pass, dbname, properties, readOnly, timezone,
                forceUnixNewlines, isMsSql, winAuth);
    }

    public static DbSource fromDbObject(PgDatabase db, String origin) {
        return new DbSourceFromDbObject(db, origin);
    }

    /**
     * Calls {@link #getDbObject()} on the argument.
     */
    public static DbSource fromDbObject(DbSource dbSource) {
        return fromDbObject(dbSource.getDbObject(), dbSource.getOrigin());
    }
}

class DbSourceDirTree extends DbSource {

    private final boolean forceUnixNewlines;
    private final String dirTreePath;
    private final String encoding;
    private final boolean isMsSql;

    DbSourceDirTree(boolean forceUnixNewlines, String dirTreePath, String encoding, boolean isMsSql) {
        super(dirTreePath);

        this.forceUnixNewlines = forceUnixNewlines;
        this.dirTreePath = dirTreePath;
        this.encoding = encoding;
        this.isMsSql = isMsSql;
    }

    @Override
    protected PgDatabase loadInternal(SubMonitor monitor)
            throws InterruptedException, IOException {
        monitor.subTask(Messages.dbSource_loading_tree);

        List<AntlrError> er = new ArrayList<>();
        PgDatabase db = new ProjectLoader(dirTreePath, getPgDiffArgs(encoding,
                forceUnixNewlines, isMsSql), monitor, er).loadDatabaseSchemaFromDirTree();
        errors = er;
        return db;
    }
}

class DbSourceProject extends DbSource {

    private final PgDbProject proj;

    DbSourceProject(PgDbProject proj) {
        super(proj.getProjectName());
        this.proj = proj;
    }

    @Override
    protected PgDatabase loadInternal(SubMonitor monitor)
            throws IOException, InterruptedException, CoreException {
        String charset = proj.getProjectCharset();
        monitor.subTask(Messages.dbSource_loading_tree);
        IProject project = proj.getProject();

        monitor.setWorkRemaining(UIProjectLoader.countFiles(project));

        IEclipsePreferences pref = proj.getPrefs();
        List<AntlrError> er = new ArrayList<>();

        PgDiffArguments arguments = getPgDiffArgs(charset,
                pref.getBoolean(PROJ_PREF.FORCE_UNIX_NEWLINES, true),
                OpenProjectUtils.checkMsSql(project));

        PgDatabase db = new UIProjectLoader(project, arguments, monitor, null, er)
                .loadDatabaseWithLibraries();
        errors = er;
        return db;
    }
}

class DbSourceFile extends DbSource {
    /*
     * Магическая константа AVERAGE_STATEMENT_LENGTH получена эмпирическим путем.
     * Она равна количеству строк в файле sql, поделенному на количество выражений.
     *
     * По подсчетам, это число в районе 6. Для верности берем 5.
     */
    private static final int AVERAGE_STATEMENT_LENGTH = 5;

    private final boolean forceUnixNewlines;
    private final File filename;
    private final String encoding;
    private final boolean isMsSql;

    DbSourceFile(boolean forceUnixNewlines, File filename, String encoding, boolean isMsSql) {
        super(filename.getAbsolutePath());

        this.forceUnixNewlines = forceUnixNewlines;
        this.filename = filename;
        this.encoding = encoding;
        this.isMsSql = isMsSql;
    }

    @Override
    protected PgDatabase loadInternal(SubMonitor monitor)
            throws InterruptedException, IOException {
        monitor.subTask(Messages.dbSource_loading_dump);

        try {
            int linesCount = countLines(filename);
            monitor.setWorkRemaining(linesCount > AVERAGE_STATEMENT_LENGTH ?
                    linesCount/AVERAGE_STATEMENT_LENGTH : 1);
        } catch (IOException e) {
            Log.log(Log.LOG_INFO, "Error counting file lines. Setting 1000"); //$NON-NLS-1$
            monitor.setWorkRemaining(1000);
        }

        PgDumpLoader loader = new PgDumpLoader(filename,
                getPgDiffArgs(encoding, forceUnixNewlines, isMsSql),
                monitor, 2);
        try {
            return loader.load();
        } finally {
            errors = loader.getErrors();
        }
    }

    private int countLines(File filename) throws IOException {
        try (FileInputStream fis = new FileInputStream(filename);
                InputStream is = new BufferedInputStream(fis)){
            byte[] c = new byte[1024];
            int count = 0;
            int readChars = 0;
            boolean empty = true;
            while ((readChars = is.read(c)) != -1) {
                empty = false;
                for (int i = 0; i < readChars; ++i) {
                    if (c[i] == '\n') {
                        ++count;
                    }
                }
            }
            return (count == 0 && !empty) ? 1 : count;
        }
    }
}

class DbSourceDb extends DbSource {

    private final boolean forceUnixNewlines;
    private final String exePgdump;
    private final String customParams;

    private final String host;
    private final String user;
    private final String pass;
    private final String dbname;
    private final String encoding;
    private final String timezone;
    private final int port;

    @Override
    public String getDbName() {
        return dbname;
    }

    DbSourceDb(boolean forceUnixNewlines,
            String exePgdump, String customParams,
            String host, int port, String user, String pass,
            String dbname, String encoding, String timezone) {
        super(dbname);

        this.forceUnixNewlines = forceUnixNewlines;
        this.exePgdump = exePgdump;
        this.customParams = customParams;
        this.host = host;
        this.port = port;
        this.user = user;
        this.pass = pass;
        this.dbname = dbname;
        this.encoding = encoding;
        this.timezone = timezone;
    }

    @Override
    protected PgDatabase loadInternal(SubMonitor monitor)
            throws IOException, InterruptedException {
        SubMonitor pm = SubMonitor.convert(monitor, 2);

        try (TempFile tf = new TempFile("tmp_dump_", ".sql")) { //$NON-NLS-1$ //$NON-NLS-2$
            File dump = tf.get().toFile();

            pm.newChild(1).subTask(Messages.dbSource_executing_pg_dump);

            new PgDumper(exePgdump, customParams,
                    host, port, user, pass, dbname, encoding, timezone,
                    dump.getAbsolutePath(), new UiProgressReporter(monitor)).pgDump();

            pm.newChild(1).subTask(Messages.dbSource_loading_dump);

            PgDumpLoader loader = new PgDumpLoader(dump,
                    getPgDiffArgs(encoding, forceUnixNewlines, false), monitor);
            try {
                return loader.load();
            } finally {
                errors = loader.getErrors();
            }
        }
    }
}

class DbSourceJdbc extends DbSource {

    private final JdbcConnector jdbcConnector;
    private final String dbName;
    private final boolean forceUnixNewlines;
    private final boolean isMsSql;

    @Override
    public String getDbName() {
        return dbName;
    }

    DbSourceJdbc(String host, int port, String user, String pass, String dbName,
            Map<String, String> properties, boolean readOnly, String timezone,
            boolean forceUnixNewlines, boolean isMsSql, boolean winAuth) {
        super(dbName);
        this.dbName = dbName;
        this.forceUnixNewlines = forceUnixNewlines;
        this.isMsSql = isMsSql;
        if (isMsSql) {
            jdbcConnector = new JdbcMsConnector(host, port, user, pass, dbName, properties,
                    readOnly, winAuth);
        } else {
            jdbcConnector = new JdbcConnector(host, port, user, pass, dbName, properties,
                    readOnly, timezone);
        }
    }

    @Override
    protected PgDatabase loadInternal(SubMonitor monitor)
            throws IOException, InterruptedException {
        monitor.subTask(Messages.reading_db_from_jdbc);
        PgDiffArguments args = getPgDiffArgs(ApgdiffConsts.UTF_8, forceUnixNewlines, isMsSql);
        if (isMsSql) {
            return new JdbcMsLoader(jdbcConnector, args, monitor).readDb();
        }

        JdbcLoader loader = new JdbcLoader(jdbcConnector, args, monitor);
        PgDatabase database = loader.getDbFromJdbc();
        errors = loader.getErrors();
        return database;
    }
}

class DbSourceFromDbObject extends DbSource {

    PgDatabase db;

    protected DbSourceFromDbObject(PgDatabase db, String origin) {
        super(origin);
        this.db = db;
    }

    @Override
    protected PgDatabase loadInternal(SubMonitor monitor) throws IOException {
        return db;
    }
}
