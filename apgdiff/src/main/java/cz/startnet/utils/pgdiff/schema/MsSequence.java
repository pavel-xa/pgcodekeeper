package cz.startnet.utils.pgdiff.schema;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import cz.startnet.utils.pgdiff.MsDiffUtils;
import cz.startnet.utils.pgdiff.hashers.Hasher;

public class MsSequence extends AbstractSequence {

    private boolean isCached;
    private String presicion;

    public MsSequence(String name) {
        super(name);
    }

    @Override
    public String getCreationSQL() {
        final StringBuilder sbSQL = new StringBuilder();
        sbSQL.append("CREATE SEQUENCE ");
        sbSQL.append(getQualifiedName());

        sbSQL.append("\n\tAS ").append(MsDiffUtils.quoteName(getDataType()));
        if (getPresicion() != null) {
            sbSQL.append('(').append(getPresicion()).append(", 0)");
        }

        fillSequenceBody(sbSQL);
        sbSQL.append(GO);

        appendOwnerSQL(sbSQL);
        appendPrivileges(sbSQL);

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

        sbSQL.append("\n\tMAXVALUE ");
        sbSQL.append(getMaxValue());

        sbSQL.append("\n\tMINVALUE ");
        sbSQL.append(getMinValue());

        if (isCached()) {
            sbSQL.append("\n\tCACHE ");
            if (getCache() != null) {
                sbSQL.append(getCache());
            }
        }

        if (isCycle()) {
            sbSQL.append("\n\tCYCLE");
        }
    }

    @Override
    public boolean appendAlterSQL(PgStatement newCondition, StringBuilder sb,
            AtomicBoolean isNeedDepcies) {
        final int startLength = sb.length();
        MsSequence newSequence;
        if (newCondition instanceof MsSequence) {
            newSequence = (MsSequence) newCondition;
        } else {
            return false;
        }

        if (!newSequence.getDataType().equals(getDataType())  ||
                !Objects.equals(newSequence.getPresicion(), getPresicion())) {
            isNeedDepcies.set(true);
            return true;
        }

        StringBuilder sbSQL = new StringBuilder();
        sbSQL.setLength(0);

        if (compareSequenceBody(newSequence, sbSQL)) {
            sb.append("\n\nALTER SEQUENCE " + getQualifiedName() + sbSQL.toString() + GO);
        }

        if (!Objects.equals(getOwner(), newSequence.getOwner())) {
            newSequence.alterOwnerSQL(sb);
        }

        alterPrivileges(newSequence, sb);
        return sb.length() > startLength;
    }

    private boolean compareSequenceBody(MsSequence newSequence, StringBuilder sbSQL) {
        final String oldIncrement = getIncrement();
        final String newIncrement = newSequence.getIncrement();

        if (newIncrement != null
                && !newIncrement.equals(oldIncrement)) {
            sbSQL.append("\n\tINCREMENT BY ");
            sbSQL.append(newIncrement);
        }

        final String oldMinValue = getMinValue();
        final String newMinValue = newSequence.getMinValue();

        if (newMinValue == null && oldMinValue != null) {
            sbSQL.append("\n\tNO MINVALUE");
        } else if (newMinValue != null
                && !newMinValue.equals(oldMinValue)) {
            sbSQL.append("\n\tMINVALUE ");
            sbSQL.append(newMinValue);
        }

        final String oldMaxValue = getMaxValue();
        final String newMaxValue = newSequence.getMaxValue();

        if (newMaxValue == null && oldMaxValue != null) {
            sbSQL.append("\n\tNO MAXVALUE");
        } else if (newMaxValue != null
                && !newMaxValue.equals(oldMaxValue)) {
            sbSQL.append("\n\tMAXVALUE ");
            sbSQL.append(newMaxValue);
        }

        final String oldStart = getStartWith();
        final String newStart = newSequence.getStartWith();

        if (newStart != null && !newStart.equals(oldStart)) {
            sbSQL.append("\n\tRESTART WITH ");
            sbSQL.append(newStart);
        }

        final String oldCache = getCache();
        final String newCache = newSequence.getCache();

        if (newSequence.isCached() && !Objects.equals(newCache, oldCache)) {
            sbSQL.append("\n\tCACHE ");
            if (newCache != null) {
                sbSQL.append(newCache);
            }
        } else if (!newSequence.isCached()) {
            sbSQL.append("\n\tNO CACHE");
        }

        final boolean oldCycle = isCycle();
        final boolean newCycle = newSequence.isCycle();

        if (oldCycle && !newCycle) {
            sbSQL.append("\n\tNO CYCLE");
        } else if (!oldCycle && newCycle) {
            sbSQL.append("\n\tCYCLE");
        }

        return sbSQL.length() > 0;
    }

    @Override
    public String getDropSQL() {
        return "DROP SEQUENCE " + getQualifiedName() + GO;
    }

    @Override
    public void setMinMaxInc(long inc, Long max, Long min, String dataType,
            long precision) {
        String type = dataType != null ? dataType : BIGINT;
        this.increment = Long.toString(inc);
        this.maxValue = Long.toString(max == null ?
                getBoundaryTypeVal(type, true, precision) : max);
        this.minValue = Long.toString(min == null ?
                getBoundaryTypeVal(type, false, precision) : min);
        resetHash();
    }

    public String getPresicion() {
        return presicion;
    }

    public void setPresicion(String presicion) {
        this.presicion = presicion;
        resetHash();
    }

    public boolean isCached() {
        return isCached;
    }

    public void setCached(boolean isCached) {
        this.isCached = isCached;
        resetHash();
    }

    @Override
    public boolean compare(PgStatement obj) {
        return obj instanceof MsSequence && super.compare(obj)
                && Objects.equals(presicion, ((MsSequence) obj).getPresicion())
                && isCached == ((MsSequence) obj).isCached();
    }


    @Override
    public void computeHash(Hasher hasher) {
        super.computeHash(hasher);
        hasher.put(presicion);
        hasher.put(isCached);
    }

    @Override
    public boolean isPostgres() {
        return false;
    }

    @Override
    protected AbstractSequence getSequenceCopy() {
        MsSequence sequence = new MsSequence(getName());
        sequence.setPresicion(getPresicion());
        sequence.setCached(isCached());
        return sequence;
    }
}
