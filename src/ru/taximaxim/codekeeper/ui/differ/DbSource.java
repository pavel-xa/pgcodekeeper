package ru.taximaxim.codekeeper.ui.differ;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.preference.IPreferenceStore;

import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts;
import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts.JDBC_CONSTS;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.PgDbFilter2;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.TreeElement;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.TreeElement.DiffSide;
import ru.taximaxim.codekeeper.ui.Activator;
import ru.taximaxim.codekeeper.ui.Log;
import ru.taximaxim.codekeeper.ui.UIConsts;
import ru.taximaxim.codekeeper.ui.UIConsts.PREF;
import ru.taximaxim.codekeeper.ui.UIConsts.PROJ_PREF;
import ru.taximaxim.codekeeper.ui.externalcalls.PgDumper;
import ru.taximaxim.codekeeper.ui.fileutils.TempFile;
import ru.taximaxim.codekeeper.ui.localizations.Messages;
import ru.taximaxim.codekeeper.ui.pgdbproject.PgDbProject;
import cz.startnet.utils.pgdiff.PgDiffArguments;
import cz.startnet.utils.pgdiff.loader.JdbcConnector;
import cz.startnet.utils.pgdiff.loader.JdbcLoader;
import cz.startnet.utils.pgdiff.loader.ParserClass;
import cz.startnet.utils.pgdiff.loader.PgDumpLoader;
import cz.startnet.utils.pgdiff.schema.PgDatabase;

public abstract class DbSource {

    private final String origin;

    private PgDatabase dbObject;

    public String getOrigin() {
        return origin;
    }

    public PgDatabase getDbObject() {
        if (dbObject == null) {
            throw new IllegalStateException(
                    Messages.dbSource_db_is_not_loaded_yet_object_is_null);
        }
        return dbObject;
    }

    public PgDatabase get(SubMonitor monitor) throws IOException {
        Log.log(Log.LOG_INFO, "Loading DB from " + origin); //$NON-NLS-1$
        
        dbObject = this.loadInternal(monitor);
        return dbObject;
    }

    public boolean isLoaded(){
        return dbObject != null;
    }
    
    protected DbSource(String origin) {
        this.origin = origin;
    }
    
    protected static PgDiffArguments getPgDiffArgs(String charset, String timeZone) {
        PgDiffArguments args = new PgDiffArguments();
        IPreferenceStore mainPS = Activator.getDefault().getPreferenceStore();
        args.setInCharsetName(charset);
        args.setAddTransaction(mainPS.getBoolean(UIConsts.DB_UPDATE_PREF.SCRIPT_IN_TRANSACTION));
        args.setCheckFunctionBodies(mainPS.getBoolean(UIConsts.DB_UPDATE_PREF.CHECK_FUNCTION_BODIES));
        args.setIgnorePrivileges(mainPS.getBoolean(PREF.NO_PRIVILEGES));
        args.setTimeZone(timeZone);
        return args;
    }

    protected abstract PgDatabase loadInternal(SubMonitor monitor)
            throws IOException;

    public static DbSource fromDirTree(ParserClass parser,
            String dirTreePath, String encoding) {
        return new DbSourceDirTree(parser, dirTreePath, encoding);
    }

    public static DbSource fromProject(ParserClass parser, PgDbProject proj) {
        return new DbSourceProject(parser, proj);
    }

    public static DbSource fromFile(ParserClass parser,
            String filename, String encoding) {
        return new DbSourceFile(parser, filename, encoding);
    }

    public static DbSource fromDb(ParserClass parser,
            String exePgdump, String customParams,
            PgDbProject proj, String password) throws CoreException {
        return new DbSourceDb(parser, exePgdump, customParams, proj, password);
    }

    public static DbSource fromDb(ParserClass parser, String exePgdump, String customParams,
            String host, int port, String user, String pass, String dbname,
            String encoding, String timezone) {
        return new DbSourceDb(parser, exePgdump, customParams,
                host, port, user, pass, dbname, encoding, timezone);
    }

    public static DbSource fromFilter(DbSource src, TreeElement filter,
            DiffSide side) {
        return new DbSourceFilter(src, filter, side);
    }
    
    public static DbSource fromJdbc(PgDbProject proj, String password,
            boolean useAntrlForViews) throws CoreException{
        return fromJdbc(proj.getPrefs().get(PROJ_PREF.DB_HOST, ""),  //$NON-NLS-1$
                proj.getPrefs().getInt(PROJ_PREF.DB_PORT, JDBC_CONSTS.JDBC_DEFAULT_PORT),
                proj.getPrefs().get(PROJ_PREF.DB_USER, ""),  //$NON-NLS-1$
                password,
                proj.getPrefs().get(PROJ_PREF.DB_NAME, ""),  //$NON-NLS-1$
                proj.getProjectCharset(), 
                proj.getPrefs().get(PROJ_PREF.TIMEZONE, ApgdiffConsts.UTC),
                useAntrlForViews);
    }
    
    public static DbSource fromJdbc(String host, int port, String user, String pass, String dbname,
            String encoding, String timezone, boolean useAntrlForViews) {
        return new DbSourceJdbc(host, port, user, pass, dbname,
                encoding, timezone, useAntrlForViews);
    }
    
    public static DbSource fromDbObject(PgDatabase db, String origin) {
        return new DbSourceFromDbObject(db, origin);
    }
}

class DbSourceDirTree extends DbSource {

    private final ParserClass parser;
    private final String dirTreePath;
    private final String encoding;

    DbSourceDirTree(ParserClass parser, String dirTreePath, String encoding) {
        super(dirTreePath);

        this.parser = parser;
        this.dirTreePath = dirTreePath;
        this.encoding = encoding;
    }

    @Override
    protected PgDatabase loadInternal(SubMonitor monitor) {
        monitor.subTask(Messages.dbSource_loading_tree);

        return PgDumpLoader.loadDatabaseSchemaFromDirTree(dirTreePath,
                getPgDiffArgs(encoding, ApgdiffConsts.UTC), parser);
    }
}

class DbSourceProject extends DbSource {

    private final ParserClass parser;
    private final PgDbProject proj;

    DbSourceProject(ParserClass parser, PgDbProject proj) {
        super(proj.getPathToProject().toString());

        this.parser = parser;
        this.proj = proj;
    }

    @Override
    protected PgDatabase loadInternal(SubMonitor monitor) throws IOException {
        int filesCount = countFilesInDir(proj.getPathToProject());  
        monitor.subTask(Messages.dbSource_loading_tree);
        monitor.setWorkRemaining(filesCount);
        
        parser.setMonitor(monitor);
        parser.setMonitoringLevel(1);
        
        String charset;
        try {
            charset = proj.getProjectCharset();
        } catch (CoreException e) {
            throw new IOException(e.getLocalizedMessage(), e);
        }
        return PgDumpLoader.loadDatabaseSchemaFromDirTree(
                proj.getPathToProject().toString(), 
                getPgDiffArgs(charset, ApgdiffConsts.UTC), parser);
    }
    
    private int countFilesInDir(Path path) {
        int count = 0;
        File[] filesList = path.toFile().listFiles();
        
        if (filesList != null){
            for (File file : filesList) {
                if (!file.isDirectory()) {
                    count++;
                } else {
                    count += countFilesInDir(file.toPath());
                }
            }
        }
        return count;
    }
}

class DbSourceFile extends DbSource {
    /*
     * Магическая константа AVERAGE_STATEMENT_LENGTH получена эмпирическим путем. 
     * Она равна количеству строк в файле sql, поделенному на количество выражений.
     * 
     * По подсчетам, это число в районе 6. Для верности берем 5.
     */
    final static int AVERAGE_STATEMENT_LENGTH = 5;
    
    private final ParserClass parser;
    private final String filename;
    private final String encoding;

    DbSourceFile(ParserClass parser, String filename, String encoding) {
        super(filename);

        this.parser = parser;
        this.filename = filename;
        this.encoding = encoding;
    }

    @Override
    protected PgDatabase loadInternal(SubMonitor monitor) {
        monitor.subTask(Messages.dbSource_loading_dump);
        
        try {
            int linesCount = countLines(filename);
            monitor.setWorkRemaining(linesCount > AVERAGE_STATEMENT_LENGTH ? 
                    linesCount/AVERAGE_STATEMENT_LENGTH : 1);
        } catch (IOException e) {
            Log.log(Log.LOG_INFO, "Error counting file lines. Setting 1000"); //$NON-NLS-1$
            monitor.setWorkRemaining(1000);
        }
        parser.setMonitor(monitor);
        parser.setMonitoringLevel(2);
        
        return PgDumpLoader.loadDatabaseSchemaFromDump(filename, 
                getPgDiffArgs(encoding, ApgdiffConsts.UTC), parser);
    }
    
    private int countLines(String filename) throws IOException {
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

    private final ParserClass parser;
    private final String exePgdump;
    private final String customParams;

    private final String host, user, pass, dbname, encoding, timezone;
    private final int port;

    DbSourceDb(ParserClass parser, String exePgdump, String customParams,
            PgDbProject props, String password) throws CoreException {
        this(parser, exePgdump, customParams,
                props.getPrefs().get(PROJ_PREF.DB_HOST, ""), //$NON-NLS-1$
                props.getPrefs().getInt(PROJ_PREF.DB_PORT, JDBC_CONSTS.JDBC_DEFAULT_PORT),
                props.getPrefs().get(PROJ_PREF.DB_USER, ""), //$NON-NLS-1$
                password,
                props.getPrefs().get(PROJ_PREF.DB_NAME, ""), //$NON-NLS-1$
                props.getProjectCharset(), 
                props.getPrefs().get(PROJ_PREF.TIMEZONE, ApgdiffConsts.UTC));
    }

    DbSourceDb(ParserClass parser, String exePgdump, String customParams,
            String host, int port, String user, String pass,
            String dbname, String encoding, String timezone) {
        super((dbname.isEmpty() ? Messages.unknown_db : dbname) + "@" //$NON-NLS-1$
                + (host.isEmpty() ? Messages.unknown_host : host));

        this.parser = parser;
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
    protected PgDatabase loadInternal(SubMonitor monitor) throws IOException {
        SubMonitor pm = SubMonitor.convert(monitor, 2);

        try (TempFile tf = new TempFile("tmp_dump_", ".sql")) { //$NON-NLS-1$ //$NON-NLS-2$
            File dump = tf.get();

            pm.newChild(1).subTask(Messages.dbSource_executing_pg_dump);

            new PgDumper(exePgdump, customParams,
                    host, port, user, pass, dbname, encoding, timezone, 
                    dump.getAbsolutePath()).pgDump();

            pm.newChild(1).subTask(Messages.dbSource_loading_dump);

            return PgDumpLoader.loadDatabaseSchemaFromDump(
                    dump.getAbsolutePath(), getPgDiffArgs(encoding, timezone), parser);
        }
    }
}

class DbSourceFilter extends DbSource {

    final DbSource src;

    final TreeElement filter;

    final DiffSide side;

    DbSourceFilter(DbSource src, TreeElement filter, DiffSide side) {
        super(MessageFormat.format(Messages.dbSource_filter_on, src.getOrigin()));
        this.src = src;
        this.filter = filter;
        this.side = side;
    }

    @Override
    protected PgDatabase loadInternal(SubMonitor monitor) throws IOException {
        PgDatabase db;
        try {
            db = src.getDbObject();
        } catch (Exception ex) {
            db = src.get(monitor);
        }

        return new PgDbFilter2(db, filter, side).apply();
    }
}

class DbSourceJdbc extends DbSource {

    private JdbcLoader jdbcLoader;
    
    DbSourceJdbc(String host, int port, String user, String pass, String dbName, 
            String encoding, String timezone, boolean useAntrlForViews) {
        super(dbName);
        jdbcLoader = new JdbcLoader(
                new JdbcConnector(host, port, user, pass, dbName, encoding, timezone),
                useAntrlForViews, getPgDiffArgs(encoding, timezone));
    }
    
    @Override
    protected PgDatabase loadInternal(SubMonitor monitor) throws IOException {
        monitor.subTask(Messages.reading_db_from_jdbc);
        return jdbcLoader.getDbFromJdbc(monitor);
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
