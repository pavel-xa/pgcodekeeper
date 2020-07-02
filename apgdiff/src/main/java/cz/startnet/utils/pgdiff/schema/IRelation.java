package cz.startnet.utils.pgdiff.schema;

import java.util.stream.Stream;

import ru.taximaxim.codekeeper.apgdiff.utils.Pair;

public interface IRelation extends ISearchPath {
    Stream<Pair<String, String>> getRelationColumns();
}
