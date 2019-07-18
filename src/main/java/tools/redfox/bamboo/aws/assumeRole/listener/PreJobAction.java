package tools.redfox.bamboo.aws.assumeRole.listener;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.*;
import com.atlassian.bamboo.chains.StageExecution;
import com.atlassian.bamboo.credentials.CredentialsAccessor;
import com.atlassian.bamboo.credentials.CredentialsData;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.agent.capability.Capability;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilitySetManager;
import com.atlassian.bamboo.variable.VariableDefinitionContext;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.concurrent.RejectedExecutionException;

public class PreJobAction implements com.atlassian.bamboo.chains.plugins.PreJobAction {
    private static final Logger logger = LoggerFactory.getLogger(PreJobAction.class);
    private CapabilitySetManager capabilitySetManager;
    private CredentialsAccessor credentialsAccessor;

    public PreJobAction(@ComponentImport CapabilitySetManager capabilitySetManager, @ComponentImport CredentialsAccessor credentialsAccessor) {
        this.capabilitySetManager = capabilitySetManager;
        this.credentialsAccessor = credentialsAccessor;
    }

    @Override
    public void execute(@NotNull StageExecution stageExecution, @NotNull BuildContext buildContext) {
        Capability roleCapability = getCapability(buildContext);
        String roleSessionName = "bamboo-" + buildContext.getPlanResultKey();

        if (roleCapability == null) {
            logger.debug("Ignore AWS credentials injection as no role defined for current job.");
            return;
        }

        try {
            AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.standard()
                    .withCredentials(getCredentials(roleCapability))
                    .build();

            AssumeRoleRequest roleRequest = new AssumeRoleRequest()
                    .withDurationSeconds(900)
                    .withRoleArn(roleCapability.getValue())
                    .withRoleSessionName(roleSessionName);

            Credentials sessionCredentials = stsClient.assumeRole(roleRequest).getCredentials();

            buildContext.getVariableContext().addLocalVariable("aws.accessKey.password", sessionCredentials.getAccessKeyId());
            buildContext.getVariableContext().addLocalVariable("aws.secretAccessKey.password", sessionCredentials.getSecretAccessKey());
            buildContext.getVariableContext().addLocalVariable("aws.sessionToken.password", sessionCredentials.getSessionToken());
            buildContext.getVariableContext().addLocalVariable(
                    "aws.env.password",
                    MessageFormat.format(
                            "AWS_ACCESS_KEY_ID={0} AWS_SECRET_ACCESS_KEY={1} AWS_SESSION_TOKEN={2}",
                            sessionCredentials.getAccessKeyId(),
                            sessionCredentials.getSecretAccessKey(),
                            sessionCredentials.getSessionToken()
                    )
            );
        } catch (SdkClientException e) {
            buildContext.getBuildResult().addBuildErrors(Collections.singletonList("Failed to assume AWS role" + e.getMessage()));
            throw new RejectedExecutionException("Unable to assume AWS Role");
        }
    }

    private Capability getCapability(BuildContext buildContext) {
        VariableDefinitionContext variable = buildContext
                .getVariableContext()
                .getEffectiveVariables()
                .get("aws.role");

        if (variable == null) {
            return null;
        }

        return capabilitySetManager
                .getSystemCapabilitiesByKey(
                        variable.getValue()
                )
                .stream()
                .findFirst()
                .orElse(null);
    }

    private AWSCredentialsProvider getCredentials(Capability capability) {
        String[] parts = capability.getKey().replace("tools.redfox.aws.role.capability.", "").split("\\.");
        if (parts[0].equals("-")) {
            return new EC2ContainerCredentialsProviderWrapper();
        }
        @NotNull CredentialsData credentials = credentialsAccessor.getCredentialsByName(parts[0]);

        return new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(
                        credentials.getConfiguration().get("accessKey"),
                        credentials.getConfiguration().get("secretKey")
                )
        );
    }
}
