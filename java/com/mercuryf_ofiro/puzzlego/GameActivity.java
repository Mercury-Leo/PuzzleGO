package com.mercuryf_ofiro.puzzlego;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;

public class GameActivity extends AppCompatActivity {


    GameBoard gameBoard;
    TableLayout Game_table;
    Button Start_Game;
    TextView Number_moves;
    TextView Timer_clock;
    private TextView[] tiles;
    private int stopgame;
    int count = 0, pause_time = 0;
    Handler hand = new Handler();
    long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L ;
    int pause_flag = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Puzzle 15");
        setSupportActionBar(toolbar);

        Intent Photo_get = getIntent();
        Bitmap photo = (Bitmap) Photo_get.getParcelableExtra("BitmapImage");
        Log.d("debug", "test " + photo + "test");
        tiles = new TextView[16];
        stopgame = 0;
        Game_table = findViewById(R.id.table_Game);
        Start_Game = findViewById(R.id.btn_new_game);
        Number_moves = findViewById(R.id.text_moves);
        Timer_clock = findViewById(R.id.text_timer);

        Start_Game.setOnClickListener(view -> {
            StartTime = SystemClock.uptimeMillis();
            hand.postDelayed(runnable, 0);
            Start_Game();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopgame = 0;
        if(count > 0)
            stopgame();
        hand.removeCallbacks(runnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pause_flag = 1;
        stopgame = 0;
        if(count >0)
            stopgame();
        pause_time = count;
        hand.removeCallbacks(runnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(pause_flag == 1){
            if(gameBoard != null && !gameBoard.Game_End()) {
                stopgame = 1;
                if(count > 0)
                    stopgame();
                hand.postDelayed(runnable, 0);
                StartTime = SystemClock.uptimeMillis();
            }

            pause_flag = 0;
        }
    }


    public Runnable runnable = new Runnable() {

        public void run() {

            MillisecondTime = SystemClock.uptimeMillis() - StartTime + pause_time*1000;

            UpdateTime = TimeBuff + MillisecondTime;

            count = (int) (UpdateTime / 1000);

            Timer_clock.setText("Time: " + String.format("%04d", count));

            hand.postDelayed(this, 0);

        }
    };


    private void Start_Game(){
        stopgame = 1;
        count = 0;
        gameBoard = new GameBoard();
        gameBoard.shuffle_Board();
        gameBoard.setMoves();
        Number_moves.setText("Moves: " + String.format("%04d", gameBoard.getMoves()));
        int check = -1;
        TextView text_holder = (TextView)findViewById(R.id.Tile0);
        String tile_start = "Tile";
        String temp_start = "";
        for (int i = 0; i < tiles.length; i++){
            temp_start = tile_start + String.valueOf(i);
            tiles[i] = findViewById(getResources().getIdentifier(temp_start, "id", getPackageName()));
            tiles[i].setOnClickListener(new Listener());//Listener for every textview
            tiles[i].setText(gameBoard.getBoard()[i] + "");//Sets the textviews text to be as the game board.
            if(i != 16){
                tiles[i].setBackgroundResource(R.drawable.gamepiece);
                tiles[i].setTextColor(Color.WHITE);
            }
            else{
                tiles[i].setTextColor(Color.TRANSPARENT);
                tiles[i].setBackgroundColor(Color.WHITE);
            }
        }
        for (int j = 0; j < tiles.length; j++) {
            check = Integer.parseInt(tiles[j].getText().toString());
            if (check == 16) {//searches for the empty slot and gives it the pressed textview number and picture.
                tiles[j].setText("16");
                tiles[j].setTextColor(Color.TRANSPARENT);
                tiles[j].setBackgroundColor(Color.WHITE);
                break;
            }
        }
        TextView holder = findViewById(R.id.Tile15);
        if(Integer.parseInt(holder.getText().toString()) != 16){
            holder.setBackgroundResource(R.drawable.gamepiece);
            holder.setTextColor(Color.WHITE);
        }

        for(int i = 0; i < tiles.length; i++){
            gameBoard.Board[i] = Integer.parseInt(tiles[i].getText().toString());
        }

        if(stopgame == 1){
            stopgame();
        }
    }




    private class Listener implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            int check = -1, num = -1; //check: finds the empty slot index, num the index of the moving slot.
            for (int i = 0; i < tiles.length; i++) {
                if (v.getId() == tiles[i].getId()) {//Waits til the textview pressed is the right text view.
                    num = Integer.parseInt(tiles[i].getText().toString());//Gets the text from the text view pressed.
                    if (gameBoard.shift_piece(num)) {//shifts the game board

                        for (int j = 0; j < tiles.length; j++) {
                            check = Integer.parseInt(tiles[j].getText().toString());
                            if (check == 16) {//searches for the empty slot and gives it the pressed textview number and picture.
                                tiles[j].setBackgroundResource(R.drawable.gamepiece);
                                tiles[j].setTextColor(Color.WHITE);
                                tiles[j].setText(num + "");
                                break;
                            }
                        }
                        tiles[i].setText("16");
                        tiles[i].setTextColor(Color.TRANSPARENT);
                        tiles[i].setBackgroundColor(Color.WHITE);
                    }
                    Number_moves.setText("Moves: " + String.format("%04d", gameBoard.getMoves()));

                    if (gameBoard.Game_End()){
                        hand.removeCallbacks(runnable);
                        stopgame = 0;
                        stopgame();//When the game end: go to the wining layout.
                        Toast.makeText(GameActivity.this, "Game Over - Puzzle Solved", Toast.LENGTH_LONG).show();
                        Puzzle_Solved();
                    }
                    break;
                }

            }
        }
    }

    private void Puzzle_Solved() {

    }

    private int stopgame(){
        if(stopgame == 0) {
            for (int i = 0; i < tiles.length; i++) {
                tiles[i].setEnabled(false);
            }

            stopgame++;
        }
        else if( stopgame == 1){
            for (int i = 0; i < tiles.length; i++) {
                tiles[i].setEnabled(true);
            }
        }
        return stopgame;
    }



}