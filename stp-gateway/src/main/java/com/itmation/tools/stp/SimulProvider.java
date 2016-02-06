package com.itmation.tools.stp; /**
 * Created by pjones on 12/8/14.
 */

import com.inductiveautomation.ignition.common.BundleUtil;
import com.inductiveautomation.ignition.common.TypeUtilities;
import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.common.model.values.CommonQualities;
import com.inductiveautomation.ignition.common.model.values.Quality;
import com.inductiveautomation.ignition.common.sqltags.model.TagPath;
import com.inductiveautomation.ignition.common.sqltags.model.types.*;
import com.inductiveautomation.ignition.common.sqltags.parser.TagPathParser;
import com.inductiveautomation.ignition.gateway.datasource.BasicStreamingDataset;
import com.inductiveautomation.ignition.gateway.localdb.persistence.IRecordListener;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.sqltags.model.BasicWriteRequest;
import com.inductiveautomation.ignition.gateway.sqltags.model.WriteRequest;
import com.inductiveautomation.ignition.gateway.sqltags.simple.SimpleTagProvider;
import com.inductiveautomation.ignition.gateway.web.components.LabelConfigMenuNode;
import com.inductiveautomation.ignition.gateway.web.components.LinkConfigMenuNode;
import com.inductiveautomation.ignition.gateway.web.models.KeyValue;
import com.itmation.tools.stp.records.SPSettingsRecord;
import com.itmation.tools.stp.web.SPSettingsPage;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TreeMap;


/**
 * The "gateway hook" is the entry point for a module on the gateway. Since this example is so simple, we just do
 * everything here.
 * <p/>
 * This example uses the {@link SimpleTagProvider} to expose tags through SQLTags. We create a number of tags under a
 * folder, and update their values every second with random values.
 * <p/>
 * There is a "control" tag that can be used to modify the number of tags provided. This tag illustrates how to set up
 * write handling.
 */
public class SimulProvider extends AbstractGatewayModuleHook {
    // Local declarations

    private static final String TASK_NAME = "UpdateSimulValues";
    private static final String TAG_PROVIDER_NAME = "ITMation_SimulTags";
    private static final String CONFIG_PATH = "Config/";
    private static final String CONTROL_PATH = "Control/";
    private static final String STATUS_PATH = "";
    private static final String CTRL_RUN_TAG = CONTROL_PATH+"Run";
    private static final String CTRL_AUTO_APPLY_TAG = CONTROL_PATH+"Auto Apply";
    private static final String STATUS_CURRENT_TS = STATUS_PATH +"Current TS";
    private static final String STATUS_LAST_TS = STATUS_PATH +"Last TS Applied";
    private static final String STATUS_CURRENT_NBEVENTS = STATUS_PATH +"Current NbEvents";
    private static final String STATUS_CURRENT_EVENTS = STATUS_PATH +"Current Events";
    private static final String STATUS_LAST_EVENTS = STATUS_PATH +"Last Events Applied";

    private static final String CONFIG_NBEVENTS_TAG = CONFIG_PATH+"nbEvents";


    // Complex declarations
    public TreeMap<String, SimulEvent> tmSimulEvents;
    public ArrayList <SimulEvent> alSimulEvents;

    Logger logger;
    GatewayContext context;
    SimpleTagProvider ourProvider;
    ExtendedTagType ourTagType;

    DbManager dbManager;

    private Boolean ctrl_run = false;
    private Boolean ctrl_autoApply = false;
    private int config_nbEvents = -1;
    private int status_nbEvents = 0;

    private String status_currentTS;
    private SimpleDateFormat dateFormat;
    private BasicStreamingDataset bds;
    private static final String[] SPROV_MENU_PATH = {"SimulProvider"};

    public SimulProvider() {
        logger = LogManager.getLogger(this.getClass());
    }

    @Override
    public void setup(GatewayContext context) {
        try {

            this.context = context;
            logger.debug("Beginning setup of SimulProvider Module");

            dbManager = new DbManager(this.context, logger);

            // Local
            BundleUtil.get().addBundle("SimulProvider", getClass(), "SimulProvider");

            verifySchema(context);
            maybeCreateSPSettings(context);
            // get the settings record and do something with it...
            SPSettingsRecord theOneRecord = context.getLocalPersistenceInterface().find(SPSettingsRecord.META, 0L);
            // listen for updates to the settings record...
            SPSettingsRecord.META.addRecordListener(new IRecordListener<SPSettingsRecord>() {
                @Override
                public void recordUpdated(SPSettingsRecord spSettingsRecord) {
                    logger.info("recordUpdated()");
                }

                @Override
                public void recordAdded(SPSettingsRecord spSettingsRecord) {
                    logger.info("recordAdded()");
                }

                @Override
                public void recordDeleted(KeyValue keyValue) {
                    logger.info("recordDeleted()");
                }
            });

            initMenu();



            // Provider
            ourProvider = new SimpleTagProvider(TAG_PROVIDER_NAME);

            // Prepare le treemap

            tmSimulEvents = new TreeMap<>();
            alSimulEvents = new ArrayList<>();



            // charge le treemap avec les données de la base
            // Read db
            config_nbEvents = dbManager.getNbEvents();

            ourTagType = TagType.Custom;
            ourProvider.configureTagType(ourTagType, TagEditingFlags.STANDARD_STATUS, null);

            // CTRL_TAGS
            ourProvider.configureTag(CTRL_RUN_TAG, DataType.Boolean, ourTagType);
            ourProvider.registerWriteHandler(CTRL_RUN_TAG, this::changeValue);
            ourProvider.configureTag(CTRL_AUTO_APPLY_TAG, DataType.Boolean, ourTagType);
            ourProvider.registerWriteHandler(CTRL_AUTO_APPLY_TAG, (tagPath, value) -> {return changeValue(tagPath,value);});

            // CONFIG_TAGS
            ourProvider.configureTag(CONFIG_NBEVENTS_TAG, DataType.Int2, ourTagType);
            ourProvider.updateValue(CONFIG_NBEVENTS_TAG, config_nbEvents, DataQuality.GOOD_DATA);

            // STATE_TAGS
            ourProvider.configureTag(STATUS_CURRENT_TS, DataType.String, ourTagType);
            ourProvider.registerWriteHandler(STATUS_CURRENT_TS, (tagPath, value) -> {return changeValue(tagPath,value);});
            ourProvider.configureTag(STATUS_CURRENT_NBEVENTS, DataType.Int2, ourTagType);
            ourProvider.configureTag(STATUS_CURRENT_EVENTS, DataType.DataSet, ourTagType);
            ourProvider.configureTag(STATUS_LAST_TS, DataType.String, ourTagType);
            ourProvider.configureTag(STATUS_LAST_EVENTS, DataType.DataSet, ourTagType);


            dateFormat = new SimpleDateFormat("HH:mm:ss");
            updateAutoApply(false);
            updateRun(false);

        } catch (Exception e) {
            logger.fatal("Error setting up SimulTagProvider module.", e);
        }
    }

    // Gateway config Part
    private void verifySchema(GatewayContext context) {
        try {
            context.getSchemaUpdater().updatePersistentRecords(SPSettingsRecord.META);
        } catch (SQLException e) {
            logger.error("Error verifying persistent record schemas for SimulProvider com.itmation.tools.stp.records.", e);
        }
    }

    public void maybeCreateSPSettings(GatewayContext context) {
        logger.trace("Attempting to create SimulProvider Settings Record");
        try {
            SPSettingsRecord settingsRecord = context.getLocalPersistenceInterface().createNew(SPSettingsRecord.META);
            settingsRecord.setId(0L);
            settingsRecord.setSPDBName("dbITMation");
/*            settingsRecord.setHCIPAddress("192.168.1.99");
            settingsRecord.setHCHubName("HomeConnect Hub");
            settingsRecord.setHCPowerOutput(23);
            settingsRecord.setHCDeviceCount(15);
            settingsRecord.setBroadcastSSID(false);
*/
// Todo: Change settings

        /*
			 * This doesn't override existing settings, only replaces it with these if we didn't
			 * exist already.
			 */
            context.getSchemaUpdater().ensureRecordExists(settingsRecord);
        } catch (Exception e) {
            logger.error("Failed to establish SPSettings Record exists", e);
        }

        logger.trace("SimulProvider Settings Record Established");
    }

    private void initMenu() {
        /* header is the top-level title in the gateway config page, e.g. System, Configuration, etc */
        LabelConfigMenuNode header = new LabelConfigMenuNode(SPROV_MENU_PATH[0], "SimulProvider.nav.header");
        header.setPosition(801);

        context.getConfigMenuModel().addConfigMenuNode(null, header);

        /* Create the nodes/links that will exist under our parent nav header */
        LinkConfigMenuNode settingsNode = new LinkConfigMenuNode("settings",
                "SimulProvider.nav.settings.title",
                SPSettingsPage.class);
        /* register our nodes with the context config menu model */
        context.getConfigMenuModel().addConfigMenuNode(SPROV_MENU_PATH, settingsNode);

    }
    protected synchronized  Quality changeValue(TagPath target,java.lang.Object value){
        Boolean boolVal = TypeUtilities.toBool(value);
        String stringVal = TypeUtilities.toString(value);
        logger.debug("Update Tag : " + target.toStringPartial());
        switch(target.toStringPartial()){
            case CTRL_RUN_TAG:
                logger.debug("Update CTRL_RUN_TAG : " + String.valueOf(boolVal));
                updateRun(boolVal);
                break;
            case CTRL_AUTO_APPLY_TAG:
                logger.debug("Update CTRL_AUTO_APPLY_TAG : " + String.valueOf(boolVal));
                updateAutoApply(boolVal);
                break;
            case STATUS_CURRENT_TS:
                logger.debug("Update STATUS_CURRENT_TS : " + stringVal);
                updateCurrentTS(stringVal);
                break;
        }
        return CommonQualities.GOOD;
    }
    @Override
    public void startup(LicenseState activationState) {
        try {
            ourProvider.startup(context);

            //Register a task with the execution system to update values every second.
            context.getExecutionManager().register(getClass().getName(), TASK_NAME, new Runnable() {
                @Override
                public void run() {
                    updateValues();
                }
            }, 1000);

            logger.info("Simul Tag module started.");
        } catch (Exception e) {
            logger.fatal("Error starting up SimulTagProvider = module.", e);
        }


    }


    @Override
    public void shutdown() {
        //Clean up the things we've registered with the platform, namely, our provider type.
        alSimulEvents.clear();
        try {
            dbManager.closeConnection();
            if (context != null) {
                //Remove our value update task
                context.getExecutionManager().unRegister(getClass().getName(), TASK_NAME);
                //Shutdown our provider
                ourProvider.shutdown();
            }
        } catch (Exception e) {
            logger.error("Error stopping SimpleTagProvider example module.", e);
        }
        logger.info("SimpleTagProvider Example module stopped.");
    }

    /**
     * This function adds or removes tags to/from our custom provider. Notice that it is synchronized, since we are
     * updating the values asynchronously. If we weren't careful to synchronize the threading, it might happen that
     * right as we remove tags, they're added again implicitly, because the value update is happening at the same time.
     *
     * @param newRun
     */
    protected synchronized void updateRun(Boolean newRun) {

        ctrl_run = newRun;
        ourProvider.updateValue(CTRL_RUN_TAG, ctrl_run, DataQuality.GOOD_DATA);
    }
    protected synchronized void updateAutoApply(Boolean newAuto) {

        ctrl_autoApply = newAuto;
        ourProvider.updateValue(CTRL_AUTO_APPLY_TAG, ctrl_autoApply, DataQuality.GOOD_DATA);
    }

    protected synchronized void updateCurrentTS() {
        updateCurrentTS(dateFormat.format(Calendar.getInstance().getTime()));
    }
    protected synchronized void updateCurrentTS(String newVal) {
        status_currentTS = newVal;
        ourProvider.updateValue(STATUS_CURRENT_TS, status_currentTS, DataQuality.GOOD_DATA);
        updateEvents(status_currentTS);
    }
    protected synchronized void updateEvents(String TS){
        alSimulEvents.clear();

        bds = dbManager.getEventsInList( TS,alSimulEvents );
        status_nbEvents = alSimulEvents.size();
        ourProvider.updateValue(STATUS_CURRENT_NBEVENTS,status_nbEvents, DataQuality.GOOD_DATA);
        ourProvider.updateValue(STATUS_CURRENT_EVENTS, dbManager.dsToTagDataSet(bds), DataQuality.GOOD_DATA);
    }
    protected synchronized void applyTags(){
        List<WriteRequest<TagPath>> writes = new ArrayList<>();
        for (SimulEvent se:alSimulEvents){
            if (se.type== SimulEvent.EventType.VALUE) {
                try {
                    writes.add(new BasicWriteRequest<>(TagPathParser.parse(se.tagPath),se.value));
                }
                catch(Exception e){}
            }
        }
        if (writes.size()>0) {
            try {
                context.getTagManager().write(writes, null, true);
                ourProvider.updateValue(STATUS_LAST_TS, status_currentTS, DataQuality.GOOD_DATA);
                ourProvider.updateValue(STATUS_LAST_EVENTS, dbManager.dsToTagDataSet(bds), DataQuality.GOOD_DATA);
            } catch (Exception e) {
                logger.debug("Erreur de Ecriture de tag" + String.valueOf(Thread.currentThread().getStackTrace()[1].getLineNumber()));
            }
        }
        writes.clear();

    }

    /**
     * Update the values of the tags.
     */
    protected synchronized void updateValues() {
        if (ctrl_run){
            updateCurrentTS();
            if (ctrl_autoApply){
                applyTags();
//                logger.debug("Apply for :" + status_currentTS);

            }
        }
    }


}
/*            Tag tag = context.getTagManager().getTag(tp);
            logger.debug(tp.getItemName() + " = " + tp.toString());
            // Read
            TagValue tv = tag.getValue();
            logger.debug("Class = " + tv.getClass().toString());

                try {
            List<WriteRequest<TagPath>> writes = new ArrayList<>();
            writes.add(new BasicWriteRequest<>(TagPathParser.parse("[Tis]Tag1"),dbManager.dsToTagDataSet(bds)));
            context.getTagManager().write(writes, null, true);

        } catch(Exception e){
            logger.debug("Erreur de Ecriture de tag"+String.valueOf(Thread.currentThread().getStackTrace()[1].getLineNumber()));
        }*/
