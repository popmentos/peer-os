/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.kiskis.mgmt.ui.zookeeper;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Runo;
import org.safehaus.kiskis.mgmt.ui.zookeeper.manager.Manager;
import org.safehaus.kiskis.mgmt.ui.zookeeper.wizard.Wizard;

/**
 * @author dilshat
 */
public class Form extends CustomComponent {

    public Form() {
        setSizeFull();
        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setSpacing(true);
        verticalLayout.setSizeFull();
        TabSheet mongoSheet = new TabSheet();
        mongoSheet.setStyleName(Runo.TABSHEET_SMALL);
        mongoSheet.setSizeFull();
        Manager manager = new Manager();
        Wizard wizard = new Wizard();
        mongoSheet.addTab(wizard.getContent(), "Install");
        mongoSheet.addTab(manager.getContent(), "Manage");
        verticalLayout.addComponent(mongoSheet);
        setCompositionRoot(verticalLayout);
        manager.refreshClustersInfo();
    }

}
