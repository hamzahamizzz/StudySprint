package com.example.studysprint.scratch;

import com.example.studysprint.utils.MyDataBase;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

public class CheckDbSchema {
    public static void main(String[] args) {
        try {
            Connection cnx = MyDataBase.getInstance().getCnx();
            DatabaseMetaData metaData = cnx.getMetaData();
            
            System.out.println("Checking columns in table 'user'...");
            ResultSet rs = metaData.getColumns(null, null, "user", null);
            
            boolean hasToken = false;
            boolean hasExpiry = false;
            
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                if ("reset_token".equalsIgnoreCase(columnName)) hasToken = true;
                if ("reset_token_expires_at".equalsIgnoreCase(columnName)) hasExpiry = true;
                System.out.println("- " + columnName);
            }
            
            System.out.println("\nMissing reset_token: " + !hasToken);
            System.out.println("Missing reset_token_expires_at: " + !hasExpiry);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
