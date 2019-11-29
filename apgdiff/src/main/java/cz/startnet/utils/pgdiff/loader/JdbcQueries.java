package cz.startnet.utils.pgdiff.loader;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import ru.taximaxim.codekeeper.apgdiff.ApgdiffUtils;
import ru.taximaxim.codekeeper.apgdiff.log.Log;

/**
 * For every field in this class that starts with 'QUERY_'
 * the static initializer tries to: <br>
 * - if the field is String: find a file named %FIELD_NAME%.sql in this package
 *   and assign its contents to the field.<br>
 * - if the field is Map: load %FIELD_NAME%.sql as described above and map its contents to null,
 *   try to load every %FIELD_NAME%_%VERSION%.sql and map their contents with their versions.
 *
 * Similar to {@link org.eclipse.osgi.util.NLS}, OSGi localization classes.
 *
 * @author levsha_aa, galiev_mr
 */
public final class JdbcQueries {

    // SONAR-OFF

    public static String QUERY_TOTAL_OBJECTS_COUNT;
    public static String QUERY_TYPES_FOR_CACHE_ALL;
    public static String QUERY_CHECK_VERSION;
    public static String QUERY_CHECK_LAST_SYS_OID;
    public static String QUERY_CHECK_TIMESTAMPS;

    public static final JdbcQuery QUERY_EXTENSIONS = new JdbcQuery();
    public static final JdbcQuery QUERY_SCHEMAS = new JdbcQuery();

    public static final JdbcQuery QUERY_TABLES = new JdbcQuery();
    public static final JdbcQuery QUERY_FUNCTIONS = new JdbcQuery();
    public static final JdbcQuery QUERY_SEQUENCES = new JdbcQuery();
    public static final JdbcQuery QUERY_INDICES = new JdbcQuery();
    public static final JdbcQuery QUERY_CONSTRAINTS = new JdbcQuery();
    public static final JdbcQuery QUERY_TRIGGERS = new JdbcQuery();
    public static final JdbcQuery QUERY_VIEWS = new JdbcQuery();
    public static final JdbcQuery QUERY_TYPES = new JdbcQuery();
    public static final JdbcQuery QUERY_RULES = new JdbcQuery();
    public static final JdbcQuery QUERY_FTS_PARSERS = new JdbcQuery();
    public static final JdbcQuery QUERY_FTS_TEMPLATES = new JdbcQuery();
    public static final JdbcQuery QUERY_FTS_DICTIONARIES = new JdbcQuery();
    public static final JdbcQuery QUERY_FTS_CONFIGURATIONS = new JdbcQuery();
    public static final JdbcQuery QUERY_OPERATORS = new JdbcQuery();

    public static String QUERY_SCHEMAS_ACCESS;
    public static String QUERY_SEQUENCES_ACCESS;
    public static String QUERY_SEQUENCES_DATA;

    public static String QUERY_SYSTEM_FUNCTIONS;
    public static String QUERY_SYSTEM_RELATIONS;
    public static String QUERY_SYSTEM_OPERATORS;
    public static String QUERY_SYSTEM_CASTS;

    public static final JdbcQuery QUERY_MS_SCHEMAS = new JdbcQuery();

    public static final JdbcQuery QUERY_MS_TABLES = new JdbcQuery();
    public static final JdbcQuery QUERY_MS_FUNCTIONS_PROCEDURES_VIEWS_TRIGGERS = new JdbcQuery();
    public static final JdbcQuery QUERY_MS_EXTENDED_FUNCTIONS_AND_PROCEDURES = new JdbcQuery();
    public static final JdbcQuery QUERY_MS_SEQUENCES = new JdbcQuery();
    public static final JdbcQuery QUERY_MS_INDICES_AND_PK = new JdbcQuery();
    public static final JdbcQuery QUERY_MS_FK = new JdbcQuery();
    public static final JdbcQuery QUERY_MS_CHECK_CONSTRAINTS = new JdbcQuery();
    public static final JdbcQuery QUERY_MS_ASSEMBLIES = new JdbcQuery();
    public static final JdbcQuery QUERY_MS_ROLES = new JdbcQuery();
    public static final JdbcQuery QUERY_MS_TYPES = new JdbcQuery();
    public static final JdbcQuery QUERY_MS_USERS = new JdbcQuery();

    // SONAR-ON

    static {
        for (Field f : JdbcQueries.class.getFields()) {
            if (!f.getName().startsWith("QUERY")) {
                continue;
            }
            try {
                if (JdbcQuery.class.isAssignableFrom(f.getType())) {
                    fillQueries(f);
                } else if (String.class.isAssignableFrom(f.getType())) {
                    String res = f.getName().startsWith("QUERY_SYSTEM") ? "system/" + f.getName() : f.getName();
                    f.set(null, readResource(res));
                }
            } catch (Exception ex) {
                Log.log(Log.LOG_ERROR,
                        "Error while loading JDBC SQL Queries resource: " + f.getName(), ex);
            }
        }
    }

    private static void fillQueries (Field f) throws Exception {
        JdbcQuery query = (JdbcQuery) f.get(null);

        if (f.getName().startsWith("QUERY_MS")) {
            query.setQuery(readResource("ms/" + f.getName()));
            return;
        }

        query.setQuery(readResource(f.getName()));

        for (SupportedVersion version : SupportedVersion.values()) {
            URL url = JdbcQueries.class.getResource(f.getName() + '_' + version + ".sql");
            if (url != null) {
                query.addSinceQuery(version, readResource(url));
            }
            for (SupportedVersion v2 : SupportedVersion.values()) {
                URL urlInterval = JdbcQueries.class.getResource(f.getName() + '_'
                        + version.getVersion() + '_' + v2.getVersion() + ".sql");
                if (urlInterval != null) {
                    query.addIntervalQuery(version, v2, readResource(urlInterval));
                }
            }
        }
    }

    private static String readResource(String name) throws IOException, URISyntaxException {
        return readResource(JdbcQueries.class.getResource(name + ".sql"));
    }

    private static String readResource(URL url) throws IOException, URISyntaxException {
        return new String(Files.readAllBytes(ApgdiffUtils.getFileFromOsgiRes(url).toPath()),
                StandardCharsets.UTF_8);
    }

    private JdbcQueries() {
    }
}