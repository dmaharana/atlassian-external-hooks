package com.ngs.stash.externalhooks.hook;

import com.atlassian.bitbucket.cluster.ClusterService;
import com.atlassian.bitbucket.hook.repository.*;
import com.atlassian.bitbucket.repository.*;
import com.atlassian.bitbucket.setting.*;
import com.atlassian.bitbucket.auth.*;
import com.atlassian.bitbucket.permission.*;
import com.atlassian.bitbucket.server.*;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.INFO;
import java.util.logging.Logger;

import com.atlassian.upm.api.license.PluginLicenseManager;

public class ExternalAsyncPostReceiveHook
    implements PostRepositoryHook<RepositoryHookRequest>, SettingsValidator
{
    private final PluginLicenseManager pluginLicenseManager;

    private static Logger log = Logger.getLogger(
        ExternalAsyncPostReceiveHook.class.getSimpleName()
    );

    private AuthenticationContext authCtx;
    private PermissionService permissions;
    private RepositoryService repoService;
    private ClusterService clusterService;
    private ApplicationPropertiesService properties;

    public ExternalAsyncPostReceiveHook(
        AuthenticationContext authenticationContext,
        PermissionService permissions,
        RepositoryService repoService,
        ApplicationPropertiesService properties,
        PluginLicenseManager pluginLicenseManager,
        ClusterService clusterService
    ) {
        this.authCtx = authenticationContext;
        this.permissions = permissions;
        this.repoService = repoService;
        this.properties = properties;
        this.clusterService = clusterService;
        this.pluginLicenseManager = pluginLicenseManager;
    }

	@Override
	public void postUpdate(PostRepositoryHookContext context, RepositoryHookRequest request) {
        ExternalPreReceiveHook impl = new ExternalPreReceiveHook(
                this.authCtx, this.permissions, this.repoService, this.properties,
                this.pluginLicenseManager, this.clusterService
                );

        impl.preUpdateImpl(context, request);
	}

    @Override
    public void validate(
        Settings settings,
        SettingsValidationErrors errors,
        Repository repository
    ) {
        ExternalPreReceiveHook impl = new ExternalPreReceiveHook(this.authCtx,
            this.permissions, this.repoService, this.properties,
            this.pluginLicenseManager, this.clusterService
            );

        impl.validate(settings, errors, repository);
    }
}
