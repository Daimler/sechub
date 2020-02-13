package com.daimler.sechub.developertools.admin.ui.action.adapter;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

public abstract class AbstractAdapterDialogAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private AdapterDialogUI dialogUI;

    public AbstractAdapterDialogAction(String text, AdapterDialogUI ui) {
        super(text);
        this.dialogUI = ui;
    }

    protected AdapterDialogUI getDialogUI() {
        return dialogUI;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            execute(e);
        }catch(Exception ex) {
            getDialogUI().getContext().getOutputUI().error("Was not able to perform action", ex);
            ex.printStackTrace();
        }
        
    }
    
    protected abstract void execute(ActionEvent e) throws Exception ;


}
