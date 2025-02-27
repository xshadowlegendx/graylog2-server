/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.datanode;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.ParameterException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.Validator;
import com.github.joschi.jadconfig.ValidatorMethod;
import com.github.joschi.jadconfig.converters.IntegerConverter;
import com.github.joschi.jadconfig.converters.StringListConverter;
import com.github.joschi.jadconfig.converters.StringSetConverter;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.StringNotBlankValidator;
import com.github.joschi.jadconfig.validators.URIAbsoluteValidator;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.InetAddresses;
import org.graylog.datanode.configuration.BaseConfiguration;
import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog2.Configuration.SafeClassesValidator;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.SuppressForbidden;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Helper class to hold configuration of DataNode
 */
@SuppressWarnings("FieldMayBeFinal")
public class Configuration extends BaseConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);
    public static final String TRANSPORT_CERTIFICATE_PASSWORD_PROPERTY = "transport_certificate_password";
    public static final String HTTP_CERTIFICATE_PASSWORD_PROPERTY = "http_certificate_password";

    public static final int DATANODE_DEFAULT_PORT = 8999;
    public static final String DEFAULT_BIND_ADDRESS = "0.0.0.0";

    public static final String OVERRIDE_HEADER = "X-Graylog-Server-URL";
    public static final String PATH_WEB = "";
    public static final String PATH_API = "api/";

    @Parameter(value = "installation_source", validator = StringNotBlankValidator.class)
    private String installationSource = "unknown";

    @Parameter(value = "insecure_startup")
    private boolean insecureStartup = false;

    @Parameter(value = "skip_preflight_checks")
    private boolean skipPreflightChecks = false;

    @Parameter(value = "shutdown_timeout", validator = PositiveIntegerValidator.class)
    protected int shutdownTimeout = 30000;

    @Parameter(value = "opensearch_location")
    private String opensearchDistributionRoot = "dist";

    @Parameter(value = "opensearch_plugins_location", validators = DirectoryReadableValidator.class)
    private Path opensearchPluginsDir = Path.of("dist/plugins");

    @Parameter(value = "opensearch_data_location", required = true, validators = DirectoryWritableValidator.class)
    private Path opensearchDataLocation = Path.of("datanode/data");

    @Parameter(value = "opensearch_logs_location", required = true, validators = DirectoryWritableValidator.class)
    private Path opensearchLogsLocation = Path.of("datanode/logs");

    @Parameter(value = "opensearch_config_location", required = true, validators = DirectoryWritableValidator.class)
    private Path opensearchConfigLocation = Path.of("datanode/config");

    @Parameter(value = "config_location", validators = DirectoryReadableValidator.class)
    private Path configLocation = null;

    @Parameter(value = "native_lib_dir", required = true)
    private Path nativeLibDir = Path.of("native_libs");

    @Parameter(value = "process_logs_buffer_size")
    private Integer opensearchProcessLogsBufferSize = 500;


    @Parameter(value = "node_name")
    private String datanodeNodeName;

    /**
     * Comma separated list of opensearch nodes that are eligible as manager nodes.
     */
    @Parameter(value = "initial_cluster_manager_nodes")
    private String initialClusterManagerNodes;

    @Parameter(value = "opensearch_http_port", converter = IntegerConverter.class)
    private int opensearchHttpPort = 9200;


    @Parameter(value = "opensearch_transport_port", converter = IntegerConverter.class)
    private int opensearchTransportPort = 9300;

    @Parameter(value = "opensearch_discovery_seed_hosts", converter = StringListConverter.class)
    private List<String> opensearchDiscoverySeedHosts = Collections.emptyList();

    @Parameter(value = "opensearch_network_host")
    private String opensearchNetworkHost = null;

    @Parameter(value = "transport_certificate")
    private String datanodeTransportCertificate = null;

    @Parameter(value = TRANSPORT_CERTIFICATE_PASSWORD_PROPERTY)
    private String datanodeTransportCertificatePassword;

    @Parameter(value = "http_certificate")
    private String datanodeHttpCertificate = null;

    @Parameter(value = HTTP_CERTIFICATE_PASSWORD_PROPERTY)
    private String datanodeHttpCertificatePassword;

    @Parameter(value = "stale_leader_timeout", validators = PositiveIntegerValidator.class)
    private Integer staleLeaderTimeout = 2000;

    @Parameter(value = "root_password_sha2")
    private String rootPasswordSha2;

    @Parameter(value = "user_password_default_algorithm")
    private String userPasswordDefaultAlgorithm = "bcrypt";

    @Parameter(value = "user_password_bcrypt_salt_size", validators = PositiveIntegerValidator.class)
    private int userPasswordBCryptSaltSize = 10;

    @Parameter(value = "password_secret", required = true, validators = StringNotBlankValidator.class)
    private String passwordSecret;

    @Parameter(value = "indexer_jwt_auth_token_caching_duration")
    Duration indexerJwtAuthTokenCachingDuration = Duration.seconds(60);

    @Parameter(value = "indexer_jwt_auth_token_expiration_duration")
    Duration indexerJwtAuthTokenExpirationDuration = Duration.seconds(180);

    @Parameter(value = "node_id_file", validators = NodeIdFileValidator.class)
    private String nodeIdFile = "data/node-id";

    @Parameter(value = "root_username")
    private String rootUsername = "admin";

    @Parameter(value = "root_timezone")
    private DateTimeZone rootTimeZone = DateTimeZone.UTC;

    @Parameter(value = "root_email")
    private String rootEmail = "";

    @Parameter(value = "bind_address", required = true)
    private String bindAddress = DEFAULT_BIND_ADDRESS;

    @Parameter(value = "datanode_http_port", required = true)
    private int datanodeHttpPort = DATANODE_DEFAULT_PORT;

    @Parameter(value = "hostname")
    private String hostname = null;

    @Parameter(value = "clustername")
    private String clustername = "datanode-cluster";

    @Parameter(value = "http_publish_uri", validator = URIAbsoluteValidator.class)
    private URI httpPublishUri;

    @Parameter(value = "http_enable_cors")
    private boolean httpEnableCors = false;

    @Parameter(value = "http_enable_gzip")
    private boolean httpEnableGzip = true;

    @Parameter(value = "http_max_header_size", required = true, validator = PositiveIntegerValidator.class)
    private int httpMaxHeaderSize = 8192;

    @Parameter(value = "http_thread_pool_size", required = true, validator = PositiveIntegerValidator.class)
    private int httpThreadPoolSize = 64;

    @Parameter(value = "http_selector_runners_count", required = true, validator = PositiveIntegerValidator.class)
    private int httpSelectorRunnersCount = 1;

    @Parameter(value = "http_enable_tls")
    private boolean httpEnableTls = false;

    @Parameter(value = "http_tls_cert_file")
    private Path httpTlsCertFile;

    @Parameter(value = "http_tls_key_file")
    private Path httpTlsKeyFile;

    @Parameter(value = "http_tls_key_password")
    private String httpTlsKeyPassword;

    @Parameter(value = "http_external_uri")
    private URI httpExternalUri;

    @Parameter(value = "http_allow_embedding")
    private boolean httpAllowEmbedding = false;

    /**
     * Classes considered safe to load by name. A set of prefixes matched against the fully qualified class name.
     */
    @Parameter(value = org.graylog2.Configuration.SAFE_CLASSES, converter = StringSetConverter.class, validators = SafeClassesValidator.class)
    private Set<String> safeClasses = Set.of("org.graylog.", "org.graylog2.");

    @Parameter(value = "metrics_timestamp")
    private String metricsTimestamp = "timestamp";

    @Parameter(value = "metrics_stream")
    private String metricsStream = "gl-datanode-metrics";

    @Parameter(value = "metrics_retention")
    private String metricsRetention = "14d";

    @Parameter(value = "metrics_daily_retention")
    private String metricsDailyRetention = "365d";

    @Parameter(value = "metrics_daily_index")
    private String metricsDailyIndex = "gl-datanode-metrics-daily";

    @Parameter(value = "metrics_policy")
    private String metricsPolicy = "gl-datanode-metrics-ism";

    @Parameter(value = "node_search_cache_size")
    private String searchCacheSize = "10gb";

    /**
     * https://opensearch.org/docs/latest/tuning-your-cluster/availability-and-recovery/snapshots/snapshot-restore/#shared-file-system
     */
    @Parameter(value = "path_repo", converter = StringListConverter.class)
    private List<String> pathRepo;

    @Parameter(value = "opensearch_indices_query_bool_max_clause_count")
    private Integer indicesQueryBoolMaxClauseCount = 32768;

    public Integer getIndicesQueryBoolMaxClauseCount() {
        return indicesQueryBoolMaxClauseCount;
    }

    public boolean isInsecureStartup() {
        return insecureStartup;
    }

    public Integer getStaleLeaderTimeout() {
        return staleLeaderTimeout;
    }

    public String getInstallationSource() {
        return installationSource;
    }

    public boolean getSkipPreflightChecks() {
        return skipPreflightChecks;
    }

    public int getShutdownTimeout() {
        return shutdownTimeout;
    }

    public String getOpensearchDistributionRoot() {
        return opensearchDistributionRoot;
    }

    @Nullable
    public Path getOpensearchPluginsDir() {
        return opensearchPluginsDir;
    }

    /**
     * Use {@link DatanodeDirectories} to obtain a reference to this directory.
     */
    public Path getOpensearchConfigLocation() {
        return opensearchConfigLocation;
    }


    /**
     * This is a pointer to a directory holding configuration files (and certificates) for the datanode itself.
     * We treat it as read only for the datanode and should never persist anything in it.
     * Use {@link DatanodeDirectories} to obtain a reference to this directory.
     */
    @Nullable
    public Path getDatanodeConfigurationLocation() {
        return configLocation;
    }

    /**
     * Use {@link DatanodeDirectories} to obtain a reference to this directory.
     */
    public Path getOpensearchDataLocation() {
        return opensearchDataLocation;
    }

    /**
     * Use {@link DatanodeDirectories} to obtain a reference to this directory.
     */
    public Path getOpensearchLogsLocation() {
        return opensearchLogsLocation;
    }

    public Integer getProcessLogsBufferSize() {
        return opensearchProcessLogsBufferSize;
    }

    public String getPasswordSecret() {
        return passwordSecret;
    }

    public Duration getIndexerJwtAuthTokenCachingDuration() {
        return indexerJwtAuthTokenCachingDuration;
    }

    public Duration getIndexerJwtAuthTokenExpirationDuration() {
        return indexerJwtAuthTokenExpirationDuration;
    }

    @ValidatorMethod
    @SuppressWarnings("unused")
    public void validatePasswordSecret() throws ValidationException {
        if (passwordSecret == null || passwordSecret.length() < 64) {
            throw new ValidationException("The minimum length for \"password_secret\" is 64 characters.");
        }
    }

    public String getNodeIdFile() {
        return nodeIdFile;
    }

    public String getRootUsername() {
        return rootUsername;
    }

    public DateTimeZone getRootTimeZone() {
        return rootTimeZone;
    }

    public String getRootEmail() {
        return rootEmail;
    }

    public String getDatanodeNodeName() {
        return datanodeNodeName != null && !datanodeNodeName.isBlank() ? datanodeNodeName : getHostname();
    }

    public String getInitialClusterManagerNodes() {
        return initialClusterManagerNodes;
    }

    public int getOpensearchHttpPort() {
        return opensearchHttpPort;
    }

    public int getOpensearchTransportPort() {
        return opensearchTransportPort;
    }

    public List<String> getOpensearchDiscoverySeedHosts() {
        return opensearchDiscoverySeedHosts;
    }

    public String getDatanodeTransportCertificate() {
        return datanodeTransportCertificate;
    }

    public String getDatanodeTransportCertificatePassword() {
        return datanodeTransportCertificatePassword;
    }

    public String getDatanodeHttpCertificate() {
        return datanodeHttpCertificate;
    }

    public String getDatanodeHttpCertificatePassword() {
        return datanodeHttpCertificatePassword;
    }

    public Optional<String> getOpensearchNetworkHost() {
        return Optional.ofNullable(opensearchNetworkHost);
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public int getDatanodeHttpPort() {
        return datanodeHttpPort;
    }

    public String getClustername() {
        return clustername;
    }


    public String getMetricsTimestamp() {
        return metricsTimestamp;
    }

    public String getMetricsStream() {
        return metricsStream;
    }

    public String getMetricsRetention() {
        return metricsRetention;
    }

    public String getMetricsDailyRetention() {
        return metricsDailyRetention;
    }

    public String getMetricsDailyIndex() {
        return metricsDailyIndex;
    }

    public String getMetricsPolicy() {
        return metricsPolicy;
    }

    public Path getNativeLibDir() {
        return nativeLibDir;
    }

    public static class NodeIdFileValidator implements Validator<String> {
        @Override
        public void validate(String name, String path) throws ValidationException {
            if (path == null) {
                return;
            }
            final File file = Paths.get(path).toFile();
            final StringBuilder b = new StringBuilder();

            if (!file.exists()) {
                final File parent = file.getParentFile();
                if (!parent.isDirectory()) {
                    throw new ValidationException("Parent path " + parent + " for Node ID file at " + path + " is not a directory");
                } else {
                    if (!parent.canRead()) {
                        throw new ValidationException("Parent directory " + parent + " for Node ID file at " + path + " is not readable");
                    }
                    if (!parent.canWrite()) {
                        throw new ValidationException("Parent directory " + parent + " for Node ID file at " + path + " is not writable");
                    }

                    // parent directory exists and is readable and writable
                    return;
                }
            }

            if (!file.isFile()) {
                b.append("a file");
            }
            final boolean readable = file.canRead();
            final boolean writable = file.canWrite();
            if (!readable) {
                if (b.length() > 0) {
                    b.append(", ");
                }
                b.append("readable");
            }
            final boolean empty = file.length() == 0;
            if (!writable && readable && empty) {
                if (b.length() > 0) {
                    b.append(", ");
                }
                b.append("writable, but it is empty");
            }
            if (b.length() == 0) {
                // all good
                return;
            }
            throw new ValidationException("Node ID file at path " + path + " isn't " + b + ". Please specify the correct path or change the permissions");
        }
    }

    public String getUriScheme() {
        return isHttpEnableTls() ? "https" : "http";
    }

    @Nullable
    private InetAddress toInetAddress(String host) {
        try {
            return InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            LOG.debug("Couldn't resolve \"{}\"", host, e);
            return null;
        }
    }

    public URI getHttpPublishUri() {
        if (httpPublishUri == null) {
            final URI defaultHttpUri = getDefaultHttpUri();
            LOG.debug("No \"http_publish_uri\" set. Using default <{}>.", defaultHttpUri);
            return defaultHttpUri;
        } else {
            final InetAddress inetAddress = toInetAddress(httpPublishUri.getHost());
            if (Tools.isWildcardInetAddress(inetAddress)) {
                final URI defaultHttpUri = getDefaultHttpUri(httpPublishUri.getPath());
                LOG.warn("\"{}\" is not a valid setting for \"http_publish_uri\". Using default <{}>.", httpPublishUri, defaultHttpUri);
                return defaultHttpUri;
            } else {
                return Tools.normalizeURI(httpPublishUri, httpPublishUri.getScheme(), DATANODE_DEFAULT_PORT, httpPublishUri.getPath());
            }
        }
    }

    @VisibleForTesting
    URI getDefaultHttpUri() {
        return getDefaultHttpUri("/");
    }

    private URI getDefaultHttpUri(String path) {
        final URI publishUri;
        final InetAddress inetAddress = toInetAddress(bindAddress);
        if (inetAddress != null && Tools.isWildcardInetAddress(inetAddress)) {
            final InetAddress guessedAddress;
            try {
                guessedAddress = Tools.guessPrimaryNetworkAddress(inetAddress instanceof Inet4Address);

                if (guessedAddress.isLoopbackAddress()) {
                    LOG.debug("Using loopback address {}", guessedAddress);
                }
            } catch (Exception e) {
                LOG.error("Could not guess primary network address for \"http_publish_uri\". Please configure it in your Graylog configuration.", e);
                throw new ParameterException("No http_publish_uri.", e);
            }

            try {
                publishUri = new URI(
                        getUriScheme(),
                        null,
                        guessedAddress.getHostAddress(),
                        datanodeHttpPort,
                        path,
                        null,
                        null
                );
            } catch (URISyntaxException e) {
                throw new RuntimeException("Invalid http_publish_uri.", e);
            }
        } else {
            try {
                publishUri = new URI(
                        getUriScheme(),
                        null,
                        bindAddress,
                        datanodeHttpPort,
                        path,
                        null,
                        null
                );
            } catch (URISyntaxException e) {
                throw new RuntimeException("Invalid http_publish_uri.", e);
            }
        }

        return publishUri;
    }

    public boolean isHttpEnableCors() {
        return httpEnableCors;
    }

    public boolean isHttpEnableGzip() {
        return httpEnableGzip;
    }

    public int getHttpMaxHeaderSize() {
        return httpMaxHeaderSize;
    }

    public int getHttpThreadPoolSize() {
        return httpThreadPoolSize;
    }

    public int getHttpSelectorRunnersCount() {
        return httpSelectorRunnersCount;
    }

    public boolean isHttpEnableTls() {
        return httpEnableTls;
    }

    public Path getHttpTlsCertFile() {
        return httpTlsCertFile;
    }

    public Path getHttpTlsKeyFile() {
        return httpTlsKeyFile;
    }

    public String getHttpTlsKeyPassword() {
        return httpTlsKeyPassword;
    }

    @ValidatorMethod
    @SuppressWarnings("unused")
    public void validateTlsConfig() throws ValidationException {
        if (isHttpEnableTls()) {
            if (!isRegularFileAndReadable(getHttpTlsKeyFile())) {
                throw new ValidationException("Unreadable or missing HTTP private key: " + getHttpTlsKeyFile());
            }

            if (!isRegularFileAndReadable(getHttpTlsCertFile())) {
                throw new ValidationException("Unreadable or missing HTTP X.509 certificate: " + getHttpTlsCertFile());
            }
        }
    }

    private boolean isRegularFileAndReadable(Path path) {
        return path != null && Files.isRegularFile(path) && Files.isReadable(path);
    }

    @SuppressForbidden("Deliberate invocation of DNS lookup")
    public String getHostname() {
        if (hostname != null && !hostname.isBlank()) {
            // config setting always takes precedence
            return hostname;
        }

        if (DEFAULT_BIND_ADDRESS.equals(bindAddress)) {
            // no hostname is set, bind address is to 0.0.0.0 -> return host name, the OS finds
            return Tools.getLocalCanonicalHostname();
        }

        if (InetAddresses.isInetAddress(bindAddress)) {
            // bindaddress is a real IP, resolving the hostname
            try {
                InetAddress addr = InetAddress.getByName(bindAddress);
                return addr.getHostName();
            } catch (UnknownHostException e) {
                final var hostname = Tools.getLocalCanonicalHostname();
                LOG.error("Could not resolve {} to hostname, check your DNS. Using {} instead.", bindAddress, hostname);
                return hostname;
            }
        }

        // bindaddress is configured as the hostname
        return bindAddress;
    }

    public String getRootPasswordSha2() {
        return rootPasswordSha2;
    }


    public String getNodeSearchCacheSize() {
        return searchCacheSize;
    }

    public List<String> getPathRepo() {
        return pathRepo;
    }
}
