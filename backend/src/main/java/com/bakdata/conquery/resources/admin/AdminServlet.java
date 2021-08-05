package com.bakdata.conquery.resources.admin;

import java.util.Collections;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.freemarker.Freemarker;
import com.bakdata.conquery.io.jackson.IdRefPathParamConverterProvider;
import com.bakdata.conquery.io.jersey.IdParamConverter;
import com.bakdata.conquery.io.jersey.RESTServer;
import com.bakdata.conquery.models.auth.web.AuthCookieFilter;
import com.bakdata.conquery.models.auth.web.RedirectingAuthFilter;
import com.bakdata.conquery.resources.admin.rest.*;
import com.bakdata.conquery.resources.admin.ui.AdminUIResource;
import com.bakdata.conquery.resources.admin.ui.AuthOverviewUIResource;
import com.bakdata.conquery.resources.admin.ui.ConceptsUIResource;
import com.bakdata.conquery.resources.admin.ui.DatasetsUIResource;
import com.bakdata.conquery.resources.admin.ui.GroupUIResource;
import com.bakdata.conquery.resources.admin.ui.RoleUIResource;
import com.bakdata.conquery.resources.admin.ui.TablesUIResource;
import com.bakdata.conquery.resources.admin.ui.UserUIResource;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.views.ViewMessageBodyWriter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * Organizational class to provide a single implementation point for configuring
 * the admin servlet container and registering resources for it.
 */
@Getter
@Slf4j
public class AdminServlet {

    private final AdminProcessor adminProcessor;
    private final DropwizardResourceConfig jerseyConfig;
    private final AdminDatasetProcessor adminDatasetProcessor;
    private final DropwizardResourceConfig jerseyConfigUI;

    public AdminServlet(ManagerNode manager) {
        jerseyConfig = new DropwizardResourceConfig(manager.getEnvironment().metrics());
        jerseyConfig.setUrlPattern("/admin");
        jerseyConfigUI = new DropwizardResourceConfig(manager.getEnvironment().metrics());
        jerseyConfigUI.setUrlPattern("/admin-ui");

        RESTServer.configure(manager.getConfig(), jerseyConfig);

        manager.getEnvironment().admin().addServlet("admin", new ServletContainer(jerseyConfig)).addMapping("/admin/*");
        manager.getEnvironment().admin().addServlet("admin-ui", new ServletContainer(jerseyConfigUI)).addMapping("/admin-ui/*");

        jerseyConfig.register(new JacksonMessageBodyProvider(manager.getEnvironment().getObjectMapper()));
        // freemarker support
        jerseyConfigUI.register(new ViewMessageBodyWriter(manager.getEnvironment().metrics(), Collections.singleton(Freemarker.HTML_RENDERER)));


        adminProcessor = new AdminProcessor(
                manager.getConfig(),
                manager.getStorage(),
                manager.getDatasetRegistry(),
                manager.getJobManager(),
                manager.getMaintenanceService(),
                manager.getValidator()
        );

        adminDatasetProcessor = new AdminDatasetProcessor(
                manager.getStorage(),
                manager.getConfig(),
                manager.getValidator(),
                manager.getDatasetRegistry(),
                manager.getJobManager()
        );


        // inject required services
        jerseyConfig.register(new AbstractBinder() {

            @Override
            protected void configure() {
                bind(adminProcessor).to(AdminProcessor.class);
                bind(adminDatasetProcessor).to(AdminDatasetProcessor.class);
            }
        });

        // inject required services
        jerseyConfigUI.register(new AbstractBinder() {

            @Override
            protected void configure() {
                bind(new UIProcessor(adminProcessor)).to(UIProcessor.class);
            }
        });

        jerseyConfig.register(new IdRefPathParamConverterProvider(manager.getDatasetRegistry(), manager.getDatasetRegistry().getMetaRegistry()));
        jerseyConfigUI.register(new IdRefPathParamConverterProvider(manager.getDatasetRegistry(), manager.getDatasetRegistry().getMetaRegistry()));
    }

    public void register(ManagerNode manager) {

        // register root resources
        jerseyConfig
                .register(AdminDatasetResource.class)
                .register(AdminDatasetsResource.class)
                .register(AdminConceptsResource.class)
                .register(AdminTablesResource.class)
                .register(RoleResource.class)
                .register(UserResource.class)
                .register(GroupResource.class)
                .register(PermissionResource.class)
                .register(AuthOverviewResource.class)
                .register(AdminResource.class);

        jerseyConfigUI
                .register(AdminUIResource.class)
                .register(RoleUIResource.class)
                .register(UserUIResource.class)
                .register(GroupUIResource.class)
                .register(DatasetsUIResource.class)
                .register(TablesUIResource.class)
                .register(ConceptsUIResource.class)
                .register(AuthOverviewUIResource.class);

        // register features
        jerseyConfig
                .register(new MultiPartFeature())
                .register(IdParamConverter.Provider.INSTANCE)
                .register(AuthCookieFilter.class)
                .register(manager.getAuthController().getAuthenticationFilter());


        jerseyConfigUI
                .register(AuthCookieFilter.class)
                .register(manager.getAuthController().getRedirectingAuthFilter());
    }
}
