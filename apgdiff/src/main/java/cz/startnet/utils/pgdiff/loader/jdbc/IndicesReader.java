package cz.startnet.utils.pgdiff.loader.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import cz.startnet.utils.pgdiff.PgDiffUtils;
import cz.startnet.utils.pgdiff.loader.JdbcQueries;
import cz.startnet.utils.pgdiff.parsers.antlr.statements.CreateIndex;
import cz.startnet.utils.pgdiff.schema.AbstractIndex;
import cz.startnet.utils.pgdiff.schema.AbstractSchema;
import cz.startnet.utils.pgdiff.schema.GenericColumn;
import cz.startnet.utils.pgdiff.schema.IStatementContainer;
import cz.startnet.utils.pgdiff.schema.PgIndex;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;

public class IndicesReader extends JdbcReader {

    public IndicesReader(JdbcLoaderBase loader) {
        super(JdbcQueries.QUERY_INDICES, loader);
    }

    @Override
    protected void processResult(ResultSet result, AbstractSchema schema) throws SQLException {
        IStatementContainer cont = schema.getStatementContainer(result.getString("table_name"));
        if (cont != null) {
            AbstractIndex index = getIndex(result, schema, cont.getName());
            loader.monitor.worked(1);
            cont.addIndex(index);
        }
    }

    private AbstractIndex getIndex(ResultSet res, AbstractSchema schema, String tableName) throws SQLException {
        String schemaName = schema.getName();
        String indexName = res.getString(CLASS_RELNAME);
        loader.setCurrentObject(new GenericColumn(schemaName, indexName, DbObjType.INDEX));
        PgIndex i = new PgIndex(indexName);

        String tablespace = res.getString("table_space");
        String definition = res.getString("definition");
        checkObjectValidity(definition, DbObjType.INDEX, indexName);
        loader.submitAntlrTask(definition,
                p -> p.sql().statement(0).schema_statement().schema_create()
                .create_index_statement().index_rest(),
                ctx -> CreateIndex.parseIndex(ctx, tablespace, schemaName, tableName, i,
                        schema.getDatabase(), loader.getCurrentLocation()));
        loader.setAuthor(i, res);

        i.setClusterIndex(res.getBoolean("isclustered"));
        i.setUnique(res.getBoolean("indisunique"));

        // COMMENT
        String comment = res.getString("comment");
        if (comment != null && !comment.isEmpty()) {
            i.setComment(loader.args, PgDiffUtils.quoteString(comment));
        }

        String inhnspname = res.getString("inhnspname");

        if (inhnspname != null) {
            String inhrelname = res.getString("inhrelname");
            i.addInherit(inhnspname, inhrelname);
            i.addDep(new GenericColumn(inhnspname, inhrelname, DbObjType.INDEX));
        }

        return i;
    }
}