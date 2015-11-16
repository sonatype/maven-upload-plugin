package org.sonatype.maven.plugins.upload;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
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

    /** @parameter property="session" */
    protected MavenSession session;

    /** @parameter property="upload.serverId" */
    protected String serverId;

    /** @parameter property="upload.repositoryUrl" */
    protected String repositoryUrl;

    protected CloseableHttpClient getHttpClient( ArtifactRepository repository )
        throws MojoExecutionException
    {
        CloseableHttpClient client;

        Authentication authentication = repository.getAuthentication();
        if ( authentication != null )
        {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    new AuthScope(AuthScope.ANY),
                    new UsernamePasswordCredentials(authentication.getUsername(),authentication.getPassword()));
            client = HttpClients.custom()
                    .setDefaultCredentialsProvider(credsProvider)
                    .build();
        } else {
        	client = HttpClients.createDefault();
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

    protected void uploadFile( CloseableHttpClient client, File file, String targetUrl )
        throws MojoExecutionException
    {
        getLog().info( "Uploading " + file.getAbsolutePath() + " to " + targetUrl );
        HttpPut putRequest = new HttpPut(targetUrl);
        CloseableHttpResponse response = null;
        try
        {
            ContentType contentType = null;
            if ( file.getName().endsWith( ".xml" ) )
            {
                contentType = ContentType.APPLICATION_XML;
            }

            putRequest.setEntity( new FileEntity( file , contentType ) );

            response = client.execute(putRequest);

            int status = response.getStatusLine().getStatusCode();
            if ( status < 200 || status > 299 )
            {
                String message = "Could not upload file: " + response.getStatusLine().toString();
                getLog().error( message );
                String responseBody = EntityUtils.toString(response.getEntity());
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
           	putRequest.releaseConnection();
        }
    }

}
