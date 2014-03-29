/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.kiskis.mgmt.ui.mongodb.manager;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.terminal.Sizeable;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.safehaus.kiskis.mgmt.api.mongodb.Config;
import org.safehaus.kiskis.mgmt.api.mongodb.NodeType;
import org.safehaus.kiskis.mgmt.server.ui.ConfirmationDialogCallback;
import org.safehaus.kiskis.mgmt.server.ui.MgmtApplication;
import org.safehaus.kiskis.mgmt.shared.protocol.Agent;
import org.safehaus.kiskis.mgmt.shared.protocol.Util;
import org.safehaus.kiskis.mgmt.shared.protocol.enums.NodeState;
import org.safehaus.kiskis.mgmt.ui.mongodb.MongoUI;

/**
 *
 * @author dilshat
 *
 */
public class Manager {
    
    private final VerticalLayout contentRoot;
    private final ComboBox clusterCombo;
    private final Table configServersTable;
    private final Table routersTable;
    private final Table dataNodesTable;
    private final Label replicaSetName;
    private final Label domainName;
    private final Label cfgSrvPort;
    private final Label routerPort;
    private final Label dataNodePort;
    private Config config;
    
    public Manager() {
        
        contentRoot = new VerticalLayout();
        contentRoot.setSpacing(true);
        contentRoot.setWidth(90, Sizeable.UNITS_PERCENTAGE);
        contentRoot.setHeight(100, Sizeable.UNITS_PERCENTAGE);
        
        VerticalLayout content = new VerticalLayout();
        content.setWidth(100, Sizeable.UNITS_PERCENTAGE);
        content.setHeight(100, Sizeable.UNITS_PERCENTAGE);
        
        contentRoot.addComponent(content);
        contentRoot.setComponentAlignment(content, Alignment.TOP_CENTER);
        contentRoot.setMargin(true);

        //tables go here
        configServersTable = createTableTemplate("Config Servers", 150);
        routersTable = createTableTemplate("Query Routers", 150);
        dataNodesTable = createTableTemplate("Data Nodes", 200);
        //tables go here

        HorizontalLayout controlsContent = new HorizontalLayout();
        controlsContent.setSpacing(true);
        
        Label clusterNameLabel = new Label("Select the cluster");
        controlsContent.addComponent(clusterNameLabel);
        
        clusterCombo = new ComboBox();
        clusterCombo.setMultiSelect(false);
        clusterCombo.setImmediate(true);
        clusterCombo.setTextInputAllowed(false);
        clusterCombo.setWidth(200, Sizeable.UNITS_PIXELS);
        clusterCombo.addListener(new Property.ValueChangeListener() {
            
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                config = (Config) event.getProperty().getValue();
                refreshUI();
            }
        });
        
        controlsContent.addComponent(clusterCombo);
        
        Button refreshClustersBtn = new Button("Refresh clusters");
        refreshClustersBtn.addListener(new Button.ClickListener() {
            
            @Override
            public void buttonClick(Button.ClickEvent event) {
                refreshClustersInfo();
            }
        });
        
        controlsContent.addComponent(refreshClustersBtn);
        
        Button checkAllBtn = new Button("Check all");
        checkAllBtn.addListener(new Button.ClickListener() {
            
            @Override
            public void buttonClick(Button.ClickEvent event) {
                checkNodesStatus(configServersTable);
                checkNodesStatus(routersTable);
                checkNodesStatus(dataNodesTable);
            }
            
        });
        
        controlsContent.addComponent(checkAllBtn);
        
        Button destroyClusterBtn = new Button("Destroy cluster");
        destroyClusterBtn.addListener(new Button.ClickListener() {
            
            @Override
            public void buttonClick(Button.ClickEvent event) {
                if (config != null) {
                    MgmtApplication.showConfirmationDialog(
                            "Cluster destruction confirmation",
                            String.format("Do you want to destroy the %s cluster?", config.getClusterName()),
                            "Yes", "No", new ConfirmationDialogCallback() {
                                
                                @Override
                                public void response(boolean ok) {
                                    if (ok) {
                                        UUID trackID = MongoUI.getMongoManager().uninstallCluster(config.getClusterName());
                                        MongoUI.showProgressWindow(trackID, new Window.CloseListener() {
                                            
                                            public void windowClose(Window.CloseEvent e) {
                                                refreshClustersInfo();
                                            }
                                        });
                                    }
                                }
                            });
                } else {
                    show("Please, select cluster");
                }
            }
            
        });
        
        controlsContent.addComponent(destroyClusterBtn);
        
        Button addRouterBtn = new Button("Add Router");
        addRouterBtn.addListener(new Button.ClickListener() {
            
            public void buttonClick(Button.ClickEvent event) {
                if (config != null) {
                    MgmtApplication.showConfirmationDialog(
                            "Confirm adding node",
                            String.format("Do you want to add ROUTER to the %s cluster?", config.getClusterName()),
                            "Yes", "No", new ConfirmationDialogCallback() {
                                
                                @Override
                                public void response(boolean ok) {
                                    if (ok) {
                                        UUID trackID = MongoUI.getMongoManager().addNode(config.getClusterName(), NodeType.ROUTER_NODE);
                                        MongoUI.showProgressWindow(trackID, new Window.CloseListener() {
                                            
                                            public void windowClose(Window.CloseEvent e) {
                                                refreshClustersInfo();
                                            }
                                        });
                                    }
                                }
                            });
                } else {
                    show("Please, select cluster");
                }
            }
        });
        
        Button addDataNodeBtn = new Button("Add Data Node");
        
        addDataNodeBtn.addListener(new Button.ClickListener() {
            
            @Override
            public void buttonClick(Button.ClickEvent event) {
                if (config != null) {
                    MgmtApplication.showConfirmationDialog(
                            "Confirm adding node",
                            String.format("Do you want to add DATA_NODE to the %s cluster?", config.getClusterName()),
                            "Yes", "No", new ConfirmationDialogCallback() {
                                
                                @Override
                                public void response(boolean ok) {
                                    if (ok) {
                                        UUID trackID = MongoUI.getMongoManager().addNode(config.getClusterName(), NodeType.DATA_NODE);
                                        MongoUI.showProgressWindow(trackID, new Window.CloseListener() {
                                            
                                            public void windowClose(Window.CloseEvent e) {
                                                refreshClustersInfo();
                                            }
                                        });
                                    }
                                }
                            });
                } else {
                    show("Please, select cluster");
                }
            }
        });
        
        controlsContent.addComponent(addRouterBtn);
        controlsContent.addComponent(addDataNodeBtn);
        
        content.addComponent(controlsContent);
        
        HorizontalLayout configContent = new HorizontalLayout();
        configContent.setSpacing(true);
        
        replicaSetName = new Label();
        domainName = new Label();
        cfgSrvPort = new Label();
        routerPort = new Label();
        dataNodePort = new Label();
        
        configContent.addComponent(new Label("Replica Set:"));
        configContent.addComponent(replicaSetName);
        configContent.addComponent(new Label("Domain:"));
        configContent.addComponent(domainName);
        configContent.addComponent(new Label("Config server port:"));
        configContent.addComponent(cfgSrvPort);
        configContent.addComponent(new Label("Router port:"));
        configContent.addComponent(routerPort);
        configContent.addComponent(new Label("Data node port:"));
        configContent.addComponent(dataNodePort);
        
        content.addComponent(configContent);
        
        content.addComponent(configServersTable);
        
        content.addComponent(routersTable);
        
        content.addComponent(dataNodesTable);
        
    }
    
    public Component getContent() {
        return contentRoot;
    }
    
    private void show(String notification) {
        contentRoot.getWindow().showNotification(notification);
    }
    
    private void populateTable(final Table table, Set<Agent> agents, final NodeType nodeType) {
        
        table.removeAllItems();
        
        for (Iterator it = agents.iterator(); it.hasNext();) {
            final Agent agent = (Agent) it.next();
            
            final Button checkBtn = new Button("Check");
            final Button startBtn = new Button("Start");
            final Button stopBtn = new Button("Stop");
            final Button destroyBtn = new Button("Destroy");
            final Embedded progressIcon = new Embedded("", new ThemeResource("../base/common/img/loading-indicator.gif"));
            stopBtn.setEnabled(false);
            startBtn.setEnabled(false);
            progressIcon.setVisible(false);
            
            final Object rowId = table.addItem(new Object[]{
                agent.getHostname(),
                checkBtn,
                startBtn,
                stopBtn,
                destroyBtn,
                progressIcon},
                    null);
            
            checkBtn.addListener(new Button.ClickListener() {
                
                @Override
                public void buttonClick(Button.ClickEvent event) {
                    
                    progressIcon.setVisible(true);
                    startBtn.setEnabled(false);
                    stopBtn.setEnabled(false);
                    destroyBtn.setEnabled(false);
                    
                    MongoUI.getExecutor().execute(new CheckTask(config.getClusterName(), agent.getHostname(), new CompleteEvent() {
                        
                        public void onComplete(NodeState state) {
                            synchronized (progressIcon) {
                                if (state == NodeState.RUNNING) {
                                    stopBtn.setEnabled(true);
                                } else if (state == NodeState.STOPPED) {
                                    startBtn.setEnabled(true);
                                }
                                destroyBtn.setEnabled(true);
                                progressIcon.setVisible(false);
                            }
                        }
                    }));
                }
            });
            
            startBtn.addListener(new Button.ClickListener() {
                
                @Override
                public void buttonClick(Button.ClickEvent event) {
                    
                    progressIcon.setVisible(true);
                    startBtn.setEnabled(false);
                    stopBtn.setEnabled(false);
                    destroyBtn.setEnabled(false);
                    
                    MongoUI.getExecutor().execute(new StartTask(nodeType, config.getClusterName(), agent.getHostname(), new CompleteEvent() {
                        
                        public void onComplete(NodeState state) {
                            synchronized (progressIcon) {
                                if (state == NodeState.RUNNING) {
                                    stopBtn.setEnabled(true);
                                } else {
                                    startBtn.setEnabled(true);
                                }
                                destroyBtn.setEnabled(true);
                                progressIcon.setVisible(false);
                            }
                        }
                    }));
                    
                }
            });
            
            stopBtn.addListener(new Button.ClickListener() {
                
                @Override
                public void buttonClick(Button.ClickEvent event) {
                    
                    progressIcon.setVisible(true);
                    startBtn.setEnabled(false);
                    stopBtn.setEnabled(false);
                    destroyBtn.setEnabled(false);
                    
                    MongoUI.getExecutor().execute(new StopTask(config.getClusterName(), agent.getHostname(), new CompleteEvent() {
                        
                        public void onComplete(NodeState state) {
                            synchronized (progressIcon) {
                                if (state == NodeState.STOPPED) {
                                    startBtn.setEnabled(true);
                                } else {
                                    stopBtn.setEnabled(true);
                                }
                                destroyBtn.setEnabled(true);
                                progressIcon.setVisible(false);
                            }
                        }
                    }));
                }
            });
            
            destroyBtn.addListener(new Button.ClickListener() {
                
                @Override
                public void buttonClick(Button.ClickEvent event) {
                    
                    MgmtApplication.showConfirmationDialog(
                            "Node destruction confirmation",
                            String.format("Do you want to destroy the %s node?", agent.getHostname()),
                            "Yes", "No", new ConfirmationDialogCallback() {
                                
                                @Override
                                public void response(boolean ok) {
                                    if (ok) {
                                        UUID trackID = MongoUI.getMongoManager().destroyNode(config.getClusterName(), agent.getHostname());
                                        MongoUI.showProgressWindow(trackID, new Window.CloseListener() {
                                            
                                            public void windowClose(Window.CloseEvent e) {
                                                refreshClustersInfo();
                                            }
                                        });
                                    }
                                }
                            });
                    
                }
            });
        }
    }
    
    private void refreshUI() {
        if (config != null) {
            populateTable(configServersTable, config.getConfigServers(), NodeType.CONFIG_NODE);
            populateTable(routersTable, config.getRouterServers(), NodeType.ROUTER_NODE);
            populateTable(dataNodesTable, config.getDataNodes(), NodeType.DATA_NODE);
            replicaSetName.setValue(config.getReplicaSetName());
            domainName.setValue(config.getDomainName());
            cfgSrvPort.setValue(config.getCfgSrvPort());
            routerPort.setValue(config.getRouterPort());
            dataNodePort.setValue(config.getDataNodePort());
        } else {
            configServersTable.removeAllItems();
            routersTable.removeAllItems();
            dataNodesTable.removeAllItems();
            replicaSetName.setValue("");
            domainName.setValue("");
            cfgSrvPort.setValue("");
            routerPort.setValue("");
            dataNodePort.setValue("");
        }
    }
    
    public void refreshClustersInfo() {
        List<Config> mongoClusterInfos = MongoUI.getMongoManager().getClusters();
        Config clusterInfo = (Config) clusterCombo.getValue();
        clusterCombo.removeAllItems();
        if (mongoClusterInfos != null && mongoClusterInfos.size() > 0) {
            for (Config mongoClusterInfo : mongoClusterInfos) {
                clusterCombo.addItem(mongoClusterInfo);
                clusterCombo.setItemCaption(mongoClusterInfo,
                        mongoClusterInfo.getClusterName());
            }
            if (clusterInfo != null) {
                for (Config mongoClusterInfo : mongoClusterInfos) {
                    if (mongoClusterInfo.getClusterName().equals(clusterInfo.getClusterName())) {
                        clusterCombo.setValue(mongoClusterInfo);
                        return;
                    }
                }
            } else {
                clusterCombo.setValue(mongoClusterInfos.iterator().next());
            }
        }
    }
    
    public static void checkNodesStatus(Table table) {
        for (Iterator it = table.getItemIds().iterator(); it.hasNext();) {
            int rowId = (Integer) it.next();
            Item row = table.getItem(rowId);
            Button checkBtn = (Button) (row.getItemProperty("Check").getValue());
            checkBtn.click();
        }
    }
    
    private Table createTableTemplate(String caption, int size) {
        final Table table = new Table(caption);
        table.addContainerProperty("Host", String.class, null);
        table.addContainerProperty("Check", Button.class, null);
        table.addContainerProperty("Start", Button.class, null);
        table.addContainerProperty("Stop", Button.class, null);
        table.addContainerProperty("Destroy", Button.class, null);
        table.addContainerProperty("Status", Embedded.class, null);
        table.setWidth(100, Sizeable.UNITS_PERCENTAGE);
        table.setHeight(size, Sizeable.UNITS_PIXELS);
        table.setPageLength(10);
        table.setSelectable(false);
        table.setImmediate(true);
        
        table.addListener(new ItemClickEvent.ItemClickListener() {
            
            public void itemClick(ItemClickEvent event) {
                if (event.isDoubleClick()) {
                    String lxcHostname = (String) table.getItem(event.getItemId()).getItemProperty("Host").getValue();
                    Agent lxcAgent = MongoUI.getAgentManager().getAgentByHostname(lxcHostname);
                    if (lxcAgent != null) {
                        Window terminal = MgmtApplication.createTerminalWindow(Util.wrapAgentToSet(lxcAgent));
                        MgmtApplication.addCustomWindow(terminal);
                    } else {
                        show("Agent is not connected");
                    }
                }
            }
        });
        return table;
    }
    
}
