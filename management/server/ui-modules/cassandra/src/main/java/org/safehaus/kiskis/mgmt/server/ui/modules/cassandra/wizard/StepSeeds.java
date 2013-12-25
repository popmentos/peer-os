/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.kiskis.mgmt.server.ui.modules.cassandra.wizard;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.terminal.Sizeable;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TwinColSelect;
import com.vaadin.ui.VerticalLayout;
import java.util.ArrayList;
import java.util.Set;
import org.safehaus.kiskis.mgmt.server.ui.modules.cassandra.commands.CassandraCommands;
import org.safehaus.kiskis.mgmt.shared.protocol.Agent;
import org.safehaus.kiskis.mgmt.shared.protocol.Command;
import org.safehaus.kiskis.mgmt.shared.protocol.RequestUtil;
import org.safehaus.kiskis.mgmt.shared.protocol.Task;
import org.safehaus.kiskis.mgmt.shared.protocol.Util;

/**
 *
 * @author dilshat
 */
public class StepSeeds extends Panel {
    
    private Task task;
    
    public StepSeeds(final CassandraWizard wizard) {
        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setSizeFull();
        verticalLayout.setHeight(100, Sizeable.UNITS_PERCENTAGE);
        verticalLayout.setMargin(true);
        
        GridLayout grid = new GridLayout(10, 10);
        grid.setSpacing(true);
        grid.setSizeFull();
        
        Panel panel = new Panel();
        Label menu = new Label("Cluster Installation Wizard<br>"
                + " 1) <font color=\"#f14c1a\"><strong>Config Servers and Routers</strong></font><br>"
                + " 2) Replica Set Configurations");
        
        menu.setContentMode(Label.CONTENT_XHTML);
        panel.addComponent(menu);
        grid.addComponent(menu, 0, 0, 2, 1);
        grid.setComponentAlignment(panel, Alignment.TOP_CENTER);
        
        VerticalLayout verticalLayoutForm = new VerticalLayout();
        verticalLayoutForm.setSizeFull();
        verticalLayoutForm.setSpacing(true);
        
        final TextField clusterNameTxtFld = new TextField("Enter cluster name");
        clusterNameTxtFld.setInputPrompt("Cluster name");
        clusterNameTxtFld.setRequired(true);
        clusterNameTxtFld.setMaxLength(20);
        verticalLayoutForm.addComponent(clusterNameTxtFld);
        
        Label configServersLabel = new Label("<strong>Choose hosts that will act as seeds<br>"
                + "(Recommended 1 servers)</strong>");
        configServersLabel.setContentMode(Label.CONTENT_XHTML);
        verticalLayoutForm.addComponent(configServersLabel);
        
        final TwinColSelect configServersColSel = new TwinColSelect("", new ArrayList<Agent>());
        configServersColSel.setItemCaptionPropertyId("hostname");
        configServersColSel.setRows(7);
        configServersColSel.setNullSelectionAllowed(true);
        configServersColSel.setMultiSelect(true);
        configServersColSel.setImmediate(true);
        configServersColSel.setLeftColumnCaption("Available Nodes");
        configServersColSel.setRightColumnCaption("Config Servers");
        configServersColSel.setWidth(100, Sizeable.UNITS_PERCENTAGE);
        configServersColSel.setRequired(true);
        
        verticalLayoutForm.addComponent(configServersColSel);
        
        grid.addComponent(verticalLayoutForm, 3, 0, 9, 9);
        grid.setComponentAlignment(verticalLayoutForm, Alignment.TOP_CENTER);
        
        Button next = new Button("Next");
        next.addListener(new Button.ClickListener() {
            
            @Override
            public void buttonClick(Button.ClickEvent event) {
                wizard.getConfig().setConfigServers((Set<Agent>) configServersColSel.getValue());
                
                if (Util.isCollectionEmpty(wizard.getConfig().getConfigServers())) {
                    show("Please add seeds servers");
                } else {
                    task = RequestUtil.createTask(CassandraWizard.getCommandManager(), "Set lister and rpc addresses");
                    for (Agent agent : wizard.getConfig().getSelectedAgents()) {
                        
                        Command command1 = CassandraCommands.getSetSeedsCommand();
                        command1.getRequest().setUuid(agent.getUuid());
                        command1.getRequest().setSource(CassandraWizard.SOURCE);
                        command1.getRequest().setUuid(agent.getUuid());
                        command1.getRequest().setTaskUuid(task.getUuid());
                        command1.getRequest().setRequestSequenceNumber(task.getIncrementedReqSeqNumber());
                        
                        Command command2 = CassandraCommands.getSetSeedsCommand();
                        command2.getRequest().setUuid(agent.getUuid());
                        command2.getRequest().setSource(CassandraWizard.SOURCE);
                        command2.getRequest().setUuid(agent.getUuid());
                        command2.getRequest().setTaskUuid(task.getUuid());
                        command2.getRequest().setRequestSequenceNumber(task.getIncrementedReqSeqNumber());

//                    CassandraWizard.getCommandManager().executeCommand(command1);
//                    CassandraWizard.getCommandManager().executeCommand(command2);
                    }
                    wizard.next();
                }
            }
        });
        
        Button back = new Button("Back");
        back.addListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                wizard.back();
            }
        });
        
        verticalLayout.addComponent(grid);
        
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.addComponent(back);
        horizontalLayout.addComponent(next);
        verticalLayout.addComponent(horizontalLayout);
        
        addComponent(verticalLayout);
        
        configServersColSel.setContainerDataSource(
                new BeanItemContainer<Agent>(
                        Agent.class, wizard.getConfig().getSelectedAgents()));

        //set values if this is a second visit
        configServersColSel.setValue(Util.retainValues(wizard.getConfig().getConfigServers(), wizard.getConfig().getSelectedAgents()));
    }
    
    private void show(String notification) {
        getWindow().showNotification(notification);
    }
    
}
