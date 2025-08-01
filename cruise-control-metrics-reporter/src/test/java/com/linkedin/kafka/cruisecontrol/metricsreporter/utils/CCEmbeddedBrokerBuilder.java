/*
 * Copyright 2018 LinkedIn Corp. Licensed under the BSD 2-Clause License (the "License").  See License in the project root for license information.
 */

package com.linkedin.kafka.cruisecontrol.metricsreporter.utils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.coordinator.group.GroupCoordinatorConfig;
import org.apache.kafka.network.SocketServerConfigs;
import org.apache.kafka.server.config.ReplicationConfigs;
import org.apache.kafka.server.config.ServerConfigs;
import org.apache.kafka.server.config.ServerLogConfigs;
import org.apache.kafka.server.config.ZkConfigs;
import org.apache.kafka.storage.internals.log.CleanerConfig;


public class CCEmbeddedBrokerBuilder {
  private static final AtomicInteger BROKER_ID_COUNTER = new AtomicInteger();

  //mandatory fields
  private int _nodeId = BROKER_ID_COUNTER.incrementAndGet();
  private String _zkConnect;
  //storage config
  private File _logDirectory;
  //networking config
  private int _plaintextPort = -1;
  private int _sslPort = -1;
  private File _trustStore;
  private long _socketTimeoutMs = 1500;
  //feature control
  private boolean _enableControlledShutdown;
  private long _controlledShutdownRetryBackoff = 100;
  private boolean _enableDeleteTopic;
  private boolean _enableLogCleaner;
  //resource management
  // 2MB
  private long _logCleanerDedupBufferSize = 2097152;
  private String _rack;

  public CCEmbeddedBrokerBuilder() {
  }

  /**
   * Set node id.
   *
   * @param nodeId Node id to set.
   * @return This.
   */
  public CCEmbeddedBrokerBuilder nodeId(int nodeId) {
    _nodeId = nodeId;
    return this;
  }

  /**
   * Set Zk connect.
   *
   * @param zkConnect Zk connect String.
   * @return This.
   */
  public CCEmbeddedBrokerBuilder zkConnect(String zkConnect) {
    _zkConnect = zkConnect;
    return this;
  }

  /**
   * Set Zk connect.
   *
   * @param zk Embedded Zookeeper.
   * @return This.
   */
  public CCEmbeddedBrokerBuilder zkConnect(CCEmbeddedZookeeper zk) {
    return zkConnect(zk.connectionString());
  }

  /**
   * Set log directory.
   *
   * @param logDirectory Log directory to set.
   * @return This.
   */
  public CCEmbeddedBrokerBuilder logDirectory(File logDirectory) {
    _logDirectory = logDirectory;
    return this;
  }

  /**
   * Enable security protocol.
   *
   * @param protocol Security protocol to enable.
   * @return This.
   */
  public CCEmbeddedBrokerBuilder enable(SecurityProtocol protocol) {
    switch (protocol) {
      case PLAINTEXT:
        enablePlaintext();
        break;
      case SSL:
        enableSsl();
        break;
      default:
        throw new IllegalStateException("unhandled: " + protocol);
    }
    return this;
  }

  /**
   * Set plaintext port.
   *
   * @param plaintextPort Plaintext port.
   * @return This.
   */
  public CCEmbeddedBrokerBuilder plaintextPort(int plaintextPort) {
    _plaintextPort = plaintextPort;
    return this;
  }

  /**
   * Enable plaintext by setting its port to 0.
   * @return This.
   */
  public CCEmbeddedBrokerBuilder enablePlaintext() {
    return plaintextPort(0);
  }

  /**
   * Set SSL port.
   * @param sslPort SSL port to set.
   * @return This.
   */
  public CCEmbeddedBrokerBuilder sslPort(int sslPort) {
    _sslPort = sslPort;
    return this;
  }

  /**
   * Enable SSL by settin ssl port to 0.
   * @return This.
   */
  public CCEmbeddedBrokerBuilder enableSsl() {
    return sslPort(0);
  }

  /**
   * Set trust store.
   *
   * @param trustStore Trust store.
   * @return This
   */
  public CCEmbeddedBrokerBuilder trustStore(File trustStore) {
    _trustStore = trustStore;
    return this;
  }

  /**
   * Set socket timeout
   * @param socketTimeoutMs Socket timeout.
   * @return This
   */
  public CCEmbeddedBrokerBuilder socketTimeoutMs(long socketTimeoutMs) {
    _socketTimeoutMs = socketTimeoutMs;
    return this;
  }

  /**
   * Set enableControlledShutdown.
   *
   * @param enableControlledShutdown {@code true} to enable controlled shutdown, {@code false} otherwise.
   * @return This
   */
  public CCEmbeddedBrokerBuilder enableControlledShutdown(boolean enableControlledShutdown) {
    _enableControlledShutdown = enableControlledShutdown;
    return this;
  }

  /**
   * @param controlledShutdownRetryBackoff controlled shutdown retry backoff.
   * @return This
   */
  public CCEmbeddedBrokerBuilder controlledShutdownRetryBackoff(long controlledShutdownRetryBackoff) {
    _controlledShutdownRetryBackoff = controlledShutdownRetryBackoff;
    return this;
  }

  /**
   * Enable delete topic.
   *
   * @param enableDeleteTopic {@code true} to enable delete topic, {@code false} otherwise.
   * @return This
   */
  public CCEmbeddedBrokerBuilder enableDeleteTopic(boolean enableDeleteTopic) {
    _enableDeleteTopic = enableDeleteTopic;
    return this;
  }

  /**
   * Enable log cleaner.
   *
   * @param enableLogCleaner {@code true} to enable log cleaner, {@code false} otherwise.
   * @return This.
   */
  public CCEmbeddedBrokerBuilder enableLogCleaner(boolean enableLogCleaner) {
    _enableLogCleaner = enableLogCleaner;
    return this;
  }

  /**
   * Set log cleaner dedup buffer size.
   * @param logCleanerDedupBufferSize log cleaner dedup buffer size.
   * @return This.
   */
  public CCEmbeddedBrokerBuilder logCleanerDedupBufferSize(long logCleanerDedupBufferSize) {
    _logCleanerDedupBufferSize = logCleanerDedupBufferSize;
    return this;
  }

  /**
   * @param rack Rack to set.
   * @return This.
   */
  public CCEmbeddedBrokerBuilder rack(String rack) {
    _rack = rack;
    return this;
  }

  private void applyDefaults() {
    if (_logDirectory == null) {
      _logDirectory = CCKafkaTestUtils.newTempDir();
    }
  }

  private void validate() throws IllegalArgumentException {
    if (_plaintextPort < 0 && _sslPort < 0) {
      throw new IllegalArgumentException("at least one protocol must be used");
    }
    if (_logDirectory == null) {
      throw new IllegalArgumentException("log directory must be specified");
    }
    if (_zkConnect == null) {
      throw new IllegalArgumentException("zkConnect must be specified");
    }
  }

  /**
   * @return Config properties.
   */
  public Map<Object, Object> buildConfig() {
    applyDefaults();
    validate();

    Map<Object, Object> props = new HashMap<>();

    StringJoiner csvJoiner = new StringJoiner(",");
    if (_plaintextPort >= 0) {
      csvJoiner.add(SecurityProtocol.PLAINTEXT.name + "://localhost:" + _plaintextPort);
    }
    if (_sslPort >= 0) {
      csvJoiner.add(SecurityProtocol.SSL.name + "://localhost:" + _sslPort);
    }
    props.put(ServerConfigs.BROKER_ID_CONFIG, Integer.toString(_nodeId));
    props.put(SocketServerConfigs.LISTENERS_CONFIG, csvJoiner.toString());
    props.put(ServerLogConfigs.LOG_DIR_CONFIG, _logDirectory.getAbsolutePath());
    props.put(ZkConfigs.ZK_CONNECT_CONFIG, _zkConnect);
    props.put(ReplicationConfigs.REPLICA_SOCKET_TIMEOUT_MS_CONFIG, Long.toString(_socketTimeoutMs));
    props.put(ReplicationConfigs.CONTROLLER_SOCKET_TIMEOUT_MS_CONFIG, Long.toString(_socketTimeoutMs));
    props.put(ServerConfigs.CONTROLLED_SHUTDOWN_ENABLE_CONFIG, Boolean.toString(_enableControlledShutdown));
    props.put(ServerConfigs.DELETE_TOPIC_ENABLE_CONFIG, Boolean.toString(_enableDeleteTopic));
    props.put(ServerConfigs.CONTROLLED_SHUTDOWN_RETRY_BACKOFF_MS_CONFIG, Long.toString(_controlledShutdownRetryBackoff));
    props.put(CleanerConfig.LOG_CLEANER_DEDUPE_BUFFER_SIZE_PROP, Long.toString(_logCleanerDedupBufferSize));
    props.put(CleanerConfig.LOG_CLEANER_ENABLE_PROP, Boolean.toString(_enableLogCleaner));
    props.put(GroupCoordinatorConfig.OFFSETS_TOPIC_REPLICATION_FACTOR_CONFIG, "1");
    props.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
    if (_rack != null) {
      props.put(ServerConfigs.BROKER_RACK_CONFIG, _rack);
    }
    if (_trustStore != null || _sslPort > 0) {
      try {
        props.putAll(CCSslTestUtils.createSslConfig(false, true,
            CCSslTestUtils.ConnectionMode.SERVER, _trustStore, "server" + _nodeId));
        // Switch interbroker to ssl
        props.put(ReplicationConfigs.INTER_BROKER_SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name);
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }

    return props;
  }

  public CCEmbeddedBroker build() {
    return new CCEmbeddedBroker(buildConfig());
  }
}
