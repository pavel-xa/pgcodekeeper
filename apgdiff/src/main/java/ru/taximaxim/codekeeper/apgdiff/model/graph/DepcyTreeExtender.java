package ru.taximaxim.codekeeper.apgdiff.model.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgStatement;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.TreeElement;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.TreeElement.DiffSide;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.TreeFlattener;

/**
 * По выбранным элементам в дереве, находит зависимые элементы в дереве,
 * используя мех-м поиска зависимостей
 *
 * @author botov_av
 *
 */
public class DepcyTreeExtender {

    private final PgDatabase dbSource;
    private final PgDatabase dbTarget;
    private final SimpleDepcyResolver depRes;
    private final TreeElement root;
    /**
     * Элементы выбранные пользователем для наката на Проект
     */
    private final List<TreeElement> userSelection;
    /**
     * Зависимые элементы от создаваемых(содержат выбор пользователя)
     */
    private final List<TreeElement> treeDepcyNewEdit = new ArrayList<>();
    /**
     * Зависимые элементы от удаляемых(содержат выбор пользователя)
     */
    private final List<TreeElement> treeDepcyDelete = new ArrayList<>();

    public DepcyTreeExtender(PgDatabase dbSource, PgDatabase dbTarget, TreeElement root) {
        this.dbSource = dbSource;
        this.dbTarget = dbTarget;
        this.root = root;
        userSelection = new TreeFlattener().onlySelected().flatten(root);
        depRes = new SimpleDepcyResolver(dbSource, dbTarget);
    }

    /**
     * При редактированном состоянии или создаваемом объекте тянет зависимости
     * сверху, для создания,изменения объекта
     */
    private void fillDepcyOfNewEdit() {
        PgStatement markedToCreate;
        Set<PgStatement> newEditDepcy = new HashSet<>();
        for (TreeElement sel : userSelection) {
            if (sel.getSide() != DiffSide.LEFT
                    && (markedToCreate = sel.getPgStatement(dbTarget)) != null){
                newEditDepcy.addAll(depRes.getCreateDepcies(markedToCreate));
            }
        }
        fillTreeDepcies(treeDepcyNewEdit , newEditDepcy);
    }

    /**
     * При удалении объекта тянет зависимости снизу
     */
    private void fillDepcyOfDeleted() {
        PgStatement markedToDelete;
        Set<PgStatement> deleteDepcy = new HashSet<>();
        for (TreeElement sel : userSelection) {
            if (sel.getSide() == DiffSide.LEFT
                    && sel.getType() != DbObjType.SEQUENCE
                    && (markedToDelete = sel.getPgStatement(dbSource)) != null) {
                deleteDepcy.addAll(depRes.getDropDepcies(markedToDelete));
            }
        }
        fillTreeDepcies(treeDepcyDelete, deleteDepcy);
    }

    /**
     * вытаскивает из дерева объект для зависимости
     * @param treeDepcy
     * @param pgDecies
     */
    private void fillTreeDepcies(List<TreeElement> treeDepcy, Collection<PgStatement> pgDecies) {
        for (PgStatement depcy : pgDecies) {
            TreeElement finded = root.findElement(depcy);
            if (finded != null) {
                if (finded.getSide() == DiffSide.BOTH) {
                    if (!finded.getPgStatement(dbSource).compare(finded.getPgStatement(dbTarget))) {
                        treeDepcy.add(finded);
                    }
                } else {
                    treeDepcy.add(finded);
                }
            }
        }
    }
    /**
     * Возвращает все зависимые элементы для показа в нижней части таблицы
     * @return зависимые элементы от выбора
     */
    public Set<TreeElement> getDepcies() {
        Set<TreeElement> res = new HashSet<>();
        fillDepcyOfNewEdit();
        fillDepcyOfDeleted();
        res.addAll(treeDepcyNewEdit);
        res.addAll(treeDepcyDelete);
        // удалить все объекты которые выбрал пользователь
        res.removeAll(userSelection);
        return res;
    }
}
