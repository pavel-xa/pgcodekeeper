package ru.taximaxim.codekeeper.apgdiff.model.difftree;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cz.startnet.utils.pgdiff.PgDiffUtils;

public class IgnoredObject {

    public enum AddStatus {
        ADD, ADD_SUBTREE, SKIP, SKIP_SUBTREE
    }

    private final String name;
    private final Pattern regex;
    private final String dbRegexStr;
    private final Pattern dbRegex;
    private Set<DbObjType> objTypes;
    private boolean isShow;
    private boolean isRegular;
    private boolean ignoreContent;
    private boolean isQualified;

    public IgnoredObject(String name, boolean isRegular,
            boolean ignoreContent, boolean isQualified, Set<DbObjType> objTypes) {
        this(name, null, false, isRegular, ignoreContent, isQualified, objTypes);
    }

    public IgnoredObject(String name, String dbRegex, boolean isShow, boolean isRegular,
            boolean ignoreContent, boolean isQualified, Set<DbObjType> objTypes) {
        this.name = name;
        this.isShow = isShow;
        this.isRegular = isRegular;
        this.ignoreContent = ignoreContent;
        this.isQualified = isQualified;
        this.objTypes = objTypes;
        this.regex = isRegular ? Pattern.compile(name) : null;
        this.dbRegexStr = dbRegex;
        this.dbRegex = dbRegex == null ? null : Pattern.compile(dbRegex);
    }

    public String getName() {
        return name;
    }

    public boolean isShow() {
        return isShow;
    }

    public boolean isRegular() {
        return isRegular;
    }

    public boolean isIgnoreContent() {
        return ignoreContent;
    }

    public boolean isQualified() {
        return isQualified;
    }

    public Set<DbObjType> getObjTypes() {
        return objTypes;
    }

    public void setShow(boolean isShow) {
        this.isShow = isShow;
    }

    public void setRegular(boolean isRegular) {
        this.isRegular = isRegular;
    }

    public void setIgnoreContent(boolean ignoreContent) {
        this.ignoreContent = ignoreContent;
    }

    public void setQualified(boolean isQualified) {
        this.isQualified = isQualified;
    }

    public void setObjTypes(Set<DbObjType> objTypes) {
        this.objTypes = objTypes;
    }

    public IgnoredObject copy(String name) {
        return new IgnoredObject(name, dbRegexStr, isShow, isRegular,
                ignoreContent, isQualified, EnumSet.copyOf(objTypes));
    }

    public boolean match(TreeElement el, String... dbNames) {
        boolean matches;
        if (isRegular) {
            matches = regex.matcher(isQualified ? el.getQualifiedName() : el.getName()).find();
        } else {
            matches = name.equals(isQualified ?  el.getQualifiedName() : el.getName());
        }
        if (!objTypes.isEmpty()) {
            matches = matches && objTypes.contains(el.getType());
        }
        if (matches && dbRegex != null) {
            if (dbNames != null && dbNames.length != 0) {
                boolean foundDbMatch = false;
                for (String dbName : dbNames) {
                    if (dbName != null && dbRegex.matcher(dbName).find()) {
                        foundDbMatch = true;
                        break;
                    }
                }
                matches &= foundDbMatch;
            } else {
                matches = false;
            }
        }
        return matches;
    }

    boolean hasSameMatchingCondition(IgnoredObject rule) {
        return Objects.equals(name, rule.name) && Objects.equals(dbRegexStr, rule.dbRegexStr);
    }

    public AddStatus getAddStatus() {
        if (isShow) {
            return ignoreContent ? AddStatus.ADD_SUBTREE : AddStatus.ADD;
        } else {
            return ignoreContent ? AddStatus.SKIP_SUBTREE : AddStatus.SKIP;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        final int itrue = 1231;
        final int ifalse = 1237;
        int result = 1;
        result = prime * result + (ignoreContent ? itrue : ifalse);
        result = prime * result + (isQualified ? itrue : ifalse);
        result = prime * result + (isRegular ? itrue : ifalse);
        result = prime * result + (isShow ? itrue : ifalse);
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((dbRegexStr == null) ? 0 : dbRegexStr.hashCode());
        result = prime * result + objTypes.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        boolean eq = false;
        if (obj instanceof IgnoredObject) {
            IgnoredObject other = (IgnoredObject) obj;
            eq = ignoreContent == other.ignoreContent
                    && isQualified == other.isQualified
                    && isRegular == other.isRegular
                    && isShow == other.isShow
                    && Objects.equals(name, other.name)
                    && Objects.equals(dbRegexStr, other.dbRegexStr)
                    && objTypes.equals(other.objTypes);
        }
        return eq;
    }


    @Override
    public String toString() {
        return appendRuleCode(new StringBuilder()).toString();
    }

    StringBuilder appendRuleCode(StringBuilder sb) {
        sb.append(isShow ? "SHOW " : "HIDE ");
        if (ignoreContent || isRegular || isQualified) {
            if (ignoreContent) {
                sb.append("CONTENT").append(',');
            }
            if (isRegular) {
                sb.append("REGEX").append(',');
            }
            if (isQualified) {
                sb.append("QUALIFIED").append(',');
            }
            sb.setLength(sb.length() - 1);
        } else {
            sb.append("NONE");
        }
        sb.append(' ');

        sb.append(getValidId(name));

        if (dbRegex != null) {
            sb.append(" db=");
            sb.append(getValidId(dbRegexStr));
        }

        if (!objTypes.isEmpty()) {
            sb.append(" type=");
            sb.append(objTypes.stream().map(Enum::toString).map(IgnoredObject::getValidId)
                    .collect(Collectors.joining(",")));
        }

        return sb;
    }

    private static String getValidId(String id) {
        if (PgDiffUtils.isValidId(id, true, true)) {
            return id;
        } else {
            return quoteWithDq(id) ? PgDiffUtils.quoteName(id) : PgDiffUtils.quoteString(id);
        }
    }

    private static boolean quoteWithDq(String str) {
        int dq = 0;
        int sq = 0;
        for (int i = 0; i < str.length(); ++i) {
            switch (str.charAt(i)) {
            case '\'': ++sq; break;
            case '"' : ++dq; break;
            default : break;
            }
        }
        return sq > dq;
    }
}