package cz.startnet.utils.pgdiff.loader.jdbc;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;

import cz.startnet.utils.pgdiff.PgDiffUtils;
import cz.startnet.utils.pgdiff.loader.JdbcQueries;
import cz.startnet.utils.pgdiff.loader.SupportedVersion;
import cz.startnet.utils.pgdiff.parsers.antlr.statements.CreateTrigger;
import cz.startnet.utils.pgdiff.schema.AbstractSchema;
import cz.startnet.utils.pgdiff.schema.AbstractTrigger;
import cz.startnet.utils.pgdiff.schema.GenericColumn;
import cz.startnet.utils.pgdiff.schema.PgStatementContainer;
import cz.startnet.utils.pgdiff.schema.PgTrigger;
import cz.startnet.utils.pgdiff.schema.PgTrigger.TgTypes;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;

public class TriggersReader extends JdbcReader {

    // SONAR-OFF
    // pg_trigger.h
    private static final int TRIGGER_TYPE_ROW       = 1 << 0;
    private static final int TRIGGER_TYPE_BEFORE    = 1 << 1;
    private static final int TRIGGER_TYPE_INSERT    = 1 << 2;
    private static final int TRIGGER_TYPE_DELETE    = 1 << 3;
    private static final int TRIGGER_TYPE_UPDATE    = 1 << 4;
    private static final int TRIGGER_TYPE_TRUNCATE  = 1 << 5;
    private static final int TRIGGER_TYPE_INSTEAD   = 1 << 6;
    // SONAR-ON

    public TriggersReader(JdbcLoaderBase loader) {
        super(JdbcQueries.QUERY_TRIGGERS, loader);
    }

    @Override
    protected void processResult(ResultSet result, AbstractSchema schema) throws SQLException {
        String contName = result.getString(CLASS_RELNAME);
        PgStatementContainer c = schema.getStatementContainer(contName);
        if (c != null) {
            c.addTrigger(getTrigger(result, schema, contName));
        }
    }

    private AbstractTrigger getTrigger(ResultSet res, AbstractSchema schema, String tableName) throws SQLException {
        String schemaName = schema.getName();
        String triggerName = res.getString("tgname");
        loader.setCurrentObject(new GenericColumn(schemaName, tableName, triggerName, DbObjType.TRIGGER));
        PgTrigger t = new PgTrigger(triggerName);

        int firingConditions = res.getInt("tgtype");
        if ((firingConditions & TRIGGER_TYPE_DELETE) != 0) {
            t.setOnDelete(true);
        }
        if ((firingConditions & TRIGGER_TYPE_INSERT) != 0) {
            t.setOnInsert(true);
        }
        if ((firingConditions & TRIGGER_TYPE_UPDATE) != 0) {
            t.setOnUpdate(true);
        }
        if ((firingConditions & TRIGGER_TYPE_TRUNCATE) != 0) {
            t.setOnTruncate(true);
        }
        if ((firingConditions & TRIGGER_TYPE_ROW) != 0) {
            t.setForEachRow(true);
        }
        if ((firingConditions & TRIGGER_TYPE_BEFORE) != 0) {
            t.setType(TgTypes.BEFORE);
        } else if ((firingConditions & TRIGGER_TYPE_INSTEAD) != 0) {
            t.setType(TgTypes.INSTEAD_OF);
        } else {
            t.setType(TgTypes.AFTER);
        }

        String funcName = res.getString("proname");
        String funcSchema = res.getString(NAMESPACE_NSPNAME);

        StringBuilder functionCall = new StringBuilder(funcName.length() + 2);
        functionCall.append(PgDiffUtils.getQuotedName(funcSchema)).append('.')
        .append(PgDiffUtils.getQuotedName(funcName)).append('(');

        byte[] args = res.getBytes("tgargs");
        if (args.length > 0) {
            functionCall.append('\'');
            int start = 0;
            for (int i = 0; i < args.length; ++i) {
                if (args[i] != 0) {
                    continue;
                }

                functionCall.append(new String(args, start, i - start, StandardCharsets.UTF_8));
                if (i != args.length - 1) {
                    functionCall.append("', '");
                }
                start = i + 1;
            }
            functionCall.append('\'');
        }
        functionCall.append(')');
        t.setFunction(functionCall.toString());

        t.addDep(new GenericColumn(funcSchema, funcName + "()", DbObjType.FUNCTION));

        if (res.getLong("tgconstraint") != 0) {
            t.setConstraint(true);

            String refRelName = res.getString("refrelname");
            if (refRelName != null) {
                String refSchemaName = res.getString("refnspname");
                StringBuilder sb = new StringBuilder();
                sb.append(PgDiffUtils.getQuotedName(refSchemaName)).append('.');
                sb.append(PgDiffUtils.getQuotedName(refRelName));

                t.setRefTableName(sb.toString());
                t.addDep(new GenericColumn(refSchemaName, refRelName, DbObjType.TABLE));
            }

            // before PostgreSQL 9.5
            if (res.getBoolean("tgdeferrable")) {
                t.setImmediate(!res.getBoolean("tginitdeferred"));
            }
        }

        //after Postgresql 10
        if (SupportedVersion.VERSION_10.isLE(loader.version)) {
            t.setOldTable(res.getString("tgoldtable"));
            t.setNewTable(res.getString("tgnewtable"));
        }

        String[] arrCols = getColArray(res, "cols");
        if (arrCols != null) {
            for (String col_name : arrCols) {
                t.addUpdateColumn(col_name);
                t.addDep(new GenericColumn(schemaName, tableName, col_name, DbObjType.COLUMN));
            }
        }

        String definition = res.getString("definition");
        checkObjectValidity(definition, DbObjType.TRIGGER, triggerName);
        loader.submitAntlrTask(definition, p -> p.sql().statement(0).schema_statement()
                .schema_create().create_trigger_statement().when_trigger(),
                ctx -> CreateTrigger.parseWhen(ctx, t, schema.getDatabase(), loader.getCurrentLocation()));

        loader.setAuthor(t, res);

        // COMMENT
        String comment = res.getString("comment");
        if (comment != null && !comment.isEmpty()) {
            t.setComment(loader.args, PgDiffUtils.quoteString(comment));
        }
        return t;
    }
}
