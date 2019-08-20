/**
 * Copyright 2006 StartNet s.r.o.
 *
 * Distributed under MIT license
 */
package cz.startnet.utils.pgdiff.loader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import cz.startnet.utils.pgdiff.PgDiffArguments;
import cz.startnet.utils.pgdiff.schema.AbstractColumn;
import cz.startnet.utils.pgdiff.schema.AbstractConstraint;
import cz.startnet.utils.pgdiff.schema.AbstractIndex;
import cz.startnet.utils.pgdiff.schema.AbstractSchema;
import cz.startnet.utils.pgdiff.schema.AbstractSequence;
import cz.startnet.utils.pgdiff.schema.AbstractTable;
import cz.startnet.utils.pgdiff.schema.Argument;
import cz.startnet.utils.pgdiff.schema.PgColumn;
import cz.startnet.utils.pgdiff.schema.PgConstraint;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgExtension;
import cz.startnet.utils.pgdiff.schema.PgFunction;
import cz.startnet.utils.pgdiff.schema.PgIndex;
import cz.startnet.utils.pgdiff.schema.PgPrivilege;
import cz.startnet.utils.pgdiff.schema.PgRule;
import cz.startnet.utils.pgdiff.schema.PgRule.PgRuleEventType;
import cz.startnet.utils.pgdiff.schema.PgSchema;
import cz.startnet.utils.pgdiff.schema.PgSequence;
import cz.startnet.utils.pgdiff.schema.PgTrigger;
import cz.startnet.utils.pgdiff.schema.PgTrigger.TgTypes;
import cz.startnet.utils.pgdiff.schema.PgType;
import cz.startnet.utils.pgdiff.schema.PgType.PgTypeForm;
import cz.startnet.utils.pgdiff.schema.PgView;
import cz.startnet.utils.pgdiff.schema.SimplePgTable;
import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts;
import ru.taximaxim.codekeeper.apgdiff.ApgdiffTestUtils;
import ru.taximaxim.codekeeper.apgdiff.fileutils.TempDir;
import ru.taximaxim.codekeeper.apgdiff.model.exporter.ModelExporter;

/**
 * An abstract 'factory' that creates 'artificial'
 * PgDatabase objects for specific test-cases.
 *
 * @author Alexander Levsha
 */
interface PgDatabaseObjectCreator {

    /**
     * The method makes up a PgDatabase object specific to the test needs.
     */
    PgDatabase getDatabase();
}

/**
 * Tests for PgDiffLoader class.
 *
 * @author fordfrog
 */
@RunWith(value = Parameterized.class)
public class PgAntlrLoaderTest {

    private static final String ENCODING = ApgdiffConsts.UTF_8;
    /**
     * Provides parameters for running the tests.
     *
     * @return parameters for the tests
     */
    @Parameters
    public static Collection<?> parameters() {
        return Arrays.asList(
                new Object[][]{
                    // SONAR-OFF
                    {1},
                    {2},
                    {3},
                    {4},
                    {5},
                    {6},
                    {7},
                    {8},
                    {9},
                    {10},
                    {11},
                    {12},
                    {13},
                    {14},
                    {15},
                    {16},
                    {17}
                    // SONAR-ON
                });
    }
    /**
     * Index of the file that should be tested.
     */
    private final int fileIndex;

    /**
     * Array of implementations of {@link PgDatabaseObjectCreator}
     * each returning a specific {@link PgDatabase} for a test-case.
     */
    private static final PgDatabaseObjectCreator[] DB_OBJS = {
            new PgDB1(),
            new PgDB2(),
            new PgDB3(),
            new PgDB4(),
            new PgDB5(),
            new PgDB6(),
            new PgDB7(),
            new PgDB8(),
            new PgDB9(),
            new PgDB10(),
            new PgDB11(),
            new PgDB12(),
            new PgDB13(),
            new PgDB14(),
            new PgDB15(),
            new PgDB16(),
            new PgDB17()
    };

    /**
     * Creates a new instance of PgDumpLoaderTest.
     *
     * @param fileIndex {@link #fileIndex}
     */
    public PgAntlrLoaderTest(final int fileIndex) {
        this.fileIndex = fileIndex;
    }

    @Test
    public void loadSchema() throws InterruptedException, IOException {

        // first test the dump loader itself
        String filename = "schema_" + fileIndex + ".sql";
        PgDiffArguments args = new PgDiffArguments();
        args.setInCharsetName(ENCODING);
        args.setKeepNewlines(true);
        PgDatabase d = ApgdiffTestUtils.loadTestDump(
                filename, PgAntlrLoaderTest.class, args);

        // then check result's validity against handmade DB object
        if(fileIndex > DB_OBJS.length) {
            Assert.fail("No predefined object for file: " + filename);
        }

        PgDatabase dbPredefined = DB_OBJS[fileIndex - 1].getDatabase();
        Assert.assertEquals("PgDumpLoader: predefined object is not equal to file "
                + filename, dbPredefined, d);

        // test deepCopy mechanism
        Assert.assertEquals("PgStatement deep copy altered", d, d.deepCopy());
        Assert.assertEquals("PgStatement deep copy altered original", dbPredefined, d);
    }

    /**
     * Tests ModelExporter exportFull() method
     * @throws InterruptedException
     */
    @Test
    public void exportFullDb() throws IOException, InterruptedException {
        // prepare db object from sql file
        String filename = "schema_" + fileIndex + ".sql";
        PgDiffArguments args = new PgDiffArguments();
        args.setInCharsetName(ENCODING);
        args.setKeepNewlines(true);
        PgDatabase dbFromFile = ApgdiffTestUtils.loadTestDump(
                filename, PgAntlrLoaderTest.class, args);

        PgDatabase dbPredefined = DB_OBJS[fileIndex - 1].getDatabase();
        Path exportDir = null;
        try (TempDir dir = new TempDir("pgCodekeeper-test-files")) {
            exportDir = dir.get();
            new ModelExporter(exportDir, dbPredefined, ENCODING).exportFull();

            args = new PgDiffArguments();
            args.setInCharsetName(ENCODING);
            args.setKeepNewlines(true);
            PgDatabase dbAfterExport = new ProjectLoader(exportDir.toString(), args).loadDatabaseSchemaFromDirTree();

            // check the same db similarity before and after export
            Assert.assertEquals("ModelExporter: predefined object PgDB" + fileIndex +
                    " is not equal to exported'n'loaded.", dbPredefined, dbAfterExport);

            Assert.assertEquals("ModelExporter: exported predefined object is not "
                    + "equal to file " + filename, dbAfterExport, dbFromFile);
        }
    }
}

// SONAR-OFF

class PgDB1 implements PgDatabaseObjectCreator {
    @Override
    public PgDatabase getDatabase() {
        PgDatabase d = ApgdiffTestUtils.createDumpDB();
        AbstractSchema schema = d.getDefaultSchema();

        AbstractTable table = new SimplePgTable("fax_boxes");
        schema.addTable(table);

        AbstractColumn col = new PgColumn("fax_box_id");
        col.setType("serial");
        col.setNullValue(false);
        table.addColumn(col);

        col = new PgColumn("name");
        col.setType("text");
        table.addColumn(col);

        AbstractConstraint constraint = new PgConstraint("fax_boxes_pkey");
        table.addConstraint(constraint);
        constraint.setDefinition("PRIMARY KEY (fax_box_id)");

        table.setOwner("postgres");

        table = new SimplePgTable("faxes");
        schema.addTable(table);

        col = new PgColumn("fax_id");
        col.setType("serial");
        col.setNullValue(false);
        table.addColumn(col);

        col = new PgColumn("fax_box_id");
        col.setType("integer");
        table.addColumn(col);

        col = new PgColumn("from_name");
        col.setType("text");
        table.addColumn(col);

        col = new PgColumn("from_number");
        col.setType("text");
        table.addColumn(col);

        col = new PgColumn("status");
        col.setType("integer");
        table.addColumn(col);

        col = new PgColumn("pages");
        col.setType("integer");
        table.addColumn(col);

        col = new PgColumn("time_received");
        col.setType("timestamp");
        col.setDefaultValue("now()");
        table.addColumn(col);

        col = new PgColumn("time_finished_received");
        col.setType("timestamp");
        table.addColumn(col);

        col = new PgColumn("read");
        col.setType("smallint");
        col.setDefaultValue("0");
        table.addColumn(col);

        col = new PgColumn("station_id");
        col.setType("text");
        table.addColumn(col);

        constraint = new PgConstraint("faxes_pkey");
        constraint.setDefinition("PRIMARY KEY (fax_id)");
        table.addConstraint(constraint);

        constraint = new PgConstraint("faxes_fax_box_id_fkey");
        constraint.setDefinition("FOREIGN KEY (fax_box_id)\n      REFERENCES public.fax_boxes (fax_box_id) MATCH SIMPLE\n      ON UPDATE RESTRICT ON DELETE CASCADE");
        table.addConstraint(constraint);

        table = new SimplePgTable("extensions");
        schema.addTable(table);

        col = new PgColumn("id");
        col.setType("serial");
        col.setNullValue(false);
        table.addColumn(col);

        constraint = new PgConstraint("extensions_fax_box_id_fkey");
        constraint.setDefinition("FOREIGN KEY (id) REFERENCES public.fax_boxes\n(fax_box_id)    ON UPDATE RESTRICT ON DELETE RESTRICT");
        table.addConstraint(constraint);

        return d;
    }
}

class PgDB2 implements PgDatabaseObjectCreator {
    @Override
    public PgDatabase getDatabase() {
        PgDatabase d = ApgdiffTestUtils.createDumpDB();

        AbstractSchema schema = new PgSchema("postgis");
        d.addSchema(schema);

        PgExtension ext = new PgExtension("postgis");
        ext.setSchema("postgis");
        d.addExtension(ext);
        ext.setComment("'PostGIS geometry, geography, and raster spatial types and functions'");

        schema = d.getSchema(ApgdiffConsts.PUBLIC);

        AbstractTable table = new SimplePgTable("contacts");
        schema.addTable(table);

        AbstractColumn col = new PgColumn("id");
        col.setType("integer");
        table.addColumn(col);

        col = new PgColumn("number_pool_id");
        col.setType("integer");
        table.addColumn(col);

        col = new PgColumn("name");
        col.setType("character varying(50)");
        table.addColumn(col);

        AbstractIndex idx = new PgIndex("contacts_number_pool_id_idx");
        table.addIndex(idx);
        idx.setDefinition("(number_pool_id)");

        return d;
    }
}

class PgDB3 implements PgDatabaseObjectCreator {
    @Override
    public PgDatabase getDatabase() {
        PgDatabase d = ApgdiffTestUtils.createDumpDB();
        AbstractSchema schema = d.getDefaultSchema();

        AbstractSequence seq = new PgSequence("admins_aid_seq");
        seq.setStartWith("1");
        seq.setMinMaxInc(1L, 1000000000L, null, null, 0L);
        seq.setCache("1");
        schema.addSequence(seq);

        AbstractTable table = new SimplePgTable("admins");
        schema.addTable(table);

        AbstractColumn col = new PgColumn("aid");
        col.setType("integer");
        col.setDefaultValue("nextval('\"admins_aid_seq\"'::regclass)");
        col.setNullValue(false);
        table.addColumn(col);

        col = new PgColumn("companyid");
        col.setType("integer");
        col.setDefaultValue("0");
        col.setNullValue(false);
        table.addColumn(col);

        col = new PgColumn("groupid");
        col.setType("integer");
        col.setDefaultValue("0");
        col.setNullValue(false);
        table.addColumn(col);

        col = new PgColumn("username");
        col.setType("character varying");
        col.setNullValue(false);
        table.addColumn(col);

        col = new PgColumn("password");
        col.setType("character varying(40)");
        col.setNullValue(false);
        table.addColumn(col);

        col = new PgColumn("superuser");
        col.setType("boolean");
        col.setDefaultValue("'f'::bool");
        col.setNullValue(false);
        table.addColumn(col);

        col = new PgColumn("name");
        col.setType("character varying(40)");
        table.addColumn(col);

        col = new PgColumn("surname");
        col.setType("character varying(40)");
        table.addColumn(col);

        col = new PgColumn("email");
        col.setType("character varying(100)");
        col.setNullValue(false);
        table.addColumn(col);

        col = new PgColumn("tel");
        col.setType("character varying(40)");
        table.addColumn(col);

        col = new PgColumn("mobile");
        col.setType("character varying(40)");
        table.addColumn(col);

        col = new PgColumn("enabled");
        col.setType("boolean");
        col.setDefaultValue("'t'::bool");
        col.setNullValue(false);
        table.addColumn(col);

        col = new PgColumn("lastlogints");
        col.setType("timestamp with time zone");
        col.setDefaultValue("now()");
        col.setNullValue(false);
        table.addColumn(col);

        col = new PgColumn("expirienced");
        col.setType("boolean");
        col.setDefaultValue("'f'::bool");
        table.addColumn(col);

        AbstractConstraint constraint = new PgConstraint("admins_pkey");
        constraint.setDefinition("Primary Key (\"aid\")");
        table.addConstraint(constraint);

        return d;
    }
}

class PgDB4 implements PgDatabaseObjectCreator {
    @Override
    public PgDatabase getDatabase() {
        PgDatabase d = ApgdiffTestUtils.createDumpDB();
        AbstractSchema schema = d.getDefaultSchema();

        AbstractTable table = new SimplePgTable("call_logs");
        schema.addTable(table);

        AbstractColumn col = new PgColumn("id");
        col.setType("bigint");
        col.setNullValue(false);
        col.setDefaultValue("nextval('public.call_logs_id_seq'::regclass)");
        table.addColumn(col);

        return d;
    }
}

class PgDB5 implements PgDatabaseObjectCreator {
    @Override
    public PgDatabase getDatabase() {
        PgDatabase d = ApgdiffTestUtils.createDumpDB();
        AbstractSchema schema = d.getDefaultSchema();

        PgFunction func = new PgFunction("gtsq_in");
        func.setLanguageCost("c", null);
        func.setStrict(true);
        func.setBody("'$libdir/tsearch2', 'gtsq_in'");
        func.setReturns("gtsq");

        Argument arg = new Argument(null, "cstring");
        func.addArgument(arg);

        schema.addFunction(func);

        func = new PgFunction("multiply_numbers");
        func.setLanguageCost("plpgsql", null);
        func.setStrict(true);
        func.setBody("$$\r\nbegin\r\n\treturn number1 * number2;\r\nend;\r\n$$");
        func.setReturns("integer");

        arg = new Argument("number1", "integer");
        func.addArgument(arg);

        arg = new Argument("number2", "integer");
        func.addArgument(arg);

        schema.addFunction(func);

        func = new PgFunction("select_something");
        func.setLanguageCost("plpgsql", null);
        func.setBody("$_$SELECT number1 * number2$_$");
        func.setReturns("integer");

        arg = new Argument("number1", "integer");
        func.addArgument(arg);

        arg = new Argument("number2", "integer");
        func.addArgument(arg);

        schema.addFunction(func);

        func = new PgFunction("select_something2");
        func.setLanguageCost("plpgsql", null);
        func.setBody("'SELECT number1 * number2 || ''text'''");
        func.setReturns("integer");

        arg = new Argument("number1", "integer");
        func.addArgument(arg);

        arg = new Argument("number2", "integer");
        func.addArgument(arg);

        schema.addFunction(func);

        func = new PgFunction("select_something3");
        func.setLanguageCost("plpgsql", null);
        func.setBody("'\nSELECT number1 * number2 || ''text''\n'");
        func.setReturns("integer");

        arg = new Argument("number1", "integer");
        func.addArgument(arg);

        arg = new Argument("number2", "integer");
        func.addArgument(arg);

        schema.addFunction(func);

        return d;
    }
}

class PgDB6 implements PgDatabaseObjectCreator {
    @Override
    public PgDatabase getDatabase() {
        PgDatabase d = ApgdiffTestUtils.createDumpDB();
        AbstractSchema schema = d.getDefaultSchema();
        //    schema.setComment("'Standard public schema'");

        schema.addPrivilege(new PgPrivilege("REVOKE", "ALL", "SCHEMA public", "PUBLIC", false));
        schema.addPrivilege(new PgPrivilege("REVOKE", "ALL", "SCHEMA public", "postgres", false));
        schema.addPrivilege(new PgPrivilege("GRANT", "ALL", "SCHEMA public", "postgres", false));
        schema.addPrivilege(new PgPrivilege("GRANT", "ALL", "SCHEMA public", "PUBLIC", false));

        AbstractTable table = new SimplePgTable("test_table");
        schema.addTable(table);

        AbstractColumn col = new PgColumn("id");
        col.setType("bigint");
        table.addColumn(col);

        col = new PgColumn("date_deleted");
        col.setType("timestamp without time zone");
        table.addColumn(col);

        table.setOwner("postgres");

        PgIndex idx = new PgIndex("test_table_deleted");
        idx.setMethod("btree");
        idx.setDefinition("(date_deleted)");
        idx.setWhere("(date_deleted IS NULL)");
        table.addIndex(idx);

        return d;
    }
}

class PgDB7 implements PgDatabaseObjectCreator {
    @Override
    public PgDatabase getDatabase() {
        PgDatabase d = ApgdiffTestUtils.createDumpDB();

        AbstractSchema schema = new PgSchema("common");
        d.addSchema(schema);
        d.setDefaultSchema("common");

        PgFunction func = new PgFunction("t_common_casttotext");
        func.setLanguageCost("sql", null);
        func.setVolatileType("IMMUTABLE");
        func.setStrict(true);
        func.setBody("$_$SELECT textin(timetz_out($1));$_$");
        func.setReturns("text");

        Argument arg = new Argument(null, "time with time zone");
        func.addArgument(arg);

        schema.addFunction(func);

        func = new PgFunction("t_common_casttotext");
        func.setLanguageCost("sql", null);
        func.setVolatileType("IMMUTABLE");
        func.setStrict(true);
        func.setBody("$_$SELECT textin(time_out($1));$_$");
        func.setReturns("text");

        arg = new Argument(null, "time without time zone");
        func.addArgument(arg);

        schema.addFunction(func);

        func = new PgFunction("t_common_casttotext");
        func.setLanguageCost("sql", null);
        func.setVolatileType("IMMUTABLE");
        func.setStrict(true);
        func.setBody("$_$SELECT textin(timestamptz_out($1));$_$");
        func.setReturns("text");

        arg = new Argument(null, "timestamp with time zone");
        func.addArgument(arg);

        schema.addFunction(func);

        func = new PgFunction("t_common_casttotext");
        func.setLanguageCost("sql", null);
        func.setVolatileType("IMMUTABLE");
        func.setStrict(true);
        func.setBody("$_$SELECT textin(timestamp_out($1));$_$");
        func.setReturns("text");

        arg = new Argument(null, "timestamp without time zone");
        func.addArgument(arg);

        schema.addFunction(func);

        return d;
    }
}

class PgDB8 implements PgDatabaseObjectCreator {
    @Override
    public PgDatabase getDatabase() {
        PgDatabase d = ApgdiffTestUtils.createDumpDB();
        AbstractSchema schema = d.getDefaultSchema();
        //    schema.setComment("'Standard public schema'");

        PgType type = new PgType("testtt", PgTypeForm.COMPOSITE);
        AbstractColumn col = new PgColumn("a");
        col.setType("integer");
        type.addAttr(col);
        col = new PgColumn("b");
        col.setType("text");
        type.addAttr(col);
        type.setOwner("madej");
        schema.addType(type);

        schema = new PgSchema("``54'253-=9!@#$%^&*()__<>?:\"{}[];',./");
        d.addSchema(schema);

        PgFunction func = new PgFunction(".x\".\"\".");
        func.setLanguageCost("plpgsql", null);
        func.setBody("$_$\ndeclare\nbegin\nraise notice 'inside: %', $1;\nreturn true;\nend;\n$_$");
        func.setReturns("boolean");

        Argument arg = new Argument(null, "integer");
        func.addArgument(arg);

        schema.addFunction(func);
        func.setOwner("madej");

        return d;
    }
}

class PgDB9 implements PgDatabaseObjectCreator {
    @Override
    public PgDatabase getDatabase() {
        PgDatabase d = ApgdiffTestUtils.createDumpDB();
        AbstractSchema schema = d.getDefaultSchema();

        AbstractTable table = new SimplePgTable("user_data");
        schema.addTable(table);

        AbstractColumn col = new PgColumn("id");
        col.setType("bigint");
        col.setNullValue(false);
        col.setDefaultValue("nextval('public.user_id_seq'::regclass)");
        table.addColumn(col);

        col = new PgColumn("email");
        col.setType("character varying(128)");
        col.setNullValue(false);
        table.addColumn(col);

        col = new PgColumn("created");
        col.setType("timestamp with time zone");
        col.setDefaultValue("now()");
        table.addColumn(col);
        table.setOwner("postgres");

        PgRule rule = new PgRule("on_select");
        rule.setEvent(PgRuleEventType.SELECT);
        rule.setCondition("(1=1)");
        rule.setInstead(true);
        table.addRule(rule);

        PgSequence seq = new PgSequence("user_id_seq");
        seq.setMinMaxInc(1L, null, null, null, 0L);
        seq.setCache("1");
        seq.setOwnedBy("public.user_data.id");
        schema.addSequence(seq);
        seq.setOwner("postgres");

        table = new SimplePgTable("t1");
        schema.addTable(table);

        col = new PgColumn("c1");
        col.setType("integer");
        table.addColumn(col);

        PgView view = new PgView("user");
        view.setQuery("( SELECT user_data.id, user_data.email, user_data.created FROM public.user_data)");
        view.addColumnDefaultValue("created", "now()");
        schema.addView(view);

        view.setOwner("postgres");

        rule = new PgRule("on_delete");
        rule.setEvent(PgRuleEventType.DELETE);
        rule.addCommand("DELETE FROM public.user_data WHERE (user_data.id = old.id)");
        view.addRule(rule);

        rule = new PgRule("on_insert");
        rule.setEvent(PgRuleEventType.INSERT);
        rule.setInstead(true);
        rule.addCommand("INSERT INTO public.user_data (id, email, created) VALUES (new.id, new.email, new.created)");
        rule.addCommand("INSERT INTO public.t1(c1) DEFAULT VALUES");
        view.addRule(rule);

        rule = new PgRule("on_update");
        rule.setEvent(PgRuleEventType.UPDATE);
        rule.setInstead(true);
        rule.addCommand("UPDATE public.user_data SET id = new.id, email = new.email, created = new.created WHERE (user_data.id = old.id)");
        view.addRule(rule);

        view = new PgView("ws_test");
        view.setQuery("SELECT ud.id \"   i   d   \" FROM public.user_data ud");
        schema.addView(view);

        return d;
    }
}

class PgDB10 implements PgDatabaseObjectCreator {
    @Override
    public PgDatabase getDatabase() {
        PgDatabase d = ApgdiffTestUtils.createDumpDB();
        AbstractSchema schema = new PgSchema("admin");
        d.addSchema(schema);
        d.setDefaultSchema("admin");

        schema.setOwner("postgres");

        AbstractTable table = new SimplePgTable("acl_role");
        schema.addTable(table);

        AbstractColumn col = new PgColumn("id");
        col.setType("bigint");
        col.setNullValue(false);
        table.addColumn(col);

        AbstractConstraint constraint = new PgConstraint("acl_role_pkey");
        constraint.setDefinition("PRIMARY KEY (id)");
        table.addConstraint(constraint);

        table.setOwner("postgres");

        table = new SimplePgTable("user");
        schema.addTable(table);

        col = new PgColumn("id");
        col.setType("bigint");
        col.setNullValue(false);
        table.addColumn(col);

        col = new PgColumn("email");
        col.setType("character varying(255)");
        col.setNullValue(false);
        table.addColumn(col);

        col = new PgColumn("name");
        col.setType("character varying(255)");
        col.setNullValue(false);
        table.addColumn(col);

        col = new PgColumn("password");
        col.setType("character varying(40)");
        col.setNullValue(false);
        table.addColumn(col);

        col = new PgColumn("is_active");
        col.setType("boolean");
        col.setDefaultValue("false");
        col.setNullValue(false);
        table.addColumn(col);

        col = new PgColumn("updated");
        col.setType("timestamp without time zone");
        col.setDefaultValue("now()");
        col.setNullValue(false);
        table.addColumn(col);

        col = new PgColumn("created");
        col.setType("timestamp without time zone");
        col.setDefaultValue("now()");
        col.setNullValue(false);
        table.addColumn(col);

        col = new PgColumn("role_id");
        col.setType("bigint");
        col.setNullValue(false);
        table.addColumn(col);

        col = new PgColumn("last_visit");
        col.setType("timestamp without time zone");
        col.setDefaultValue("now()");
        col.setNullValue(false);
        table.addColumn(col);

        PgIndex idx = new PgIndex("fki_user_role_id_fkey");
        idx.setMethod("btree");
        idx.setDefinition("(role_id)");
        table.addIndex(idx);

        constraint = new PgConstraint("user_role_id_fkey");
        constraint.setDefinition("FOREIGN KEY (role_id) REFERENCES admin.acl_role(id)");
        table.addConstraint(constraint);

        table.setOwner("postgres");

        return d;
    }
}

class PgDB11 implements PgDatabaseObjectCreator {
    @Override
    public PgDatabase getDatabase() {
        PgDatabase d = ApgdiffTestUtils.createDumpDB();
        AbstractSchema schema = d.getDefaultSchema();

        PgFunction func = new PgFunction("curdate");
        func.setLanguageCost("sql", null);
        func.setBody("$$SELECT CAST('now' AS date);\n$$");
        func.setReturns("date");
        schema.addFunction(func);

        return d;
    }
}

class PgDB12 implements PgDatabaseObjectCreator {
    @Override
    public PgDatabase getDatabase() {
        PgDatabase d = ApgdiffTestUtils.createDumpDB();

        // d.setComment("'The status : ''E'' for enabled, ''D'' for disabled.'");

        return d;
    }
}

class PgDB13 implements PgDatabaseObjectCreator {
    @Override
    public PgDatabase getDatabase() {
        PgDatabase d = ApgdiffTestUtils.createDumpDB();
        AbstractSchema schema = d.getDefaultSchema();

        PgFunction func = new PgFunction("drop_fk_except_for");
        func.setLanguageCost("plpgsql", null);
        func.setBody("$$\nDECLARE\nBEGIN\n  -- aaa\nEND;\n$$");
        func.setReturns("SETOF character varying");

        Argument arg = new Argument("tables_in", "character varying[]");
        func.addArgument(arg);

        schema.addFunction(func);

        return d;
    }
}

class PgDB14 implements PgDatabaseObjectCreator {
    @Override
    public PgDatabase getDatabase() {
        PgDatabase d = ApgdiffTestUtils.createDumpDB();
        AbstractSchema schema = d.getDefaultSchema();

        schema.addPrivilege(new PgPrivilege("REVOKE", "ALL", "SCHEMA public", "PUBLIC", false));
        schema.addPrivilege(new PgPrivilege("REVOKE", "ALL", "SCHEMA public", "postgres", false));
        schema.addPrivilege(new PgPrivilege("GRANT", "ALL", "SCHEMA public", "postgres", false));
        schema.addPrivilege(new PgPrivilege("GRANT", "ALL", "SCHEMA public", "PUBLIC", false));

        // d.setComment("'comments database'");
        //    schema.setComment("'public schema'");

        PgFunction func = new PgFunction("test_fnc");
        func.setLanguageCost("plpgsql", null);
        func.setBody("$$BEGIN\nRETURN true;\nEND;$$");
        func.setReturns("boolean");

        Argument arg = new Argument("arg", "character varying");
        func.addArgument(arg);

        func.setOwner("fordfrog");

        func.setComment("'test function'");

        schema.addFunction(func);

        func = new PgFunction("trigger_fnc");
        func.setLanguageCost("plpgsql", null);
        func.setBody("$$begin\nend;$$");
        func.setReturns("trigger");
        schema.addFunction(func);

        func.setOwner("fordfrog");

        AbstractTable table = new SimplePgTable("test");
        schema.addTable(table);

        AbstractColumn col = new PgColumn("id");
        col.setType("integer");
        col.setNullValue(false);
        col.setComment("'id column'");
        col.setDefaultValue("nextval('public.test_id_seq'::regclass)");
        table.addColumn(col);

        col = new PgColumn("text");
        col.setType("character varying(20)");
        col.setNullValue(false);
        col.setComment("'text column'");
        table.addColumn(col);

        AbstractConstraint constraint = new PgConstraint("text_check");
        constraint.setDefinition("CHECK ((length((text)::text) > 0))");
        constraint.setComment("'text check'");
        table.addConstraint(constraint);

        table.setComment("'test table'");

        constraint = new PgConstraint("test_pkey");
        constraint.setDefinition("PRIMARY KEY (id)");
        table.addConstraint(constraint);

        constraint.setComment("'primary key'");

        table.setOwner("fordfrog");

        PgSequence seq = new PgSequence("test_id_seq");
        seq.setStartWith("1");
        seq.setMinMaxInc(1L, null, null, null, 0L);
        seq.setCache("1");
        schema.addSequence(seq);

        seq.setOwnedBy("public.test.id");

        seq.setOwner("fordfrog");

        seq.setComment("'test table sequence'");

        PgView view = new PgView("test_view");
        view.setQuery("SELECT test.id, test.text FROM public.test");
        schema.addView(view);

        view.setComment("'test view'");
        view.addColumnComment("id", "'view id col'");

        view.setOwner("fordfrog");

        PgTrigger trigger = new PgTrigger("test_trigger");
        trigger.setType(TgTypes.BEFORE);
        trigger.setOnUpdate(true);
        trigger.setForEachRow(false);
        trigger.setFunction("public.trigger_fnc()");
        table.addTrigger(trigger);

        trigger.setComment("'test trigger'");

        return d;
    }
}

class PgDB15 implements PgDatabaseObjectCreator {
    @Override
    public PgDatabase getDatabase() {
        PgDatabase d = ApgdiffTestUtils.createDumpDB();
        AbstractSchema schema = d.getDefaultSchema();

        AbstractTable table = new SimplePgTable("test");
        schema.addTable(table);

        AbstractColumn col = new PgColumn("id");
        col.setType("bigint");
        table.addColumn(col);

        table.setComment("'multiline\ncomment\n'");

        return d;
    }
}

/**
 * Tests subselect parser
 *
 * @author ryabinin_av
 *
 */
class PgDB16 implements PgDatabaseObjectCreator {
    @Override
    public PgDatabase getDatabase() {
        PgDatabase d = ApgdiffTestUtils.createDumpDB();
        AbstractSchema schema = d.getDefaultSchema();

        // table1
        AbstractTable table = new SimplePgTable("t_work");
        schema.addTable(table);

        AbstractColumn col = new PgColumn("id");
        col.setType("integer");
        table.addColumn(col);

        // table2
        AbstractTable table2 = new SimplePgTable("t_chart");
        schema.addTable(table2);
        col = new PgColumn("id");
        col.setType("integer");
        table2.addColumn(col);

        // view
        PgView view = new PgView("v_subselect");
        view.setQuery("SELECT c.id, t.id FROM ( SELECT t_work.id FROM public.t_work) t"
                + " JOIN public.t_chart c ON t.id = c.id");
        schema.addView(view);

        return d;
    }
}

/**
 * Tests subselect parser with double subselect
 *
 * @author ryabinin_av
 *
 */
class PgDB17 implements PgDatabaseObjectCreator {
    @Override
    public PgDatabase getDatabase() {
        PgDatabase d = ApgdiffTestUtils.createDumpDB();
        AbstractSchema schema = d.getDefaultSchema();

        // table1
        AbstractTable table = new SimplePgTable("t_work");
        schema.addTable(table);

        AbstractColumn col = new PgColumn("id");
        col.setType("integer");
        table.addColumn(col);

        // table2
        AbstractTable table2 = new SimplePgTable("t_chart");
        schema.addTable(table2);
        col = new PgColumn("id");
        col.setType("integer");
        table2.addColumn(col);

        // table 3
        AbstractTable table3 = new SimplePgTable("t_memo");
        schema.addTable(table3);
        col = new PgColumn("name");
        col.setType("text");
        table3.addColumn(col);

        // view
        PgView view = new PgView("v_subselect");
        view.setQuery("SELECT c.id, t.id AS second, t.name\n" +
                "   FROM (( SELECT w.id, m.name FROM (( SELECT t_work.id FROM public.t_work) w\n" +
                "             JOIN public.t_memo m ON (((w.id)::text = m.name)))) t\n" +
                "     JOIN public.t_chart c ON ((t.id = c.id)))");
        schema.addView(view);

        return d;
    }
}

// SONAR-ON
