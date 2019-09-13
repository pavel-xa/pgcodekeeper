/**
 * Copyright 2006 StartNet s.r.o.
 *
 * Distributed under MIT license
 */
package cz.startnet.utils.pgdiff.schema;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import cz.startnet.utils.pgdiff.hashers.Hasher;

/**
 * Stores sequence information.
 */
public class PgSequence extends AbstractSequence {

    private String ownedBy;

    public PgSequence(String name) {
        super(name);
        setCache("1");
    }

    @Override
    public String getCreationSQL() {
        final StringBuilder sbSQL = new StringBuilder();
        sbSQL.append("CREATE SEQUENCE ");
        sbSQL.append("IF NOT EXISTS ");		// P.Smirnov
        sbSQL.append(getQualifiedName());

        if (!BIGINT.equals(getDataType())) {
            sbSQL.append("\n\tAS ").append(getDataType());
        }

        fillSequenceBody(sbSQL);

        sbSQL.append(';');

        appendOwnerSQL(sbSQL);
        appendPrivileges(sbSQL);

        if (comment != null && !comment.isEmpty()) {
            sbSQL.append("\n\n");
            appendCommentSql(sbSQL);
        }

        return sbSQL.toString();
    }

    @Override
    public void fillSequenceBody(StringBuilder sbSQL) {
        if (getStartWith() != null) {
            sbSQL.append("\n\tSTART WITH ");
            sbSQL.append(getStartWith());
        }

        if (getIncrement() != null) {
            sbSQL.append("\n\tINCREMENT BY ");
            sbSQL.append(getIncrement());
        }

        sbSQL.append("\n\t");

        if (getMaxValue() == null) {
            sbSQL.append("NO MAXVALUE");
        } else {
            sbSQL.append("MAXVALUE ");
            sbSQL.append(getMaxValue());
        }

        sbSQL.append("\n\t");

        if (getMinValue() == null) {
            sbSQL.append("NO MINVALUE");
        } else {
            sbSQL.append("MINVALUE ");
            sbSQL.append(getMinValue());
        }

        if (getCache() != null) {
            sbSQL.append("\n\tCACHE ");
            sbSQL.append(getCache());
        }

        if (isCycle()) {
            sbSQL.append("\n\tCYCLE");
        }
    }

    /**
     * Creates SQL statement for modification "OWNED BY" parameter.
     */
    public String getOwnedBySQL() {
        if (getOwnedBy() == null || getOwnedBy().isEmpty()) {
            return "";
        }
        final StringBuilder sbSQL = new StringBuilder();

        sbSQL.append("\n\nALTER SEQUENCE ");
        sbSQL.append("IF EXISTS ");		// P.Smirnov
        sbSQL.append(getQualifiedName());
        sbSQL.append("\n\tOWNED BY ").append(getOwnedBy()).append(';');

        return sbSQL.toString();
    }

    @Override
    public String getFullSQL() {
        StringBuilder sb = new StringBuilder(super.getFullSQL());
        sb.append(getOwnedBySQL());
        return sb.toString();
    }

    @Override
    public String getDropSQL() {
        return "DROP SEQUENCE "
        		+ "IF EXISTS "
        		+ getQualifiedName() + ";";
    }

    @Override
    public boolean appendAlterSQL(PgStatement newCondition, StringBuilder sb,
            AtomicBoolean isNeedDepcies) {
        final int startLength = sb.length();
        PgSequence newSequence = (PgSequence) newCondition;
        StringBuilder sbSQL = new StringBuilder();

        if (compareSequenceBody(newSequence, sbSQL)) {
            sb.append("\n\nALTER SEQUENCE ").
            append("IF EXISTS ").		// P.Smirnov
            append(newSequence.getQualifiedName()).
            append(sbSQL).append(';');
        }

        if (!Objects.equals(getOwner(), newSequence.getOwner())) {
            newSequence.alterOwnerSQL(sb);
        }

        alterPrivileges(newSequence, sb);

        if (!Objects.equals(getComment(), newSequence.getComment())) {
            sb.append("\n\n");
            newSequence.appendCommentSql(sb);
        }

        return sb.length() > startLength;
    }

    private boolean compareSequenceBody(AbstractSequence newSequence, StringBuilder sbSQL) {
        final String oldType = getDataType();
        final String newType = newSequence.getDataType();

        if (!oldType.equals(newType)) {
            sbSQL.append("\n\tAS ");
            sbSQL.append(newType);
        }

        final String newIncrement = newSequence.getIncrement();
        if (newIncrement != null
                && !newIncrement.equals(getIncrement())) {
            sbSQL.append("\n\tINCREMENT BY ");
            sbSQL.append(newIncrement);
        }

        final String newMinValue = newSequence.getMinValue();
        if (newMinValue == null && getMinValue() != null) {
            sbSQL.append("\n\tNO MINVALUE");
        } else if (newMinValue != null
                && !newMinValue.equals(getMinValue())) {
            sbSQL.append("\n\tMINVALUE ");
            sbSQL.append(newMinValue);
        }

        final String newMaxValue = newSequence.getMaxValue();
        if (newMaxValue == null && getMaxValue() != null) {
            sbSQL.append("\n\tNO MAXVALUE");
        } else if (newMaxValue != null
                && !newMaxValue.equals(getMaxValue())) {
            sbSQL.append("\n\tMAXVALUE ");
            sbSQL.append(newMaxValue);
        }

        final String newStart = newSequence.getStartWith();
        if (newStart != null && !newStart.equals(getStartWith())) {
            sbSQL.append("\n\tRESTART WITH ");
            sbSQL.append(newStart);
        }

        final String newCache = newSequence.getCache();
        if (newCache != null && !newCache.equals(getCache())) {
            sbSQL.append("\n\tCACHE ");
            sbSQL.append(newCache);
        }

        final boolean newCycle = newSequence.isCycle();
        if (isCycle() && !newCycle) {
            sbSQL.append("\n\tNO CYCLE");
        } else if (!isCycle() && newCycle) {
            sbSQL.append("\n\tCYCLE");
        }

        return sbSQL.length() > 0;
    }

    @Override
    public void setMinMaxInc(long inc, Long max, Long min, String dataType, long precision) {
        String type = dataType != null ? dataType : BIGINT;
        this.increment = Long.toString(inc);
        if (max == null || (inc > 0 && max == getBoundaryTypeVal(type, true, 0L))
                || (inc < 0 && max == -1)) {
            this.maxValue = null;
        } else {
            this.maxValue = "" + max;
        }
        if (min == null || (inc > 0 && min == 1)
                || (inc < 0 && min == getBoundaryTypeVal(type, false, 0L))) {
            this.minValue = null;
        } else {
            this.minValue = "" + min;
        }

        if (getStartWith() == null) {
            if (this.minValue != null) {
                setStartWith(this.minValue);
            } else {
                setStartWith(inc < 0 ? "-1" : "1");
            }
        }
        resetHash();
    }

    @Override
    public boolean compare(PgStatement obj) {
        return obj instanceof PgSequence && super.compare(obj)
                && Objects.equals(ownedBy, ((PgSequence) obj).getOwnedBy());
    }


    public String getOwnedBy() {
        return ownedBy;
    }

    public void setOwnedBy(final String ownedBy) {
        this.ownedBy = ownedBy;
        resetHash();
    }

    @Override
    public void setDataType(String dataType) {
        super.setDataType(dataType.toLowerCase(Locale.ROOT));
    }

    @Override
    public void computeHash(Hasher hasher) {
        super.computeHash(hasher);
        hasher.put(ownedBy);
    }

    @Override
    protected AbstractSequence getSequenceCopy() {
        PgSequence sequence = new PgSequence(getName());
        sequence.setOwnedBy(getOwnedBy());
        return sequence;
    }
}
