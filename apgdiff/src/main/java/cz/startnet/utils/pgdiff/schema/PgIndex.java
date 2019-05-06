/**
 * Copyright 2006 StartNet s.r.o.
 *
 * Distributed under MIT license
 */
package cz.startnet.utils.pgdiff.schema;

import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import cz.startnet.utils.pgdiff.PgDiffArguments;
import cz.startnet.utils.pgdiff.PgDiffUtils;
import cz.startnet.utils.pgdiff.hashers.Hasher;

public class PgIndex extends AbstractIndex {

    private String method;

    public PgIndex(String name) {
        super(name);
    }

    @Override
    public String getCreationSQL() {
        return getCreationSQL(getName());
    }

    private String getCreationSQL(String name) {
        final StringBuilder sbSQL = new StringBuilder();
        sbSQL.append("CREATE ");

        if (isUnique()) {
            sbSQL.append("UNIQUE ");
        }

        sbSQL.append("INDEX ");
        PgDiffArguments args = getDatabase().getArguments();
        if (args != null && args.isConcurrentlyMode()) {
            sbSQL.append("CONCURRENTLY ");
        }
        sbSQL.append("IF NOT EXISTS ");		// P.Smirnov
        sbSQL.append(PgDiffUtils.getQuotedName(name));
        sbSQL.append(" ON ");

        PgStatement par = getParent();
        if (par instanceof AbstractRegularTable
                && ((AbstractRegularTable) par).getPartitionBy() != null) {
            sbSQL.append("ONLY ");
        }

        sbSQL.append(par.getQualifiedName());
        if (getMethod() != null) {
            sbSQL.append(" USING ").append(PgDiffUtils.getQuotedName(getMethod()));
        }
        sbSQL.append(' ');
        sbSQL.append(getDefinition());

        if (!includes.isEmpty()) {
            sbSQL.append(" INCLUDE (");
            for (String col : includes) {
                sbSQL.append(PgDiffUtils.getQuotedName(col)).append(", ");
            }
            sbSQL.setLength(sbSQL.length() - 2);
            sbSQL.append(')');
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : options.entrySet()) {
            sb.append(entry.getKey());
            if (!entry.getValue().isEmpty()){
                sb.append("=").append(entry.getValue());
            }
            sb.append(", ");
        }

        if (sb.length() > 0){
            sb.setLength(sb.length() - 2);
            sbSQL.append("\nWITH (").append(sb).append(")");
        }

        if (getTableSpace() != null) {
            sbSQL.append("\nTABLESPACE ").append(getTableSpace());
        }
        if (getWhere() != null) {
            sbSQL.append("\nWHERE ").append(getWhere());
        }
        sbSQL.append(';');
        sbSQL.append(getClusterSQL());

        if (comment != null && !comment.isEmpty()) {
            sbSQL.append("\n\n");
            appendCommentSql(sbSQL);
        }

        return sbSQL.toString();
    }

    @Override
    public String getDropSQL() {
        return "DROP INDEX "
        		+ "IF EXISTS " 		// P.Smirnov
        		+ PgDiffUtils.getQuotedName(getSchemaName()) + '.'
                + PgDiffUtils.getQuotedName(getName()) + ";";
    }

    @Override
    public boolean appendAlterSQL(PgStatement newCondition, StringBuilder sb,
            AtomicBoolean isNeedDepcies) {
        final int startLength = sb.length();
        PgIndex newIndex;
        if (newCondition instanceof PgIndex) {
            newIndex = (PgIndex)newCondition;
        } else {
            return false;
        }
        if (!compareUnalterable(newIndex)) {
            isNeedDepcies.set(true);

            PgDiffArguments args = getDatabase().getArguments();
            boolean concurrently = args != null && args.isConcurrentlyMode();
            if (concurrently) {
                // generate optimized command sequence for concurrent index creation
                String tmpName = "tmp" + new Random().nextInt(Integer.MAX_VALUE)
                        + "_" + getName();
                sb.append("\n\n")
                .append(newIndex.getCreationSQL(tmpName))
                .append("\n\nBEGIN TRANSACTION;\n")
                .append(getDropSQL())
                .append("\nALTER INDEX ")
                .append(PgDiffUtils.getQuotedName(getSchemaName()))
                .append('.')
                .append(PgDiffUtils.getQuotedName(tmpName))
                .append(" RENAME TO ")
                .append(PgDiffUtils.getQuotedName(getName()))
                .append(";\nCOMMIT TRANSACTION;");
            }
            return true;
        }

        if (isClusterIndex() && !newIndex.isClusterIndex() &&
                !((AbstractPgTable)newIndex.getParent()).isClustered()) {
            sb.append("\n\nALTER TABLE ")
            .append(getParent().getQualifiedName())
            .append(" SET WITHOUT CLUSTER;");
        }

        compareOptions(newIndex, sb);

        if (!Objects.equals(getComment(), newIndex.getComment())) {
            sb.append("\n\n");
            newIndex.appendCommentSql(sb);
        }
        return sb.length() > startLength;
    }

    private String getClusterSQL() {
        final StringBuilder sbSQL = new StringBuilder();
        if (isClusterIndex()) {
            sbSQL.append("\n\nALTER TABLE ");
            sbSQL.append(getParent().getQualifiedName());
            sbSQL.append(" CLUSTER ON ");
            sbSQL.append(getName());
            sbSQL.append(';');
        }
        return sbSQL.toString();
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
        resetHash();
    }

    @Override
    protected boolean compareUnalterable(AbstractIndex index) {
        return index instanceof PgIndex
                && super.compareUnalterable(index)
                && Objects.equals(method, ((PgIndex) index).method);
    }

    @Override
    public void computeHash(Hasher hasher) {
        super.computeHash(hasher);
        hasher.put(method);
    }

    @Override
    protected AbstractIndex getIndexCopy() {
        PgIndex index =  new PgIndex(getName());
        index.setMethod(getMethod());
        return index;
    }
}
