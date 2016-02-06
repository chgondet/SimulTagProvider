package com.itmation.tools.stp;

import com.inductiveautomation.ignition.common.BasicDataset;
import com.inductiveautomation.ignition.gateway.datasource.BasicStreamingDataset;
import com.inductiveautomation.ignition.gateway.datasource.SRConnection;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Christophe on 05/02/2016.
 */
public class DbManager {
    private static final String tableName = "Simultags";
    private static final String dbName = "dbITMation";
    private static final String createStatement = "CREATE TABLE `simulTags` (\n" +
            "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
            "  `ts` char(8) NOT NULL,\n" +
            "  `tagpath` varchar(255) DEFAULT NULL,\n" +
            "  `value` double DEFAULT NULL,\n" +
            "  `quality` int(11) DEFAULT NULL,\n" +
            "  `type` varchar(45) DEFAULT 'VALUE',\n" +
            "  PRIMARY KEY (`id`),\n" +
            "  UNIQUE KEY `ikTsTagPath` (`ts`,`tagpath`),\n" +
            "  KEY `ikTs` (`ts`) USING BTREE\n" +
            ") ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;\n";
    private static final String colTagPath = "tagpath";
    private static final String colTS = "ts";
    private static final String colValue = "value";
    private static final String colQuality = "quality";
    private static final String colEventType = "type";
    private Logger logger;

    private GatewayContext context;
    private SRConnection con;
    private BasicStreamingDataset ds;


    public static final void createDB(){

    }

    public DbManager(GatewayContext context, Logger logger) {
        this.logger = logger;
        this.context = context;

        try{
            con = context.getDatasourceManager().getConnection(dbName); // TODO prendre le nom de la base de données de la configuration
        }
        catch(Exception e) {
            logger.error("java.sql.SQLException: Datasource \"dbITMation\" does not exist in this Gateway", e);

        }
    }

    public void closeConnection() throws Exception{
        if (con != null) {
            con.close();
        }
    }

    public int getNbEvents(){
        int retVal = 0;
        if(con==null){
            return -1;
        }
        try {
            BasicStreamingDataset localDs = (BasicStreamingDataset) con.runQuery("SELECT " + colTS + "," + colEventType + "," + colValue + "," + colQuality + " FROM " + tableName);
            retVal = ds.getRowCount();
            localDs.finish();
            /*
            for (int i = 0; i < ds.getRowCount(); i++) {
                SimulEvent se = new SimulEvent();
                se.ts = (String) ds.getValueAt(i, colTS);
                se.setEventType((String) ds.getValueAt(i, colEventType));
                se.value = (double) ds.getValueAt(i, colValue);
                se.setQuality((int) ds.getValueAt(i, colQuality));
                tmSimulEvents.put(se.ts, se);
            }
            */
        }
        catch(Exception e){
            logger.error("java.sql.SQLException: Datasource \"dbITMation\" does not exist in this Gateway", e);
        }

        return retVal;
    };

    public BasicStreamingDataset getEventsInList(String TS, ArrayList<SimulEvent> alSimulEvents) {
        if(con==null){
            return null;
        }
        try{
            String rqSQL = "SELECT " + colTS + "," + colTagPath +","+ colEventType +","+ colValue+ "," + colQuality +" FROM " + tableName + " WHERE TS = ?";
            logger.debug(" getEventsInList for " + TS + " request = " + rqSQL);
            ds =  (BasicStreamingDataset)con.runPrepQuery(rqSQL, TS);
            logger.debug(" getEventsInList for " + TS + " = " + String.valueOf(ds.getRowCount()));
            for (int i=0;i<ds.getRowCount();i++){
                SimulEvent se=new SimulEvent();
                se.tagPath = (String)ds.getValueAt(i,colTagPath);
                //se.ts = (String)ds.getValueAt(i,colTS);
                se.setEventType((String)ds.getValueAt(i,colEventType));
                se.value = (double)ds.getValueAt(i,colValue);
                se.setQuality((int) ds.getValueAt(i,colQuality));
                alSimulEvents.add(se);
            }
            return ds;
        }
        catch(Exception e) {
            logger.error("java.sql.SQLException: Datasource \"dbITMation\" does not exist in this Gateway", e);
        }
        return null;
    }
    public BasicStreamingDataset getEvents(String TS) {

        if(con==null){
            return null;
        }
        try{
            String rqSQL = "SELECT " + colTS + "," + colTagPath +","+ colEventType +","+ colValue+ "," + colQuality +" FROM " + tableName + " WHERE TS = ?";
            logger.debug(" getEventsInList for " + TS + " request = " + rqSQL);
            return  (BasicStreamingDataset)con.runPrepQuery(rqSQL, TS);

        }
        catch(Exception e) {
            logger.error("java.sql.SQLException: Datasource \"dbITMation\" does not exist in this Gateway", e);
            return null;
        }
    }
    public BasicDataset dsToTagDataSet(BasicStreamingDataset bsds){
        BasicDataset bds;


        Object [] [] cols = new Object[bsds.getColumnCount()][];
        for (int i=0;i<bsds.getColumnCount();i++){
            cols[i] = new Object[bsds.getRowCount()];
            for (int j=0;j<bsds.getRowCount();j++){
                cols[i][j]=bsds.getValueAt(j,i);
            }
        }
        try{
            bds = new BasicDataset(bsds.getColumnNames(),bsds.getColumnTypes(), cols);
        }
        catch(Exception e){
            bds = null;
        }

        return bds;
    }
}
