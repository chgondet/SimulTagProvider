package com.itmation.tools.stp.web;


import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.web.components.RecordEditForm;
import com.inductiveautomation.ignition.gateway.web.models.LenientResourceModel;
import com.inductiveautomation.ignition.gateway.web.pages.IConfigPage;
import com.itmation.tools.stp.records.SPSettingsRecord;
import org.apache.wicket.Application;

/**
 * Filename: SPSettingsPage
 *
 * HCSettings extends  {@link RecordEditForm} to provide a page where we can edit com.itmation.tools.stp.records in our PersistentRecord.
 */
public class SPSettingsPage extends RecordEditForm {
    public static final String[] PATH = {"SimulProvider", "settings"};

    public SPSettingsPage(final IConfigPage configPage) {
        super(configPage, null, new LenientResourceModel("SimulProvider.nav.settings.panelTitle"),
                ((GatewayContext) Application.get()).getPersistenceInterface().find(SPSettingsRecord.META, 0L));
    }


    @Override
    public String[] getMenuPath() {
        return PATH;
    }

}
