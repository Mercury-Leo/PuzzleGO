package com.mercuryf_ofiro.puzzlego;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import static android.content.Context.MODE_PRIVATE;

public class DBHelp {
    private int id ;
    private SQLiteDatabase HallDB = null;
    private String sql_name = "prizes";
    private String table_name = "MyPrizes";
    Context context ;
    DBHelp(Context context){
        this.context =context;
        createDB();
    }

    private void createDB()
    {
        try
        {
            // Opens a current database or creates it
            // Pass the database name, designate that only this app can use it
            // and a DatabaseErrorHandler in the case of database corruption
            HallDB = context.openOrCreateDatabase(table_name, MODE_PRIVATE, null);
            // Execute an SQL statement that isn't select
            String sql = "CREATE TABLE IF NOT EXISTS " + sql_name + " (id integer primary key, moves VARCHAR, time VARCHAR, name VARCHAR);";
            HallDB.execSQL(sql);
        }
        catch(Exception e){
            Log.d("debug", "Error Creating Database");
        }
    }

    PrizeAdapter addPrize(String name, String moves, String time) {
        String sql = "INSERT INTO " + sql_name + " (moves, time, name) VALUES ('" + moves + "', '" + time + "', '" + name + "');";
        HallDB.execSQL(sql);
        return showPrizes();
    }

    PrizeAdapter showPrizes() {

        ArrayList<SinglePrize> Prize_list = new ArrayList<>();
        // A Cursor provides read and write access to database results
        String sql = "SELECT * FROM " + sql_name;
        Cursor cursor = HallDB.rawQuery(sql, null);

        return searchToSQL(cursor,Prize_list);
    }


    private PrizeAdapter searchToSQL(Cursor cursor, ArrayList<SinglePrize> PrizeList){
        PrizeAdapter prizeAdapter ;
        // Get the index for the column name provided

        int nameColumn = cursor.getColumnIndex("name");
        int movesColumn = cursor.getColumnIndex("moves");
        int timeColumn = cursor.getColumnIndex("time");

        // Move to the first row of results
        cursor.moveToFirst();

        // Verify that we have results
        if ( (cursor.getCount() > 0)) {

            do {
                // Get the results and store them in a String

                String name = cursor.getString(nameColumn);
                String moves = cursor.getString(movesColumn);
                String time = cursor.getString(timeColumn);
                PrizeList.add( new SinglePrize(moves, time, name));


                // Keep getting results as long as they exist
            } while (cursor.moveToNext());

            prizeAdapter= new PrizeAdapter(context,PrizeList);
            return prizeAdapter;

        } else {

            Toast.makeText(context, "No Results to Show", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    public void close(){
        HallDB.close();
    }

    public void clear(){
        HallDB.execSQL("DROP TABLE IF EXISTS " + sql_name);
    }
}