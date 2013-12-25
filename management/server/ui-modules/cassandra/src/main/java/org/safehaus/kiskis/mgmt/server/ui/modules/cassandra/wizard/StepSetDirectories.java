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
public class StepSetDirectories extends Panel {

    private Task task;

    public StepSetDirectories(final CassandraWizard wizard) {

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

        final TextField dir1 = new TextField("Enter Data directory");
        dir1.setInputPrompt("/var/lib/cassandra/data");
        dir1.setRequired(true);
        dir1.setMaxLength(20);
        verticalLayoutForm.addComponent(dir1);

        final TextField dir2 = new TextField("Enter Commitlog directory");
        dir2.setInputPrompt("/var/lib/cassandra/commitlog");
        dir2.setRequired(true);
        dir2.setMaxLength(20);
        verticalLayoutForm.addComponent(dir2);

        final TextField dir3 = new TextField("Enter Saved Caches directory");
        dir3.setInputPrompt("/var/lib/cassandra/saved_caches");
        dir3.setRequired(true);
        dir3.setMaxLength(20);
        verticalLayoutForm.addComponent(dir3);

        grid.addComponent(verticalLayoutForm, 3, 0, 9, 9);
        grid.setComponentAlignment(verticalLayoutForm, Alignment.TOP_CENTER);

        Button next = new Button("Next");
        next.addListener(new Button.ClickListener() {

            @Override
            public void buttonClick(Button.ClickEvent event) {

                task = RequestUtil.createTask(CassandraWizard.getCommandManager(), "Set directories");
                for (Agent agent : wizard.getConfig().getSelectedAgents()) {

                    Command command1 = CassandraCommands.getSetDataDirectoryCommand();
                    command1.getRequest().setUuid(agent.getUuid());
                    command1.getRequest().setSource(CassandraWizard.SOURCE);
                    command1.getRequest().setUuid(agent.getUuid());
                    command1.getRequest().setTaskUuid(task.getUuid());
                    command1.getRequest().setRequestSequenceNumber(task.getIncrementedReqSeqNumber());

                    Command command11 = CassandraCommands.getDeleteDataDirectoryCommand();
                    command11.getRequest().setUuid(agent.getUuid());
                    command11.getRequest().setSource(CassandraWizard.SOURCE);
                    command11.getRequest().setUuid(agent.getUuid());
                    command11.getRequest().setTaskUuid(task.getUuid());
                    command11.getRequest().setRequestSequenceNumber(task.getIncrementedReqSeqNumber());

                    Command command2 = CassandraCommands.getSetCommitLogDirectoryCommand();
                    command2.getRequest().setUuid(agent.getUuid());
                    command2.getRequest().setSource(CassandraWizard.SOURCE);
                    command2.getRequest().setUuid(agent.getUuid());
                    command2.getRequest().setTaskUuid(task.getUuid());
                    command2.getRequest().setRequestSequenceNumber(task.getIncrementedReqSeqNumber());

                    Command command22 = CassandraCommands.getDeleteCommitLogDirectoryCommand();
                    command22.getRequest().setUuid(agent.getUuid());
                    command22.getRequest().setSource(CassandraWizard.SOURCE);
                    command22.getRequest().setUuid(agent.getUuid());
                    command22.getRequest().setTaskUuid(task.getUuid());
                    command22.getRequest().setRequestSequenceNumber(task.getIncrementedReqSeqNumber());

                    Command command3 = CassandraCommands.getSetSavedCachesDirectoryCommand();
                    command3.getRequest().setUuid(agent.getUuid());
                    command3.getRequest().setSource(CassandraWizard.SOURCE);
                    command3.getRequest().setUuid(agent.getUuid());
                    command3.getRequest().setTaskUuid(task.getUuid());
                    command3.getRequest().setRequestSequenceNumber(task.getIncrementedReqSeqNumber());

                    Command command33 = CassandraCommands.getDeleteSavedCachesDirectoryCommand();
                    command33.getRequest().setUuid(agent.getUuid());
                    command33.getRequest().setSource(CassandraWizard.SOURCE);
                    command33.getRequest().setUuid(agent.getUuid());
                    command33.getRequest().setTaskUuid(task.getUuid());
                    command33.getRequest().setRequestSequenceNumber(task.getIncrementedReqSeqNumber());

//                        CassandraWizard.getCommandManager().executeCommand(command1);
//                        CassandraWizard.getCommandManager().executeCommand(command11);
//                        CassandraWizard.getCommandManager().executeCommand(command2);
//                        CassandraWizard.getCommandManager().executeCommand(command22);
//                        CassandraWizard.getCommandManager().executeCommand(command3);
//                        CassandraWizard.getCommandManager().executeCommand(command33);
                }
                wizard.next();
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

    }

    private void show(String notification) {
        getWindow().showNotification(notification);
    }

}
