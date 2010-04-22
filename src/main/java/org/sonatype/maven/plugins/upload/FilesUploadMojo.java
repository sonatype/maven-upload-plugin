package org.sonatype.maven.plugins.upload;

import java.io.File;

import org.apache.commons.httpclient.HttpClient;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * Uploads multiple files to remote repository.
 * 
 * @goal upload-files
 */
public class FilesUploadMojo
    extends AbstractUploadMojo
{
    /** @parameter default-value="${project.basedir}" */
    private File basedir;

    /** @parameter */
    private String[] includes;

    /** @parameter */
    private String[] excludes;

    /** @parameter */
    private String repositoryBasepath;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        ArtifactRepository repository = getArtifactRepository();

        HttpClient client = getHttpClient( repository );

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( basedir );
        scanner.addDefaultExcludes();
        scanner.setIncludes( includes );
        scanner.setExcludes( excludes );
        scanner.scan();

        String baseUrl = repository.getUrl();
        if ( !baseUrl.endsWith( "/" ) )
        {
            baseUrl = baseUrl + "/";
        }
        if ( repositoryBasepath != null )
        {
            baseUrl = baseUrl + repositoryBasepath;
        }
        if ( !baseUrl.endsWith( "/" ) )
        {
            baseUrl = baseUrl + "/";
        }

        for ( String relPath : scanner.getIncludedFiles() )
        {
            String path = relPath.replace( '\\', '/' );
            uploadFile( client, new File( basedir, path ), baseUrl + path );
        }
    }

}
