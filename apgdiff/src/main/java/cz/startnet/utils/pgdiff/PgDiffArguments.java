/**
 * Copyright 2006 StartNet s.r.o.
 *
 * Distributed under MIT license
 */
package cz.startnet.utils.pgdiff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;

public class PgDiffArguments implements Cloneable {

    private boolean modeParse;
    private String newSrc;
    private String oldSrc;
    private String newSrcFormat;
    private String oldSrcFormat;
    private String outputTarget;
    private String inCharsetName = ApgdiffConsts.UTF_8;
    private String outCharsetName = ApgdiffConsts.UTF_8;
    private boolean ignorePrivileges;
    private boolean keepNewlines;
    private boolean addTransaction;
    private boolean disableCheckFunctionBodies;
    private String timeZone;
    private boolean usingTypeCastOff;
    private boolean concurrentlyMode;
    private boolean safeMode;
    private final List<DangerStatement> allowedDangers = new ArrayList<>();
    private final List<DbObjType> allowedTypes = new ArrayList<>();
    private boolean stopNotAllowed;
    private final List<String> ignoreLists = new ArrayList<>();
    private final List<String> sourceLibs = new ArrayList<>();
    private final List<String> sourceLibXmls = new ArrayList<>();
    private final List<String> sourceLibsWithoutPriv = new ArrayList<>();
    private final List<String> targetLibXmls = new ArrayList<>();
    private final List<String> targetLibs = new ArrayList<>();
    private final List<String> targetLibsWithoutPriv = new ArrayList<>();
    private boolean libSafeMode;
    private boolean msSql;
    private boolean ignoreConcurrentModification;

    public void setModeParse(final boolean modeParse) {
        this.modeParse = modeParse;
    }

    public boolean isModeParse() {
        return modeParse;
    }

    public void setNewSrc(final String newSrc) {
        this.newSrc = newSrc;
    }

    public String getNewSrc() {
        return newSrc;
    }

    public void setOldSrc(final String oldSrc) {
        this.oldSrc = oldSrc;
    }

    public String getOldSrc() {
        return this.oldSrc;
    }

    public void setNewSrcFormat(final String newSrcFormat) {
        this.newSrcFormat = newSrcFormat;
    }

    public String getNewSrcFormat() {
        return this.newSrcFormat;
    }

    public void setOldSrcFormat(final String oldSrcFormat) {
        this.oldSrcFormat = oldSrcFormat;
    }

    public String getOldSrcFormat() {
        return this.oldSrcFormat;
    }

    public void setOutputTarget(final String outputTarget) {
        this.outputTarget = outputTarget;
    }

    public String getOutputTarget() {
        return this.outputTarget;
    }

    public void setAddTransaction(final boolean addTransaction) {
        this.addTransaction = addTransaction;
    }

    public boolean isAddTransaction() {
        return addTransaction;
    }

    public boolean isStopNotAllowed() {
        return stopNotAllowed;
    }

    public void setStopNotAllowed(boolean stopNotAllowed) {
        this.stopNotAllowed = stopNotAllowed;
    }

    public boolean isSafeMode() {
        return safeMode;
    }

    public void setSafeMode(final boolean safeMode) {
        this.safeMode = safeMode;
    }

    public Collection<DangerStatement> getAllowedDangers() {
        return Collections.unmodifiableCollection(allowedDangers);
    }

    public Collection<String> getIgnoreLists() {
        return Collections.unmodifiableCollection(ignoreLists);
    }

    public Collection<String> getSourceLibXmls() {
        return Collections.unmodifiableCollection(sourceLibXmls);
    }

    public Collection<String> getSourceLibs() {
        return Collections.unmodifiableCollection(sourceLibs);
    }

    public Collection<String> getSourceLibsWithoutPriv() {
        return Collections.unmodifiableCollection(sourceLibsWithoutPriv);
    }

    public Collection<String> getTargetLibXmls() {
        return Collections.unmodifiableCollection(targetLibXmls);
    }

    public Collection<String> getTargetLibs() {
        return Collections.unmodifiableCollection(targetLibs);
    }

    public Collection<String> getTargetLibsWithoutPriv() {
        return Collections.unmodifiableCollection(targetLibsWithoutPriv);
    }

    public boolean isLibSafeMode() {
        return libSafeMode;
    }

    public void setLibSafeMode(boolean libSafeMode) {
        this.libSafeMode = libSafeMode;
    }

    public boolean isMsSql() {
        return msSql;
    }

    public void setMsSql(boolean msSql) {
        this.msSql = msSql;
    }

    public boolean isIgnoreConcurrentModification() {
        return ignoreConcurrentModification;
    }

    public void setIgnoreConcurrentModification(boolean ignoreConcurrentModification) {
        this.ignoreConcurrentModification = ignoreConcurrentModification;
    }

    public String getInCharsetName() {
        return inCharsetName;
    }

    public void setInCharsetName(final String inCharsetName) {
        this.inCharsetName = inCharsetName;
    }

    public String getOutCharsetName() {
        return outCharsetName;
    }

    public void setOutCharsetName(final String outCharsetName) {
        this.outCharsetName = outCharsetName;
    }

    public void setDisableCheckFunctionBodies(boolean disableCheckFunctionBodies) {
        this.disableCheckFunctionBodies = disableCheckFunctionBodies;
    }

    public boolean isDisableCheckFunctionBodies() {
        return disableCheckFunctionBodies;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setIgnorePrivileges(boolean ignorePrivilleges) {
        this.ignorePrivileges = ignorePrivilleges;
    }

    public boolean isIgnorePrivileges() {
        return ignorePrivileges;
    }

    public void setKeepNewlines(boolean keepNewlines) {
        this.keepNewlines = keepNewlines;
    }

    public boolean isKeepNewlines() {
        return keepNewlines;
    }

    public Collection<DbObjType> getAllowedTypes() {
        return Collections.unmodifiableCollection(allowedTypes);
    }

    public boolean isUsingTypeCastOff() {
        return usingTypeCastOff;
    }

    public void setUsingTypeCastOff(boolean usingTypeCastOff) {
        this.usingTypeCastOff = usingTypeCastOff;
    }

    public boolean isConcurrentlyMode() {
        return concurrentlyMode;
    }

    public void setConcurrentlyMode(boolean concurrentlyMode) {
        this.concurrentlyMode = concurrentlyMode;
    }

    @Override
    public PgDiffArguments clone() {
        try {
            return (PgDiffArguments) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Impossible error", e);
        }
    }
}
