package cz.startnet.utils.pgdiff.loader;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;
import java.text.MessageFormat;

import org.eclipse.core.runtime.SubMonitor;

import cz.startnet.utils.pgdiff.PgDiffArguments;
import cz.startnet.utils.pgdiff.loader.jdbc.JdbcLoaderBase;
import cz.startnet.utils.pgdiff.loader.jdbc.MsAssembliesReader;
import cz.startnet.utils.pgdiff.loader.jdbc.MsCheckConstraintsReader;
import cz.startnet.utils.pgdiff.loader.jdbc.MsExtendedObjectsReader;
import cz.startnet.utils.pgdiff.loader.jdbc.MsFKReader;
import cz.startnet.utils.pgdiff.loader.jdbc.MsFPVTReader;
import cz.startnet.utils.pgdiff.loader.jdbc.MsIndicesAndPKReader;
import cz.startnet.utils.pgdiff.loader.jdbc.MsRolesReader;
import cz.startnet.utils.pgdiff.loader.jdbc.MsSequencesReader;
import cz.startnet.utils.pgdiff.loader.jdbc.MsTablesReader;
import cz.startnet.utils.pgdiff.loader.jdbc.MsTypesReader;
import cz.startnet.utils.pgdiff.loader.jdbc.MsUsersReader;
import cz.startnet.utils.pgdiff.loader.jdbc.SchemasMsReader;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import ru.taximaxim.codekeeper.apgdiff.localizations.Messages;
import ru.taximaxim.codekeeper.apgdiff.log.Log;

public class JdbcMsLoader extends JdbcLoaderBase {

    public JdbcMsLoader(JdbcConnector connector, PgDiffArguments args) {
        this(connector, args, SubMonitor.convert(null));
    }

    public JdbcMsLoader(JdbcConnector connector, PgDiffArguments args,
            SubMonitor monitor) {
        super(connector, monitor, args);
    }

    @Override
    public PgDatabase load() throws IOException, InterruptedException {
        PgDatabase d = new PgDatabase(args);

        Log.log(Log.LOG_INFO, "Reading db using JDBC.");
        setCurrentOperation("connection setup");
        try (Connection connection = connector.getConnection();
                Statement statement = connection.createStatement()) {
            this.connection = connection;
            this.statement = statement;

            connection.setAutoCommit(false);
            // TODO maybe not needed and/or may cause extra locking (compared to PG)
            // may need to be removed, Source Control seems to work in default READ COMMITTED state
            runner.run(statement, "SET TRANSACTION ISOLATION LEVEL REPEATABLE READ");

            // TODO add role cache if needed to process permissions, or remove this
            //queryRoles();
            // TODO add counting objects later
            //setupMonitorWork();

            new SchemasMsReader(this, d).read();
            new MsFPVTReader(this).read();
            new MsExtendedObjectsReader(this).read();
            new MsTablesReader(this).read();
            new MsSequencesReader(this).read();
            new MsIndicesAndPKReader(this).read();
            new MsFKReader(this).read();
            new MsCheckConstraintsReader(this).read();
            new MsTypesReader(this).read();
            new MsAssembliesReader(this, d).read();
            new MsRolesReader(this, d).read();
            new MsUsersReader(this, d).read();

            finishLoaders();

            connection.commit();

            Log.log(Log.LOG_INFO, "Database object has been successfully queried from JDBC");
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception e) {
            // connection is closed at this point
            throw new IOException(MessageFormat.format(Messages.Connection_DatabaseJdbcAccessError,
                    e.getLocalizedMessage(), getCurrentLocation()), e);
        }
        return d;
    }
}
