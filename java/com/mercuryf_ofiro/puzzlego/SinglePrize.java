package com.mercuryf_ofiro.puzzlego;

import android.graphics.Bitmap;

public class SinglePrize {

    private String PMoves;
    private String PTime;
    private String PName;

    SinglePrize(String Moves, String Time, String Name){
        this.PMoves = Moves;
        this.PTime = Time;
        this.PName = Name;
    }

    String getPMoves(){
        return PMoves;
    }

    String getPTime(){
        return PTime;
    }

    String getPName(){
        return PName;
    }


}
