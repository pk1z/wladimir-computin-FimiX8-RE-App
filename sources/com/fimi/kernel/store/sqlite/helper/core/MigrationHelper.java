package com.fimi.kernel.store.sqlite.helper.core;

import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;
import com.fimi.kernel.store.sqlite.dao.DaoMaster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.internal.DaoConfig;

public class MigrationHelper {
    private static final String CONVERSION_CLASS_NOT_FOUND_EXCEPTION = "MIGRATION HELPER - CLASS DOESN'T MATCH WITH THE CURRENT PARAMETERS";
    private static MigrationHelper instance;

    public static MigrationHelper getInstance() {
        if (instance == null) {
            instance = new MigrationHelper();
        }
        return instance;
    }

    public void migrate(Database db, Class<? extends AbstractDao<?, ?>>... daoClasses) {
        generateTempTables(db, daoClasses);
        DaoMaster.dropAllTables(db, true);
        DaoMaster.createAllTables(db, false);
        restoreData(db, daoClasses);
    }

    private void generateTempTables(Database db, Class<? extends AbstractDao<?, ?>>... daoClasses) {
        for (Class daoConfig : daoClasses) {
            DaoConfig daoConfig2 = new DaoConfig(db, daoConfig);
            String divider = "";
            String tableName = daoConfig2.tablename;
            String tempTableName = daoConfig2.tablename.concat("_TEMP");
            ArrayList<String> properties = new ArrayList();
            StringBuilder createTableStringBuilder = new StringBuilder();
            createTableStringBuilder.append("CREATE TABLE ").append(tempTableName).append(" (");
            for (int j = 0; j < daoConfig2.properties.length; j++) {
                String columnName = daoConfig2.properties[j].columnName;
                if (getColumns(db, tableName).contains(columnName)) {
                    properties.add(columnName);
                    String type = null;
                    try {
                        type = getTypeByClass(daoConfig2.properties[j].type);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                    createTableStringBuilder.append(divider).append(columnName).append(" ").append(type);
                    if (daoConfig2.properties[j].primaryKey) {
                        createTableStringBuilder.append(" PRIMARY KEY");
                    }
                    divider = ",";
                }
            }
            createTableStringBuilder.append(");");
            db.execSQL(createTableStringBuilder.toString());
            StringBuilder insertTableStringBuilder = new StringBuilder();
            insertTableStringBuilder.append("INSERT INTO ").append(tempTableName).append(" (");
            insertTableStringBuilder.append(TextUtils.join(",", properties));
            insertTableStringBuilder.append(") SELECT ");
            insertTableStringBuilder.append(TextUtils.join(",", properties));
            insertTableStringBuilder.append(" FROM ").append(tableName).append(";");
            db.execSQL(insertTableStringBuilder.toString());
        }
    }

    private void restoreData(Database db, Class<? extends AbstractDao<?, ?>>... daoClasses) {
        for (Class daoConfig : daoClasses) {
            DaoConfig daoConfig2 = new DaoConfig(db, daoConfig);
            String tableName = daoConfig2.tablename;
            String tempTableName = daoConfig2.tablename.concat("_TEMP");
            ArrayList<String> properties = new ArrayList();
            for (Property property : daoConfig2.properties) {
                String columnName = property.columnName;
                if (getColumns(db, tempTableName).contains(columnName)) {
                    properties.add(columnName);
                }
            }
            StringBuilder insertTableStringBuilder = new StringBuilder();
            insertTableStringBuilder.append("INSERT INTO ").append(tableName).append(" (");
            insertTableStringBuilder.append(TextUtils.join(",", properties));
            insertTableStringBuilder.append(") SELECT ");
            insertTableStringBuilder.append(TextUtils.join(",", properties));
            insertTableStringBuilder.append(" FROM ").append(tempTableName).append(";");
            StringBuilder dropTableStringBuilder = new StringBuilder();
            dropTableStringBuilder.append("DROP TABLE ").append(tempTableName);
            db.execSQL(insertTableStringBuilder.toString());
            db.execSQL(dropTableStringBuilder.toString());
        }
    }

    private String getTypeByClass(Class<?> type) throws Exception {
        if (type.equals(String.class)) {
            return "TEXT";
        }
        if (type.equals(Long.class) || type.equals(Integer.class) || type.equals(Long.TYPE)) {
            return "INTEGER";
        }
        if (type.equals(Boolean.class)) {
            return "BOOLEAN";
        }
        Exception exception = new Exception(CONVERSION_CLASS_NOT_FOUND_EXCEPTION.concat(" - Class: ").concat(type.toString()));
        exception.printStackTrace();
        throw exception;
    }

    private List<String> getColumns(Database db, String tableName) {
        List<String> columns = new ArrayList();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM " + tableName + " limit 1", null);
            if (cursor != null) {
                columns = new ArrayList(Arrays.asList(cursor.getColumnNames()));
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            Log.v(tableName, e.getMessage(), e);
            e.printStackTrace();
            if (cursor != null) {
                cursor.close();
            }
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
        return columns;
    }
}
