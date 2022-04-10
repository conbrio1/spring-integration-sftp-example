package com.example.sftp.config;

import com.jcraft.jsch.ChannelSftp.LsEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.gateway.SftpOutboundGateway;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpFileInfo;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.handler.annotation.Header;

import java.io.File;
import java.util.List;

@Configuration
@EnableIntegration
@IntegrationComponentScan
public class SftpConfig {

    @Value("${sftp.host}")
    private String host;

    @Value("${sftp.port}")
    private String port;

    @Value("${sftp.user}")
    private String user;

    @Value("${sftp.password}")
    private String password;

    @Bean
    public SessionFactory<LsEntry> sftpSessionFactory() {
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
        factory.setHost(host);
        factory.setPort(Integer.parseInt(port));
        factory.setUser(user);
        factory.setPassword(password);
        factory.setAllowUnknownKeys(true);
        CachingSessionFactory<LsEntry> cachingSessionFactory = new CachingSessionFactory<>(factory);
        cachingSessionFactory.setTestSession(true);
        return cachingSessionFactory;
    }

    @Bean
    @ServiceActivator(inputChannel = "sftpPutChannel")
    public MessageHandler sftpPutHandler() {
        // https://stackoverflow.com/questions/46650004/spring-integration-ftp-create-dynamic-directory-with-remote-directory-expressi
        // https://docs.spring.io/spring-integration/reference/html/sftp.html#configuring-with-java-configuration-2

        SftpOutboundGateway sftpOutboundGateway = new SftpOutboundGateway(sftpSessionFactory(), "put", "");
        sftpOutboundGateway.setAutoCreateDirectory(true);
        sftpOutboundGateway.setRemoteDirectoryExpression(new SpelExpressionParser().parseExpression("headers['remote-target-dir']"));
        sftpOutboundGateway.setFileNameGenerator(message -> message.getHeaders().get("target-file-name", String.class));
        return sftpOutboundGateway;
    }

    @Bean
    @ServiceActivator(inputChannel = "sftpListChannel")
    public MessageHandler sftpListHandler() {
        return new SftpOutboundGateway(sftpSessionFactory(), "ls", "payload");
    }

    @MessagingGateway
    public interface UploadGateway {

        @Gateway(requestChannel = "sftpPutChannel")
        String sendFileToSftp(File file, @Header("remote-target-dir") String remotePath, @Header("target-file-name") String fileName);

        @Gateway(requestChannel = "sftpListChannel")
        List<SftpFileInfo> listFile(String remotePath);
    }

}
