package ru.taximaxim.codekeeper.ui.sqledit;

import java.util.List;
import java.util.Optional;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.DefaultTextHover;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.EditorsUI;

import cz.startnet.utils.pgdiff.schema.PgObjLocation;
import cz.startnet.utils.pgdiff.schema.meta.MetaStatement;
import ru.taximaxim.codekeeper.ui.pgdbproject.parser.PgDbParser;

final class SQLEditorTextHover extends DefaultTextHover implements ITextHoverExtension  {

    private static final String QUICKDIFF = "org.eclipse.ui.workbench.texteditor.quickdiff"; //$NON-NLS-1$

    private final SQLEditor editor;

    public SQLEditorTextHover(ISourceViewer sourceViewer, SQLEditor editor) {
        super(sourceViewer);
        this.editor = editor;
    }

    @Override
    public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
        PgDbParser parser = editor.getParser();
        List<PgObjLocation> refs = parser.getObjsForEditor(editor.getEditorInput());
        for (PgObjLocation obj : refs) {
            if (offset > obj.getOffset()
                    && offset < (obj.getOffset() + obj.getObjLength())) {
                Optional<MetaStatement> loc = parser.getDefinitionsForObj(obj).findAny();
                if (loc.isPresent()) {
                    SQLEditorMyRegion region = new SQLEditorMyRegion(obj.getOffset(), obj.getObjLength());
                    region.setComment(loc.get().getComment());
                    return region;
                }
            }
        }
        return new Region(offset, 0);
    }

    @Override
    public IInformationControlCreator getHoverControlCreator() {
        return parent -> new DefaultInformationControl(parent, EditorsUI.getTooltipAffordanceString());
    }

    @Override
    protected boolean isIncluded(Annotation annotation) {
        // exclude text change annotations
        return !annotation.getType().startsWith(QUICKDIFF);
    }

    @Override
    public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
        @SuppressWarnings("deprecation")
        String msg = super.getHoverInfo(textViewer, hoverRegion);
        if (msg != null) {
            return msg;
        }
        if (hoverRegion instanceof SQLEditorMyRegion) {
            return ((SQLEditorMyRegion) hoverRegion).getComment();
        }
        return "";  //$NON-NLS-1$
    }
}