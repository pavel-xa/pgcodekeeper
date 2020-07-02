package ru.taximaxim.codekeeper.ui.views.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.navigator.IDescriptionProvider;

import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;
import ru.taximaxim.codekeeper.ui.Activator;
import ru.taximaxim.codekeeper.ui.sqledit.SegmentsWithParent;

public class NavigatorOutlineLabelProvider extends LabelProvider
implements IDescriptionProvider {

    @Override
    public String getDescription(Object anElement) {
        if (anElement instanceof SegmentsWithParent) {
            SegmentsWithParent seg = (SegmentsWithParent) anElement;
            return seg + " (" + seg.getType() + ')'; //$NON-NLS-1$
        }
        return null;
    }

    @Override
    public String getText(Object element) {
        if (element instanceof IFile) {
            return null;
        }
        return super.getText(element);
    }

    @Override
    public Image getImage(Object element) {
        if (element instanceof SegmentsWithParent) {
            SegmentsWithParent seg = (SegmentsWithParent) element;
            DbObjType type = seg.getType();
            return type != null ? Activator.getDbObjImage(type)
                    : Activator.getEclipseImage(ISharedImages.IMG_OBJ_FILE);
        }
        return super.getImage(element);
    }
}
