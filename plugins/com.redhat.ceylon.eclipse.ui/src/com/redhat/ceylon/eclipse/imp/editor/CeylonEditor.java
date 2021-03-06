package com.redhat.ceylon.eclipse.imp.editor;

import static org.eclipse.imp.editor.IEditorActionDefinitionIds.SHOW_OUTLINE;

import java.lang.reflect.Field;
import java.text.BreakIterator;
import java.text.CharacterIterator;

import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.imp.editor.GenerateActionGroup;
import org.eclipse.imp.editor.OpenEditorActionGroup;
import org.eclipse.imp.editor.OutlineInformationControl;
import org.eclipse.imp.editor.OutlineLabelProvider;
import org.eclipse.imp.editor.StructuredSourceViewerConfiguration;
import org.eclipse.imp.editor.UniversalEditor;
import org.eclipse.imp.services.base.TreeModelBuilderBase;
import org.eclipse.imp.ui.DefaultPartListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.text.AbstractInformationControlManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.IInformationProviderExtension;
import org.eclipse.jface.text.information.InformationPresenter;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.texteditor.TextNavigationAction;

import com.redhat.ceylon.eclipse.imp.outline.CeylonLabelDecorator;
import com.redhat.ceylon.eclipse.imp.outline.CeylonTreeModelBuilder;
import com.redhat.ceylon.eclipse.imp.parser.CeylonParseController;

public class CeylonEditor extends UniversalEditor {
    private static Field refreshContributionsField;
    private static Field generateActionGroupField;
    private static Field openEditorActionGroupField;
    private static Field labelProviderField;
    static {
        try {
            refreshContributionsField = UniversalEditor.class.getDeclaredField("fRefreshContributions");
            refreshContributionsField.setAccessible(true);
            generateActionGroupField = UniversalEditor.class.getDeclaredField("fGenerateActionGroup");
            generateActionGroupField.setAccessible(true);
            openEditorActionGroupField = UniversalEditor.class.getDeclaredField("fOpenEditorActionGroup");
            openEditorActionGroupField.setAccessible(true);
            labelProviderField = OutlineInformationControl.class.getDeclaredField("fInnerLabelProvider");
            labelProviderField.setAccessible(true);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final TreeModelBuilderBase builder = new CeylonTreeModelBuilder();

    private class OutlineInformationProvider implements IInformationProvider, IInformationProviderExtension {
        public IRegion getSubject(ITextViewer textViewer, int offset) {
            return new Region(offset, 0); // Could be anything, since it's ignored below in getInformation2()...
        }
        public String getInformation(ITextViewer textViewer, IRegion subject) {
            // shouldn't be called, given IInformationProviderExtension???
            throw new UnsupportedOperationException();
        }
        public Object getInformation2(ITextViewer textViewer, IRegion subject) {
            return builder.buildTree(getParseController().getCurrentAst());
        }
    }

    private IInformationControlCreator getOutlinePresenterControlCreator(ISourceViewer sourceViewer, final String commandId) {
        return new IInformationControlCreator() {
            @Override
            public IInformationControl createInformationControl(Shell parent) {
                return new OutlineInformationControl(parent, SWT.RESIZE, SWT.V_SCROLL | SWT.H_SCROLL, commandId, getLanguage()) {
                    @Override
                    protected TreeViewer createTreeViewer(Composite parent, int style) {
                        TreeViewer tv = super.createTreeViewer(parent, style);
                        try {
                            OutlineLabelProvider lp = (OutlineLabelProvider) labelProviderField.get(this);
                            lp.addLabelDecorator(new CeylonLabelDecorator(getLanguage()));
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        return tv;
                    }
                    @Override
                    protected String getStatusFieldText() {
                        return "";
                    }
                };
            }
        };
    }

    @Override
    protected StructuredSourceViewerConfiguration createSourceViewerConfiguration() {
        return new StructuredSourceViewerConfiguration(getPreferenceStore(), this) {
            @Override
            public IInformationPresenter getOutlinePresenter(ISourceViewer sourceViewer) {
                InformationPresenter presenter = new InformationPresenter(getOutlinePresenterControlCreator(sourceViewer, SHOW_OUTLINE));
                presenter.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
                presenter.setAnchor(AbstractInformationControlManager.ANCHOR_GLOBAL);
                presenter.setInformationProvider(new OutlineInformationProvider(), IDocument.DEFAULT_CONTENT_TYPE);
                // TODO Should associate all other partition types with this provider, too
                //presenter.setSizeConstraints(50, 20, true, false);
                //presenter.setRestoreInformationControlBounds(getSettings("outline_presenter_bounds"), true, true);
                return presenter;
            }
        };
    }
        
    /*private IDialogSettings getSettings(String sectionName) {
        IDialogSettings settings= RuntimePlugin.getInstance().getDialogSettings().getSection(sectionName);
        if (settings == null)
            settings= RuntimePlugin.getInstance().getDialogSettings().addNewSection(sectionName);
        return settings;
    }*/

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        try {
            getSite().getPage().removePartListener((DefaultPartListener) refreshContributionsField.get(this));
            generateActionGroupField.set(this, new CeylonGenerateActionGroup(this));
            openEditorActionGroupField.set(this, new CeylonOpenEditorActionGroup(this));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        getSite().getPage().hideActionSet(IMP_CODING_ACTION_SET);
        getSite().getPage().hideActionSet(IMP_OPEN_ACTION_SET);
        ((IContextService) getSite().getService(IContextService.class))
            .activateContext("com.redhat.ceylon.eclipse.ui.context");
    }
    
    class CeylonGenerateActionGroup extends GenerateActionGroup {
        public CeylonGenerateActionGroup(UniversalEditor editor) {
            super(editor, "");
        }
        @Override
        public void fillContextMenu(IMenuManager menu) {}
    }
    
    class CeylonOpenEditorActionGroup extends OpenEditorActionGroup {
        public CeylonOpenEditorActionGroup(UniversalEditor editor) {
            super(editor);
        }
        @Override
        public void fillContextMenu(IMenuManager menu) {}
    }
    
    private SourceArchiveDocumentProvider sourceArchiveDocumentProvider;
    private Object toggleBreakpointTarget;
    
    @Override
    public IDocumentProvider getDocumentProvider() {
        if (SourceArchiveDocumentProvider.canHandle(getEditorInput())) {
            if (sourceArchiveDocumentProvider == null) {
                sourceArchiveDocumentProvider= new SourceArchiveDocumentProvider();
            }
            return sourceArchiveDocumentProvider;
        }
        return super.getDocumentProvider();
    }

    public CeylonParseController getParseController() {
        return (CeylonParseController) super.getParseController();
    }
    
    @Override
    protected void createNavigationActions() {
        super.createNavigationActions();
        
        final StyledText textWidget= getSourceViewer().getTextWidget();

        /*IAction action= new SmartLineStartAction(textWidget, false);
        action.setActionDefinitionId(ITextEditorActionDefinitionIds.LINE_START);
        editor.setAction(ITextEditorActionDefinitionIds.LINE_START, action);

        action= new SmartLineStartAction(textWidget, true);
        action.setActionDefinitionId(ITextEditorActionDefinitionIds.SELECT_LINE_START);
        editor.setAction(ITextEditorActionDefinitionIds.SELECT_LINE_START, action);*/

        setAction(ITextEditorActionDefinitionIds.WORD_PREVIOUS, 
                new NavigatePreviousSubWordAction());
        textWidget.setKeyBinding(SWT.CTRL | SWT.ARROW_LEFT, SWT.NULL);

        setAction(ITextEditorActionDefinitionIds.WORD_NEXT, 
                new NavigateNextSubWordAction());
        textWidget.setKeyBinding(SWT.CTRL | SWT.ARROW_RIGHT, SWT.NULL);

        setAction(ITextEditorActionDefinitionIds.SELECT_WORD_PREVIOUS, 
                new SelectPreviousSubWordAction());
        textWidget.setKeyBinding(SWT.CTRL | SWT.SHIFT | SWT.ARROW_LEFT, SWT.NULL);

        setAction(ITextEditorActionDefinitionIds.SELECT_WORD_NEXT, 
                new SelectNextSubWordAction());
        textWidget.setKeyBinding(SWT.CTRL | SWT.SHIFT | SWT.ARROW_RIGHT, SWT.NULL);

        setAction(ITextEditorActionDefinitionIds.DELETE_PREVIOUS_WORD, new DeletePreviousSubWordAction());
        textWidget.setKeyBinding(SWT.CTRL | SWT.BS, SWT.NULL);
        markAsStateDependentAction(ITextEditorActionDefinitionIds.DELETE_PREVIOUS_WORD, true);

        setAction(ITextEditorActionDefinitionIds.DELETE_NEXT_WORD, new DeleteNextSubWordAction());
        textWidget.setKeyBinding(SWT.CTRL | SWT.DEL, SWT.NULL);
        markAsStateDependentAction(ITextEditorActionDefinitionIds.DELETE_NEXT_WORD, true);
    }
    
    protected class NavigateNextSubWordAction extends NextSubWordAction {
        public NavigateNextSubWordAction() {
            super(ST.WORD_NEXT);
            setActionDefinitionId(ITextEditorActionDefinitionIds.WORD_NEXT);
        }
        @Override
        protected void setCaretPosition(final int position) {
            getTextWidget().setCaretOffset(modelOffset2WidgetOffset(getSourceViewer(), position));
        }
    }

    protected class NavigatePreviousSubWordAction extends PreviousSubWordAction {
        public NavigatePreviousSubWordAction() {
            super(ST.WORD_PREVIOUS);
            setActionDefinitionId(ITextEditorActionDefinitionIds.WORD_PREVIOUS);
        }
        @Override
        protected void setCaretPosition(final int position) {
            getTextWidget().setCaretOffset(modelOffset2WidgetOffset(getSourceViewer(), position));
        }
    }

    protected abstract class NextSubWordAction extends TextNavigationAction {

        protected CeylonWordIterator fIterator= new CeylonWordIterator();

        /**
         * Creates a new next sub-word action.
         *
         * @param code Action code for the default operation. Must be an action code from @see org.eclipse.swt.custom.ST.
         */
        protected NextSubWordAction(int code) {
            super(getSourceViewer().getTextWidget(), code);
        }

        /*
         * @see org.eclipse.jface.action.IAction#run()
         */
        @Override
        public void run() {
            // Check whether we are in a java code partition and the preference is enabled

            final ISourceViewer viewer= getSourceViewer();
            final IDocument document= viewer.getDocument();
            try {
                fIterator.setText((CharacterIterator)new DocumentCharacterIterator(document));
                int position= widgetOffset2ModelOffset(viewer, viewer.getTextWidget().getCaretOffset());
                if (position == -1)
                    return;

                int next= findNextPosition(position);
                if (next != BreakIterator.DONE) {
                    setCaretPosition(next);
                    getTextWidget().showSelection();
                    fireSelectionChanged();
                }
            } catch (BadLocationException x) {
                // ignore
            }
        }
        
        /**
         * Finds the next position after the given position.
         *
         * @param position the current position
         * @return the next position
         */
        protected int findNextPosition(int position) {
            ISourceViewer viewer= getSourceViewer();
            int widget= -1;
            int next= position;
            while (next != BreakIterator.DONE && widget == -1) { // XXX: optimize
                next= fIterator.following(next);
                if (next != BreakIterator.DONE)
                    widget= modelOffset2WidgetOffset(viewer, next);
            }

            IDocument document= viewer.getDocument();
            LinkedModeModel model= LinkedModeModel.getModel(document, position);
            if (model != null) {
                LinkedPosition linkedPosition= model.findPosition(new LinkedPosition(document, position, 0));
                if (linkedPosition != null) {
                    int linkedPositionEnd= linkedPosition.getOffset() + linkedPosition.getLength();
                    if (position != linkedPositionEnd && linkedPositionEnd < next)
                        next= linkedPositionEnd;
                } else {
                    LinkedPosition nextLinkedPosition= model.findPosition(new LinkedPosition(document, next, 0));
                    if (nextLinkedPosition != null) {
                        int nextLinkedPositionOffset= nextLinkedPosition.getOffset();
                        if (position != nextLinkedPositionOffset && nextLinkedPositionOffset < next)
                            next= nextLinkedPositionOffset;
                    }
                }
            }

            return next;
        }

        /**
         * Sets the caret position to the sub-word boundary given with <code>position</code>.
         *
         * @param position Position where the action should move the caret
         */
        protected abstract void setCaretPosition(int position);
    }

    protected abstract class PreviousSubWordAction extends TextNavigationAction {

        protected CeylonWordIterator fIterator= new CeylonWordIterator();

        /**
         * Creates a new previous sub-word action.
         *
         * @param code Action code for the default operation. Must be an action code from @see org.eclipse.swt.custom.ST.
         */
        protected PreviousSubWordAction(final int code) {
            super(getSourceViewer().getTextWidget(), code);
        }

        @Override
        public void run() {
            // Check whether we are in a java code partition and the preference is enabled

            final ISourceViewer viewer= getSourceViewer();
            final IDocument document= viewer.getDocument();
            try {
                fIterator.setText((CharacterIterator)new DocumentCharacterIterator(document));
                int position= widgetOffset2ModelOffset(viewer, viewer.getTextWidget().getCaretOffset());
                if (position == -1)
                    return;

                int previous= findPreviousPosition(position);
                if (previous != BreakIterator.DONE) {
                    setCaretPosition(previous);
                    getTextWidget().showSelection();
                    fireSelectionChanged();
                }
            } catch (BadLocationException x) {
                // ignore - getLineOfOffset failed
            }

        }

        /**
         * Finds the previous position before the given position.
         *
         * @param position the current position
         * @return the previous position
         */
        protected int findPreviousPosition(int position) {
            ISourceViewer viewer= getSourceViewer();
            int widget= -1;
            int previous= position;
            while (previous != BreakIterator.DONE && widget == -1) { // XXX: optimize
                previous= fIterator.preceding(previous);
                if (previous != BreakIterator.DONE)
                    widget= modelOffset2WidgetOffset(viewer, previous);
            }

            IDocument document= viewer.getDocument();
            LinkedModeModel model= LinkedModeModel.getModel(document, position);
            if (model != null) {
                LinkedPosition linkedPosition= model.findPosition(new LinkedPosition(document, position, 0));
                if (linkedPosition != null) {
                    int linkedPositionOffset= linkedPosition.getOffset();
                    if (position != linkedPositionOffset && previous < linkedPositionOffset)
                        previous= linkedPositionOffset;
                } else {
                    LinkedPosition previousLinkedPosition= model.findPosition(new LinkedPosition(document, previous, 0));
                    if (previousLinkedPosition != null) {
                        int previousLinkedPositionEnd= previousLinkedPosition.getOffset() + previousLinkedPosition.getLength();
                        if (position != previousLinkedPositionEnd && previous < previousLinkedPositionEnd)
                            previous= previousLinkedPositionEnd;
                    }
                }
            }

            return previous;
        }

        /**
         * Sets the caret position to the sub-word boundary given with <code>position</code>.
         *
         * @param position Position where the action should move the caret
         */
        protected abstract void setCaretPosition(int position);
    }
    
    protected class SelectNextSubWordAction extends NextSubWordAction {
        public SelectNextSubWordAction() {
            super(ST.SELECT_WORD_NEXT);
            setActionDefinitionId(ITextEditorActionDefinitionIds.SELECT_WORD_NEXT);
        }
        @Override
        protected void setCaretPosition(final int position) {
            final ISourceViewer viewer= getSourceViewer();

            final StyledText text= viewer.getTextWidget();
            if (text != null && !text.isDisposed()) {

                final Point selection= text.getSelection();
                final int caret= text.getCaretOffset();
                final int offset= modelOffset2WidgetOffset(viewer, position);

                if (caret == selection.x)
                    text.setSelectionRange(selection.y, offset - selection.y);
                else
                    text.setSelectionRange(selection.x, offset - selection.x);
            }
        }
    }
    
    protected class SelectPreviousSubWordAction extends PreviousSubWordAction {
        public SelectPreviousSubWordAction() {
            super(ST.SELECT_WORD_PREVIOUS);
            setActionDefinitionId(ITextEditorActionDefinitionIds.SELECT_WORD_PREVIOUS);
        }
        @Override
        protected void setCaretPosition(final int position) {
            final ISourceViewer viewer= getSourceViewer();

            final StyledText text= viewer.getTextWidget();
            if (text != null && !text.isDisposed()) {

                final Point selection= text.getSelection();
                final int caret= text.getCaretOffset();
                final int offset= modelOffset2WidgetOffset(viewer, position);

                if (caret == selection.x)
                    text.setSelectionRange(selection.y, offset - selection.y);
                else
                    text.setSelectionRange(selection.x, offset - selection.x);
            }
        }
    }
    
    protected class DeleteNextSubWordAction extends NextSubWordAction implements IUpdate {
        public DeleteNextSubWordAction() {
            super(ST.DELETE_WORD_NEXT);
            setActionDefinitionId(ITextEditorActionDefinitionIds.DELETE_NEXT_WORD);
        }
        @Override
        protected void setCaretPosition(final int position) {
            if (!validateEditorInputState())
                return;

            final ISourceViewer viewer= getSourceViewer();
            StyledText text= viewer.getTextWidget();
            Point selection= viewer.getSelectedRange();
            final int caret, length;
            if (selection.y != 0) {
                caret= selection.x;
                length= selection.y;
            } else {
                caret= widgetOffset2ModelOffset(viewer, text.getCaretOffset());
                length= position - caret;
            }

            try {
                viewer.getDocument().replace(caret, length, ""); //$NON-NLS-1$
            } catch (BadLocationException exception) {
                // Should not happen
            }
        }
        public void update() {
            setEnabled(isEditorInputModifiable());
        }
    }

    protected class DeletePreviousSubWordAction extends PreviousSubWordAction implements IUpdate {
        public DeletePreviousSubWordAction() {
            super(ST.DELETE_WORD_PREVIOUS);
            setActionDefinitionId(ITextEditorActionDefinitionIds.DELETE_PREVIOUS_WORD);
        }
        @Override
        protected void setCaretPosition(int position) {
            if (!validateEditorInputState())
                return;

            final int length;
            final ISourceViewer viewer= getSourceViewer();
            StyledText text= viewer.getTextWidget();
            Point selection= viewer.getSelectedRange();
            if (selection.y != 0) {
                position= selection.x;
                length= selection.y;
            } else {
                length= widgetOffset2ModelOffset(viewer, text.getCaretOffset()) - position;
            }

            try {
                viewer.getDocument().replace(position, length, ""); //$NON-NLS-1$
            } catch (BadLocationException exception) {
                // Should not happen
            }
        }
        public void update() {
            setEnabled(isEditorInputModifiable());
        }
    }

    @Override
    public Object getAdapter(Class required) {
        if (IToggleBreakpointsTarget.class.equals(required)) {
            if (toggleBreakpointTarget == null) {
                toggleBreakpointTarget = new com.redhat.ceylon.eclipse.debug.ui.actions.ToggleBreakpointAdapter();
            }
            return toggleBreakpointTarget;
        }
        return super.getAdapter(required);
    }

}
