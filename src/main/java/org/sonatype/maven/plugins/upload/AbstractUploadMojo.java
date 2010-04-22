package org.sonatype.maven.plugins.upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.Authentication;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.repository.Proxy;
import org.apache.maven.repository.RepositorySystem;

public abstract class AbstractUploadMojo
    extends AbstractMojo
{
    /** @component */
    protected RepositorySystem repositorySystem;

    /** @component */
    protected ArtifactRepositoryLayout repositoryLayout;

    /** @parameter expression="${session}" */
    protected MavenSession session;

    /** @parameter expression="${upload.serverId}" */
    protected String serverId;

    /** @parameter expression="${upload.repositoryUrl}" */
    protected String repositoryUrl;

    protected HttpClient getHttpClient( ArtifactRepository repository )
        throws MojoExecutionException
    {
        HttpClient client = new HttpClient();

        Authentication authentication = repository.getAuthentication();
        if ( authentication != null )
        {
            client.getState().setCredentials(
                                              AuthScope.ANY,
                                              new UsernamePasswordCredentials( authentication.getUsername(),
                                                                               authentication.getPassword() ) );

            // workaround for https://issues.sonatype.org/browse/NXCM-1321
            client.getParams().setAuthenticationPreemptive( true );
        }

        Proxy proxy = repository.getProxy();
        if ( proxy != null )
        {
            throw new MojoExecutionException( "Proxy is not supporyed yet" );
        }
        return client;
    }

    protected ArtifactRepository getArtifactRepository()
    {
        ArtifactRepositoryPolicy policy = new ArtifactRepositoryPolicy();
        ArtifactRepository repository =
            repositorySystem.createArtifactRepository( serverId, repositoryUrl, repositoryLayout, policy, policy );

        List<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();
        repositories.add( repository );

        // repositorySystem.injectMirror( artifactRepositories, session.getRequest().getMirrors() );

        repositorySystem.injectProxy( repositories, session.getRequest().getProxies() );

        repositorySystem.injectAuthentication( repositories, session.getRequest().getServers() );

        repository = repositories.get( 0 );
        return repository;
    }

    protected void uploadFile( HttpClient client, File file, String targetUrl )
        throws MojoExecutionException
    {
        getLog().info( "Uploading " + file.getAbsolutePath() + " to " + targetUrl );
        PutMethod putMethod = new PutMethod( targetUrl );
        try
        {
            String contentType = null;
            if ( file.getName().endsWith( ".xml" ) )
            {
                contentType = "application/xml";
            }
            putMethod.setRequestEntity( new InputStreamRequestEntity( new FileInputStream( file ), contentType ) );

            client.executeMethod( putMethod );

            int status = putMethod.getStatusCode();
            if ( status < 200 || status > 299 )
            {
                String message = "Could not upload file: " + putMethod.getStatusLine().toString();
                getLog().error( message );
                String responseBody = putMethod.getResponseBodyAsString();
                if ( responseBody != null )
                {
                    getLog().info( responseBody );
                }
                throw new MojoExecutionException( message );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Could not upload file: ", e );
        }
        finally
        {
            putMethod.releaseConnection();
        }
    }

}
