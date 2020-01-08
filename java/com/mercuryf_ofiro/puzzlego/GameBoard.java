package com.mercuryf_ofiro.puzzlego;

import android.util.Log;

public class GameBoard {

    private final int left = -1;
    private final int right = 1;
    private final int top = -4;
    private final int bottom = 4;
    public int[] Board = new int[16];
    private int Empty_slot = 16;
    public static int number_of_moves = 0;

    public int[] getBoard(){
        return Board;
    }

    public  int getMoves(){ return number_of_moves;}

    public void setMoves(){ number_of_moves = 0;}

    public void shuffle_Board(){
        for(int j = 0; j < Board.length; j++){
            Board[j] = j+1;

        }
        int random_num = 0;
        for(int i = 0; i < 800; i++){
            random_num  = (int)Math.ceil(Math.random()*Empty_slot);//Random number from 1 to 16.
            shift_piece(random_num-1);

        }
    }

    public boolean shift_piece(int piece){
        int piece_index = 0;
        if(piece>0 && piece < 17){
            for (int i = 0; i < Board.length; i++) {
                if(Board[i] == piece){
                    piece_index = i;
                    break;
                }
            }
            if (piece_index + right < 16 && Board[piece_index + 1] == Empty_slot && piece_index!=3 && piece_index!=7 && piece_index!=11 && piece_index != 15) {
                Board[piece_index + 1] = piece;
                Board[piece_index] = Empty_slot;
                number_of_moves++;
                return true;
            }
            if (piece_index + left >= 0 && Board[piece_index - 1] == Empty_slot && piece_index!=4 && piece_index!=8 && piece_index!= 12 && piece_index!= 0) {
                Board[piece_index - 1] = piece;
                Board[piece_index] = Empty_slot;
                number_of_moves++;
                return true;
            }
            if (piece_index + bottom < 16 && Board[piece_index + 4] == Empty_slot) {
                Board[piece_index + 4] = piece;
                Board[piece_index] = Empty_slot;
                number_of_moves++;
                return true;
            }
            if (piece_index + top >= 0 && Board[piece_index - 4] == Empty_slot) {
                Board[piece_index - 4] = piece;
                Board[piece_index] = Empty_slot;
                number_of_moves++;
                return true;
            }

        }
        else{
            Log.d("DeBug", "Piece is higher than 16 or lower than 0.");
            return false;
        }
        return false;
    }

    public boolean Game_End(){
        int counter = 0;
        for(int i = 0, j = i+1; i< Board.length; i++, j++){
            if(j != Empty_slot){
                if(Board[i] < Board[j] && i < Empty_slot){
                    counter++;
                }
            }
            else{
                if( counter == 15)
                    counter++;
            }

        }
        if(counter == Empty_slot)
            return true;
        return false;
    }

}
