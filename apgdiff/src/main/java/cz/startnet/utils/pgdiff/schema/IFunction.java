package cz.startnet.utils.pgdiff.schema;

import java.util.List;
import java.util.Map;

public interface IFunction extends ISearchPath {
    String getReturns();
    Map<String, String> getReturnsColumns();
    List<Argument> getArguments();
}