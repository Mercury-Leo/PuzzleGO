package com.mercuryf_ofiro.puzzlego;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class PrizeAdapter extends ArrayAdapter<SinglePrize> {

    private Context mContext;
    private List<SinglePrize> SinglePrizeList;

    PrizeAdapter(Context context, ArrayList<SinglePrize> list){
        super(context, 0, list);
        mContext = context;
        SinglePrizeList = list;
    }

    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null)
            listItem = LayoutInflater.from(mContext).inflate(R.layout.prize_item, parent, false);

        SinglePrize current_singlePrize = SinglePrizeList.get(position);

        TextView moves = listItem.findViewById(R.id.list_moves);
        moves.setText("Number of moves: " + current_singlePrize.getPMoves());

        TextView time = listItem.findViewById(R.id.list_time);
        time.setText("Time: " + current_singlePrize.getPTime());

        TextView name = listItem.findViewById(R.id.list_name);
        name.setText(current_singlePrize.getPName());

        return listItem;
    }
}
