package ru.taximaxim.codekeeper.ui.consoles;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;

import cz.startnet.utils.pgdiff.IProgressReporter;

public class UiProgressReporter implements IProgressReporter {

    private final CodekeeperConsole console;

    public UiProgressReporter(IProgressMonitor monitor) {
        IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
        console = CodekeeperConsole.createInstance(monitor);
        manager.addConsoles(new IConsole[] { console });
    }

    @Override
    public void writeMessage(String message) {
        console.write(message);
    }

    @Override
    public void writeWarning(String message) {
        console.writeWarning("WARNING: " + message);
    }

    @Override
    public void writeError(String message) {
        console.writeError(message);
    }

    @Override
    public void terminate() {
        console.terminate();
    }
}