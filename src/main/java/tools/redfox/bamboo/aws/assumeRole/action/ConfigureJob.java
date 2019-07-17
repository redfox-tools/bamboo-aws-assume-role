package tools.redfox.bamboo.aws.assumeRole.action;

import com.atlassian.bamboo.build.BuildDefinitionManager;
import com.atlassian.bamboo.build.BuildExecutionManager;
import com.atlassian.bamboo.configuration.AdministrationConfigurationAccessor;
import com.atlassian.bamboo.jsonator.Jsonator;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plan.PlanExecutionManager;
import com.atlassian.bamboo.plan.PlanManager;
import com.atlassian.bamboo.plan.cache.CachedPlanManager;
import com.atlassian.bamboo.security.BambooPermissionManager;
import com.atlassian.bamboo.user.BambooAuthenticationContext;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilitySetManager;
import com.atlassian.bamboo.variable.VariableDefinition;
import com.atlassian.bamboo.variable.VariableDefinitionImpl;
import com.atlassian.bamboo.variable.VariableDefinitionManager;
import com.atlassian.bamboo.variable.VariableType;
import com.atlassian.bamboo.ww2.actions.BuildActionSupport;
import com.atlassian.bamboo.ww2.aware.permissions.PlanEditSecurityAware;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.web.WebInterfaceManager;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.struts2.ServletActionContext;
import org.jetbrains.annotations.Nullable;
import tools.redfox.bamboo.aws.assumeRole.capability.AWSAssumeRoleCapabilityType;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class ConfigureJob extends BuildActionSupport implements PlanEditSecurityAware {
    private CapabilitySetManager capabilitySetManager;
    private VariableDefinitionManager variableDefinitionManager;

    public ConfigureJob(
            @ComponentImport WebInterfaceManager webInterfaceManager,
            @ComponentImport BuildExecutionManager buildExecutionManager,
            @ComponentImport AdministrationConfigurationAccessor administrationConfigurationAccessor,
            @ComponentImport BuildDefinitionManager buildDefinitionManager,
            @ComponentImport BambooAuthenticationContext authenticationContext,
            @ComponentImport Jsonator jsonator,
            @ComponentImport BambooPermissionManager bambooPermissionManager,
            @ComponentImport PlanExecutionManager planExecutionManager,
            @ComponentImport CapabilitySetManager capabilitySetManager,
            @ComponentImport PlanManager planManager,
            @ComponentImport CachedPlanManager cachedPlanManager,
            @ComponentImport VariableDefinitionManager variableDefinitionManager
    ) {
        this.capabilitySetManager = capabilitySetManager;
        this.variableDefinitionManager = variableDefinitionManager;
        setWebInterfaceManager(webInterfaceManager);
        setBuildExecutionManager(buildExecutionManager);
        setAdministrationConfigurationAccessor(administrationConfigurationAccessor);
        setBuildDefinitionManager(buildDefinitionManager);
        setAuthenticationContext(authenticationContext);
        setJsonator(jsonator);
        setBambooPermissionManager(bambooPermissionManager);
        setPlanExecutionManager(planExecutionManager);
        setPlanManager(planManager);
        setCachedPlanManager(cachedPlanManager);
    }

    @Override
    public String execute() throws Exception {
        @Nullable Plan plan = planManager.getPlanByKey(getPlanKey());
        HttpServletRequest request = ServletActionContext.getRequest();
        Map<String, Object> context = ServletActionContext.getValueStack(request).getContext();
        Pattern name = Pattern.compile("system\\.aws\\.role\\.\\w+.\\w+");

        VariableDefinition variable = variableDefinitionManager.getPlanVariables(plan)
                .stream()
                .filter(v -> v.getKey().equals("aws.role"))
                .findFirst()
                .orElse(new VariableDefinitionImpl());

        String awsAssumedRole = ObjectUtils.firstNonNull(
                request.getParameter("awsAssumedRole"),
                variable.getValue(),
                ""
        );

        Map<String, String> awsRoles = new HashMap<>();
        awsRoles.put("", "");
        capabilitySetManager
                .findUniqueCapabilityKeys()
                .stream()
                .filter(c -> name.matcher(c).find())
                .forEach(c -> {
                    awsRoles.put(c, AWSAssumeRoleCapabilityType.getFormattedLabel(c));
                });

        context.put("awsRoles", awsRoles);
        context.put("awsAssumedRole", awsAssumedRole);
        context.put("saved", request.getParameterMap().containsKey("saved"));

        if (request.getMethod().equals("POST")) {
            variableDefinitionManager.getPlanVariables(plan)
                    .stream()
                    .filter(v -> v.getKey().equals("aws.role"))
                    .forEach(v -> variableDefinitionManager.deleteVariableDefinition(v));

            if (!awsAssumedRole.equals("")) {
                variableDefinitionManager.saveVariableDefinition(
                        new VariableDefinitionImpl("aws.role", awsAssumedRole, plan, VariableType.JOB)
                );
            }
            return "reload";
        }

        return super.execute();
    }

}
