package com.datasharing;

import android.net.wifi.ScanResult;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CircularItemAdapter extends RecyclerView.Adapter<CircularItemAdapter.ItemHolder> {

    private CallBackListener callBackListener;
    private List<ScanResult> mItems;       // custom data, here we simply use string

    CircularItemAdapter(CallBackListener callBackListener, List<ScanResult> items) {
        this.callBackListener = callBackListener;
        this.mItems = items;
    }

    private ScanResult getScanItemAt(int i) {
        return mItems.get(i);
    }

    @NonNull
    @Override
    public ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.circular_item, parent, false);
        return new CircularItemAdapter.ItemHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemHolder holder, int position) {
        ScanResult scanItemAt = getScanItemAt(position);
        holder.btItem.setText(String.format("%s", scanItemAt.SSID.toUpperCase().charAt(0)));
        holder.itemName.setText(scanItemAt.SSID);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    /**
     * ItemHolder
     */
    public class ItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView btItem;
        final TextView itemName;
        final LinearLayout mainLay;

        ItemHolder(final View view) {
            super(view);
            btItem = view.findViewById(R.id.bt_item);
            itemName = view.findViewById(R.id.item_name);
            mainLay = view.findViewById(R.id.main_lay);
            mainLay.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            callBackListener.onItemClick(getScanItemAt(getAdapterPosition()).SSID);
        }
    }


}