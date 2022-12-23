package com.gigaspaces.tierdirectcall.service;

import com.gigaspaces.client.storage_adapters.class_storage_adapters.ClassBinaryStorageAdapter;
import com.gigaspaces.client.storage_adapters.class_storage_adapters.GSObjectInputStream;
import com.gigaspaces.client.storage_adapters.class_storage_adapters.GSObjectOutputStream;
import com.gigaspaces.internal.io.GSByteArrayInputStream;
import com.gigaspaces.internal.io.GSByteArrayOutputStream;
import com.gigaspaces.internal.metadata.EntryType;
import com.gigaspaces.internal.metadata.TypeDesc;
import com.gigaspaces.internal.server.space.tiered_storage.TieredStorageTableConfig;
import com.j_spaces.core.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TypeUpdater {
    Connection connection;
    String typeName;
    TieredStorageTableConfig newTableConfig;
    private Logger logger = LoggerFactory.getLogger(TypeUpdater.class);

    public TypeUpdater(Connection connection, String typeName, TieredStorageTableConfig newTableConfig) {
        this.connection = connection;
        this.typeName = typeName;
        this.newTableConfig = newTableConfig;
    }

    private TypeDesc deserializeType(byte[] bytes) throws IOException, ClassNotFoundException {
        try (GSByteArrayInputStream bis = new GSByteArrayInputStream(bytes); GSObjectInputStream in = new GSObjectInputStream(bis)) {
            final TypeDesc typeDesc = new TypeDesc();
            typeDesc.readExternal(in);
            return typeDesc;
        }
    }

    private byte[] serializeType(TypeDesc typeDesc) throws IOException {
        try (GSByteArrayOutputStream bos = new GSByteArrayOutputStream(); GSObjectOutputStream out = new GSObjectOutputStream(bos)) {
            typeDesc.writeExternal(out);
            return bos.toByteArray();
        }
    }

    TypeDesc createNewTypeDesc() throws Exception {
        String sqlQuery = "SELECT typeDesc FROM '" + Constants.TieredStorage.TIERED_STORAGE_TYPES_TABLE + "' where name='" + typeName + "'";
        PreparedStatement statement = connection.prepareStatement(sqlQuery);
        ResultSet resultSet = statement.executeQuery();
        if (resultSet != null && resultSet.next()) {
            final byte[] typeDescs = resultSet.getBytes("typeDesc");
            TypeDesc typeDesc = deserializeType(typeDescs);
            String seqPropertyName = null;
            if (typeDesc.getSequenceNumberFixedPropertyID() != -1)
                seqPropertyName = typeDesc.getFixedProperty(typeDesc.getSequenceNumberFixedPropertyID()).getName();
            Class<? extends ClassBinaryStorageAdapter> storageClass = null;
            if (typeDesc.getClassBinaryStorageAdapter() != null)
                storageClass = typeDesc.getClassBinaryStorageAdapter().getClass();

            TypeDesc newTypDesc = new TypeDesc(typeDesc.getTypeName(), typeDesc.getCodeBase(), typeDesc.getSuperClassesNames(), typeDesc.getProperties(),
                    typeDesc.supportsDynamicProperties(), typeDesc.getIndexes(),
                    typeDesc.getIdPropertiesNames(), typeDesc.isAutoGenerateId(), typeDesc.getDefaultPropertyName(), typeDesc.getRoutingPropertyName(),
                    typeDesc.getFifoGroupingPropertyPath(), typeDesc.getFifoGroupingIndexesPaths(),
                    typeDesc.isSystemType(), typeDesc.getFifoSupport(), typeDesc.isReplicable(), typeDesc.supportsOptimisticLocking(),
                    typeDesc.getStorageType(), typeDesc.getObjectType(), typeDesc.getObjectClass(),
                    typeDesc.getExternalEntryWrapperClass(), typeDesc.getDocumentWrapperClass(),
                    typeDesc.getDotnetDocumentWrapperTypeName(), typeDesc.getDotnetDynamicPropertiesStorageType(), typeDesc.isBlobstoreEnabled(),
                    seqPropertyName, typeDesc.getQueryExtensions(), storageClass, typeDesc.isBroadcast(),
                    newTableConfig);
            return newTypDesc;
        } else {
            return null;
        }
    }

    public boolean update() {
        try {
            TypeDesc newType = createNewTypeDesc();
            if (newType == null) {
                return false;
            }
            byte[] serializedType = serializeType(newType.cloneWithoutObjectClass(EntryType.DOCUMENT_JAVA));
            String sqlQuery = "update '" + Constants.TieredStorage.TIERED_STORAGE_TYPES_TABLE + "' set typeDesc=?" + " where name='" + typeName + "'";
            PreparedStatement statement = connection.prepareStatement(sqlQuery);
            statement.setBytes(1, serializedType);
            statement.execute();
            statement.close();
            return true;
        } catch (Throwable t) {
            System.out.println("update failed for type " + typeName + " failed:" + t);
            t.printStackTrace();
            return false;
        }
    }


}
