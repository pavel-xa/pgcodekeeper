package cz.startnet.utils.pgdiff.parsers.antlr.expr;

import java.util.Collections;
import java.util.List;

import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.From_itemContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Table_subqueryContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Update_setContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Update_stmt_for_psqlContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.VexContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.With_clauseContext;
import cz.startnet.utils.pgdiff.parsers.antlr.rulectx.Vex;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;
import ru.taximaxim.codekeeper.apgdiff.utils.ModPair;

public class Update extends AbstractExprWithNmspc<Update_stmt_for_psqlContext> {

    protected Update(AbstractExpr parent) {
        super(parent);
    }

    public Update(PgDatabase db, DbObjType... disabledDepcies) {
        super(db, disabledDepcies);
    }

    @Override
    public List<ModPair<String, String>> analyze(Update_stmt_for_psqlContext update) {
        With_clauseContext with = update.with_clause();
        if (with != null) {
            analyzeCte(with);
        }

        addNameReference(update.update_table_name, update.alias, null);

        if (update.FROM() != null) {
            for (From_itemContext fromItem : update.from_item()) {
                //TODO collect to current namespace
                new Select(this).from(fromItem);
            }
        }

        for (Update_setContext updateSet : update.update_set()) {
            addColumnsDepcies(update.update_table_name, updateSet.column);

            Table_subqueryContext subQuery = updateSet.table_subquery();
            if (subQuery != null) {
                new Select(this).analyze(subQuery.select_stmt());
            } else if (!updateSet.value.isEmpty()) {
                ValueExpr vex = new ValueExpr(this);
                for (VexContext vexCtx : updateSet.value) {
                    vex.analyze(new Vex(vexCtx));
                }
            }
        }

        if (update.WHERE() != null) {
            VexContext vex = update.vex();
            if (vex != null) {
                new ValueExpr(this).analyze(new Vex(vex));
            }
        }

        return Collections.emptyList();
    }
}
