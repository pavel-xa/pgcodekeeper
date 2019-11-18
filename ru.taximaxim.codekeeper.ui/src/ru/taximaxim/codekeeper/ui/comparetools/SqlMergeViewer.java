package ru.taximaxim.codekeeper.ui.comparetools;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.widgets.Composite;

import ru.taximaxim.codekeeper.ui.localizations.Messages;
import ru.taximaxim.codekeeper.ui.sqledit.SqlSourceViewer;

public class SqlMergeViewer extends TextMergeViewer {

    public SqlMergeViewer(Composite parent, int style, CompareConfiguration conf) {
        super(parent, style, conf);
        // add initial input in order to avoid problems when disposing the viewer later
        updateContent(null, null, null);
    }

    @Override
    protected void configureTextViewer(TextViewer textViewer) {
        // viewer configures itself
    }

    @Override
    protected SourceViewer createSourceViewer(Composite parent,
            int textOrientation) {
        return new SqlSourceViewer(parent, textOrientation);
    }

    @Override
    public String getTitle() {
        return Messages.SqlMergeViewer_compare_label;
    }
}
