package com.mercuryf_ofiro.puzzlego;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.google.android.libraries.places.api.model.PhotoMetadata;

public class SinglePrize {

    private String PMoves;
    private String PTime;
    private Bitmap PhotoMeta;

    SinglePrize(String Moves, String Time,Bitmap  Meta){
        this.PhotoMeta = Meta;
        this.PMoves = Moves;
        this.PTime = Time;
    }

    String getPMoves(){
        return PMoves;
    }

    String getPTime(){
        return PTime;
    }

    Bitmap getPhotoMeta(){
        return PhotoMeta;
    }
}
