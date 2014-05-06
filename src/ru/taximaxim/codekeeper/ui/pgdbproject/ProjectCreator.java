package ru.taximaxim.codekeeper.ui.pgdbproject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;

import ru.taximaxim.codekeeper.apgdiff.model.exporter.ModelExporter;
import ru.taximaxim.codekeeper.ui.Log;
import ru.taximaxim.codekeeper.ui.UIConsts;
import ru.taximaxim.codekeeper.ui.differ.DbSource;
import ru.taximaxim.codekeeper.ui.externalcalls.IRepoWorker;
import ru.taximaxim.codekeeper.ui.externalcalls.JGitExec;
import ru.taximaxim.codekeeper.ui.fileutils.Dir;
import ru.taximaxim.codekeeper.ui.fileutils.TempDir;
import cz.startnet.utils.pgdiff.schema.PgDatabase;

// TODO log new creation process, log more
public class ProjectCreator implements IRunnableWithProgress {

    final private String exePgdump;

    final private PgDbProject props;

    final private String dumpPath;

    final private boolean doInit;

    private String repoName = "";

    private final IPreferenceStore mainPrefStore;
    
    public ProjectCreator(final IPreferenceStore mainPrefStore,
            final PgDbProject props, final String dumpPath, boolean doInit) {
        this.mainPrefStore = mainPrefStore;
        this.exePgdump = mainPrefStore.getString(UIConsts.PREF_PGDUMP_EXE_PATH);
        this.props = props;
        this.dumpPath = dumpPath;
        this.doInit = doInit;
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException {
        try {
            Log.log(Log.LOG_INFO, "Creating project at " + props.getProjectFile());
            
            int workToDo = doInit ? 100 : 1;
            SubMonitor pm = SubMonitor.convert(monitor, "Creating project...",
                    workToDo); // 0

            if (doInit) {
                IRepoWorker repo = new JGitExec(props, 
                        mainPrefStore.getString(UIConsts.PREF_GIT_KEY_PRIVATE_FILE));
                repoName = UIConsts.PROJ_REPO_TYPE_GIT_NAME;
                initRepoFromSource(pm, repo);
            }
            monitor.done();
        } catch (IOException ex) {
            throw new InvocationTargetException(ex,
                    "IOException while creating project!");
        }
    }

    /**
     * clean repository, generate new file structure, preserve and fix repo
     * metadata, repo rm/add, commit new revision
     * 
     * @param pm
     * @param repo
     * @throws IOException
     * @throws InvocationTargetException
     */

    private void initRepoFromSource(SubMonitor pm, IRepoWorker repo)
            throws IOException, InvocationTargetException {
        File dirRepo = props.getProjectWorkingDir();
        SubMonitor taskpm = pm.newChild(25); // 50

        PgDatabase db;
        switch (props.getString(UIConsts.PROJ_PREF_SOURCE)) {
        case UIConsts.PROJ_SOURCE_TYPE_DB:
            db = DbSource.fromDb(exePgdump, props).get(taskpm);
            break;

        case UIConsts.PROJ_SOURCE_TYPE_DUMP:
            db = DbSource.fromFile(dumpPath,
                    props.getString(UIConsts.PROJ_PREF_ENCODING)).get(taskpm);
            break;

        default:
            throw new InvocationTargetException(new IllegalStateException(
                    "Init requested but no Schema Source"));
        }

        pm.newChild(25).subTask("Exporting DB model..."); // 75

        try (TempDir tmpRepoMeta = new TempDir(
                props.getProjectWorkingDir().toPath().getParent(),
                "tmp_repo_meta_")) {
            File repoMetaProj = new File(props.getRepoRoot(), repo.getRepoMetaFolder());
            File repoMetaTmp = new File(tmpRepoMeta.get(), repo.getRepoMetaFolder());
            Files.move(repoMetaProj.toPath(), repoMetaTmp.toPath());
            Dir.deleteRecursive(dirRepo);

            new ModelExporter(dirRepo.getAbsolutePath(), db,
                    props.getString(UIConsts.PROJ_PREF_ENCODING)).export();

            Files.move(repoMetaTmp.toPath(), repoMetaProj.toPath());
        }

        pm.newChild(25).subTask(repoName + " committing..."); // 100
        repo.repoRemoveMissingAddNew(dirRepo);
        repo.repoCommit(dirRepo, "new rev");
    }
}