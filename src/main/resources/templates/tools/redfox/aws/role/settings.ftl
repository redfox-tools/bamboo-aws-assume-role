[#-- @ftlvariable name="action" type="com.atlassian.bamboo.ww2.actions.build.admin.config.ConfigureBuildDocker" --]
[#-- @ftlvariable name="" type="com.atlassian.bamboo.ww2.actions.build.admin.config.ConfigureBuildDocker" --]

[#import "/build/edit/editBuildConfigurationCommon.ftl" as ebcc/]
[#import "/fragments/docker/docker.ftl" as docker/]

[#assign pageDescription]
    [@s.text name='agent.capability.type.tools.redfox.aws.role.capability.title']
        [@s.param][@help.href pageKey='docker.pipelines' /][/@s.param]
    [/@s.text]
[/#assign]

[@ebcc.editConfigurationPage plan=immutablePlan selectedTab='awsAssumeRole' titleKey='agent.capability.type.tools.redfox.aws.role.capability.title']
    [@s.form action="awsAssumeRole" namespace="/build/admin/edit"
        cancelUri='/build/admin/edit/awsAssumeRole.action?buildKey=${immutableBuild.key}'
        submitLabelKey='global.buttons.update' ]

        [@ww.select labelKey='agent.capability.type.tools.redfox.aws.role.capability.title' cssClass="long-field"
            name='tools.redfox.aws.role' list=awsRoles listKey='key' listValue='value' /]
        [@s.hidden name="buildKey" /]
    [/@s.form]
[/@ebcc.editConfigurationPage]
