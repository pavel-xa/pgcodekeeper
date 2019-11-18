package cz.startnet.utils.pgdiff.schema;

import java.io.Serializable;
import java.util.Objects;

import cz.startnet.utils.pgdiff.hashers.Hasher;
import cz.startnet.utils.pgdiff.hashers.IHashable;
import cz.startnet.utils.pgdiff.hashers.JavaHasher;

/**
 * Subclass when need to reset hashes
 * (like when setting hashed fields after adding the arg to its container).
 */
public class Argument implements Serializable, IHashable {

    private static final long serialVersionUID = 7466228261754446064L;

    private final ArgMode mode;
    private final String name;
    private final String dataType;
    private String defaultExpression;
    private boolean isReadOnly;

    public Argument(String name, String dataType) {
        this(ArgMode.IN, name, dataType);
    }

    public Argument(ArgMode mode, String name, String dataType) {
        this.mode = mode;
        this.name = (name != null && name.isEmpty()) ? null : name;
        this.dataType = dataType;
    }

    public String getDataType() {
        return dataType;
    }

    public String getDefaultExpression() {
        return defaultExpression;
    }

    public void setDefaultExpression(final String defaultExpression) {
        this.defaultExpression = defaultExpression;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public void setReadOnly(final boolean isReadOnly) {
        this.isReadOnly = isReadOnly;
    }

    public ArgMode getMode() {
        return mode;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        boolean eq = false;

        if (this == obj) {
            eq = true;
        } else if (obj instanceof Argument) {
            final Argument arg = (Argument) obj;
            eq = Objects.equals(dataType, arg.getDataType())
                    && Objects.equals(defaultExpression, arg.getDefaultExpression())
                    && mode == arg.getMode()
                    && isReadOnly == arg.isReadOnly()
                    && Objects.equals(name, arg.getName());
        }

        return eq;
    }

    @Override
    public int hashCode() {
        JavaHasher hasher = new JavaHasher();
        computeHash(hasher);
        return hasher.getResult();
    }

    @Override
    public void computeHash(Hasher hasher) {
        hasher.put(dataType);
        hasher.put(defaultExpression);
        hasher.put(mode);
        hasher.put(name);
        hasher.put(isReadOnly);
    }
}
