package io.subutai.common.host;


import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;


public class Snapshots
{
    @JsonProperty( "snapshots" )
    private Set<Snapshot> snapshots = Sets.newHashSet();


    public Set<Snapshot> getSnapshots()
    {
        return snapshots;
    }


    public void addSnapshot( Snapshot snapshot )
    {
        Preconditions.checkNotNull( snapshot );

        snapshots.add( snapshot );
    }
}
