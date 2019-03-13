package ru.taximaxim.codekeeper.ui.sqledit;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Image;

import cz.startnet.utils.pgdiff.PgDiffUtils;
import cz.startnet.utils.pgdiff.schema.PgObjLocation;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;
import ru.taximaxim.codekeeper.apgdiff.sql.Keyword;
import ru.taximaxim.codekeeper.ui.Activator;
import ru.taximaxim.codekeeper.ui.Log;
import ru.taximaxim.codekeeper.ui.pgdbproject.parser.PgDbParser;

public class SQLEditorCompletionProcessor implements IContentAssistProcessor {

    private final SQLEditor editor;
    private final List<String> keywords;

    public SQLEditorCompletionProcessor(SQLEditor editor) {
        this.editor = editor;
        keywords = Keyword.KEYWORDS.keySet().stream()
                .sorted()
                .map(s -> s.toUpperCase(Locale.ROOT))
                .collect(Collectors.toList());
    }

    @Override
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
            int offset) {
        String part;
        try {
            part = viewer.getDocument().get(0, offset);
        } catch (BadLocationException ex) {
            Log.log(Log.LOG_ERROR, "Document doesn't contain such offset", ex); //$NON-NLS-1$
            return null;
        }
        int nonid = offset - 1;
        while (nonid > 0 && PgDiffUtils.isValidIdChar(part.charAt(nonid))) {
            --nonid;
        }
        String text = part.substring(nonid + 1, offset);

        List<ICompletionProposal> result = new LinkedList<>();
        List<ICompletionProposal> partResult = new LinkedList<>();

        PgDbParser parser = editor.getParser();
        Stream<PgObjLocation> loc = parser.getAllObjDefinitions();
        loc
        .filter(o -> text.isEmpty() || o.getObjName().startsWith(text))
        .filter(o -> o.type != DbObjType.SEQUENCE && o.type != DbObjType.INDEX)
        .sorted((o1, o2) -> o1.getFilePath().compareTo(o2.getFilePath()))
        .forEach(obj -> {
            Image img = Activator.getDbObjImage(obj.type);
            String displayText = obj.getObjName();
            if (!obj.getComment().isEmpty()) {
                displayText += " - " + obj.getComment(); //$NON-NLS-1$
            }
            IContextInformation info = new ContextInformation(
                    obj.getObjName(), obj.getComment());
            if (!text.isEmpty()) {
                result.add(new CompletionProposal(obj.getObjName(), offset - text.length(),
                        text.length(), obj.getObjLength(), img, displayText, info,
                        obj.getObjName()));
            } else {
                result.add(new CompletionProposal(obj.getObjName(), offset, 0,
                        obj.getObjLength(), img, displayText, info, obj.getObjName()));
            }
        });

        // SQL Templates + Keywords
        if (text.isEmpty()) {
            keywords.forEach(k -> result.add(new CompletionProposal(k, offset, 0, k.length())));
            result.addAll(new SQLEditorTemplateAssistProcessor().getAllTemplates(viewer, offset));
        } else {
            String textUpper = text.toUpperCase(Locale.ROOT);
            for (String keyword : keywords) {
                int location = keyword.indexOf(textUpper);
                if (location != -1) {
                    CompletionProposal proposal = new CompletionProposal(keyword + ' ',
                            offset - text.length(), text.length(), keyword.length() + 1);
                    if (location  == 0) {
                        result.add(proposal);
                    } else {
                        partResult.add(proposal);
                    }
                }
            }

            result.addAll(partResult);

            ICompletionProposal[] templates = new SQLEditorTemplateAssistProcessor()
                    .computeCompletionProposals(viewer, offset);
            if (templates != null) {
                result.addAll(Arrays.asList(templates));
            }
        }

        return result.toArray(new ICompletionProposal[result.size()]);
    }

    @Override
    public IContextInformation[] computeContextInformation(ITextViewer viewer,
            int offset) {
        return null;
    }

    @Override
    public char[] getCompletionProposalAutoActivationCharacters() {
        return new char[] { '.', '(' };
    }

    @Override
    public char[] getContextInformationAutoActivationCharacters() {
        return new char[] { '#' };
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    @Override
    public IContextInformationValidator getContextInformationValidator() {
        return null;
    }
}
