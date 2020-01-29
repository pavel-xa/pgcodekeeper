package cz.startnet.utils.pgdiff.loader;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.SubMonitor;

import cz.startnet.utils.pgdiff.PgDiffArguments;
import cz.startnet.utils.pgdiff.PgDiffUtils;
import cz.startnet.utils.pgdiff.loader.jdbc.ConstraintsReader;
import cz.startnet.utils.pgdiff.loader.jdbc.ExtensionsReader;
import cz.startnet.utils.pgdiff.loader.jdbc.FtsConfigurationsReader;
import cz.startnet.utils.pgdiff.loader.jdbc.FtsDictionariesReader;
import cz.startnet.utils.pgdiff.loader.jdbc.FtsParsersReader;
import cz.startnet.utils.pgdiff.loader.jdbc.FtsTemplatesReader;
import cz.startnet.utils.pgdiff.loader.jdbc.FunctionsReader;
import cz.startnet.utils.pgdiff.loader.jdbc.IndicesReader;
import cz.startnet.utils.pgdiff.loader.jdbc.JdbcLoaderBase;
import cz.startnet.utils.pgdiff.loader.jdbc.OperatorsReader;
import cz.startnet.utils.pgdiff.loader.jdbc.RulesReader;
import cz.startnet.utils.pgdiff.loader.jdbc.SchemasReader;
import cz.startnet.utils.pgdiff.loader.jdbc.SequencesReader;
import cz.startnet.utils.pgdiff.loader.jdbc.TablesReader;
import cz.startnet.utils.pgdiff.loader.jdbc.TriggersReader;
import cz.startnet.utils.pgdiff.loader.jdbc.TypesReader;
import cz.startnet.utils.pgdiff.loader.jdbc.ViewsReader;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import ru.taximaxim.codekeeper.apgdiff.localizations.Messages;
import ru.taximaxim.codekeeper.apgdiff.log.Log;

public class JdbcLoader extends JdbcLoaderBase {

    public JdbcLoader(JdbcConnector connector, PgDiffArguments pgDiffArguments) {
        this(connector, pgDiffArguments, SubMonitor.convert(null));
    }

    public JdbcLoader(JdbcConnector connector, PgDiffArguments pgDiffArguments,
            SubMonitor monitor) {
        super(connector, monitor, pgDiffArguments);
    }

    public List<Object> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public PgDatabase getDbFromJdbc() throws IOException, InterruptedException {
        PgDatabase d = getDbFromJdbc(new PgDatabase(args));
        FullAnalyze.fullAnalyze(d, errors);
        return d;
    }

    public PgDatabase getDbFromJdbc(PgDatabase d) throws IOException, InterruptedException {
        Log.log(Log.LOG_INFO, "Reading db using JDBC.");
        setCurrentOperation("connection setup");
        try (Connection connection = connector.getConnection();
                Statement statement = connection.createStatement()) {
            this.connection = connection;
            this.statement = statement;
            connection.setAutoCommit(false);
            runner.run(statement, "SET TRANSACTION ISOLATION LEVEL REPEATABLE READ, READ ONLY");
            runner.run(statement, "SET search_path TO pg_catalog;");
            runner.run(statement, "SET timezone = " + PgDiffUtils.quoteString(connector.getTimezone()));

            queryCheckVersion();
            queryCheckLastSysOid();
            queryTypesForCache();
            queryRoles();
            queryCheckExtension();
            setupMonitorWork();

            new SchemasReader(this, d).read();

            // NOTE: order of readers has been changed to move the heaviest ANTLR tasks to the beginning
            // to give them a chance to finish while JDBC processes other non-ANTLR stuff
            new ViewsReader(this).read();
            new TablesReader(this).read();
            new RulesReader(this).read();
            new TriggersReader(this).read();
            new IndicesReader(this).read();
            // Reads FUNCTIONs, PROCEDUREs and AGGREGATEs from JDBC.
            new FunctionsReader(this).read();
            // non-ANTLR tasks
            new ConstraintsReader(this).read();
            new TypesReader(this).read();
            new SequencesReader(this).read();
            new FtsParsersReader(this).read();
            new FtsTemplatesReader(this).read();
            new FtsDictionariesReader(this).read();
            if (SupportedVersion.VERSION_9_3.isLE(version)) {
                new FtsConfigurationsReader(this).read();
            }
            new OperatorsReader(this).read();

            new ExtensionsReader(this, d).read();

            if (!SupportedVersion.VERSION_10.isLE(version)) {
                SequencesReader.querySequencesData(d, this);
            }

            connection.commit();
            finishAntlr();

            d.sortColumns();

            d.setPostgresVersion(SupportedVersion.valueOf(version));
            Log.log(Log.LOG_INFO, "Database object has been successfully queried from JDBC");
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception e) {
            // connection is closed at this point, trust Postgres to rollback it; we're a read-only xact anyway
            throw new IOException(MessageFormat.format(Messages.Connection_DatabaseJdbcAccessError,
                    e.getLocalizedMessage(), getCurrentLocation()), e);
        }
        return d;
    }
}