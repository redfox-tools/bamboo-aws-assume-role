[#-- @ftlvariable name="action" type="com.atlassian.bamboo.ww2.actions.build.admin.config.ConfigureBuildDocker" --]
[#-- @ftlvariable name="" type="com.atlassian.bamboo.ww2.actions.build.admin.config.ConfigureBuildDocker" --]

[#import "/build/edit/editBuildConfigurationCommon.ftl" as ebcc/]
[#import "/fragments/docker/docker.ftl" as docker/]

[#assign pageDescription]
    [@s.text name='tools.redfox.bamboo.aws.assumeRole.description']
        [@s.param][@help.href pageKey='docker.pipelines' /][/@s.param]
    [/@s.text]
[/#assign]

[@ebcc.editConfigurationPage plan=immutablePlan selectedTab='awsAssumeRole' titleKey='tools.redfox.bamboo.aws.assumeRole.title']
    [@s.form action="awsAssumeRole" namespace="/build/admin/edit" id="configureAWSRole"
    cancelUri='/build/admin/edit/awsAssumeRole.action?buildKey=${immutableBuild.key}'
    submitLabelKey='global.buttons.update' ]

        [@ww.select labelKey='agent.capability.type.system.aws.role.title' cssClass="long-field"
            name='awsAssumedRole' list=awsRoles listKey='key' listValue='value' /]
        [@s.hidden name="buildKey" /]
    [/@s.form]
[/@ebcc.editConfigurationPage]
