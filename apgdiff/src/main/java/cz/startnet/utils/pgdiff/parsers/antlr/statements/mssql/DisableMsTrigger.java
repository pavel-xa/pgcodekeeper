package cz.startnet.utils.pgdiff.parsers.antlr.statements.mssql;

import java.util.Arrays;

import org.antlr.v4.runtime.ParserRuleContext;

import cz.startnet.utils.pgdiff.parsers.antlr.TSQLParser.Enable_disable_triggerContext;
import cz.startnet.utils.pgdiff.parsers.antlr.TSQLParser.IdContext;
import cz.startnet.utils.pgdiff.parsers.antlr.TSQLParser.Names_referencesContext;
import cz.startnet.utils.pgdiff.parsers.antlr.TSQLParser.Qualified_nameContext;
import cz.startnet.utils.pgdiff.parsers.antlr.statements.ParserAbstract;
import cz.startnet.utils.pgdiff.schema.AbstractSchema;
import cz.startnet.utils.pgdiff.schema.GenericColumn;
import cz.startnet.utils.pgdiff.schema.IStatementContainer;
import cz.startnet.utils.pgdiff.schema.MsTrigger;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgObjLocation;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;
import ru.taximaxim.codekeeper.apgdiff.utils.Pair;

public class DisableMsTrigger extends ParserAbstract {

    private final Enable_disable_triggerContext ctx;

    public DisableMsTrigger(Enable_disable_triggerContext ctx, PgDatabase db) {
        super(db);
        this.ctx = ctx;
    }

    @Override
    public void parseObject() {
        Names_referencesContext triggers = ctx.names_references();
        Qualified_nameContext parent = ctx.qualified_name();
        if (triggers == null || parent == null) {
            return;
        }

        IdContext schemaCtx = parent.schema;
        IStatementContainer cont = getSafe(AbstractSchema::getStatementContainer,
                getSchemaSafe(Arrays.asList(schemaCtx, parent.name)), parent.name);
        addObjReference(Arrays.asList(parent.schema, parent.name),
                DbObjType.TABLE, null);

        for (Qualified_nameContext qname : triggers.qualified_name()) {
            MsTrigger trig = (MsTrigger) getSafe(IStatementContainer::getTrigger,
                    cont, qname.name);
            addObjReference(Arrays.asList(schemaCtx, parent.name, qname.name),
                    DbObjType.TRIGGER, ACTION_ALTER);
            if (ctx.DISABLE() != null) {
                doSafe(MsTrigger::setDisable, trig, true);
            }
        }
    }

    @Override
    protected PgObjLocation fillQueryLocation(ParserRuleContext ctx) {
        StringBuilder sb = new StringBuilder();
        Enable_disable_triggerContext ctxEnableDisableTr = (Enable_disable_triggerContext) ctx;
        sb.append(ctxEnableDisableTr.DISABLE() != null ? "DISABLE " : "ENABLE ")
        .append("TRIGGER ");


        Names_referencesContext triggers = ctxEnableDisableTr.names_references();
        Qualified_nameContext parent = ctxEnableDisableTr.qualified_name();
        if (triggers == null || parent == null) {
            PgObjLocation loc = new PgObjLocation(sb.toString(), ctx, getFullCtxText(ctx));
            db.addToQueries(fileName, loc);
            return loc;
        }

        IdContext schemaCtx = parent.schema;

        for (Qualified_nameContext qname : triggers.qualified_name()) {
            sb.append(schemaCtx.getText())
            .append('.').append(parent.name.getText())
            .append('.').append(qname.name.getText())
            .append(", ");

        }
        sb.setLength(sb.length() - 2);

        PgObjLocation loc = new PgObjLocation(sb.toString(), ctx, getFullCtxText(ctx));
        db.addToQueries(fileName, loc);
        return loc;
    }

    @Override
    protected Pair<String, GenericColumn> getActionAndObjForStmtAction() {
        return null;
    }
}
