package org.sonatype.maven.plugins.upload;

import java.io.File;

import org.apache.http.impl.client.CloseableHttpClient;
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
    /** @parameter expression="${upload.basedir}" default-value="${project.basedir}" */
    private File basedir;

    /** @parameter expression="${upload.includes}" */
    private String[] includes;

    /** @parameter expression="${upload.excludes}" */
    private String[] excludes;

    /** @parameter expression="${upload.repositoryBasePath}" */
    private String repositoryBasepath;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        ArtifactRepository repository = getArtifactRepository();

        CloseableHttpClient client = getHttpClient( repository );

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
