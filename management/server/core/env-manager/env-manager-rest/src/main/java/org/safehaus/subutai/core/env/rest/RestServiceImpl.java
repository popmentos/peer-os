package org.safehaus.subutai.core.env.rest;


import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.safehaus.subutai.common.environment.ContainerHostNotFoundException;
import org.safehaus.subutai.common.environment.Environment;
import org.safehaus.subutai.common.environment.EnvironmentModificationException;
import org.safehaus.subutai.common.environment.EnvironmentNotFoundException;
import org.safehaus.subutai.common.environment.NodeGroup;
import org.safehaus.subutai.common.environment.Topology;
import org.safehaus.subutai.common.host.ContainerHostState;
import org.safehaus.subutai.common.peer.ContainerHost;
import org.safehaus.subutai.common.peer.Peer;
import org.safehaus.subutai.common.peer.PeerException;
import org.safehaus.subutai.common.util.JsonUtil;
import org.safehaus.subutai.common.util.UUIDUtil;
import org.safehaus.subutai.core.env.api.EnvironmentManager;
import org.safehaus.subutai.core.env.api.exception.EnvironmentCreationException;
import org.safehaus.subutai.core.env.api.exception.EnvironmentDestructionException;
import org.safehaus.subutai.core.peer.api.PeerManager;
import org.safehaus.subutai.core.registry.api.TemplateRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.gson.JsonSyntaxException;


public class RestServiceImpl implements RestService
{
    private static final Logger LOG = LoggerFactory.getLogger( RestServiceImpl.class.getName() );

    private static final String ERROR_KEY = "ERROR";
    private final EnvironmentManager environmentManager;
    private final PeerManager peerManager;
    private final TemplateRegistry templateRegistry;


    public RestServiceImpl( final EnvironmentManager environmentManager, final PeerManager peerManager,
                            final TemplateRegistry templateRegistry )
    {
        Preconditions.checkNotNull( environmentManager );
        Preconditions.checkNotNull( peerManager );
        Preconditions.checkNotNull( templateRegistry );

        this.environmentManager = environmentManager;
        this.peerManager = peerManager;
        this.templateRegistry = templateRegistry;
    }


    @Override
    public Response createEnvironment( final String topologyJsonString )
    {
        TopologyJson topologyJson;

        try
        {
            topologyJson = JsonUtil.fromJson( topologyJsonString, TopologyJson.class );
            checkTopology( topologyJson );
        }
        catch ( JsonSyntaxException | NullPointerException | EnvironmentCreationException e )
        {
            return Response.status( Response.Status.BAD_REQUEST ).entity( JsonUtil.toJson( ERROR_KEY, e.getMessage() ) )
                           .build();
        }

        try
        {
            Topology topology = new Topology();

            for ( Map.Entry<UUID, Set<NodeGroup>> placementEntry : topologyJson.getNodeGroupPlacement().entrySet() )
            {
                Peer peer = peerManager.getPeer( placementEntry.getKey() );
                for ( NodeGroup nodeGroup : placementEntry.getValue() )
                {
                    topology.addNodeGroupPlacement( peer, nodeGroup );
                }
            }

            Environment environment =
                    environmentManager.createEnvironment( topologyJson.getEnvironmentName(), topology, false );

            return Response.ok( JsonUtil.toJson(
                    new EnvironmentJson( environment.getId(), environment.getName(), environment.getStatus(),
                            environment.getSshKey(),
                            convertContainersToContainerJson( environment.getContainerHosts() ) ) ) ).build();
        }
        catch ( EnvironmentCreationException e )
        {
            return Response.serverError().entity( JsonUtil.toJson( ERROR_KEY, e.getMessage() ) ).build();
        }
    }


    private void checkTopology( TopologyJson topologyJson ) throws EnvironmentCreationException
    {
        if ( Strings.isNullOrEmpty( topologyJson.getEnvironmentName() ) )
        {
            throw new EnvironmentCreationException( "Invalid environment name" );
        }
        else if ( topologyJson.getNodeGroupPlacement() == null || topologyJson.getNodeGroupPlacement().isEmpty() )
        {
            throw new EnvironmentCreationException( "Invalid node group placement" );
        }
        else
        {
            for ( Map.Entry<UUID, Set<NodeGroup>> placementKey : topologyJson.getNodeGroupPlacement().entrySet() )
            {
                UUID peerId = placementKey.getKey();
                Set<NodeGroup> nodeGroups = placementKey.getValue();
                if ( peerId == null )
                {
                    throw new EnvironmentCreationException( "Invalid peer id" );
                }
                else if ( peerManager.getPeer( peerId ) == null )
                {
                    throw new EnvironmentCreationException( String.format( "Peer %s not found", peerId ) );
                }
                for ( NodeGroup nodeGroup : nodeGroups )
                {
                    if ( Strings.isNullOrEmpty( nodeGroup.getName() ) )
                    {
                        throw new EnvironmentCreationException( "Invalid node group name" );
                    }
                    else if ( Strings.isNullOrEmpty( nodeGroup.getDomainName() ) )
                    {
                        throw new EnvironmentCreationException( "Invalid domain name" );
                    }
                    else if ( nodeGroup.getNumberOfContainers() <= 0 )
                    {
                        throw new EnvironmentCreationException( "Invalid number of containers" );
                    }
                    else if ( Strings.isNullOrEmpty( nodeGroup.getTemplateName() ) )
                    {
                        throw new EnvironmentCreationException( "Invalid templateName" );
                    }
                    else if ( templateRegistry.getTemplate( nodeGroup.getTemplateName() ) == null )
                    {
                        throw new EnvironmentCreationException(
                                String.format( "Template %s does not exist", nodeGroup.getTemplateName() ) );
                    }
                    else if ( nodeGroup.getContainerPlacementStrategy() == null )
                    {
                        throw new EnvironmentCreationException( "Invalid node container placement strategy" );
                    }
                }
            }
        }
    }


    @Override
    public Response getContainerEnvironmentId( final String containerId )
    {
        if ( !UUIDUtil.isStringAUuid( containerId ) )
        {
            return Response.status( Response.Status.BAD_REQUEST )
                           .entity( JsonUtil.toJson( ERROR_KEY, "Invalid container id" ) ).build();
        }

        UUID hostId = UUID.fromString( containerId );

        Environment environment = findEnvironmentByContainerId( hostId );

        if ( environment != null )
        {
            return Response.ok( environment.getId().toString() ).build();
        }

        return Response.status( Response.Status.NOT_FOUND ).build();
    }


    @Override
    public Response getEnvironment( final String environmentId )
    {
        if ( !UUIDUtil.isStringAUuid( environmentId ) )
        {
            return Response.status( Response.Status.BAD_REQUEST )
                           .entity( JsonUtil.toJson( ERROR_KEY, "Invalid environment id" ) ).build();
        }

        UUID envId = UUID.fromString( environmentId );

        try
        {
            Environment environment = environmentManager.findEnvironment( envId );

            return Response.ok( JsonUtil.toJson(
                    new EnvironmentJson( environment.getId(), environment.getName(), environment.getStatus(),
                            environment.getSshKey(),
                            convertContainersToContainerJson( environment.getContainerHosts() ) ) ) ).build();
        }
        catch ( EnvironmentNotFoundException e )
        {
            return Response.status( Response.Status.NOT_FOUND ).build();
        }
    }


    @Override
    public Response destroyEnvironment( final String environmentId )
    {
        if ( !UUIDUtil.isStringAUuid( environmentId ) )
        {
            return Response.status( Response.Status.BAD_REQUEST )
                           .entity( JsonUtil.toJson( ERROR_KEY, "Invalid environment id" ) ).build();
        }

        UUID envId = UUID.fromString( environmentId );

        try
        {
            environmentManager.destroyEnvironment( envId, false, false );

            return Response.ok().build();
        }
        catch ( EnvironmentNotFoundException e )
        {
            return Response.status( Response.Status.NOT_FOUND ).build();
        }
        catch ( EnvironmentDestructionException e )
        {
            return Response.serverError().entity( JsonUtil.toJson( ERROR_KEY, e.getMessage() ) ).build();
        }
    }


    @Override
    public Response destroyContainer( final String containerId )
    {
        if ( !UUIDUtil.isStringAUuid( containerId ) )
        {
            return Response.status( Response.Status.BAD_REQUEST )
                           .entity( JsonUtil.toJson( ERROR_KEY, "Invalid container id" ) ).build();
        }
        UUID hostId = UUID.fromString( containerId );

        Environment environment = findEnvironmentByContainerId( hostId );

        if ( environment != null )
        {
            try
            {
                ContainerHost containerHost = environment.getContainerHostById( hostId );

                environmentManager.destroyContainer( containerHost, false, false );

                return Response.ok().build();
            }
            catch ( ContainerHostNotFoundException | EnvironmentNotFoundException | EnvironmentModificationException e )
            {
                return Response.serverError().entity( JsonUtil.toJson( ERROR_KEY, e.getMessage() ) ).build();
            }
        }

        return Response.status( Response.Status.NOT_FOUND ).build();
    }


    private Environment findEnvironmentByContainerId( UUID containerId )
    {
        for ( Environment environment : environmentManager.getEnvironments() )
        {
            for ( ContainerHost containerHost : environment.getContainerHosts() )
            {
                if ( containerHost.getId().equals( containerId ) )
                {
                    return environment;
                }
            }
        }

        return null;
    }


    @Override
    public Response growEnvironment( final String environmentId, final String topologyJsonString )
    {

        if ( !UUIDUtil.isStringAUuid( environmentId ) )
        {
            return Response.status( Response.Status.BAD_REQUEST )
                           .entity( JsonUtil.toJson( ERROR_KEY, "Invalid environment id" ) ).build();
        }

        TopologyJson topologyJson;

        try
        {
            topologyJson = JsonUtil.fromJson( topologyJsonString, TopologyJson.class );
            checkTopology( topologyJson );
        }
        catch ( JsonSyntaxException | NullPointerException | EnvironmentCreationException e )
        {
            return Response.status( Response.Status.BAD_REQUEST ).entity( JsonUtil.toJson( ERROR_KEY, e.getMessage() ) )
                           .build();
        }

        try
        {
            UUID envId = UUID.fromString( environmentId );

            Topology topology = new Topology();

            for ( Map.Entry<UUID, Set<NodeGroup>> placementEntry : topologyJson.getNodeGroupPlacement().entrySet() )
            {
                Peer peer = peerManager.getPeer( placementEntry.getKey() );
                for ( NodeGroup nodeGroup : placementEntry.getValue() )
                {
                    topology.addNodeGroupPlacement( peer, nodeGroup );
                }
            }

            Set<ContainerHost> newContainers = environmentManager.growEnvironment( envId, topology, false );

            return Response.ok( JsonUtil.toJson( convertContainersToContainerJson( newContainers ) ) ).build();
        }
        catch ( EnvironmentNotFoundException e )
        {
            return Response.status( Response.Status.NOT_FOUND ).build();
        }
        catch ( EnvironmentModificationException e )
        {
            return Response.serverError().entity( JsonUtil.toJson( ERROR_KEY, e.getMessage() ) ).build();
        }
    }


    @Override
    public Response getContainerState( final String containerId )
    {
        if ( !UUIDUtil.isStringAUuid( containerId ) )
        {
            return Response.status( Response.Status.BAD_REQUEST )
                           .entity( JsonUtil.toJson( ERROR_KEY, "Invalid container id" ) ).build();
        }

        UUID hostId = UUID.fromString( containerId );

        Environment environment = findEnvironmentByContainerId( hostId );

        if ( environment != null )
        {
            try
            {
                ContainerHost containerHost = environment.getContainerHostById( hostId );

                return Response.ok().entity( JsonUtil.toJson( "STATE", containerHost.getState() ) ).build();
            }
            catch ( ContainerHostNotFoundException | PeerException e )
            {
                return Response.serverError().entity( JsonUtil.toJson( ERROR_KEY, e.getMessage() ) ).build();
            }
        }

        return Response.status( Response.Status.NOT_FOUND ).build();
    }


    @Override
    public Response setSshKey( final String environmentId, final String key )
    {
        if ( !UUIDUtil.isStringAUuid( environmentId ) )
        {
            return Response.status( Response.Status.BAD_REQUEST )
                           .entity( JsonUtil.toJson( ERROR_KEY, "Invalid environment id" ) ).build();
        }
        else if ( Strings.isNullOrEmpty( key ) )
        {
            return Response.status( Response.Status.BAD_REQUEST )
                           .entity( JsonUtil.toJson( ERROR_KEY, "Invalid ssh key" ) ).build();
        }

        UUID envId = UUID.fromString( environmentId );

        try
        {
            environmentManager.setSshKey( envId, key, false );

            return Response.ok().build();
        }
        catch ( EnvironmentNotFoundException e )
        {
            return Response.status( Response.Status.NOT_FOUND ).build();
        }
        catch ( EnvironmentModificationException e )
        {
            return Response.serverError().entity( JsonUtil.toJson( ERROR_KEY, e.getMessage() ) ).build();
        }
    }


    @Override
    public Response removeSshKey( final String environmentId )
    {
        if ( !UUIDUtil.isStringAUuid( environmentId ) )
        {
            return Response.status( Response.Status.BAD_REQUEST )
                           .entity( JsonUtil.toJson( ERROR_KEY, "Invalid environment id" ) ).build();
        }

        UUID envId = UUID.fromString( environmentId );

        try
        {
            environmentManager.setSshKey( envId, null, false );

            return Response.ok().build();
        }
        catch ( EnvironmentNotFoundException e )
        {
            return Response.status( Response.Status.NOT_FOUND ).build();
        }
        catch ( EnvironmentModificationException e )
        {
            return Response.serverError().entity( JsonUtil.toJson( ERROR_KEY, e.getMessage() ) ).build();
        }
    }


    private Set<ContainerJson> convertContainersToContainerJson( Set<ContainerHost> containerHosts )
    {
        Set<ContainerJson> jsonSet = Sets.newHashSet();
        for ( ContainerHost containerHost : containerHosts )
        {
            ContainerHostState state = ContainerHostState.UNKNOWN;

            try
            {
                state = containerHost.getState();
            }
            catch ( PeerException e )
            {
                LOG.error( "Failed to obtain container state", e );
            }

            jsonSet.add( new ContainerJson( containerHost.getId(), UUID.fromString( containerHost.getEnvironmentId() ),
                    containerHost.getHostname(), state, containerHost.getIpByInterfaceName( "eth0" ),
                    containerHost.getTemplateName() ) );
        }
        return jsonSet;
    }
}