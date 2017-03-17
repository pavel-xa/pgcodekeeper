package cz.startnet.utils.pgdiff.parsers.antlr.statements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.startnet.utils.pgdiff.parsers.antlr.AntlrError;
import cz.startnet.utils.pgdiff.parsers.antlr.QNameParser;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Body_rulesContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Function_parametersContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.IdentifierContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Rule_commonContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Schema_qualified_nameContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Table_column_privilegesContext;
import cz.startnet.utils.pgdiff.schema.PgColumn;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgFunction;
import cz.startnet.utils.pgdiff.schema.PgPrivilege;
import cz.startnet.utils.pgdiff.schema.PgStatement;
import cz.startnet.utils.pgdiff.schema.PgTable;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;

public class CreateRule extends ParserAbstract {
    private final Rule_commonContext ctx;
    private final boolean revoke;

    public CreateRule(Rule_commonContext ctx, PgDatabase db, List<AntlrError> errors) {
        super(db, errors);
        this.ctx = ctx;
        revoke = ctx.REVOKE() != null;
    }

    @Override
    public PgStatement getObject() {
        if (db.getArguments().isIgnorePrivileges()) {
            return null;
        }
        String col_rule = "";
        DbObjType type = null;
        List<Schema_qualified_nameContext> obj_name = new ArrayList<>();
        if (ctx.body_rule.body_rules_rest().obj_name != null) {
            obj_name = ctx.body_rule.body_rules_rest().obj_name.name;
        } else if (ctx.body_rule.on_table() != null) {
            parseColumns(ctx.body_rule);
        } else if (ctx.body_rule.on_sequence() != null) {
            type = DbObjType.SEQUENCE;
            obj_name = ctx.body_rule.on_sequence().obj_name.name;
        } else if (ctx.body_rule.on_database() != null) {
            type = DbObjType.DATABASE;
            obj_name = ctx.body_rule.on_database().obj_name.name;
        } else if (ctx.body_rule.on_datawrapper_server_lang() != null) {
            obj_name = ctx.body_rule.on_datawrapper_server_lang().obj_name.name;
        } else if (ctx.body_rule.on_function() != null) {
            type = DbObjType.FUNCTION;
            for (Function_parametersContext functparam : ctx.body_rule.on_function().obj_name) {
                PgFunction func = new PgFunction(QNameParser.getFirstName(functparam.name.identifier()), null);
                fillArguments(functparam.function_args(), func, getDefSchemaName());
                db.getSchema(getDefSchemaName())
                .getFunction(func.getSignature())
                .addPrivilege(
                        new PgPrivilege(revoke,
                                getFullCtxText(ctx.body_rule),
                                getFullCtxText(ctx)));
            }
        } else if (ctx.body_rule.on_large_object() != null) {
            obj_name = ctx.body_rule.on_large_object().obj_name.name;
        } else if (ctx.body_rule.on_schema() != null) {
            type = DbObjType.SCHEMA;
            obj_name = ctx.body_rule.on_schema().obj_name.name;
        } else if (ctx.body_rule.on_tablespace() != null) {
            obj_name = ctx.body_rule.on_tablespace().obj_name.name;
        } else if (ctx.body_rule.on_type() != null) {
            type = DbObjType.TYPE;
            obj_name = ctx.body_rule.on_type().obj_name.name;
        } else if (ctx.body_rule.on_domain() != null) {
            type = DbObjType.DOMAIN;
            obj_name = ctx.body_rule.on_domain().obj_name.name;
        }


        for (Schema_qualified_nameContext name : obj_name) {
            addToDB(name, type, new PgPrivilege(revoke,
                    col_rule.isEmpty() ? getFullCtxText(ctx.body_rule) : col_rule, getFullCtxText(ctx)));
        }

        return null;
    }

    /**
     * Вычитывает из контекста привилегию, и применяет её к таблице, сиквенсу,
     * view, колонкам в таблице
     * @param ctx_body
     */
    private void parseColumns(Body_rulesContext ctx_body) {
        List<String> tbl_priv = new ArrayList<>();
        // собрать информацию о привилегиях на колонки
        Map<String, List<String>> colPriv = new HashMap<>();
        for (Table_column_privilegesContext priv : ctx_body.on_table().priv_tbl_col) {
            String privName = getFullCtxText(priv.table_column_privilege());
            // это привилегия на таблицу
            if (priv.column.isEmpty()) {
                tbl_priv.add(privName);
                continue;
            }
            for (IdentifierContext col : priv.column) {
                String colName = getFullCtxText(col);
                List<String> privList = colPriv.get(colName);
                if (privList == null) {
                    privList = new ArrayList<>();
                    colPriv.put(colName, privList);
                }
                privList.add(privName);
            }
        }
        // заполнить привилегии на объекты таблицу/сиквенс/вью
        StringBuilder tblPrivilege = new StringBuilder();
        for (String priv : tbl_priv) {
            tblPrivilege.append(priv).append(",");
        }
        if (tblPrivilege.length() > 0) {
            tblPrivilege.setLength(tblPrivilege.length() - 1);
            tblPrivilege.append(" ON TABLE ");
        }
        // Разобрать объекты
        for (Schema_qualified_nameContext tbl : ctx_body.on_table().obj_name.name) {
            String tableName = getFullCtxText(tbl);
            List<IdentifierContext> ids = tbl.identifier();
            String firstPart = QNameParser.getFirstName(ids);
            String schemaName = QNameParser.getSchemaName(ids, getDefSchemaName());
            //привилегии пишем так как получили одной строкой
            PgTable tblSt = db.getSchema(schemaName).getTable(firstPart);
            // если таблица не найдена попробовать вьюхи и проч. общим методом
            if (tblSt == null) {
                addToDB(tbl, DbObjType.TABLE, new PgPrivilege(
                        revoke, getFullCtxText(ctx.body_rule),
                        getFullCtxText(ctx)));
            } else {
                // применить привилегию к текущему объекту таблица
                // здесь рассматривается случай grant SELECT, UPDATE(c2)
                // SELECT добавляется тут ко всему объекту
                if (tblPrivilege.length() > 0) {
                    tblSt.addPrivilege(new PgPrivilege(revoke,
                            tblPrivilege + tableName + ' ' + getFullCtxText(ctx_body.body_rules_rest()),
                            getFullCtxText(ctx)));
                }

                // Если таблица, то поискать в ней колонки и добавить в каждую свою привилегию
                for (PgColumn col : tblSt.getColumns()) {
                    List<String> privList = colPriv.get(col.getName());
                    if (privList != null) {
                        StringBuilder privilege = new StringBuilder();
                        for (String priv : privList) {
                            privilege.append(priv).append('(').append(col.getName()).append("), ");
                        }
                        // Здесь не должно быть пустых привилегий для колонок,
                        // т.к. пустые привилегии ушли в таблицу/вью/сиквенс
                        privilege.setLength(privilege.length() - 2);
                        privilege.append(" ON TABLE ").append(tableName).append(' ');
                        privilege.append(getFullCtxText(ctx_body.body_rules_rest()));

                        col.addPrivilege(new PgPrivilege(revoke,
                                privilege.toString(), getFullCtxText(ctx)));
                    }
                }
            }
        }
    }

    private PgStatement addToDB(Schema_qualified_nameContext name, DbObjType type, PgPrivilege pgPrivilege) {
        if (type == null) {
            return null;
        }
        List<IdentifierContext> ids = name.identifier();
        String firstPart = QNameParser.getFirstName(ids);
        String schemaName = QNameParser.getSchemaName(ids, getDefSchemaName());
        PgStatement statement = null;
        switch (type) {
        case TABLE:
            statement = db.getSchema(schemaName).getTable(firstPart);
            if (statement == null) {
                statement = db.getSchema(schemaName).getView(firstPart);
            }
            if (statement == null) {
                statement = db.getSchema(schemaName).getSequence(firstPart);
            }
            break;
        case SEQUENCE:
            statement = db.getSchema(schemaName).getSequence(firstPart);
            break;
        case DATABASE:
            statement = db;
            break;
        case SCHEMA:
            schemaName = null;
            statement = db.getSchema(firstPart);
            break;
        case TYPE:
            statement = db.getSchema(schemaName).getType(firstPart);
            // if type not found try domain
            if (statement != null) {
                break;
            }
        case DOMAIN:
            statement = db.getSchema(schemaName).getDomain(firstPart);
            break;
        default:
            break;
        }
        if (statement != null) {
            statement.addPrivilege(pgPrivilege);
            return statement;
        }
        return null;
    }
}
