/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.subutai.plugin.hbase.ui.manager;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import com.google.common.base.Preconditions;
import org.safehaus.subutai.common.protocol.Agent;
import org.safehaus.subutai.common.util.ServiceLocator;
import org.safehaus.subutai.core.agent.api.AgentManager;
import org.safehaus.subutai.core.command.api.CommandRunner;
import org.safehaus.subutai.core.tracker.api.Tracker;
import org.safehaus.subutai.plugin.hadoop.api.Hadoop;
import org.safehaus.subutai.plugin.hbase.api.HBase;
import org.safehaus.subutai.plugin.hbase.api.HBaseClusterConfig;
import org.safehaus.subutai.plugin.hbase.api.HBaseType;
import org.safehaus.subutai.plugin.hbase.ui.HBaseUI;
import org.safehaus.subutai.server.ui.component.ConfirmationDialog;
import org.safehaus.subutai.server.ui.component.ProgressWindow;
import org.safehaus.subutai.server.ui.component.TerminalWindow;

import com.google.common.collect.Sets;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.server.Sizeable;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import javax.naming.NamingException;


/**
 * @author dilshat
 */
public class Manager
{
    protected final static String AVAILABLE_OPERATIONS_COLUMN_CAPTION = "AVAILABLE_OPERATIONS";
    protected final static String CHECK_ALL_BUTTON_CAPTION = "Check All";
    protected final static String CHECK_BUTTON_CAPTION = "Check";
    protected final static String START_ALL_BUTTON_CAPTION = "Start All";
    protected final static String START_BUTTON_CAPTION = "Start";
    protected final static String STOP_ALL_BUTTON_CAPTION = "Stop All";
    protected final static String STOP_BUTTON_CAPTION = "Stop";
    protected final static String DESTROY_CLUSTER_BUTTON_CAPTION = "Destroy Cluster";
    protected final static String DESTROY_BUTTON_CAPTION = "Destroy";
    protected final static String HOST_COLUMN_CAPTION = "Host";
    protected final static String IP_COLUMN_CAPTION = "IP List";
    protected final static String NODE_ROLE_COLUMN_CAPTION = "Node Role";
    protected final static String STATUS_COLUMN_CAPTION = "Status";
    protected final static String ADD_NODE_CAPTION = "Add Node";

    protected final Button refreshClustersBtn, startAllNodesBtn, stopAllNodesBtn, checkAllBtn, destroyClusterBtn;

    private final VerticalLayout contentRoot;
    private final ComboBox clusterCombo;
    private final Table masterTable;
    private final Table regionTable;
    private final Table quorumTable;
    private final Table backUpMasterTable;
    private final ExecutorService executor;
    private HBaseClusterConfig config;
    private HBaseUI hBaseUI;

    private final HBase hbase;
    private final Tracker tracker;
    private final Hadoop hadoop;
    private final AgentManager agentManager;
    private final CommandRunner commandRunner;
    private final String message = "No cluster is installed !";
    private final Embedded progressIcon = new Embedded( "", new ThemeResource( "img/spinner.gif" ) );



    public Manager( final ExecutorService executor, final ServiceLocator serviceLocator ) throws NamingException
    {
        Preconditions.checkNotNull( executor, "Executor is null" );
        Preconditions.checkNotNull( serviceLocator, "Service Locator is null" );

        this.hbase = serviceLocator.getService( HBase.class );
        this.tracker = serviceLocator.getService( Tracker.class );
        this.hadoop = serviceLocator.getService( Hadoop.class );
        this.agentManager = serviceLocator.getService( AgentManager.class );
        this.commandRunner = serviceLocator.getService( CommandRunner.class );
        this.executor = executor;

        contentRoot = new VerticalLayout();
        contentRoot.setSpacing( true );
        contentRoot.setSizeFull();

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();

        contentRoot.addComponent( content );
        contentRoot.setComponentAlignment( content, Alignment.TOP_CENTER );
        contentRoot.setMargin( true );

        //tables go here
        masterTable = createTableTemplate( "Master" );
        regionTable = createTableTemplate( "Region" );
        quorumTable = createTableTemplate( "Quorum" );
        backUpMasterTable = createTableTemplate( "Backup master" );
        //tables go here

        HorizontalLayout controlsContent = new HorizontalLayout();
        controlsContent.setSpacing( true );

        Label clusterNameLabel = new Label( "Select the cluster" );
        controlsContent.addComponent( clusterNameLabel );

        clusterCombo = new ComboBox();
        clusterCombo.setImmediate( true );
        clusterCombo.setTextInputAllowed( false );
        clusterCombo.setWidth( 200, Sizeable.Unit.PIXELS );
        clusterCombo.addValueChangeListener( new Property.ValueChangeListener()
        {
            @Override
            public void valueChange( Property.ValueChangeEvent event )
            {
                Object value = event.getProperty().getValue();
                config = value != null ? ( HBaseClusterConfig ) value : null;
                refreshUI();
            }
        } );

        controlsContent.addComponent( clusterCombo );

        refreshClustersBtn = new Button( "Refresh clusters" );
        refreshClustersBtn.addStyleName( "default" );
        refreshClustersBtn.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                refreshClustersInfo();
            }
        } );

        controlsContent.addComponent( refreshClustersBtn );

        startAllNodesBtn = new Button( "Start cluster" );
        startAllNodesBtn.addStyleName( "default" );
        startAllNodesBtn.addClickListener( new Button.ClickListener() {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent ) {
                if( config != null ) {
                    UUID trackID = hbase.startCluster( config.getClusterName() );
                    ProgressWindow window = new ProgressWindow( executor, tracker, trackID,
                            HBaseClusterConfig.PRODUCT_KEY );
                    window.getWindow().addCloseListener( new Window.CloseListener() {
                        @Override
                        public void windowClose( Window.CloseEvent closeEvent ) {
                            refreshClustersInfo();
                        }
                    } );
                    contentRoot.getUI().addWindow( window.getWindow() );
                } else {
                    show( "Please, select cluster" );
                }
            }
        } );

        controlsContent.addComponent( startAllNodesBtn );

        stopAllNodesBtn = new Button( "Stop cluster" );
        stopAllNodesBtn.addStyleName( "default" );
        stopAllNodesBtn.addClickListener( new Button.ClickListener() {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent ) {
                if( config != null ) {
                    UUID trackID = hbase.stopCluster( config.getClusterName() );
                    ProgressWindow window = new ProgressWindow( executor, tracker, trackID,
                            HBaseClusterConfig.PRODUCT_KEY );
                    window.getWindow().addCloseListener( new Window.CloseListener() {
                        @Override
                        public void windowClose( Window.CloseEvent closeEvent ) {
                            refreshClustersInfo();
                        }
                    } );
                    contentRoot.getUI().addWindow( window.getWindow() );
                } else {
                    show( "Please, select cluster" );
                }
            }
        } );

        controlsContent.addComponent( stopAllNodesBtn );

        checkAllBtn = new Button( "Check cluster" );
        checkAllBtn.addStyleName( "default" );
        checkAllBtn.addClickListener( new Button.ClickListener() {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent ) {
                if( config != null ) {
                    UUID trackID = hbase.checkCluster( config.getClusterName() );
                    ProgressWindow window = new ProgressWindow( executor, tracker, trackID,
                            HBaseClusterConfig.PRODUCT_KEY );
                    window.getWindow().addCloseListener( new Window.CloseListener() {
                        @Override
                        public void windowClose( Window.CloseEvent closeEvent ) {
                            refreshClustersInfo();
                        }
                    } );
                    contentRoot.getUI().addWindow( window.getWindow() );
                } else {
                    show( "Please, select cluster" );
                }
            }
        } );

        controlsContent.addComponent( checkAllBtn );

        destroyClusterBtn = new Button( "Destroy cluster" );
        destroyClusterBtn.addStyleName( "default" );
        destroyClusterBtn.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                if ( config != null )
                {
                    ConfirmationDialog alert = new ConfirmationDialog(
                            String.format( "Do you want to add node to the %s cluster?", config.getClusterName() ),
                            "Yes", "No" );
                    alert.getOk().addClickListener( new Button.ClickListener()
                    {
                        @Override
                        public void buttonClick( Button.ClickEvent clickEvent )
                        {
                            UUID trackID = hbase.uninstallCluster( config.getClusterName() );
                            ProgressWindow window =
                                    new ProgressWindow( executor, tracker, trackID,
                                            HBaseClusterConfig.PRODUCT_KEY );
                            window.getWindow().addCloseListener( new Window.CloseListener()
                            {
                                @Override
                                public void windowClose( Window.CloseEvent closeEvent )
                                {
                                    refreshClustersInfo();
                                }
                            } );
                            contentRoot.getUI().addWindow( window.getWindow() );
                        }
                    } );

                    contentRoot.getUI().addWindow( alert.getAlert() );
                }
                else
                {
                    show( "Please, select cluster" );
                }
            }
        } );

        controlsContent.addComponent( destroyClusterBtn );
        content.addComponent( controlsContent );
        content.addComponent( masterTable );
        content.addComponent( regionTable );
        content.addComponent( quorumTable );
        content.addComponent( backUpMasterTable );
    }


    private void refreshUI()
    {
        if ( config != null )
        {
            populateTable( regionTable, config.getRegionServers(), HBaseType.HRegionServer  );
            populateTable( quorumTable, config.getQuorumPeers(), HBaseType.HRegionServer  );
            populateTable( backUpMasterTable, config.getBackupMasters(), HBaseType.BackupMaster );

            Set<Agent> masterSet = new HashSet<>();
            masterSet.add( config.getHbaseMaster() );
            populateMasterTable( masterTable, masterSet, HBaseType.HMaster );
        }
        else
        {
            regionTable.removeAllItems();
            quorumTable.removeAllItems();
            backUpMasterTable.removeAllItems();
            masterTable.removeAllItems();
        }
    }


    private void populateMasterTable( final Table table, Set<Agent> agents, final HBaseType type )
    {

        table.removeAllItems();

        for ( final Agent agent : agents )
        {
            final Embedded progressIcon = new Embedded( "", new ThemeResource( "img/spinner.gif" ) );
            progressIcon.setVisible( false );

            if ( agent == null ){
                continue;
            }

            final Object rowId = table.addItem( new Object[] {
                    agent.getHostname(), type, progressIcon
            }, null );
        }
    }


    private void populateTable( final Table table, Set<Agent> agents,  final HBaseType type )
    {

        table.removeAllItems();

        for ( final Agent agent : agents )
        {
            final Label resultHolder = new Label();
            final Button checkBtn = new Button( CHECK_BUTTON_CAPTION );
            checkBtn.addStyleName( "default" );
            final Button startBtn = new Button( START_BUTTON_CAPTION );
            startBtn.addStyleName( "default" );
            final Button stopBtn = new Button( STOP_BUTTON_CAPTION );
            stopBtn.addStyleName( "default" );

            final Button destroyBtn = new Button( DESTROY_BUTTON_CAPTION );
            destroyBtn.addStyleName( "default" );
            stopBtn.setEnabled( false );
            startBtn.setEnabled( false );
            progressIcon.setVisible( false );

            final HorizontalLayout availableOperations = new HorizontalLayout();
            availableOperations.addStyleName( "default" );
            availableOperations.setSpacing( true );

            availableOperations.addComponent( checkBtn );
            availableOperations.addComponent( startBtn );
            availableOperations.addComponent( stopBtn );
            availableOperations.addComponent( destroyBtn );

            table.addItem( new Object[] {
                    agent.getHostname(), agent.getListIP().get( 0 ), "fill here !", resultHolder,
                    availableOperations
            }, null );

//            checkBtn.addClickListener( new Button.ClickListener()
//            {
//                @Override
//                public void buttonClick( Button.ClickEvent clickEvent )
//                {
//                    progressIcon.setVisible( true );
//                    startBtn.setEnabled( false );
//                    stopBtn.setEnabled( false );
//                    checkBtn.setEnabled( false );
//                    destroyBtn.setEnabled( false );
//
//                    executor.execute( new CheckTaskSlave( spark, tracker, config.getClusterName(), agent.getHostname(),
//                            new CompleteEvent()
//                            {
//                                @Override
//                                public void onComplete( String result )
//                                {
//                                    synchronized ( progressIcon )
//                                    {
//                                        resultHolder.setValue( result );
//                                        if ( result.contains( "NOT" ) )
//                                        {
//                                            startBtn.setEnabled( true );
//                                            stopBtn.setEnabled( false );
//                                        }
//                                        else
//                                        {
//                                            startBtn.setEnabled( false );
//                                            stopBtn.setEnabled( true );
//                                        }
//                                        progressIcon.setVisible( false );
//                                        destroyBtn.setEnabled( true );
//                                        checkBtn.setEnabled( true );
//                                    }
//                                }
//                            } ) );
//                }
//            } );
//
//            startBtn.addClickListener( new Button.ClickListener()
//            {
//                @Override
//                public void buttonClick( Button.ClickEvent clickEvent )
//                {
//                    progressIcon.setVisible( true );
//                    startBtn.setEnabled( false );
//                    stopBtn.setEnabled( false );
//                    destroyBtn.setEnabled( false );
//                    checkBtn.setEnabled( false );
//
//                    executor.execute(
//                            new StartTask( spark, tracker, config.getClusterName(), agent.getHostname(), false,
//                                    new CompleteEvent()
//                                    {
//                                        @Override
//                                        public void onComplete( String result )
//                                        {
//                                            synchronized ( progressIcon )
//                                            {
//                                                checkBtn.click();
//                                            }
//                                        }
//                                    } ) );
//                }
//            } );
//
//            stopBtn.addClickListener( new Button.ClickListener()
//            {
//                @Override
//                public void buttonClick( Button.ClickEvent clickEvent )
//                {
//                    progressIcon.setVisible( true );
//                    startBtn.setEnabled( false );
//                    stopBtn.setEnabled( false );
//                    destroyBtn.setEnabled( false );
//                    checkBtn.setEnabled( false );
//                    executor.execute( new StopTask( spark, tracker, config.getClusterName(), agent.getHostname(), false,
//                            new CompleteEvent()
//                            {
//                                @Override
//                                public void onComplete( String result )
//                                {
//                                    synchronized ( progressIcon )
//                                    {
//                                        checkBtn.click();
//                                    }
//                                }
//                            } ) );
//                }
//            } );
//            destroyBtn.addClickListener( new Button.ClickListener()
//            {
//                @Override
//                public void buttonClick( Button.ClickEvent clickEvent )
//                {
//                    ConfirmationDialog alert = new ConfirmationDialog(
//                            String.format( "Do you want to destroy the %s node?", agent.getHostname() ), "Yes", "No" );
//                    alert.getOk().addClickListener( new Button.ClickListener()
//                    {
//                        @Override
//                        public void buttonClick( Button.ClickEvent clickEvent )
//                        {
//                            UUID trackID = spark.destroySlaveNode( config.getClusterName(), agent.getHostname() );
//                            ProgressWindow window =
//                                    new ProgressWindow( executor, tracker, trackID, SparkClusterConfig.PRODUCT_KEY );
//                            window.getWindow().addCloseListener( new Window.CloseListener()
//                            {
//                                @Override
//                                public void windowClose( Window.CloseEvent closeEvent )
//                                {
//                                    refreshClustersInfo();
//                                }
//                            } );
//                            contentRoot.getUI().addWindow( window.getWindow() );
//                        }
//                    } );
//
//                    contentRoot.getUI().addWindow( alert.getAlert() );
//                }
//            } );
        }
    }
//    private void populateTable( final Table table, Set<String> agents, final HBaseType type )
//    {
//
//        table.removeAllItems();
//
//        for ( final String hostname : agents )
//        {
//            final Embedded progressIcon = new Embedded( "", new ThemeResource( "img/spinner.gif" ) );
//            progressIcon.setVisible( false );
//
//            Agent a = agentManager.getAgentByHostname( hostname );
//            if ( a == null )
//            {
//                continue;
//            }
//
//            final Object rowId = table.addItem( new Object[] {
//                    a.getHostname(), type, progressIcon
//            }, null );
//        }
//    }


    public void refreshClustersInfo()
    {
        List<HBaseClusterConfig> clusters = hbase.getClusters();
        HBaseClusterConfig clusterInfo = ( HBaseClusterConfig ) clusterCombo.getValue();
        clusterCombo.removeAllItems();
        if ( clusters != null && clusters.size() > 0 )
        {
            for ( HBaseClusterConfig info : clusters )
            {
                clusterCombo.addItem( info );
                clusterCombo.setItemCaption( info, info.getClusterName() );
            }
            if ( clusterInfo != null )
            {
                for ( HBaseClusterConfig c : clusters )
                {
                    if ( c.getClusterName().equals( clusterInfo.getClusterName() ) )
                    {
                        clusterCombo.setValue( c );
                        return;
                    }
                }
            }
            else
            {
                clusterCombo.setValue( clusters.iterator().next() );
            }
        }
    }


    private Table createTableTemplate( String caption )
    {
        final Table table = new Table( caption );
        table.addContainerProperty( "Host", String.class, null );
        table.addContainerProperty( "Type", HBaseType.class, null );
        table.addContainerProperty( "Status", Embedded.class, null );
        table.setSizeFull();

        table.setPageLength( 10 );
        table.setSelectable( false );
        table.setImmediate( true );

        table.addItemClickListener( new ItemClickEvent.ItemClickListener()
        {
            @Override
            public void itemClick( ItemClickEvent event )
            {
                if ( event.isDoubleClick() )
                {
                    String lxcHostname =
                            ( String ) table.getItem( event.getItemId() ).getItemProperty( "Host" ).getValue();
                    Agent lxcAgent = agentManager.getAgentByHostname( lxcHostname );
                    if ( lxcAgent != null )
                    {
                        TerminalWindow terminal =
                                new TerminalWindow( Sets.newHashSet( lxcAgent ), executor,
                                        commandRunner, agentManager );
                        contentRoot.getUI().addWindow( terminal.getWindow() );
                    }
                    else
                    {
                        show( "Agent is not connected" );
                    }
                }
            }
        } );
        return table;
    }


    private void show( String notification )
    {
        Notification.show( notification );
    }


    public static void checkNodesStatus( Table table )
    {
        for ( Object o : table.getItemIds() )
        {
            int rowId = ( Integer ) o;
            Item row = table.getItem( rowId );
            Button checkBtn = ( Button ) ( row.getItemProperty( "Check" ).getValue() );
            checkBtn.click();
        }
    }


    public Component getContent()
    {
        return contentRoot;
    }
}
