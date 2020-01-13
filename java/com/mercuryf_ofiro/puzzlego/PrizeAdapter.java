package com.mercuryf_ofiro.puzzlego;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class PrizeAdapter {

    private Context mContext;
    private List<SinglePrize> SinglePrizeList;

    PrizeAdapter(Context context, ArrayList<SinglePrize> list){
        super();
        mContext = context;
        SinglePrizeList = list;
    }

    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null)
            listItem = LayoutInflater.from(mContext).inflate(R.layout.prize_item, parent, false);

        SinglePrize current_singlePrize = SinglePrizeList.get(position);
        ImageView PuzzleView = listItem.findViewById(R.id.list_PuzzlePhoto);
        PuzzleView.setImageBitmap(current_singlePrize.getPhotoMeta());

        TextView moves = listItem.findViewById(R.id.list_Moves);
        moves.setText(current_singlePrize.getPMoves());

        TextView time = listItem.findViewWithTag(R.id.list_Time);
        time.setText(current_singlePrize.getPTime());
        return listItem;
    }
}
