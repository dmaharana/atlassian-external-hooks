package com.ngs.stash.externalhooks.hook;

import java.io.IOException;

import javax.annotation.Nonnull;

import com.atlassian.bitbucket.auth.AuthenticationContext;
import com.atlassian.bitbucket.cluster.ClusterService;
import com.atlassian.bitbucket.event.hook.RepositoryHookDeletedEvent;
import com.atlassian.bitbucket.event.hook.RepositoryHookDisabledEvent;
import com.atlassian.bitbucket.event.hook.RepositoryHookEnabledEvent;
import com.atlassian.bitbucket.hook.repository.GetRepositoryHookSettingsRequest;
import com.atlassian.bitbucket.hook.repository.PreRepositoryHookContext;
import com.atlassian.bitbucket.hook.repository.PullRequestMergeHookRequest;
import com.atlassian.bitbucket.hook.repository.RepositoryHookResult;
import com.atlassian.bitbucket.hook.repository.RepositoryHookService;
import com.atlassian.bitbucket.hook.repository.RepositoryHookSettings;
import com.atlassian.bitbucket.hook.repository.RepositoryMergeCheck;
import com.atlassian.bitbucket.hook.repository.StandardRepositoryHookTrigger;
import com.atlassian.bitbucket.hook.script.HookScriptService;
import com.atlassian.bitbucket.hook.script.HookScriptType;
import com.atlassian.bitbucket.permission.PermissionService;
import com.atlassian.bitbucket.scope.Scope;
import com.atlassian.bitbucket.server.StorageService;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.bitbucket.setting.SettingsValidationErrors;
import com.atlassian.bitbucket.setting.SettingsValidator;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.event.api.EventListener;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.upm.api.license.PluginLicenseManager;

public class ExternalMergeCheckHook implements RepositoryMergeCheck, SettingsValidator {
  private ExternalHookScript externalHookScript;
  private RepositoryHookService repositoryHookService;

  public ExternalMergeCheckHook(
      AuthenticationContext authenticationContext,
      PermissionService permissions,
      PluginLicenseManager pluginLicenseManager,
      ClusterService clusterService,
      StorageService storageProperties,
      HookScriptService hookScriptService,
      PluginSettingsFactory pluginSettingsFactory,
      RepositoryHookService repositoryHookService,
      SecurityService securityService)
      throws IOException {

    this.repositoryHookService = repositoryHookService;
    this.externalHookScript =
        new ExternalHookScript(
            authenticationContext,
            permissions,
            pluginLicenseManager,
            clusterService,
            storageProperties,
            hookScriptService,
            pluginSettingsFactory,
            securityService,
            "external-merge-check-hook",
            HookScriptType.PRE,
            StandardRepositoryHookTrigger.PULL_REQUEST_MERGE);
  }

  @Override
  public RepositoryHookResult preUpdate(
      PreRepositoryHookContext context, PullRequestMergeHookRequest request) {
    if (!this.externalHookScript.isLicenseValid()) {
      return RepositoryHookResult.rejected(
          "License is not valid.",
          "License for External Hooks Plugin is expired.\n"
              + "Visit \"Manage Apps\" page in your Bitbucket instance for more info.");
    }

    return RepositoryHookResult.accepted();
  }

  @Override
  public void validate(
      @Nonnull Settings settings, @Nonnull SettingsValidationErrors errors, @Nonnull Scope scope) {
    this.externalHookScript.validate(settings, errors, scope);
  }

  @EventListener
  public void onRepositoryHookDisabledEvent(RepositoryHookDisabledEvent event) {
    if (!this.externalHookScript.hookId.equals(event.getRepositoryHookKey())) {
      return;
    }

    this.externalHookScript.deleteHookScriptByKey(event.getRepositoryHookKey(), event.getScope());
  }

  @EventListener
  public void onRepositoryHookEnabledEvent(RepositoryHookEnabledEvent event) {
    if (!this.externalHookScript.hookId.equals(event.getRepositoryHookKey())) {
      return;
    }

    GetRepositoryHookSettingsRequest request =
        (new GetRepositoryHookSettingsRequest.Builder(
                event.getScope(), event.getRepositoryHookKey()))
            .build();

    RepositoryHookSettings hookSettings = this.repositoryHookService.getSettings(request);

    this.externalHookScript.install(hookSettings.getSettings(), event.getScope());
  }

  @EventListener
  public void onRepositoryHookSettingsDeletedEvent(RepositoryHookDeletedEvent event) {
    if (!this.externalHookScript.hookId.equals(event.getRepositoryHookKey())) {
      return;
    }

    this.externalHookScript.deleteHookScriptByKey(event.getRepositoryHookKey(), event.getScope());
  }
}
