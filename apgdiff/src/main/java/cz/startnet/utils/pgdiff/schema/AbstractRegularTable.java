package cz.startnet.utils.pgdiff.schema;

import java.util.Map.Entry;
import java.util.Objects;

import cz.startnet.utils.pgdiff.hashers.Hasher;
import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts;

/**
 * Base implementation of regular table
 *
 * @since 4.1.1
 * @author galiev_mr
 *
 */
public abstract class AbstractRegularTable extends AbstractPgTable {

    protected boolean isLogged = true;
    protected String tablespace;
    protected boolean isRowSecurity;
    protected boolean isForceSecurity;
    protected String partitionBy;

    public AbstractRegularTable(String name) {
        super(name);
    }

    @Override
    protected void appendName(StringBuilder sbSQL) {
        sbSQL.append("CREATE ");
        if (!isLogged()) {
            sbSQL.append("UNLOGGED ");
        }
        sbSQL.append("TABLE ")
        .append("IF NOT EXISTS ")		//P.Smirnov
        .append(getQualifiedName());
        
    }

    @Override
    protected String getAlterTable(boolean nextLine, boolean only) {
        StringBuilder sb = new StringBuilder();
        if (nextLine) {
            sb.append("\n\n");
        }
        sb.append("ALTER TABLE ");
        sb.append("IF EXISTS ");		 //P.Smirnov
        if (only) {
            sb.append("ONLY ");
        }
        sb.append(getQualifiedName());
        return sb.toString();
    }

    @Override
    protected void appendOptions(StringBuilder sbSQL) {
        StringBuilder sb = new StringBuilder();

        if (partitionBy != null) {
            sbSQL.append("\nPARTITION BY ");
            sbSQL.append(partitionBy);
        }

        for (Entry <String, String> entry : options.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            sb.append(key);
            if (!value.isEmpty()) {
                sb.append('=').append(value);
            }
            sb.append(", ");
        }

        if (hasOids) {
            sb.append("OIDS=").append(hasOids).append(", ");
        }

        if (sb.length() > 0){
            sb.setLength(sb.length() - 2);
            sbSQL.append("\nWITH (").append(sb).append(")");
        }

        if (tablespace != null && !tablespace.isEmpty()) {
            sbSQL.append("\nTABLESPACE ");
            sbSQL.append(tablespace);
        }
        sbSQL.append(';');
    }

    @Override
    protected void appendAlterOptions(StringBuilder sbSQL) {
        // since 9.5 PostgreSQL
        if (isRowSecurity) {
            sbSQL.append(getAlterTable(true, false));
            sbSQL.append(" ENABLE ROW LEVEL SECURITY;");
        }

        // since 9.5 PostgreSQL
        if (isForceSecurity) {
            sbSQL.append(getAlterTable(true, true));
            sbSQL.append(" FORCE ROW LEVEL SECURITY;");
        }
    }

    @Override
    protected void compareTableOptions(AbstractPgTable newTable, StringBuilder sb) {
        super.compareTableOptions(newTable, sb);

        AbstractRegularTable newRegTable = (AbstractRegularTable) newTable;
        if (!Objects.equals(tablespace, newRegTable.getTablespace())) {
            sb.append(getAlterTable(true, false))
            .append("\n\tSET TABLESPACE ");

            String newSpace = newRegTable.getTablespace();
            sb.append(newSpace == null ? ApgdiffConsts.PG_DEFAULT : newSpace).append(';');
        }

        // since 9.5 PostgreSQL
        if (isLogged != newRegTable.isLogged) {
            sb.append(getAlterTable(true, false))
            .append("\n\tSET ")
            .append(newRegTable.isLogged ? "LOGGED" : "UNLOGGED")
            .append(';');
        }

        // since 9.5 PostgreSQL
        if (isRowSecurity != newRegTable.isRowSecurity) {
            sb.append(getAlterTable(true, false))
            .append(newRegTable.isRowSecurity ? " ENABLE" : " DISABLE")
            .append(" ROW LEVEL SECURITY;");
        }

        // since 9.5 PostgreSQL
        if (isForceSecurity != newRegTable.isForceSecurity) {
            sb.append(getAlterTable(true, true))
            .append(newRegTable.isForceSecurity ? "" : " NO")
            .append(" FORCE ROW LEVEL SECURITY;");
        }
    }

    protected abstract void convertTable(StringBuilder sb);

    @Override
    protected boolean isNeedRecreate(AbstractTable newTable) {
        return  !(newTable instanceof AbstractRegularTable) ||
                !Objects.equals(getPartitionBy(),
                        ((AbstractRegularTable)newTable).getPartitionBy());
    }

    public boolean isLogged() {
        return isLogged;
    }

    public void setLogged(boolean isLogged) {
        this.isLogged = isLogged;
        resetHash();
    }

    public String getTablespace() {
        return tablespace;
    }

    public void setTablespace(final String tablespace) {
        this.tablespace = tablespace;
        resetHash();
    }

    public boolean isRowSecurity() {
        return isRowSecurity;
    }

    public void setRowSecurity(final boolean isRowSecurity) {
        this.isRowSecurity = isRowSecurity;
        resetHash();
    }

    public boolean isForceSecurity() {
        return isForceSecurity;
    }

    public void setForceSecurity(final boolean isForceSecurity) {
        this.isForceSecurity = isForceSecurity;
        resetHash();
    }

    public String getPartitionBy() {
        return partitionBy;
    }

    public void setPartitionBy(final String partionBy) {
        this.partitionBy = partionBy;
        resetHash();
    }

    @Override
    public boolean compare(PgStatement obj) {
        if (obj instanceof AbstractRegularTable && super.compare(obj)) {
            AbstractRegularTable table = (AbstractRegularTable) obj;
            return Objects.equals(tablespace, table.getTablespace())
                    && isLogged == table.isLogged()
                    && isRowSecurity == table.isRowSecurity()
                    && isForceSecurity == table.isForceSecurity()
                    && Objects.equals(partitionBy, table.getPartitionBy());
        }

        return false;
    }

    @Override
    public AbstractTable shallowCopy() {
        AbstractRegularTable copy = (AbstractRegularTable) super.shallowCopy();
        copy.setLogged(isLogged());
        copy.setTablespace(getTablespace());
        copy.setRowSecurity(isRowSecurity());
        copy.setForceSecurity(isForceSecurity());
        copy.setPartitionBy(getPartitionBy());
        return copy;
    }

    @Override
    public void computeHash(Hasher hasher) {
        super.computeHash(hasher);
        hasher.put(isLogged);
        hasher.put(tablespace);
        hasher.put(isRowSecurity);
        hasher.put(isForceSecurity);
        hasher.put(partitionBy);
    }
}
