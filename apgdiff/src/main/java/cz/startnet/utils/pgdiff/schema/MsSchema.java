package cz.startnet.utils.pgdiff.schema;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import cz.startnet.utils.pgdiff.MsDiffUtils;

/**
 * MS SQL schema code generation.
 */
public class MsSchema extends AbstractSchema {

    public MsSchema(String name) {
        super(name);
    }

    @Override
    public String getCreationSQL() {
        final StringBuilder sbSQL = new StringBuilder();
        sbSQL.append("CREATE SCHEMA ");
        sbSQL.append(MsDiffUtils.quoteName(getName()));
        if (owner != null) {
            sbSQL.append("\nAUTHORIZATION ").append(MsDiffUtils.quoteName(owner));
        }
        sbSQL.append(GO);
        appendPrivileges(sbSQL);

        return sbSQL.toString();
    }

    @Override
    public boolean appendAlterSQL(PgStatement newCondition, StringBuilder sb,
            AtomicBoolean isNeedDepcies) {
        final int startLength = sb.length();
        if (!Objects.equals(getOwner(), newCondition.getOwner())) {
            newCondition.alterOwnerSQL(sb);
        }
        alterPrivileges(newCondition, sb);
        return sb.length() > startLength;
    }

    @Override
    public String getDropSQL() {
        return "DROP SCHEMA " + MsDiffUtils.quoteName(getName()) + GO;
    }

    @Override
    public boolean isPostgres() {
        return false;
    }

    @Override
    protected AbstractSchema getSchemaCopy() {
        return new MsSchema(getName());
    }
}
