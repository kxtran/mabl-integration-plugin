package com.mabl.integration.jenkins;

import com.google.common.base.Strings;
import com.mabl.integration.jenkins.domain.CreateDeploymentProperties;
import hudson.EnvVars;

import java.util.Map;

public class Converter {

    public static CreateDeploymentProperties convert(EnvVars vars) {
        CreateDeploymentProperties props = new CreateDeploymentProperties();

        // Repository specific props (Only exists because of other non-mabl steps)
        props.setRepositoryBranchName(getProperty(vars, "GIT_BRANCH"));
        props.setRepositoryRevisionNumber(getProperty(vars, "GIT_COMMIT", "SVN_REVISION"));
        String repoUrl = getProperty(vars, "GIT_URL", "SVN_URL");
        props.setRepositoryUrl(repoUrl);
        props.setRepositoryName(getRepositoryName(repoUrl));
        props.setRepositoryPreviousRevisionNumber(getProperty(vars, "GIT_PREVIOUS_COMMIT"));
        //props.setRepositoryCommitUsername(getProperty(vars, ""));

        // Jenkins info about the mabl step that should be there no matter what
        props.setBuildPlanId(getProperty(vars, "JOB_NAME"));
        props.setBuildPlanName(getProperty(vars, "JOB_NAME"));
        props.setBuildPlanNumber(getProperty(vars, "BUILD_NUMBER"));
        props.setBuildPlanResultUrl(getProperty(vars, "RUN_DISPLAY_URL"));

        return props;
    }

    private static <String> String getProperty(Map<String, String> vars, String ...possibleProperties) {
        for(String property : possibleProperties) {
            if(vars.containsKey(property)) {
                return vars.get(property);
            }
        }

        return null;
    }

    private static String getRepositoryName(String repoUrl) {
        if(!Strings.isNullOrEmpty(repoUrl)) {
            String[] segments = repoUrl.split("/");
            String ending = segments[segments.length -1];
            return ending.endsWith(".git") ? ending.substring(0, ending.length() - 4) : ending;
        }

        return null;
    }
}
