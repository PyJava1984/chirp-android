package com.arashpayan.chirpdemo;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by Arash Payan (https://arashpayan.com) on 6/5/16.
 */
public class ServiceBindingHolder extends RecyclerView.ViewHolder {

    private ViewDataBinding binding;

    public ServiceBindingHolder(View itemView) {
        super(itemView);

        binding = DataBindingUtil.bind(itemView);
    }

    public ViewDataBinding getBinding() {
        return binding;
    }

}
