package tools.redfox.bamboo.aws.assumeRole.capability;

import com.atlassian.bamboo.credentials.CredentialsAccessor;
import com.atlassian.bamboo.credentials.CredentialsData;
import com.atlassian.bamboo.plugin.descriptor.CapabilityTypeModuleDescriptor;
import com.atlassian.bamboo.template.TemplateRenderer;
import com.atlassian.bamboo.v2.build.agent.capability.*;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class AWSAssumeRoleCapabilityType extends AbstractCapabilityTypeModule implements CapabilityDefaultsHelper {
    private final TemplateRenderer templateRenderer;
    private CredentialsAccessor credentialsAccessor;
    private String editTemplate;

    private static final Pattern arnPattern;

    static {
        arnPattern = Pattern.compile("arn:aws:iam:.*?:(\\d+):role/(.*)");
    }

    public AWSAssumeRoleCapabilityType(@ComponentImport TemplateRenderer templateRenderer, @ComponentImport CredentialsAccessor credentialsAccessor) {
        this.templateRenderer = templateRenderer;
        this.credentialsAccessor = credentialsAccessor;
        setTemplateRenderer(templateRenderer);
    }

    @Override
    public @NotNull CapabilitySet addDefaultCapabilities(@NotNull CapabilitySet capabilitySet) {
        return capabilitySet;
    }

    @Override
    public @NotNull Map<String, String> validate(@NotNull Map<String, String[]> map) {
        return Collections.emptyMap();
    }

    @NotNull
    @Override
    public Capability getCapability(@NotNull Map<String, String[]> map) {
        Matcher matcher = arnPattern.matcher(map.get("awsRoleARN")[0]);
        matcher.find();
        String key = MessageFormat.format(
                "system.aws.role.{0}.{1}.{2}",
                map.get("awsCredentialsName")[0],
                matcher.group(1),
                matcher.group(2)
        );
        return new CapabilityImpl(
                key,
                map.get("awsRoleARN")[0]
        );
    }

    @Override
    public @NotNull String getLabel(@NotNull String s) {
        return getFormattedLabel(s);
    }


    public static String getFormattedLabel(String s) {
        String[] nameParts = s.replace("system.aws.role.", "").split("\\.");
        if (nameParts.length == 0) {
            return "{SDK AutoDetection}";
        } else if (nameParts.length < 2) {
            return s;
        }
        return MessageFormat.format("{0} @ {1} ({2})", nameParts[2], nameParts[1], nameParts[0]);
    }

    @Override
    public void init(@NotNull ModuleDescriptor moduleDescriptor) {
        super.init(moduleDescriptor);
        if (moduleDescriptor instanceof CapabilityTypeModuleDescriptor) {
            CapabilityTypeModuleDescriptor descriptor = (CapabilityTypeModuleDescriptor) moduleDescriptor;
            this.editTemplate = descriptor.getEditTemplate();
        }
    }

    @Override
    public String getEditHtml() {
        HashMap<String, Object> context = new HashMap<String, Object>();
        context.put("capabilityType", this);

        Set<String> credentials = StreamSupport.stream(credentialsAccessor.getAllCredentials().spliterator(), false)
                .filter(c -> c.getPluginKey().endsWith("awsCredentials"))
                .map(CredentialsData::getName)
                .collect(Collectors.toSet());

        context.put("credentials", credentials);
        return this.templateRenderer.render(this.editTemplate, context);
    }
}
