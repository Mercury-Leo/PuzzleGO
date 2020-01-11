package com.mercuryf_ofiro.puzzlego;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

import static android.content.Context.MODE_PRIVATE;

public class DBHelp {
    private int id ;
    private SQLiteDatabase HallDB = null;
    Context context ;
    DBHelp(Context context){

        this.context =context;
        createDB();
        //showContacts();
    }

    private void createDB()
    {
        try
        {
            // Opens a current database or creates it
            // Pass the database name, designate that only this app can use it
            // and a DatabaseErrorHandler in the case of database corruption
            HallDB = context.openOrCreateDatabase("MyContacts", MODE_PRIVATE, null);

            // Execute an SQL statement that isn't select
            String sql = "CREATE TABLE IF NOT EXISTS contacts (id integer primary key, name VARCHAR, phone VARCHAR);";
            HallDB.execSQL(sql);

        }

        catch(Exception e){
            Log.d("debug", "Error Creating Database");
        }


    }
//    ContactAdapter addContact(String name, String phone) {
//
//        if( checkupdate(name)){
//            ContentValues values = new ContentValues();
//            values.put("phone",phone);
//            String strSQL = "UPDATE contacts SET phone = '"+phone+"' WHERE id = '"+ id+"';";
//            contactsDB.execSQL(strSQL);
//        }
//        else {
//            String sql = "INSERT INTO contacts (name, phone) VALUES ('" + name + "', '" + phone + "');";
//            contactsDB.execSQL(sql);
//        }
//        return   showContacts();
//    }
//    ContactAdapter showContacts() {
//
//        ArrayList<SingleContact> contactList = new ArrayList<>();
//        // A Cursor provides read and write access to database results
//        String sql = "SELECT * FROM contacts";
//        Cursor cursor = contactsDB.rawQuery(sql, null);
//
//
//        return searchToSQL(cursor,contactList);
//    }
//
//    public ContactAdapter search(boolean byName,String research){
//
//        ArrayList<SingleContact> contactList = new ArrayList<>();
//        String sql;
//        if (byName)
//            sql = "SELECT * FROM contacts WHERE name LIKE '%"+research+"%';";
//        else
//            sql = "SELECT * FROM contacts WHERE phone LIKE '%"+research+"%';";
//        Cursor cursor = contactsDB.rawQuery(sql, null);
//        return searchToSQL(cursor,contactList);
//    }
//    public ContactAdapter search(String name ,String phone){
//
//        ArrayList<SingleContact> contactList = new ArrayList<>();
//        String sql;
//        sql = "SELECT * FROM contacts WHERE name LIKE '%"+name+"%'AND phone LIKE '%"+phone+"%';";
//        Cursor cursor = contactsDB.rawQuery(sql, null);
//        return searchToSQL(cursor,contactList);
//    }

    private boolean checkupdate(String name){
        String sql = "SELECT * FROM contacts WHERE name ='"+name+"'";
        @SuppressLint("Recycle") Cursor cursor = HallDB.rawQuery(sql, null);
        if (cursor.getCount()!=0){
            cursor.moveToFirst();
            id = (Integer.parseInt(cursor.getString(0)));

            return true;
        }
        else
            return false;
    }


//    private ContactAdapter searchToSQL(Cursor cursor, ArrayList<SingleContact> contactList){
//        ContactAdapter contactAdapter ;
//        // Get the index for the column name provided
//
//        int nameColumn = cursor.getColumnIndex("name");
//        int phoneColumn = cursor.getColumnIndex("phone");
//
//
//        // Move to the first row of results
//        cursor.moveToFirst();
//
//        // Verify that we have results
//        if ( (cursor.getCount() > 0)) {
//
//            do {
//                // Get the results and store them in a String
//
//                String name = cursor.getString(nameColumn);
//                String phone = cursor.getString(phoneColumn);
//
//                contactList.add( new SingleContact(name, phone));
//
//
//                // Keep getting results as long as they exist
//            } while (cursor.moveToNext());
//
//            contactAdapter= new ContactAdapter(context,contactList);
//            return contactAdapter;
//
//        } else {
//
//            Toast.makeText(context, "No Results to Show", Toast.LENGTH_SHORT).show();
//            return null;
//        }
//    }

    public void close(){
        HallDB.close();
    }
}