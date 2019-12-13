package cz.startnet.utils.pgdiff.loader.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import cz.startnet.utils.pgdiff.PgDiffUtils;
import cz.startnet.utils.pgdiff.loader.JdbcQueries;
import cz.startnet.utils.pgdiff.schema.GenericColumn;
import cz.startnet.utils.pgdiff.schema.MsUser;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;

public class MsUsersReader {

    private final JdbcLoaderBase loader;
    private final PgDatabase db;

    public MsUsersReader(JdbcLoaderBase loader, PgDatabase db) {
        this.loader = loader;
        this.db = db;
    }

    public void read() throws SQLException, InterruptedException, XmlReaderException {
        loader.setCurrentOperation("users query");
        String query = JdbcQueries.QUERY_MS_USERS.getQuery();
        try (ResultSet res = loader.runner.runScript(loader.statement, query)) {
            while (res.next()) {
                PgDiffUtils.checkCancelled(loader.monitor);
                String name = res.getString("name");
                loader.setCurrentObject(new GenericColumn(name, DbObjType.USER));

                MsUser user = new MsUser(name);
                user.setLogin(res.getString("loginname"));

                user.setSchema(res.getString("schema_name"));

                loader.setPrivileges(user, XmlReader.readXML(res.getString("acl")));
                db.addUser(user);
            }
        }
    }
}
