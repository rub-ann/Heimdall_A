package org.littleshoot.proxy.mitm;

import android.util.Log;

import java.io.File;
import java.io.IOException;

import timber.log.Timber;

/**
 * Parameter object holding personal informations given to a SSLEngineSource.
 * 
 * XXX consider to inline within the interface SslEngineSource, if MITM is core
 */
public class Authority {

    private final File keyStoreDir;

    private final String alias;

    private final char[] password;

    private final String commonName;

    private final String organization;

    private final String organizationalUnitName;

    private final String certOrganization;

    private final String certOrganizationalUnitName;

    /**
     * Create a parameter object with example certificate and certificate
     * authority informations
     */
    public Authority() {
        keyStoreDir = new File(".");
        alias = "littleproxy-mitm"; // proxy id
        password = "Be Your Own Lantern".toCharArray();
        organization = "LittleProxy-mitm"; // proxy name
        commonName = organization + ", describe proxy here"; // MITM is bad
                                                             // normally
        organizationalUnitName = "Certificate Authority";
        certOrganization = organization; // proxy name
        certOrganizationalUnitName = organization
                + ", describe proxy purpose here, since Man-In-The-Middle is bad normally.";
    }

    /**
     * Create a parameter object with the given certificate and certificate
     * authority informations
     */
    public Authority(File keyStoreDir, String alias, char[] password,
            String commonName, String organization,
            String organizationalUnitName, String certOrganization,
            String certOrganizationalUnitName) {
        super();
        this.keyStoreDir = keyStoreDir;
        this.alias = alias;
        this.password = password;
        this.commonName = commonName;
        this.organization = organization;
        this.organizationalUnitName = organizationalUnitName;
        this.certOrganization = certOrganization;
        this.certOrganizationalUnitName = certOrganizationalUnitName;
    }

    public File aliasFile(String fileExtension) {

        File outFile = new File(keyStoreDir, alias + fileExtension);
        // Get the parent directory: / aaa / bbb / ccc /

        File check = keyStoreDir;
        // Create a folder through the parent directory, not through outFile
        if (!check.exists()) {
            // Create a file when the file does not exist
            if (check.mkdirs() && !outFile.exists()) {
                //Create a file
                try {
                    if (!outFile.createNewFile()) Timber.e("File to create failure!");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return outFile;
        }else{

            return outFile;
        }

    }

    public String alias() {
        return alias;
    }

    public char[] password() {
        return password;
    }

    public String commonName() {
        return commonName;
    }

    public String organization() {
        return organization;
    }

    public String organizationalUnitName() {
        return organizationalUnitName;
    }

    public String certOrganisation() {
        return certOrganization;
    }

    public String certOrganizationalUnitName() {
        return certOrganizationalUnitName;
    }

}
