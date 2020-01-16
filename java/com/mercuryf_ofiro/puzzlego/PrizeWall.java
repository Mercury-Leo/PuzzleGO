package com.mercuryf_ofiro.puzzlego;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class PrizeWall extends AppCompatActivity {

    private ListView listView;
    private Button listClear;
    public DBHelp db;
    String PRIZE_PREF = "prize_pref";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prize_wall);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listView = findViewById(R.id.list_view);
        listClear = findViewById(R.id.list_clear);
        db = new DBHelp(this);

        PrizeAdapter mAdapter = db.showPrizes();

        listView.setAdapter(mAdapter);

        listClear.setOnClickListener(view -> {
                db.clear();
                startActivity(new Intent(getApplicationContext(), MapsActivity.class));
        });

        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            TextView name = view.findViewById(R.id.list_name);
            TextView time = view.findViewById(R.id.list_time);
            TextView moves = view.findViewById(R.id.list_moves);
            SharedPreferences.Editor editor = getSharedPreferences(PRIZE_PREF, MODE_PRIVATE).edit();
            editor.putString("name", name.getText().toString());
            editor.putString("time", time.getText().toString());
            editor.putString("moves", moves.getText().toString());
            editor.apply();
            startActivity(new Intent(this, ContactsSMS.class));
        });
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();

    }
}
