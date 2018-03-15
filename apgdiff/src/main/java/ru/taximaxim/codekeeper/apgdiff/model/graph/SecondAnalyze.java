package ru.taximaxim.codekeeper.apgdiff.model.graph;

import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.jgrapht.DirectedGraph;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Constr_bodyContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Create_rewrite_statementContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Rewrite_commandContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Select_stmtContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.VexContext;
import cz.startnet.utils.pgdiff.parsers.antlr.expr.UtilAnalyzeExpr;
import cz.startnet.utils.pgdiff.parsers.antlr.expr.ValueExpr;
import cz.startnet.utils.pgdiff.parsers.antlr.expr.secondanalyze.Select;
import cz.startnet.utils.pgdiff.schema.PgConstraint;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgRule;
import cz.startnet.utils.pgdiff.schema.PgStatement;
import cz.startnet.utils.pgdiff.schema.PgStatementWithSearchPath;
import cz.startnet.utils.pgdiff.schema.PgTrigger;
import cz.startnet.utils.pgdiff.schema.PgView;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;

public final class SecondAnalyze {

    public static void goThroughGraphForAnalyze(PgDatabase db) {
        DirectedGraph<PgStatement, DefaultEdge> graph = new DepcyGraph(db, false).getReversedGraph();

        TopologicalOrderIterator<PgStatement, DefaultEdge> orderIterator = new TopologicalOrderIterator<>(graph);

        AnalyzeTraversalListenerAdapter adapter = new AnalyzeTraversalListenerAdapter(db);
        orderIterator.addTraversalListener(adapter);

        // 'VIEW' statements analysis.
        while (orderIterator.hasNext()) {
            orderIterator.next();
        }

        // Analysis of all statements except 'VIEW'.
        analyzeAllStmtsExceptView((Iterable<Entry<PgStatement, ParserRuleContext>>) db.getContextsForAnalyze()
                .stream().filter(e -> !DbObjType.VIEW.equals(e.getKey().getStatementType()))::iterator);
    }

    private static void analyzeAllStmtsExceptView(Iterable<Entry<PgStatement, ParserRuleContext>> allStmtsExceptView) {
        for (Entry<PgStatement, ParserRuleContext> entry : allStmtsExceptView) {
            PgStatement statement = entry.getKey();
            ParserRuleContext ctx = entry.getValue();
            DbObjType statementType = statement.getStatementType();

            String schemaName = null;
            if (statement instanceof PgStatementWithSearchPath) {
                schemaName = ((PgStatementWithSearchPath) statement).getContainingSchema().getName();
            }

            switch (statementType) {
            case RULE:
                Create_rewrite_statementContext createRewriteCtx = (Create_rewrite_statementContext) ctx;
                PgRule rule = (PgRule) statement;

                UtilAnalyzeExpr.analyzeRulesWhere(createRewriteCtx, rule, schemaName);
                for (Rewrite_commandContext cmd : createRewriteCtx.commands) {
                    UtilAnalyzeExpr.analyzeRulesCommand(cmd, rule, schemaName);
                }
                break;
            case TRIGGER:
                UtilAnalyzeExpr.analyzeTriggersWhen((VexContext) ctx,
                        (PgTrigger) statement, schemaName);
                break;
            case CONSTRAINT:
                UtilAnalyzeExpr.analyzeConstraint((Constr_bodyContext) ctx,
                        schemaName, (PgConstraint) statement);
                break;
            case INDEX:
            case DOMAIN:
            case FUNCTION:
            case COLUMN:
                UtilAnalyzeExpr.analyze((VexContext) ctx, new ValueExpr(schemaName), statement);
                break;
            default:
                throw new IllegalStateException("The analyze for the case '"
                        + statementType + ' ' + statement
                        + "' is not defined!"); //$NON-NLS-1$
            }
        }
    }

    private static class AnalyzeTraversalListenerAdapter extends TraversalListenerAdapter<PgStatement, DefaultEdge> {

        private final PgDatabase db;

        AnalyzeTraversalListenerAdapter(PgDatabase db) {
            this.db = db;
        }

        @Override
        public void vertexTraversed(VertexTraversalEvent<PgStatement> e) {
            PgStatement statement = e.getVertex();
            if (DbObjType.VIEW.equals(statement.getStatementType())) {
                String schemaName = statement.getParent().getName();

                List<ParserRuleContext> statementContexts = db.getContextsForAnalyze().stream()
                        .filter(entry -> statement.equals(entry.getKey()))
                        .map(Entry::getValue)
                        .collect(Collectors.toList());

                if (statementContexts.isEmpty()) {
                    return;
                }

                PgView view = (PgView)statement;
                Select select = new Select(schemaName, db);
                for (ParserRuleContext ctx : statementContexts) {
                    if (ctx instanceof Select_stmtContext) {
                        view.addRelationColumns(select.analyze(ctx));
                        view.addAllDeps(select.getDepcies());
                    } else {
                        UtilAnalyzeExpr.analyze((VexContext)ctx, new ValueExpr(schemaName), view);
                    }
                }
            }
        }
    }

    private SecondAnalyze() {
    }
}
