package cz.startnet.utils.pgdiff.parsers.antlr.statements;

import java.util.List;

import cz.startnet.utils.pgdiff.parsers.antlr.AntlrError;
import cz.startnet.utils.pgdiff.parsers.antlr.QNameParser;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Create_sequence_statementContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.IdentifierContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Sequence_bodyContext;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgSequence;
import cz.startnet.utils.pgdiff.schema.PgStatement;

public class CreateSequence extends ParserAbstract {
    private final Create_sequence_statementContext ctx;
    public CreateSequence(Create_sequence_statementContext ctx, PgDatabase db, List<AntlrError> errors) {
        super(db, errors);
        this.ctx = ctx;
    }

    @Override
    public PgStatement getObject() {
        List<IdentifierContext> ids = ctx.name.identifier();
        String name = QNameParser.getFirstName(ids);
        String schemaName = QNameParser.getSchemaName(ids, getDefSchemaName());
        PgSequence sequence = new PgSequence(name, getFullCtxText(ctx.getParent()));
        long inc = 1;
        Long maxValue = null, minValue = null;
        for (Sequence_bodyContext body : ctx.sequence_body()) {
            if (body.cache_val != null) {
                sequence.setCache(body.cache_val.getText());
            }
            if (body.incr!=null) {
                inc = Long.parseLong(body.incr.getText());
            }
            if (body.maxval!=null) {
                maxValue = Long.parseLong(body.maxval.getText());
            }
            if (body.minval!=null) {
                minValue = Long.parseLong(body.minval.getText());
            }
            if (body.start_val!=null) {
                sequence.setStartWith(body.start_val.getText());
            }
            if (body.cycle_val!=null) {
                sequence.setCycle(body.cycle_true==null);
            }
            if (body.col_name!=null) {
                // TODO incorrect qualified name work
                // also broken in altersequence
                sequence.setOwnedBy(body.col_name.getText());
            }
        }
        sequence.setMinMaxInc(inc, maxValue, minValue);

        if (db.getSchema(schemaName) == null) {
            logSkipedObject(schemaName, "SEQUENCE", name, ctx.getStart());
            return null;
        }
        db.getSchema(schemaName).addSequence(sequence);
        return sequence;
    }

}
