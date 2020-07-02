package ru.taximaxim.codekeeper.apgdiff.model.graph;

import java.util.Objects;

import cz.startnet.utils.pgdiff.schema.PgStatement;

/**
 * Класс используется как контейнер для объединения дейсвий с объектом БД
 * (CREATE ALTER DROP) Также хранит объект, инициировавший действие
 */
class ActionContainer {
    private final PgStatement oldObj;
    private final PgStatement newObj;
    private final StatementActions action;
    private final PgStatement starter;

    public ActionContainer(PgStatement oldObj, PgStatement newObj,
            StatementActions action, PgStatement starter) {
        this.oldObj = oldObj;
        this.newObj = newObj;
        this.action = action;
        this.starter = starter;
    }

    public PgStatement getOldObj() {
        return oldObj;
    }

    public PgStatement getNewObj() {
        return newObj;
    }

    public StatementActions getAction() {
        return action;
    }

    public PgStatement getStarter() {
        return starter;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((action == null) ? 0 : action.hashCode());
        result = prime * result + ((oldObj == null) ? 0 : oldObj.hashCode());
        result = prime * result + ((newObj == null) ? 0 : newObj.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ActionContainer) {
            ActionContainer cont = (ActionContainer) obj;
            return action == cont.getAction() &&
                    Objects.equals(oldObj, cont.getOldObj()) &&
                    Objects.equals(newObj, cont.getNewObj());
        }
        return false;
    }
    @Override
    public String toString() {
        return action + " " + (oldObj == null? " " : oldObj.getName());
    }
}