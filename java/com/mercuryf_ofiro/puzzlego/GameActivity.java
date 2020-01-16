package com.mercuryf_ofiro.puzzlego;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import com.google.android.gms.common.api.ApiException;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.android.libraries.places.api.Places.createClient;

public class GameActivity extends AppCompatActivity {

    GameBoard gameBoard;
    TableLayout Game_table;
    Button Start_Game;
    TextView Number_moves;
    TextView Timer_clock;
    TextView Player_Points;
    ImageView Image_hint;
    private TextView[] tiles;
    private int stopgame;
    int count = 0, pause_time = 0;
    Handler hand = new Handler();
    long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L ;
    int pause_flag = 0;
    private String PHOTO_PREF = "Photo_pref";
    private String POINT_PREF = "Point_pref";
    private String POINT_EDIT_REF = "Points";
    private String photoref_loc = "photoReference=";
    private int Current_points;
    private PlacesClient placesClient;
    SharedPreferences point_pref;
    AtomicReference<Bitmap> Photo_Bitmap;
    Bitmap[][] PuzzleList;
    BitmapDrawable[] Picture_Pieces;
    Boolean ready_flag = false;
    private long Hint_time = 6000;
    private int Hint_cost = 5;
    private int Prize = 10;
    SharedPreferences.Editor editor;
    private String puzzle_name = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("PuzzleGO");
        setSupportActionBar(toolbar);

        PuzzleList = new Bitmap[4][4];
        Picture_Pieces = new BitmapDrawable[17];
        Game_table = findViewById(R.id.table_Game);
        Start_Game = findViewById(R.id.btn_new_game);
        Number_moves = findViewById(R.id.text_moves);
        Timer_clock = findViewById(R.id.text_timer);
        Image_hint = findViewById(R.id.Image_hint);
        Player_Points = findViewById(R.id.points);
        Start_Game.setEnabled(false);
        Photo_Bitmap = new AtomicReference<>();
        point_pref = getSharedPreferences(POINT_PREF, MODE_PRIVATE);
        editor = point_pref.edit();
        Current_points = point_pref.getInt(POINT_EDIT_REF, 0);
        Player_Points.setText("Points: " + String.valueOf(Current_points));

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key), Locale.US);
        }
        placesClient = createClient(this);
        //Gets the photo.
        get_photo();
        hand.postDelayed(Start,1000);

    }

    //Inits the game and enables the start game button
    private void Init_start(){
        hand.removeCallbacksAndMessages(Start);
        tiles = new TextView[16];
        stopgame = 0;

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


    //Handles the timer for the puzzle.
    public Runnable runnable = new Runnable() {

        public void run() {

            MillisecondTime = SystemClock.uptimeMillis() - StartTime + pause_time*1000;

            UpdateTime = TimeBuff + MillisecondTime;

            count = (int) (UpdateTime / 1000);

            Timer_clock.setText("Time: " + String.format("%04d", count));

            hand.postDelayed(this, 0);

        }
    };

    //Waits for a photo to be found via photometadata.
    public Runnable Start = new Runnable() {
        @Override
        public void run() {
            if(ready_flag){
                Start_Game.setEnabled(true);
                ready_flag = false;
                Init_start();
            }
            if(!ready_flag)
                hand.postDelayed(this, 0);
        }
    };

    //Sets up the hint for the puzzle.
    Runnable hint = new Runnable() {
        @Override
        public void run() {//Runnable for the timer on hint.
            Image_hint.setEnabled(true);
            Image_hint.setImageBitmap(null);
            Image_hint.setBackgroundResource(R.drawable.hint_background);
        }
    };

    //Changes puzzle pieces from [][] to [].
    private void regroup_puzzle(){
        int holder = 1;
        for(int i = 0; i < 4; i++){
            for(int j = 0; j < 4; j++){
               Picture_Pieces[holder] = new BitmapDrawable(getResources(), PuzzleList[j][i]);
               holder++;
            }
        }
    }


    private void Start_Game(){
        regroup_puzzle();
        Image_hint.setOnClickListener(view ->{

            //Set points!
            if(Current_points>=5){
                new AlertDialog.Builder(this).setTitle("Hint").setMessage("Will you spend " + Hint_cost + " points to unlock the hint?\n You currently have " + Current_points +" Points").setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Image_hint.setEnabled(false);
                        Image_hint.setBackgroundResource(R.color.quantum_white_100);
                        Image_hint.setImageBitmap(Photo_Bitmap.get());
                        Current_points -= Hint_cost;
                        editor.putInt(POINT_EDIT_REF, Current_points);
                        editor.apply();
                        Player_Points.setText("Points: " + Current_points);
                        hand.postDelayed(hint, Hint_time);
                    }
                }).setNegativeButton(android.R.string.no, null).setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
            else{
                Toast.makeText(this,"Not enough points to unlock a hint. Solve more puzzles!", Toast.LENGTH_SHORT).show();
            }
        });


        int curr_piece = -1;
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
            curr_piece = gameBoard.getBoard()[i];
            tiles[i].setText(curr_piece + "");//Sets the textviews text to be as the game board.
            if(i != 16){
                //tiles[i].setBackground(puzzle_piece);
                tiles[i].setBackground(Picture_Pieces[curr_piece]);
                //tiles[i].setBackgroundResource(R.drawable.gamepiece);
                tiles[i].setTextColor(Color.TRANSPARENT);
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
            holder.setBackground(Picture_Pieces[Integer.parseInt(holder.getText().toString())]);
            //holder.setBackgroundResource(R.drawable.gamepiece);
            holder.setTextColor(Color.WHITE);
        }

        for(int i = 0; i < tiles.length; i++){
            gameBoard.Board[i] = Integer.parseInt(tiles[i].getText().toString());
        }

        if(stopgame == 1){
            stopgame();
        }
    }

    //Gets a photo via URL.
    private Bitmap[][] get_photo() {
        try {
            int xSize = 4, ySize = 4;
            //Gets the photometadata
            SharedPreferences prefs = getSharedPreferences(PHOTO_PREF, MODE_PRIVATE);
            String meta = prefs.getString("meta", "empty");
            puzzle_name = prefs.getString("name", "no name");
            if (!meta.equals("empty")) {
                int loc = meta.indexOf(photoref_loc);
                //Builds a valid photoref for google places to return an image.
                String holder = meta.substring(loc + photoref_loc.length());
                PhotoMetadata temp = PhotoMetadata.builder(holder.substring(0, holder.length() - 1)).build();

                //Requests a photo with photometadata.
                FetchPhotoRequest photoRequest = FetchPhotoRequest.builder(temp)
                        .setMaxWidth(800) // Optional.
                        .setMaxHeight(800) // Optional.
                        .build();
                placesClient.fetchPhoto(photoRequest).addOnSuccessListener((fetchPhotoResponse) -> {
                    Photo_Bitmap.set(fetchPhotoResponse.getBitmap());
                    int width, height;

                    //Cuts the photo to 4x4 grid
                    width = Photo_Bitmap.get().getWidth() / xSize;
                    height = Photo_Bitmap.get().getHeight() / ySize;
                    for (int i = 0; i < xSize; i++) {
                        for (int j = 0; j < ySize; j++) {
                            PuzzleList[i][j] = Bitmap.createBitmap(Photo_Bitmap.get(), i * width, j * height, width, height);
                            Log.d("debug", "Status is: " + PuzzleList[i][j]);
                        }
                    }
                    ready_flag = true;
                }).addOnFailureListener((exception) -> {
                    if (exception instanceof ApiException) {
                        ApiException apiException = (ApiException) exception;
                        int statusCode = apiException.getStatusCode();
                        // Handle error with given status code.
                        Log.e("error", "Photo not found: " + exception.getMessage());
                        startActivity(new Intent(this, MapsActivity.class));
                    }
                });
            }
        } catch (Exception e) {
            Log.d("Exception", "Error in splitting photo: " + e);
        }
        return PuzzleList;
    }


    //Sets click event for the puzzle
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
                                Log.d("debug", "num is: " + num);
                                tiles[j].setBackground(Picture_Pieces[num]);
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
                        Toast.makeText(GameActivity.this, "Puzzle Solved, you have won " + Prize + " Points!", Toast.LENGTH_LONG).show();
                        Puzzle_Solved();
                    }
                    break;
                }

            }
        }
    }

    //Once the puzzle is solved
    private void Puzzle_Solved() {
        Current_points += Prize;
        //Add points to user
        editor.putInt(POINT_EDIT_REF, Current_points);
        editor.apply();
        //Saves the puzzle data into sql.
        DBHelp db = new DBHelp(this);
        db.addPrize(puzzle_name, String.valueOf(gameBoard.getMoves()), String.format("%04d", count));
        Intent Map = new Intent(this, MapsActivity.class);
        startActivity(Map);
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